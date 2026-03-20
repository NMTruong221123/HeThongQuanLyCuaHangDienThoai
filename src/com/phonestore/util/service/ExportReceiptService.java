package com.phonestore.util.service;

import com.phonestore.model.ExportReceipt;
import com.phonestore.model.ExportReceiptLine;

import java.util.List;

public interface ExportReceiptService {
    List<ExportReceipt> findAll();

    List<ExportReceipt> search(String keyword);

    ExportReceipt create(ExportReceipt receipt);

    ExportReceipt update(ExportReceipt receipt);

    void delete(long id);

    /**
     * Permanently delete a receipt and compress subsequent receipt ids (shift down by 1).
     */
    void deleteAndCompress(long id);

    com.phonestore.dao.jdbc.ExportReceiptJdbcDao.PreviewInfo previewDeleteAndCompress(long id);

    void backupAndDeleteAndCompress(long id, String backupDirPath);

    /**
     * Loads item lines of a receipt (best-effort; may return empty list if schema not supported).
     */
    List<ExportReceiptLine> findLinesByReceiptId(long receiptId);
}
