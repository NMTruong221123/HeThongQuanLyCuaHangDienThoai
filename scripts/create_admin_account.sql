-- Create a default admin account for login.
-- NOTE: This project has had multiple DB schemas over time.
-- Use ONE of the blocks below, depending on your table columns.

-- =========================================================
-- BLOCK A) Older dump schema (like src/com/phonestore/resources/db/quanlikhohang.sql)
--   taikhoan(manv, matkhau, email, manhomquyen, trangthai)
--   nhanvien(manv, hoten, gioitinh, ngaysinh, sdt, trangthai)
-- Login on UI: Username = admin, Password = admin
-- =========================================================

-- 1) Ensure employee exists
INSERT INTO nhanvien (manv, hoten, gioitinh, ngaysinh, sdt, trangthai)
VALUES (1, 'Admin', 1, '2000-01-01', '0000000000', 1)
ON DUPLICATE KEY UPDATE
  hoten = VALUES(hoten),
  gioitinh = VALUES(gioitinh),
  ngaysinh = VALUES(ngaysinh),
  sdt = VALUES(sdt),
  trangthai = VALUES(trangthai);

-- 2) Ensure account exists (password stored as plaintext 'admin')
INSERT INTO taikhoan (manv, matkhau, email, manhomquyen, trangthai)
VALUES (1, 'admin', 'admin', 1, 1)
ON DUPLICATE KEY UPDATE
  matkhau = VALUES(matkhau),
  email = VALUES(email),
  manhomquyen = VALUES(manhomquyen),
  trangthai = VALUES(trangthai);


-- =========================================================
-- BLOCK B) Newer schema (if your DB has taikhoan.tendangnhap)
--   taikhoan(manv, tendangnhap, matkhau, manhomquyen, trangthai, otp)
--   nhanvien(manv, hoten, email, ...)
-- If you use this schema, uncomment and adapt columns to match your nhanvien table.
-- =========================================================

-- If your DB matches the pasted dump (taikhoan has tendangnhap + bcrypt hashes),
-- you can just RESET admin password to plaintext for quick login:
--
-- UPDATE taikhoan SET matkhau='123456', trangthai=1, otp=NULL WHERE tendangnhap='admin';
