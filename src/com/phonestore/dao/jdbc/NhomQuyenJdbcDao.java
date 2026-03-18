package com.phonestore.dao.jdbc;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.NhomQuyenDao;
import com.phonestore.model.NhomQuyen;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NhomQuyenJdbcDao implements NhomQuyenDao {

    private static final String SQL = "SELECT manhomquyen, tennhomquyen, trangthai FROM nhomquyen ORDER BY manhomquyen ASC";

    @Override
    public List<NhomQuyen> findAll() {
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(SQL);
             ResultSet rs = ps == null ? null : ps.executeQuery()) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            List<NhomQuyen> out = new ArrayList<>();
            while (rs.next()) {
                NhomQuyen r = new NhomQuyen();
                r.setId(rs.getInt("manhomquyen"));
                r.setName(rs.getString("tennhomquyen"));
                int st = rs.getInt("trangthai");
                r.setStatus(rs.wasNull() ? null : st);
                out.add(r);
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
