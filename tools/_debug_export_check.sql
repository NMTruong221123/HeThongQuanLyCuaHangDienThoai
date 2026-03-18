SELECT sp.masp, sp.tensp, sp.soluongton FROM sanpham sp WHERE sp.tensp LIKE '%iPhone 13%';
SELECT pb.maphienbansp, pb.masp, pb.soluongton FROM phienbansanpham pb WHERE pb.masp IN (SELECT masp FROM sanpham WHERE tensp LIKE '%iPhone 13%');
SELECT imei, product_id, variant_id, sold, import_receipt_id, export_receipt_id FROM imei_registry WHERE product_id IN (SELECT masp FROM sanpham WHERE tensp LIKE '%iPhone 13%') ORDER BY created_at DESC LIMIT 20;
