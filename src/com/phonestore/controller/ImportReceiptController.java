package com.phonestore.controller;

import com.phonestore.model.ImportReceipt;
import com.phonestore.model.ImportReceiptLine;
import com.phonestore.util.service.ImportReceiptService;
import com.phonestore.util.service.impl.ImportReceiptServiceImpl;

import java.util.List;

public class ImportReceiptController {

    private final ImportReceiptService receiptService;

    public ImportReceiptController() {
        this(new ImportReceiptServiceImpl());
    }

    public ImportReceiptController(ImportReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    public List<ImportReceipt> findAll() {
        return receiptService.findAll();
    }

    public List<ImportReceipt> search(String keyword) {
        return receiptService.search(keyword);
    }

    public ImportReceipt create(ImportReceipt receipt) {
        validate(receipt);
        return receiptService.create(receipt);
    }

    public ImportReceipt update(ImportReceipt receipt) {
        validate(receipt);
        return receiptService.update(receipt);
    }

    public void delete(long id) {
        if (id <= 0) throw new IllegalArgumentException("ID khong hop le");
        receiptService.delete(id);
    }

    public List<ImportReceiptLine> findLinesByReceiptId(long receiptId) {
        if (receiptId <= 0) throw new IllegalArgumentException("ID khong hop le");
        return receiptService.findLinesByReceiptId(receiptId);
    }

    private void validate(ImportReceipt r) {
        if (r == null) throw new IllegalArgumentException("Du lieu khong hop le");
        if (r.getSupplierId() == null || r.getSupplierId() <= 0) throw new IllegalArgumentException("Nha cung cap (ID) la bat buoc");
        if (r.getCreatedBy() == null || r.getCreatedBy() <= 0) throw new IllegalArgumentException("Nguoi tao (ID) la bat buoc");
        if (r.getTotal() == null || r.getTotal() < 0) throw new IllegalArgumentException("Tong tien khong hop le");
        if (r.getStatus() != null && r.getStatus() != 0 && r.getStatus() != 1) {
            throw new IllegalArgumentException("Trang thai chi nhan 0/1 hoac de trong");
        }
    }
}
