package com.phonestore.dao.jdbc;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.ProductVariantDao;
import com.phonestore.model.ProductVariant;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductVariantJdbcDao implements ProductVariantDao {

    private static final String BASE_SELECT =
        "SELECT pb.maphienbansp, pb.masp, "
            + "pb.rom, dlrom.kichthuocrom AS rom_ten, "
            + "pb.ram, dlram.kichthuocram AS ram_ten, "
            + "pb.mausac, ms.tenmau AS mausac_ten, "
            + "pb.gianhap, pb.giaxuat, pb.soluongton, pb.trangthai "
            + "FROM phienbansanpham pb "
            + "LEFT JOIN dungluongrom dlrom ON pb.rom = dlrom.madlrom "
            + "LEFT JOIN dungluongram dlram ON pb.ram = dlram.madlram "
            + "LEFT JOIN mausac ms ON pb.mausac = ms.mamau ";

    @Override
    public List<ProductVariant> findByProductId(long productId) {
        String sql = BASE_SELECT + "WHERE pb.masp=? ORDER BY pb.maphienbansp ASC";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ProductVariant> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ProductVariant create(ProductVariant v) {
        String sql = "INSERT INTO phienbansanpham (masp, rom, ram, mausac, gianhap, giaxuat, soluongton, trangthai) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            bind(ps, v, false);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) v.setId(keys.getLong(1));
            }
            return v;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ProductVariant update(ProductVariant v) {
        String sql = "UPDATE phienbansanpham SET rom=?, ram=?, mausac=?, gianhap=?, giaxuat=?, soluongton=?, trangthai=? WHERE maphienbansp=?";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            setNullableInt(ps, 1, v.getRomId());
            setNullableInt(ps, 2, v.getRamId());
            setNullableInt(ps, 3, v.getColorId());
            setNullableInt(ps, 4, v.getImportPrice());
            setNullableInt(ps, 5, v.getExportPrice());
            setNullableInt(ps, 6, v.getStock());
            setNullableInt(ps, 7, v.getStatus());
            ps.setLong(8, v.getId());

            int updated = ps.executeUpdate();
            if (updated <= 0) {
                throw new IllegalArgumentException("Không tìm thấy cấu hình");
            }
            return v;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "UPDATE phienbansanpham SET trangthai=0 WHERE maphienbansp=?";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private ProductVariant map(ResultSet rs) throws SQLException {
        ProductVariant v = new ProductVariant();
        v.setId(rs.getLong("maphienbansp"));
        v.setProductId(getNullableInt(rs, "masp"));
        v.setRomId(getNullableInt(rs, "rom"));
        v.setRomName(rs.getString("rom_ten"));
        v.setRamId(getNullableInt(rs, "ram"));
        v.setRamName(rs.getString("ram_ten"));
        v.setColorId(getNullableInt(rs, "mausac"));
        v.setColorName(rs.getString("mausac_ten"));
        v.setImportPrice(getNullableInt(rs, "gianhap"));
        v.setExportPrice(getNullableInt(rs, "giaxuat"));
        v.setStock(getNullableInt(rs, "soluongton"));

        int st = rs.getInt("trangthai");
        v.setStatus(rs.wasNull() ? null : st);
        return v;
    }

    private void bind(PreparedStatement ps, ProductVariant v, boolean includeIdAtEnd) throws SQLException {
        setNullableInt(ps, 1, v.getProductId());
        setNullableInt(ps, 2, v.getRomId());
        setNullableInt(ps, 3, v.getRamId());
        setNullableInt(ps, 4, v.getColorId());
        setNullableInt(ps, 5, v.getImportPrice());
        setNullableInt(ps, 6, v.getExportPrice());
        setNullableInt(ps, 7, v.getStock());
        setNullableInt(ps, 8, v.getStatus());

        if (includeIdAtEnd) {
            ps.setLong(9, v.getId());
        }
    }

    private Integer getNullableInt(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    private void setNullableInt(PreparedStatement ps, int idx, Integer v) throws SQLException {
        if (v == null) {
            ps.setNull(idx, Types.INTEGER);
        } else {
            ps.setInt(idx, v);
        }
    }
}
