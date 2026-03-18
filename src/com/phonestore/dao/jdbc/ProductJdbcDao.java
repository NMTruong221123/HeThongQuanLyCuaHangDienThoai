package com.phonestore.dao.jdbc;

import com.phonestore.config.JDBCUtil;
import com.phonestore.dao.ProductDao;
import com.phonestore.model.Product;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductJdbcDao implements ProductDao {

    private static final String BASE_SELECT =
        "SELECT sp.masp, sp.tensp, sp.hinhanh, "
            + "sp.xuatxu, xx.tenxuatxu AS xuatxu_ten, "
            + "sp.chipxuly, sp.dungluongpin, sp.kichthuocman, "
            + "sp.hedieuhanh, hdh.tenhedieuhanh AS hedieuhanh_ten, sp.phienbanhdh, "
            + "sp.camerasau, sp.cameratruoc, sp.thoigianbaohanh, "
            + "sp.thuonghieu, th.tenthuonghieu AS thuonghieu_ten, "
            + "sp.khuvuckho, kv.tenkhuvuc AS khuvuc_ten, "
            + "sp.soluongton, sp.trangthai "
            + "FROM sanpham sp "
            + "LEFT JOIN xuatxu xx ON sp.xuatxu = xx.maxuatxu "
            + "LEFT JOIN hedieuhanh hdh ON sp.hedieuhanh = hdh.mahedieuhanh "
            + "LEFT JOIN thuonghieu th ON sp.thuonghieu = th.mathuonghieu "
            + "LEFT JOIN khuvuckho kv ON sp.khuvuckho = kv.makhuvuc ";

    @Override
    public List<Product> findAll() {
        String sql = BASE_SELECT + "WHERE sp.trangthai=1 AND (kv.trangthai IS NULL OR kv.trangthai=1) ORDER BY sp.masp ASC";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql);
             ResultSet rs = ps == null ? null : ps.executeQuery()) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            List<Product> list = new ArrayList<>();
            while (rs.next()) {
                list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Product> search(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isBlank()) {
            return findAll();
        }
        String sql = BASE_SELECT + "WHERE sp.trangthai=1 AND (kv.trangthai IS NULL OR kv.trangthai=1) AND sp.tensp LIKE ? ORDER BY sp.masp ASC";
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            String like = "%" + kw + "%";
            ps.setString(1, like);
            try (ResultSet rs = ps.executeQuery()) {
                List<Product> list = new ArrayList<>();
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
    public Product create(Product product) {
        String sql = "INSERT INTO sanpham (tensp, hinhanh, xuatxu, chipxuly, dungluongpin, kichthuocman, hedieuhanh, phienbanhdh, camerasau, cameratruoc, thoigianbaohanh, thuonghieu, khuvuckho, soluongton, trangthai) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            bind(ps, product, false);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    product.setId(keys.getLong(1));
                }
            }
            return product;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Product update(Product product) {
        String sql = "UPDATE sanpham SET tensp=?, hinhanh=?, xuatxu=?, chipxuly=?, dungluongpin=?, kichthuocman=?, hedieuhanh=?, phienbanhdh=?, camerasau=?, cameratruoc=?, thoigianbaohanh=?, thuonghieu=?, khuvuckho=?, soluongton=?, trangthai=? "
            + "WHERE masp=?";

        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql)) {

            if (c == null) {
                throw new IllegalStateException("Chưa kết nối được DB");
            }

            bind(ps, product, true);
            int updated = ps.executeUpdate();
            if (updated <= 0) {
                throw new IllegalArgumentException("Không tìm thấy sản phẩm");
            }
            return product;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(long id) {
        String sql = "UPDATE sanpham SET trangthai=0 WHERE masp=?";
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

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setId(rs.getLong("masp"));
        p.setName(rs.getString("tensp"));
        p.setImagePath(rs.getString("hinhanh"));
        p.setOriginId(getNullableInt(rs, "xuatxu"));
        p.setOriginName(rs.getString("xuatxu_ten"));

        p.setChipProcessor(rs.getString("chipxuly"));
        p.setBatteryCapacity(getNullableInt(rs, "dungluongpin"));
        double man = rs.getDouble("kichthuocman");
        p.setScreenSize(rs.wasNull() ? null : man);
        p.setOperatingSystemId(getNullableInt(rs, "hedieuhanh"));
        p.setOperatingSystemName(rs.getString("hedieuhanh_ten"));
        p.setOsVersion(getNullableInt(rs, "phienbanhdh"));
        p.setRearCamera(rs.getString("camerasau"));
        p.setFrontCamera(rs.getString("cameratruoc"));
        p.setWarrantyMonths(getNullableInt(rs, "thoigianbaohanh"));

        p.setBrandId(getNullableInt(rs, "thuonghieu"));
        p.setBrandName(rs.getString("thuonghieu_ten"));
        p.setZoneId(getNullableInt(rs, "khuvuckho"));
        p.setZoneName(rs.getString("khuvuc_ten"));
        p.setStock(getNullableInt(rs, "soluongton"));

        int status = rs.getInt("trangthai");
        p.setStatus(rs.wasNull() ? null : status);
        return p;
    }

    private void bind(PreparedStatement ps, Product p, boolean includeIdAtEnd) throws SQLException {
        ps.setString(1, p.getName());
        ps.setString(2, p.getImagePath());
        setNullableInt(ps, 3, p.getOriginId());

        ps.setString(4, p.getChipProcessor());
        setNullableInt(ps, 5, p.getBatteryCapacity());
        if (p.getScreenSize() == null) {
            ps.setNull(6, Types.DOUBLE);
        } else {
            ps.setDouble(6, p.getScreenSize());
        }
        setNullableInt(ps, 7, p.getOperatingSystemId());
        setNullableInt(ps, 8, p.getOsVersion());
        ps.setString(9, p.getRearCamera());
        ps.setString(10, p.getFrontCamera());
        setNullableInt(ps, 11, p.getWarrantyMonths());
        setNullableInt(ps, 12, p.getBrandId());
        setNullableInt(ps, 13, p.getZoneId());
        setNullableInt(ps, 14, p.getStock());
        setNullableInt(ps, 15, p.getStatus());

        if (includeIdAtEnd) {
            ps.setLong(16, p.getId());
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
