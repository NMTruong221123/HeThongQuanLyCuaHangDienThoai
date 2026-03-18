package com.phonestore.dao.jdbc;

import com.phonestore.config.JDBCUtil;
import com.phonestore.model.ExportReceiptLine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Best-effort persistence for export receipt line items.
 *
 * Supports common schemas:
 * - ctphieuxuat(maphieuxuat, maphienbansp, soluong, dongia)
 * - ctphieuxuat(maphieuxuat, masp, soluong, dongia)
 */
public class ExportReceiptLineJdbcDao {

    private static final String TABLE = "ctphieuxuat";
    private static final String COL_RECEIPT_ID = "maphieuxuat";
    private static final String COL_QTY = "soluong";
    private static final String COL_UNIT_PRICE = "dongia";

    public boolean isSupported() {
        return JDBCUtil.hasColumn(TABLE, COL_RECEIPT_ID)
            && JDBCUtil.hasColumn(TABLE, COL_QTY)
            && JDBCUtil.hasColumn(TABLE, COL_UNIT_PRICE)
            && (hasVariantIdColumn() || hasProductIdColumn());
    }

    public List<ExportReceiptLine> findByReceiptId(long receiptId) {
        if (receiptId <= 0) return new ArrayList<>();
        if (!isSupported()) return new ArrayList<>();

        if (hasVariantIdColumn()) {
            return findByReceiptIdVariantSchema(receiptId);
        }
        return findByReceiptIdProductSchema(receiptId);
    }

    public void replaceLines(long receiptId, List<ExportReceiptLine> lines) {
        if (receiptId <= 0) throw new IllegalArgumentException("maphieuxuat không hợp lệ");
        if (!isSupported()) return;

        String itemIdCol = hasVariantIdColumn() ? variantIdColumnName() : productIdColumnName();

        String deleteSql = "DELETE FROM " + TABLE + " WHERE " + COL_RECEIPT_ID + "=?";
        String insertSql = "INSERT INTO " + TABLE + " (" + COL_RECEIPT_ID + ", " + itemIdCol + ", " + COL_QTY + ", " + COL_UNIT_PRICE + ") VALUES (?, ?, ?, ?)";

        try (Connection c = JDBCUtil.getConnectionSilent()) {
            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");

            try (PreparedStatement del = c.prepareStatement(deleteSql)) {
                del.setLong(1, receiptId);
                del.executeUpdate();
            }

            if (lines == null || lines.isEmpty()) return;

            try (PreparedStatement ins = c.prepareStatement(insertSql)) {
                for (ExportReceiptLine it : lines) {
                    bindLine(ins, receiptId, it, hasVariantIdColumn());
                    ins.addBatch();
                }
                ins.executeBatch();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ExportReceiptLine> findByReceiptIdVariantSchema(long receiptId) {
        String sql = "SELECT ct." + COL_RECEIPT_ID + ", ct." + variantIdColumnName() + " AS variant_id, "
            + "pb.masp AS product_id, sp.tensp AS product_name, "
            + "dlrom.kichthuocrom AS rom_name, dlram.kichthuocram AS ram_name, ms.tenmau AS color_name, "
            + "ct." + COL_QTY + " AS qty, ct." + COL_UNIT_PRICE + " AS unit_price "
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
                List<ExportReceiptLine> list = new ArrayList<>();
                while (rs.next()) {
                    ExportReceiptLine l = new ExportReceiptLine();
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

    private List<ExportReceiptLine> findByReceiptIdProductSchema(long receiptId) {
        String prodCol = productIdColumnName();
        String sql = "SELECT ct." + prodCol + " AS product_id, sp.tensp AS product_name, "
            + "ct." + COL_QTY + " AS qty, ct." + COL_UNIT_PRICE + " AS unit_price "
            + "FROM " + TABLE + " ct "
            + "LEFT JOIN sanpham sp ON ct." + prodCol + " = sp.masp "
            + "WHERE ct." + COL_RECEIPT_ID + "=? "
            + "ORDER BY ct." + prodCol + " ASC";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {
            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");
            ps.setLong(1, receiptId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ExportReceiptLine> list = new ArrayList<>();
                while (rs.next()) {
                    ExportReceiptLine l = new ExportReceiptLine();
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

    private void bindLine(PreparedStatement ps, long receiptId, ExportReceiptLine it, boolean variantSchema) throws SQLException {
        ps.setLong(1, receiptId);

        Long itemId = null;
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
