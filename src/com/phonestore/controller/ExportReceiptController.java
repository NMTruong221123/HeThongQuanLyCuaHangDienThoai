package com.phonestore.controller;

import com.phonestore.model.ExportReceipt;
import com.phonestore.model.ExportReceiptLine;
import com.phonestore.util.service.ExportReceiptService;
import com.phonestore.util.service.impl.ExportReceiptServiceImpl;

import java.util.List;

public class ExportReceiptController {

    private final ExportReceiptService receiptService;

    public ExportReceiptController() {
        this(new ExportReceiptServiceImpl());
    }

    public ExportReceiptController(ExportReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    public List<ExportReceipt> findAll() {
        return receiptService.findAll();
    }

    public List<ExportReceipt> search(String keyword) {
        return receiptService.search(keyword);
    }

    public ExportReceipt create(ExportReceipt receipt) {
        validate(receipt);
        return receiptService.create(receipt);
    }

    public ExportReceipt update(ExportReceipt receipt) {
        validate(receipt);
        return receiptService.update(receipt);
    }

    public void delete(long id) {
        if (id <= 0) throw new IllegalArgumentException("ID không hợp lệ");
        receiptService.delete(id);
    }

    public void deleteAndCompress(long id) {
        if (id <= 0) throw new IllegalArgumentException("ID không hợp lệ");
        receiptService.deleteAndCompress(id);
    }

    public com.phonestore.dao.jdbc.ExportReceiptJdbcDao.PreviewInfo previewDeleteAndCompress(long id) {
        if (id <= 0) throw new IllegalArgumentException("ID không hợp lệ");
        if (receiptService instanceof com.phonestore.util.service.impl.ExportReceiptServiceImpl impl) {
            return impl.previewDeleteAndCompress(id);
        }
        return new com.phonestore.dao.jdbc.ExportReceiptJdbcDao.PreviewInfo();
    }

    public void backupAndDeleteAndCompress(long id, String backupDirPath) {
        if (id <= 0) throw new IllegalArgumentException("ID không hợp lệ");
        if (receiptService instanceof com.phonestore.util.service.impl.ExportReceiptServiceImpl impl) {
            impl.backupAndDeleteAndCompress(id, backupDirPath);
            return;
        }
        // fallback
        receiptService.deleteAndCompress(id);
    }

    public List<ExportReceiptLine> findLinesByReceiptId(long receiptId) {
        if (receiptId <= 0) throw new IllegalArgumentException("ID không hợp lệ");
        return receiptService.findLinesByReceiptId(receiptId);
    }

    private void validate(ExportReceipt r) {
        if (r == null) throw new IllegalArgumentException("Dữ liệu không hợp lệ");
        if (r.getTotal() != null && r.getTotal() < 0) throw new IllegalArgumentException("Tổng tiền không hợp lệ");
        if (r.getCreatedBy() != null && r.getCreatedBy() <= 0) throw new IllegalArgumentException("Người tạo (ID) không hợp lệ");
        if (r.getCustomerId() != null && r.getCustomerId() <= 0) throw new IllegalArgumentException("Khách hàng (ID) không hợp lệ");
        if (r.getStatus() != null && r.getStatus() != 0 && r.getStatus() != 1) {
            throw new IllegalArgumentException("Trạng thái chỉ nhận 0/1 hoặc để trống");
        }
    }
}
