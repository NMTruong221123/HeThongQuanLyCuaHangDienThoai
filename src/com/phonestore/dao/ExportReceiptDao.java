package com.phonestore.dao;

import com.phonestore.model.ExportReceipt;

import java.util.List;

public interface ExportReceiptDao {
    List<ExportReceipt> findAll();

    List<ExportReceipt> search(String keyword);

    ExportReceipt create(ExportReceipt receipt);

    ExportReceipt update(ExportReceipt receipt);

    void delete(long id);
}
