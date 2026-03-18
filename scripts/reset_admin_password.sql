-- Dev helper: reset admin password to a known plaintext value.
-- For schema: taikhoan(manv, matkhau, manhomquyen, tendangnhap, trangthai, otp)
-- (matches the SQL dump you pasted).

UPDATE taikhoan
SET matkhau = '123456',
    trangthai = 1,
    otp = NULL
WHERE tendangnhap = 'admin';

-- Verify
SELECT manv, tendangnhap, trangthai, matkhau
FROM taikhoan
WHERE tendangnhap = 'admin';
