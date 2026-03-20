package com.phonestore.dao.jdbc;

import com.phonestore.config.JDBCUtil;
import com.phonestore.model.ImportReceiptLine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Best-effort persistence for import receipt line items.
 *
 * Supports common schemas:
 * - ctphieunhap(maphieunhap, maphienbansp, soluong, dongia/gianhap)
 * - ctphieunhap(maphieunhap, masp/masanpham, soluong, dongia/gianhap)
 */
public class ImportReceiptLineJdbcDao {

    private static final String TABLE = "ctphieunhap";
    private static final String COL_RECEIPT_ID = "maphieunhap";
    private static final String COL_QTY = "soluong";
    private static final String COL_IMPORT_METHOD = "hinhthucnhap";

    public boolean isSupported() {
        return JDBCUtil.hasColumn(TABLE, COL_RECEIPT_ID)
            && JDBCUtil.hasColumn(TABLE, COL_QTY)
            && (hasVariantIdColumn() || hasProductIdColumn())
            && unitPriceColumnName() != null;
    }

    public List<ImportReceiptLine> findByReceiptId(long receiptId) {
        if (receiptId <= 0) return new ArrayList<>();
        if (!isSupported()) return new ArrayList<>();

        if (hasVariantIdColumn()) {
            return findByReceiptIdVariantSchema(receiptId);
        }
        return findByReceiptIdProductSchema(receiptId);
    }

    public void replaceLines(long receiptId, List<ImportReceiptLine> lines) {
        if (receiptId <= 0) throw new IllegalArgumentException("maphieunhap không hợp lệ");
        if (!isSupported()) return;

        String itemIdCol = hasVariantIdColumn() ? variantIdColumnName() : productIdColumnName();
        String unitCol = unitPriceColumnName();
        boolean hasMethodCol = hasImportMethodColumn();

        String deleteSql = "DELETE FROM " + TABLE + " WHERE " + COL_RECEIPT_ID + "=?";
        String insertSql;
        if (hasMethodCol) {
            insertSql = "INSERT INTO " + TABLE + " (" + COL_RECEIPT_ID + ", " + itemIdCol + ", " + COL_QTY + ", " + unitCol + ", " + COL_IMPORT_METHOD + ") VALUES (?, ?, ?, ?, ?)";
        } else {
            insertSql = "INSERT INTO " + TABLE + " (" + COL_RECEIPT_ID + ", " + itemIdCol + ", " + COL_QTY + ", " + unitCol + ") VALUES (?, ?, ?, ?)";
        }

        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");

            try (PreparedStatement del = c.prepareStatement(deleteSql)) {
                del.setLong(1, receiptId);
                del.executeUpdate();
            }

            if (lines == null || lines.isEmpty()) return;

            try (PreparedStatement ins = c.prepareStatement(insertSql)) {
                boolean variantSchema = hasVariantIdColumn();
                for (ImportReceiptLine it : lines) {
                    bindLine(ins, receiptId, it, variantSchema, hasMethodCol);
                    ins.addBatch();
                }
                ins.executeBatch();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ImportReceiptLine> findByReceiptIdVariantSchema(long receiptId) {
        String unitCol = unitPriceColumnName();
        String sql = "SELECT ct." + COL_RECEIPT_ID + ", ct." + variantIdColumnName() + " AS variant_id, "
            + "pb.masp AS product_id, sp.tensp AS product_name, "
            + "dlrom.kichthuocrom AS rom_name, dlram.kichthuocram AS ram_name, ms.tenmau AS color_name, "
            + "ct." + COL_QTY + " AS qty, ct." + unitCol + " AS unit_price "
            + "FROM " + TABLE + " ct "
            + "LEFT JOIN phienbansanpham pb ON ct." + variantIdColumnName() + " = pb.maphienbansp "
            + "LEFT JOIN sanpham sp ON pb.masp = sp.masp "
            + "LEFT JOIN dungluongrom dlrom ON pb.rom = dlrom.madlrom "
            + "LEFT JOIN dungluongram dlram ON pb.ram = dlram.madlram "
            + "LEFT JOIN mausac ms ON pb.mausac = ms.mamau "
            + "WHERE ct." + COL_RECEIPT_ID + "=? "
            + "ORDER BY ct." + variantIdColumnName() + " ASC";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {
            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");
            ps.setLong(1, receiptId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ImportReceiptLine> list = new ArrayList<>();
                while (rs.next()) {
                    ImportReceiptLine l = new ImportReceiptLine();
                    long variantId = rs.getLong("variant_id");
                    l.setVariantId(rs.wasNull() ? null : variantId);

                    long productId = rs.getLong("product_id");
                    l.setProductId(rs.wasNull() ? null : productId);
                    l.setProductName(rs.getString("product_name"));

                    String label = buildVariantLabel(rs.getString("rom_name"), rs.getString("ram_name"), rs.getString("color_name"));
                    l.setVariantLabel(label);

                    int qty = rs.getInt("qty");
                    l.setQuantity(rs.wasNull() ? null : qty);

                    long unit = rs.getLong("unit_price");
                    l.setUnitPrice(rs.wasNull() ? null : unit);

                    list.add(l);
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ImportReceiptLine> findByReceiptIdProductSchema(long receiptId) {
        String prodCol = productIdColumnName();
        String unitCol = unitPriceColumnName();
        String sql = "SELECT ct." + prodCol + " AS product_id, sp.tensp AS product_name, "
            + "ct." + COL_QTY + " AS qty, ct." + unitCol + " AS unit_price "
            + "FROM " + TABLE + " ct "
            + "LEFT JOIN sanpham sp ON ct." + prodCol + " = sp.masp "
            + "WHERE ct." + COL_RECEIPT_ID + "=? "
            + "ORDER BY ct." + prodCol + " ASC";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {
            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");
            ps.setLong(1, receiptId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ImportReceiptLine> list = new ArrayList<>();
                while (rs.next()) {
                    ImportReceiptLine l = new ImportReceiptLine();
                    long productId = rs.getLong("product_id");
                    l.setProductId(rs.wasNull() ? null : productId);
                    l.setProductName(rs.getString("product_name"));

                    int qty = rs.getInt("qty");
                    l.setQuantity(rs.wasNull() ? null : qty);

                    long unit = rs.getLong("unit_price");
                    l.setUnitPrice(rs.wasNull() ? null : unit);

                    list.add(l);
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void bindLine(PreparedStatement ps, long receiptId, ImportReceiptLine it, boolean variantSchema, boolean includeImportMethod) throws SQLException {
        ps.setLong(1, receiptId);

        Long itemId;
        if (variantSchema) {
            itemId = it == null ? null : it.getVariantId();
        } else {
            itemId = it == null ? null : it.getProductId();
        }
        if (itemId == null || itemId <= 0) {
            ps.setNull(2, Types.BIGINT);
        } else {
            ps.setLong(2, itemId);
        }

        Integer qty = it == null ? null : it.getQuantity();
        if (qty == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, qty);

        Long unit = it == null ? null : it.getUnitPrice();
        if (unit == null) ps.setNull(4, Types.BIGINT); else ps.setLong(4, unit);

        if (includeImportMethod) {
            int method = guessImportMethod(it);
            ps.setInt(5, method);
        }
    }

    private boolean hasImportMethodColumn() {
        return JDBCUtil.hasColumn(TABLE, COL_IMPORT_METHOD);
    }

    /**
     * Best-effort mapping for DB column ctphieunhap.hinhthucnhap (tinyint).
     * 0: Nhập theo lô, 1: Nhập từng máy.
     */
    private int guessImportMethod(ImportReceiptLine it) {
        if (it == null) return 0;
        Integer qty = it.getQuantity();
        int q = qty == null ? 0 : qty;
        int imeiCount = (it.getImeis() == null) ? 0 : it.getImeis().size();
        // Per-device mode in UI always has qty=1 and exactly 1 IMEI.
        if (q == 1 && imeiCount == 1) return 1;
        return 0;
    }

    private boolean hasVariantIdColumn() {
        return JDBCUtil.hasColumn(TABLE, "maphienbansp");
    }

    private boolean hasProductIdColumn() {
        return JDBCUtil.hasColumn(TABLE, "masp") || JDBCUtil.hasColumn(TABLE, "masanpham");
    }

    private String variantIdColumnName() {
        return "maphienbansp";
    }

    private String productIdColumnName() {
        if (JDBCUtil.hasColumn(TABLE, "masp")) return "masp";
        return "masanpham";
    }

    private String unitPriceColumnName() {
        if (JDBCUtil.hasColumn(TABLE, "dongia")) return "dongia";
        if (JDBCUtil.hasColumn(TABLE, "gianhap")) return "gianhap";
        if (JDBCUtil.hasColumn(TABLE, "dongianhap")) return "dongianhap";
        return null;
    }

    private String buildVariantLabel(String rom, String ram, String color) {
        StringBuilder sb = new StringBuilder();
        if (rom != null && !rom.isBlank()) sb.append(rom.trim());
        if (ram != null && !ram.isBlank()) {
            if (sb.length() > 0) sb.append("/");
            sb.append(ram.trim());
        }
        if (color != null && !color.isBlank()) {
            if (sb.length() > 0) sb.append("/");
            sb.append(color.trim());
        }
        return sb.toString();
    }
}
