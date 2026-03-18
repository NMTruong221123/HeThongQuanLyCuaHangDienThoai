package com.phonestore.util.service.impl;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.ExportReceiptDao;
import com.phonestore.dao.jdbc.ImeiRegistryJdbcDao;
import com.phonestore.dao.jdbc.ExportReceiptLineJdbcDao;
import com.phonestore.dao.jdbc.ExportReceiptJdbcDao;
import com.phonestore.model.ExportReceipt;
import com.phonestore.model.ExportReceiptLine;
import com.phonestore.util.service.ExportReceiptService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportReceiptServiceImpl implements ExportReceiptService {
    private final ExportReceiptDao jdbcDao = new ExportReceiptJdbcDao();
    private final ExportReceiptLineJdbcDao lineDao = new ExportReceiptLineJdbcDao();
    private final ImeiRegistryJdbcDao imeiDao = new ImeiRegistryJdbcDao();

    private void requireDb() {
        if (!JDBCUtil.canConnect()) {
            throw new IllegalStateException("Chưa kết nối được DB");
        }
    }

    @Override
    public List<ExportReceipt> findAll() {
        requireDb();
        return jdbcDao.findAll();
    }

    @Override
    public List<ExportReceipt> search(String keyword) {
        requireDb();
        return jdbcDao.search(keyword);
    }

    @Override
    public ExportReceipt create(ExportReceipt receipt) {
        validate(receipt);
        if (receipt.getStatus() == null) receipt.setStatus(1);
        requireDb();
        ExportReceipt created = jdbcDao.create(receipt);
        // Persist lines if caller provided them (best-effort)
        if (created != null && created.getId() > 0 && created.getLines() != null) {
            lineDao.replaceLines(created.getId(), created.getLines());
            // Apply side effects (inventory + imei) at service layer so DB stays consistent.
            applyEffectsAfterCreate(created.getId(), created.getLines());
        }
        return created;
    }

    @Override
    public ExportReceipt update(ExportReceipt receipt) {
        validate(receipt);
        if (receipt.getStatus() == null) receipt.setStatus(1);
        requireDb();
        ExportReceipt updated = jdbcDao.update(receipt);
        // Replace lines if caller provided them (empty list means clear)
        if (updated != null && updated.getId() > 0 && updated.getLines() != null) {
            lineDao.replaceLines(updated.getId(), updated.getLines());
        }
        return updated;
    }

    @Override
    public void delete(long id) {
        requireDb();
        jdbcDao.delete(id);
    }

    @Override
    public void deleteAndCompress(long id) {
        requireDb();
        // perform hard delete and shift ids in DB
        if (jdbcDao instanceof com.phonestore.dao.jdbc.ExportReceiptJdbcDao dao) {
            dao.hardDeleteAndShift(id);
        } else {
            // fallback to soft-delete if underlying DAO doesn't support hard delete
            jdbcDao.delete(id);
        }
    }

    @Override
    public com.phonestore.dao.jdbc.ExportReceiptJdbcDao.PreviewInfo previewDeleteAndCompress(long id) {
        requireDb();
        if (jdbcDao instanceof com.phonestore.dao.jdbc.ExportReceiptJdbcDao dao) {
            return dao.previewHardDeleteAndShift(id);
        }
        // return empty info if not supported
        com.phonestore.dao.jdbc.ExportReceiptJdbcDao.PreviewInfo pi = new com.phonestore.dao.jdbc.ExportReceiptJdbcDao.PreviewInfo();
        return pi;
    }

    @Override
    public void backupAndDeleteAndCompress(long id, String backupDirPath) {
        requireDb();
        try {
            java.io.File dir = new java.io.File(backupDirPath == null ? "backups" : backupDirPath);
            // export main tables involved as SQL dump (CREATE + INSERT)
            String dumpName = "export_receipt_" + System.currentTimeMillis();
            com.phonestore.util.DbBackupUtil.exportTablesAsSql(dir, dumpName, "phieuxuat", "ctphieuxuat", "imei_registry");
        } catch (Exception ex) {
            // best-effort backup; if fails, throw to prevent destructive action
            throw new RuntimeException("Backup failed: " + (ex.getMessage() == null ? ex.toString() : ex.getMessage()));
        }

        // perform delete
        deleteAndCompress(id);
    }

    @Override
    public List<ExportReceiptLine> findLinesByReceiptId(long receiptId) {
        requireDb();
        try {
            List<ExportReceiptLine> lines = lineDao.findByReceiptId(receiptId);
            if (lines == null) return new ArrayList<>();
            for (ExportReceiptLine line : lines) {
                if (line == null) continue;
                int qty = line.getQuantity() == null ? 0 : Math.max(0, line.getQuantity());
                if (qty <= 0) continue;
                try {
                    List<String> imeis = imeiDao.findImeisByExportReceipt(
                        receiptId,
                        line.getVariantId(),
                        line.getProductId(),
                        qty
                    );
                    line.setImeis(imeis);
                } catch (Exception ignored) {
                }
            }
            return lines;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void validate(ExportReceipt r) {
        if (r == null) throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        if (r.getCustomerId() != null && r.getCustomerId() <= 0) throw new IllegalArgumentException("Khách hàng (ID) không hợp lệ");
        if (r.getCreatedBy() != null && r.getCreatedBy() <= 0) throw new IllegalArgumentException("Người tạo (ID) không hợp lệ");
        if (r.getTotal() != null && r.getTotal() < 0) throw new IllegalArgumentException("Tổng tiền không hợp lệ");

        if (r.getLines() != null) {
            for (ExportReceiptLine it : r.getLines()) {
                if (it == null) continue;
                if (it.getQuantity() != null && it.getQuantity() <= 0) {
                    throw new IllegalArgumentException("Số lượng phải > 0");
                }
                if (it.getUnitPrice() != null && it.getUnitPrice() < 0) {
                    throw new IllegalArgumentException("Đơn giá không hợp lệ");
                }
            }
        }
    }

    private void applyEffectsAfterCreate(long receiptId, List<ExportReceiptLine> lines) {
        if (receiptId <= 0 || lines == null || lines.isEmpty()) return;

        // 1) Mark IMEIs sold (best-effort)
        try {
            List<String> imeis = collectImeis(lines);
            if (!imeis.isEmpty()) {
                imeiDao.markSoldImeisForReceipt(imeis, receiptId);
            }
        } catch (Exception ignored) {
        }

        // 2) Decrement stocks in DB (variant + product)
        Map<Long, Integer> soldByVariant = new HashMap<>();
        Map<Long, Integer> soldByProduct = new HashMap<>();

        for (ExportReceiptLine it : lines) {
            if (it == null) continue;
            int qty = it.getQuantity() == null ? 0 : Math.max(0, it.getQuantity());
            if (qty <= 0) continue;

            Long variantId = it.getVariantId();
            if (variantId != null && variantId > 0) {
                soldByVariant.merge(variantId, qty, Integer::sum);
            }

            Long productId = it.getProductId();
            if (productId != null && productId > 0) {
                soldByProduct.merge(productId, qty, Integer::sum);
            }
        }

        if (soldByVariant.isEmpty() && soldByProduct.isEmpty()) return;

        String decVariantSql = "UPDATE phienbansanpham SET soluongton = soluongton - ? WHERE maphienbansp=? AND soluongton >= ?";
        String decProductSql = "UPDATE sanpham SET soluongton = soluongton - ? WHERE masp=? AND soluongton >= ?";

        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");
            c.setAutoCommit(false);

            if (!soldByVariant.isEmpty()) {
                try (PreparedStatement ps = c.prepareStatement(decVariantSql)) {
                    for (Map.Entry<Long, Integer> e : soldByVariant.entrySet()) {
                        long variantId = e.getKey() == null ? 0 : e.getKey();
                        int qty = e.getValue() == null ? 0 : Math.max(0, e.getValue());
                        if (variantId <= 0 || qty <= 0) continue;
                        ps.setInt(1, qty);
                        ps.setLong(2, variantId);
                        ps.setInt(3, qty);
                        int updated = ps.executeUpdate();
                        if (updated <= 0) {
                            throw new IllegalStateException("Không đủ tồn kho cho phiên bản SP: " + variantId);
                        }
                    }
                }
            }

            if (!soldByProduct.isEmpty()) {
                try (PreparedStatement ps = c.prepareStatement(decProductSql)) {
                    for (Map.Entry<Long, Integer> e : soldByProduct.entrySet()) {
                        long productId = e.getKey() == null ? 0 : e.getKey();
                        int qty = e.getValue() == null ? 0 : Math.max(0, e.getValue());
                        if (productId <= 0 || qty <= 0) continue;
                        ps.setInt(1, qty);
                        ps.setLong(2, productId);
                        ps.setInt(3, qty);
                        int updated = ps.executeUpdate();
                        if (updated <= 0) {
                            throw new IllegalStateException("Không đủ tồn kho cho sản phẩm: " + productId);
                        }
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

    private List<String> collectImeis(List<ExportReceiptLine> lines) {
        List<String> out = new ArrayList<>();
        if (lines == null) return out;
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<>();
        for (ExportReceiptLine it : lines) {
            if (it == null || it.getImeis() == null) continue;
            for (String imei : it.getImeis()) {
                if (imei == null) continue;
                String v = imei.trim();
                if (v.isBlank()) continue;
                if (seen.add(v)) out.add(v);
            }
        }
        return out;
    }

}
