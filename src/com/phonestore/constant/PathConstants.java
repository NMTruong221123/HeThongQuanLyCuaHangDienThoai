package com.phonestore.constant;

import java.io.File;

public final class PathConstants {

    private PathConstants() {}

    public static final String RES_IMAGES = "/images";
    public static final String RES_ICONS = "/images/icons";
    public static final String RES_DEFAULT = "/images/default";

    public static final String DATA_DIR = "data";
    public static final String UPLOAD_DIR = DATA_DIR + File.separator + "uploads";
    public static final String PRODUCT_UPLOAD_DIR = UPLOAD_DIR + File.separator + "products";
    public static final String EMPLOYEE_UPLOAD_DIR = UPLOAD_DIR + File.separator + "employees";

    public static final String EXPORT_DIR = DATA_DIR + File.separator + "export";
    public static final String EXPORT_EXCEL_DIR = EXPORT_DIR + File.separator + "excel";
    public static final String EXPORT_PDF_DIR = EXPORT_DIR + File.separator + "pdf";

    public static final String BACKUP_DIR = DATA_DIR + File.separator + "backup";
    public static final String LOG_DIR = DATA_DIR + File.separator + "logs";

    public static void ensureDataDirectories() {
        mkdirs(DATA_DIR);
        mkdirs(UPLOAD_DIR);
        mkdirs(PRODUCT_UPLOAD_DIR);
        mkdirs(EMPLOYEE_UPLOAD_DIR);
        mkdirs(EXPORT_DIR);
        mkdirs(EXPORT_EXCEL_DIR);
        mkdirs(EXPORT_PDF_DIR);
        mkdirs(BACKUP_DIR);
        mkdirs(LOG_DIR);
    }

    private static void mkdirs(String path) {
        new File(path).mkdirs();
    }
}
