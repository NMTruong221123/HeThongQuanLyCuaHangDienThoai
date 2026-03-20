package com.phonestore.util.service;

import com.phonestore.model.ImportReceipt;
import com.phonestore.model.ImportReceiptLine;

import java.util.List;

public interface ImportReceiptService {
    List<ImportReceipt> findAll();

    List<ImportReceipt> search(String keyword);

    ImportReceipt create(ImportReceipt receipt);

    ImportReceipt update(ImportReceipt receipt);

    void delete(long id);

    List<ImportReceiptLine> findLinesByReceiptId(long receiptId);
}
