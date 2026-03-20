package com.phonestore.dao;

import com.phonestore.model.ImportReceipt;

import java.util.List;

public interface ImportReceiptDao {
    List<ImportReceipt> findAll();

    List<ImportReceipt> search(String keyword);

    ImportReceipt create(ImportReceipt receipt);

    ImportReceipt update(ImportReceipt receipt);

    void delete(long id);

    void updateStatus(long id, int status);
}
