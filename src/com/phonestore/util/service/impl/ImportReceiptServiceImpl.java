package com.phonestore.util.service.impl;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.jdbc.ImeiRegistryJdbcDao;
import com.phonestore.dao.ImportReceiptDao;
import com.phonestore.dao.jdbc.ImportReceiptLineJdbcDao;
import com.phonestore.dao.jdbc.ImportReceiptJdbcDao;
import com.phonestore.model.ImportReceipt;
import com.phonestore.model.ImportReceiptLine;
import com.phonestore.util.service.ImportReceiptService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ImportReceiptServiceImpl implements ImportReceiptService {
    private final ImportReceiptDao jdbcDao = new ImportReceiptJdbcDao();
    private final ImportReceiptLineJdbcDao lineDao = new ImportReceiptLineJdbcDao();
    private final ImeiRegistryJdbcDao imeiDao = new ImeiRegistryJdbcDao();

    private void requireDb() {
        if (!JDBCUtil.canConnect()) {
            throw new IllegalStateException("Chưa kết nối được DB");
        }
    }

    @Override
    public List<ImportReceipt> findAll() {
        requireDb();
        return jdbcDao.findAll();
    }

    @Override
    public List<ImportReceipt> search(String keyword) {
        requireDb();
        return jdbcDao.search(keyword);
    }

    @Override
    public ImportReceipt create(ImportReceipt receipt) {
        validate(receipt);
        if (receipt.getStatus() == null) receipt.setStatus(1);
        requireDb();
        ImportReceipt created = jdbcDao.create(receipt);
        // Persist lines if caller provided them (best-effort)
        if (created != null && created.getId() > 0 && created.getLines() != null) {
            List<ImportReceiptLine> normalized = normalizeLines(created.getLines());
            created.setLines(normalized);
            lineDao.replaceLines(created.getId(), normalized);
            bindLineImeis(created.getId(), normalized);
            // Apply side effects (inventory) at service layer so other screens read correct stock from DB.
            incrementStocksAfterImport(normalized);
        }
        return created;
    }

    @Override
    public ImportReceipt update(ImportReceipt receipt) {
        validate(receipt);
        if (receipt.getStatus() == null) receipt.setStatus(1);
        requireDb();
        ImportReceipt updated = jdbcDao.update(receipt);
        // Replace lines if caller provided them (empty list means clear)
        if (updated != null && updated.getId() > 0 && updated.getLines() != null) {
            List<ImportReceiptLine> normalized = normalizeLines(updated.getLines());
            updated.setLines(normalized);
            lineDao.replaceLines(updated.getId(), normalized);
            bindLineImeis(updated.getId(), normalized);
            // NOTE: We do NOT auto-adjust stock for update() because edit flow may be used for non-stock fields.
            // Stock adjustment belongs to dedicated workflows (create import / cancel import) to avoid double counting.
        }
        return updated;
    }

    /**
     * DB schema for ctphieunhap commonly uses PRIMARY KEY (maphieunhap, maphienbansp).
     * Users may add the same variant multiple times in UI; we must merge those lines before persisting.
     */
    private List<ImportReceiptLine> normalizeLines(List<ImportReceiptLine> lines) {
        if (lines == null || lines.isEmpty()) return lines;

        java.util.LinkedHashMap<String, ImportReceiptLine> map = new java.util.LinkedHashMap<>();
        for (ImportReceiptLine it : lines) {
            if (it == null) continue;
            Long vid = it.getVariantId();
            Long pid = it.getProductId();
            String key;
            if (vid != null && vid > 0) {
                key = "v:" + vid;
            } else if (pid != null && pid > 0) {
                key = "p:" + pid;
            } else {
                // No stable key; keep as-is (but still avoid NPE)
                key = "x:" + System.identityHashCode(it);
            }

            ImportReceiptLine existing = map.get(key);
            if (existing == null) {
                // clone-ish: keep reference but ensure imeis list is mutable copy
                if (it.getImeis() != null) {
                    it.setImeis(new java.util.ArrayList<>(it.getImeis()));
                }
                map.put(key, it);
                continue;
            }

            // Merge qty
            int q1 = existing.getQuantity() == null ? 0 : Math.max(0, existing.getQuantity());
            int q2 = it.getQuantity() == null ? 0 : Math.max(0, it.getQuantity());
            existing.setQuantity(q1 + q2);

            // Keep first non-blank labels/names
            if ((existing.getProductName() == null || existing.getProductName().isBlank()) && it.getProductName() != null) {
                existing.setProductName(it.getProductName());
            }
            if ((existing.getVariantLabel() == null || existing.getVariantLabel().isBlank()) && it.getVariantLabel() != null) {
                existing.setVariantLabel(it.getVariantLabel());
            }

            // Keep unit price (prefer existing; fallback to new if missing)
            if (existing.getUnitPrice() == null || existing.getUnitPrice() <= 0) {
                existing.setUnitPrice(it.getUnitPrice());
            }

            // Merge IMEIs
            if (it.getImeis() != null && !it.getImeis().isEmpty()) {
                java.util.List<String> list = existing.getImeis();
                if (list == null) {
                    list = new java.util.ArrayList<>();
                    existing.setImeis(list);
                }
                list.addAll(it.getImeis());
            }
        }
        return new java.util.ArrayList<>(map.values());
    }

    @Override
    public void delete(long id) {
        requireDb();
        // perform soft-delete by marking status = 0 (Đã hủy)
        jdbcDao.updateStatus(id, 0);
    }

    @Override
    public List<ImportReceiptLine> findLinesByReceiptId(long receiptId) {
        requireDb();
        if (receiptId <= 0) return List.of();
        List<ImportReceiptLine> lines = lineDao.findByReceiptId(receiptId);
        for (ImportReceiptLine line : lines) {
            if (line == null) continue;
            int qty = line.getQuantity() == null ? 0 : Math.max(0, line.getQuantity());
            if (qty <= 0) continue;
            try {
                List<String> imeis = imeiDao.findImeisByImportReceipt(
                    receiptId,
                    line.getVariantId(),
                    line.getProductId(),
                    qty
                );
                if (imeis != null && imeis.size() > qty) {
                    imeis = new java.util.ArrayList<>(imeis.subList(0, qty));
                }
                line.setImeis(imeis);
            } catch (Exception ignored) {
            }
        }
        return lines;
    }

    private void bindLineImeis(long receiptId, List<ImportReceiptLine> lines) {
        if (receiptId <= 0 || lines == null || lines.isEmpty()) return;
        List<String> allImeis = new java.util.ArrayList<>();
        List<Long> productIds = new java.util.ArrayList<>();
        List<Long> variantIds = new java.util.ArrayList<>();
        for (ImportReceiptLine line : lines) {
            if (line == null || line.getImeis() == null || line.getImeis().isEmpty()) continue;
            Long pid = line.getProductId();
            Long vid = line.getVariantId();
            for (String im : line.getImeis()) {
                allImeis.add(im);
                productIds.add(pid);
                variantIds.add(vid);
            }
        }
        if (!allImeis.isEmpty()) imeiDao.bindDetailedImeisToImportReceipt(receiptId, allImeis, productIds, variantIds);
    }

    private void incrementStocksAfterImport(List<ImportReceiptLine> lines) {
        if (lines == null || lines.isEmpty()) return;

        Map<Long, Integer> incByVariant = new HashMap<>();
        Map<Long, Integer> incByProduct = new HashMap<>();

        for (ImportReceiptLine it : lines) {
            if (it == null) continue;
            int qty = it.getQuantity() == null ? 0 : Math.max(0, it.getQuantity());
            if (qty <= 0) continue;

            Long vid = it.getVariantId();
            if (vid != null && vid > 0) {
                incByVariant.merge(vid, qty, Integer::sum);
            }

            Long pid = it.getProductId();
            if (pid != null && pid > 0) {
                incByProduct.merge(pid, qty, Integer::sum);
            }
        }

        if (incByVariant.isEmpty() && incByProduct.isEmpty()) return;

        String incVariantSql = "UPDATE phienbansanpham SET soluongton = COALESCE(soluongton, 0) + ? WHERE maphienbansp=?";
        String incProductSql = "UPDATE sanpham SET soluongton = COALESCE(soluongton, 0) + ? WHERE masp=?";

        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");
            c.setAutoCommit(false);

            if (!incByVariant.isEmpty()) {
                try (PreparedStatement ps = c.prepareStatement(incVariantSql)) {
                    for (Map.Entry<Long, Integer> e : incByVariant.entrySet()) {
                        long variantId = e.getKey() == null ? 0 : e.getKey();
                        int qty = e.getValue() == null ? 0 : Math.max(0, e.getValue());
                        if (variantId <= 0 || qty <= 0) continue;
                        ps.setInt(1, qty);
                        ps.setLong(2, variantId);
                        ps.executeUpdate();
                    }
                }
            }

            if (!incByProduct.isEmpty()) {
                try (PreparedStatement ps = c.prepareStatement(incProductSql)) {
                    for (Map.Entry<Long, Integer> e : incByProduct.entrySet()) {
                        long productId = e.getKey() == null ? 0 : e.getKey();
                        int qty = e.getValue() == null ? 0 : Math.max(0, e.getValue());
                        if (productId <= 0 || qty <= 0) continue;
                        ps.setInt(1, qty);
                        ps.setLong(2, productId);
                        ps.executeUpdate();
                    }
                }
            }

            c.commit();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private void validate(ImportReceipt r) {
        if (r == null) throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        if (r.getSupplierId() == null || r.getSupplierId() <= 0) throw new IllegalArgumentException("Nhà cung cấp (ID) là bắt buộc");
        if (r.getCreatedBy() == null || r.getCreatedBy() <= 0) throw new IllegalArgumentException("Người tạo (ID) là bắt buộc");
        if (r.getTotal() == null || r.getTotal() < 0) throw new IllegalArgumentException("Tổng tiền không hợp lệ");
    }
}
