package com.phonestore.util.service.impl;

import com.phonestore.config.JDBCUtil;
import com.phonestore.model.AttributeItem;
import com.phonestore.model.AttributeType;
import com.phonestore.util.service.AttributeService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttributeServiceImpl implements AttributeService {

    @Override
    public List<AttributeItem> findByType(AttributeType type) {
        if (type == null) {
            return new ArrayList<>();
        }

        SqlMeta meta = meta(type);
        String sql = "SELECT " + meta.idCol + ", " + meta.nameCol + ", trangthai FROM " + meta.table + " WHERE COALESCE(trangthai,1)=1 ORDER BY " + meta.idCol + " ASC";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql);
             ResultSet rs = ps == null ? null : ps.executeQuery()) {

            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");

            List<AttributeItem> list = new ArrayList<>();
            while (rs.next()) {
                long id = rs.getLong(meta.idCol);
                String name = readName(rs, meta);
                int st = rs.getInt("trangthai");
                Integer status = rs.wasNull() ? null : st;
                list.add(new AttributeItem(id, type, name, status));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AttributeItem create(AttributeType type, String name) {
        if (type == null) throw new IllegalArgumentException("Loại thuộc tính không hợp lệ");
        String n = name == null ? "" : name.trim();
        if (n.isBlank()) throw new IllegalArgumentException("Tên thuộc tính là bắt buộc");

        SqlMeta meta = meta(type);
        String sql = "INSERT INTO " + meta.table + " (" + meta.nameCol + ", trangthai) VALUES (?, 1)";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");

            bindName(ps, 1, n, meta);
            ps.executeUpdate();

            long id = 0;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) id = keys.getLong(1);
            }
            return new AttributeItem(id, type, n, 1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AttributeItem update(long id, AttributeType type, String name) {
        if (id <= 0) throw new IllegalArgumentException("ID không hợp lệ");
        if (type == null) throw new IllegalArgumentException("Loại thuộc tính không hợp lệ");
        String n = name == null ? "" : name.trim();
        if (n.isBlank()) throw new IllegalArgumentException("Tên thuộc tính là bắt buộc");

        SqlMeta meta = meta(type);
        String sql = "UPDATE " + meta.table + " SET " + meta.nameCol + "=? WHERE " + meta.idCol + "=?";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");

            bindName(ps, 1, n, meta);
            ps.setLong(2, id);
            int updated = ps.executeUpdate();
            if (updated <= 0) throw new IllegalArgumentException("Không tìm thấy thuộc tính");
            return new AttributeItem(id, type, n);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(long id, AttributeType type) {
        if (id <= 0) throw new IllegalArgumentException("ID không hợp lệ");
        if (type == null) throw new IllegalArgumentException("Loại thuộc tính không hợp lệ");

        SqlMeta meta = meta(type);
        String sql = "UPDATE " + meta.table + " SET trangthai=0 WHERE " + meta.idCol + "=?";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) throw new IllegalStateException("Chưa kết nối được DB");
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private SqlMeta meta(AttributeType type) {
        return switch (type) {
            case BRAND -> new SqlMeta("thuonghieu", "mathuonghieu", "tenthuonghieu", false);
            case OPERATING_SYSTEM -> new SqlMeta("hedieuhanh", "mahedieuhanh", "tenhedieuhanh", false);
            case ORIGIN -> new SqlMeta("xuatxu", "maxuatxu", "tenxuatxu", false);
            case RAM -> new SqlMeta("dungluongram", "madlram", "kichthuocram", true);
            case ROM -> new SqlMeta("dungluongrom", "madlrom", "kichthuocrom", true);
            case COLOR -> new SqlMeta("mausac", "mamau", "tenmau", false);
        };
    }

    private String readName(ResultSet rs, SqlMeta meta) throws SQLException {
        if (!meta.numericName) {
            return rs.getString(meta.nameCol);
        }
        int v = rs.getInt(meta.nameCol);
        return rs.wasNull() ? "" : String.valueOf(v);
    }

    private void bindName(PreparedStatement ps, int idx, String name, SqlMeta meta) throws SQLException {
        if (!meta.numericName) {
            ps.setString(idx, name);
            return;
        }
        try {
            ps.setInt(idx, Integer.parseInt(name));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Giá trị phải là số nguyên");
        }
    }

    private static final class SqlMeta {
        final String table;
        final String idCol;
        final String nameCol;
        final boolean numericName;

        private SqlMeta(String table, String idCol, String nameCol, boolean numericName) {
            this.table = table;
            this.idCol = idCol;
            this.nameCol = nameCol;
            this.numericName = numericName;
        }
    }
}
