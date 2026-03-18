-- PhoneStoreManagement
-- Migration: add email column to `khachhang` for sending export invoices to buyers.
-- Safe to run multiple times on MySQL 8+ (uses IF NOT EXISTS).

ALTER TABLE khachhang
    ADD COLUMN IF NOT EXISTS email VARCHAR(255) NULL AFTER diachi;

-- (Optional) Helpful index for searching customers by email
CREATE INDEX IF NOT EXISTS idx_khachhang_email ON khachhang(email);
