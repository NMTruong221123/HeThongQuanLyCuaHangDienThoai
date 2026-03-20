package com.phonestore.controller;

import com.phonestore.model.AttributeType;
import com.phonestore.model.ExportReceipt;
import com.phonestore.model.ImportReceipt;
import com.phonestore.model.Product;
import com.phonestore.config.JDBCUtil;
import com.phonestore.util.service.AttributeService;
import com.phonestore.util.service.CustomerService;
import com.phonestore.util.service.EmployeeService;
import com.phonestore.util.service.ExportReceiptService;
import com.phonestore.util.service.ImportReceiptService;
import com.phonestore.util.service.ProductService;
import com.phonestore.util.service.SupplierService;
import com.phonestore.util.service.WarehouseZoneService;
import com.phonestore.util.service.impl.AttributeServiceImpl;
import com.phonestore.util.service.impl.CustomerServiceImpl;
import com.phonestore.util.service.impl.EmployeeServiceImpl;
import com.phonestore.util.service.impl.ExportReceiptServiceImpl;
import com.phonestore.util.service.impl.ImportReceiptServiceImpl;
import com.phonestore.util.service.impl.ProductServiceImpl;
import com.phonestore.util.service.impl.SupplierServiceImpl;
import com.phonestore.util.service.impl.WarehouseZoneServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class StatisticsController {

    public static final class SupplierImportRow {
        private final long supplierId;
        private final String supplierName;
        private final long importCount;
        private final double totalAmount;

        public SupplierImportRow(long supplierId, String supplierName, long importCount, double totalAmount) {
            this.supplierId = supplierId;
            this.supplierName = supplierName;
            this.importCount = importCount;
            this.totalAmount = totalAmount;
        }

        public long getSupplierId() {
            return supplierId;
        }

        public String getSupplierName() {
            return supplierName;
        }

        public long getImportCount() {
            return importCount;
        }

        public double getTotalAmount() {
            return totalAmount;
        }
    }

    public static final class CustomerExportRow {
        private final long customerId;
        private final String customerName;
        private final long receiptCount;
        private final double totalAmount;

        public CustomerExportRow(long customerId, String customerName, long receiptCount, double totalAmount) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.receiptCount = receiptCount;
            this.totalAmount = totalAmount;
        }

        public long getCustomerId() {
            return customerId;
        }

        public String getCustomerName() {
            return customerName;
        }

        public long getReceiptCount() {
            return receiptCount;
        }

        public double getTotalAmount() {
            return totalAmount;
        }
    }

    public static final class RevenueYear {
        private final int year;
        private final double cost;
        private final double revenue;

        public RevenueYear(int year, double cost, double revenue) {
            this.year = year;
            this.cost = cost;
            this.revenue = revenue;
        }

        public int getYear() {
            return year;
        }

        public double getCost() {
            return cost;
        }

        public double getRevenue() {
            return revenue;
        }

        public double getProfit() {
            return revenue - cost;
        }
    }

    public static final class InventoryRow {
        private final long productId;
        private final String productName;
        private final long opening;
        private final long inPeriod;
        private final long outPeriod;
        private final long closing;

        public InventoryRow(long productId, String productName, long opening, long inPeriod, long outPeriod, long closing) {
            this.productId = productId;
            this.productName = productName;
            this.opening = opening;
            this.inPeriod = inPeriod;
            this.outPeriod = outPeriod;
            this.closing = closing;
        }

        public long getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public long getOpening() {
            return opening;
        }

        public long getInPeriod() {
            return inPeriod;
        }

        public long getOutPeriod() {
            return outPeriod;
        }

        public long getClosing() {
            return closing;
        }
    }

    public static final class OverviewSnapshot {
        private final int productsInStock;
        private final int totalCustomers;
        private final int activeEmployees;

        public OverviewSnapshot(int productsInStock, int totalCustomers, int activeEmployees) {
            this.productsInStock = productsInStock;
            this.totalCustomers = totalCustomers;
            this.activeEmployees = activeEmployees;
        }

        public int getProductsInStock() {
            return productsInStock;
        }

        public int getTotalCustomers() {
            return totalCustomers;
        }

        public int getActiveEmployees() {
            return activeEmployees;
        }
    }

    public static final class RevenueDay {
        private final LocalDate date;
        private final double cost;
        private final double revenue;

        public RevenueDay(LocalDate date, double cost, double revenue) {
            this.date = date;
            this.cost = cost;
            this.revenue = revenue;
        }

        public LocalDate getDate() {
            return date;
        }

        public double getCost() {
            return cost;
        }

        public double getRevenue() {
            return revenue;
        }

        public double getProfit() {
            return revenue - cost;
        }
    }

    public static final class Snapshot {
        private final int totalProducts;
        private final int activeProducts;
        private final long totalStock;
        private final int totalZones;
        private final int totalAttributes;

        public Snapshot(int totalProducts, int activeProducts, long totalStock, int totalZones, int totalAttributes) {
            this.totalProducts = totalProducts;
            this.activeProducts = activeProducts;
            this.totalStock = totalStock;
            this.totalZones = totalZones;
            this.totalAttributes = totalAttributes;
        }

        public int getTotalProducts() {
            return totalProducts;
        }

        public int getActiveProducts() {
            return activeProducts;
        }

        public long getTotalStock() {
            return totalStock;
        }

        public int getTotalZones() {
            return totalZones;
        }

        public int getTotalAttributes() {
            return totalAttributes;
        }
    }

    private final ProductService productService;
    private final WarehouseZoneService zoneService;
    private final AttributeService attributeService;

    private final CustomerService customerService;
    private final EmployeeService employeeService;
    private final ImportReceiptService importReceiptService;
    private final ExportReceiptService exportReceiptService;

    public StatisticsController() {
        this(
                new ProductServiceImpl(),
                new WarehouseZoneServiceImpl(),
                new AttributeServiceImpl(),
                new CustomerServiceImpl(),
                new EmployeeServiceImpl(),
                new ImportReceiptServiceImpl(),
                new ExportReceiptServiceImpl()
        );
    }

    public StatisticsController(
            ProductService productService,
            WarehouseZoneService zoneService,
            AttributeService attributeService,
            CustomerService customerService,
            EmployeeService employeeService,
            ImportReceiptService importReceiptService,
            ExportReceiptService exportReceiptService
    ) {
        this.productService = productService;
        this.zoneService = zoneService;
        this.attributeService = attributeService;
        this.customerService = customerService;
        this.employeeService = employeeService;
        this.importReceiptService = importReceiptService;
        this.exportReceiptService = exportReceiptService;
    }

    public OverviewSnapshot overview() {
        int productsInStock = 0;
        try {
            List<Product> products = productService.findAll();
            if (products != null) {
                for (Product p : products) {
                    if (p == null) continue;
                    Integer stock = p.getStock();
                    if (stock != null && stock > 0) productsInStock++;
                }
            }
        } catch (Exception ignored) {
            productsInStock = 0;
        }

        int totalCustomers = 0;
        try {
            var customers = customerService.findAll();
            totalCustomers = customers == null ? 0 : customers.size();
        } catch (Exception ignored) {
            totalCustomers = 0;
        }

        int activeEmployees = 0;
        try {
            var employees = employeeService.findAll();
            if (employees != null) {
                for (var e : employees) {
                    if (e == null) continue;
                    Integer st = e.getStatus();
                    if (st != null && st == 1) activeEmployees++;
                }
            }
        } catch (Exception ignored) {
            activeEmployees = 0;
        }

        return new OverviewSnapshot(productsInStock, totalCustomers, activeEmployees);
    }

    /**
     * Returns last N days (inclusive) cost/revenue series.
     * Cost is approximated from ImportReceipts total per day.
     * Revenue is approximated from ExportReceipts total per day.
     */
    public java.util.List<RevenueDay> revenueLastDays(int days) {
        int n = Math.max(1, days);
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(n - 1L);

        java.util.LinkedHashMap<LocalDate, double[]> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            LocalDate d = start.plusDays(i);
            map.put(d, new double[] {0d, 0d}); // [cost, revenue]
        }

        try {
            List<ImportReceipt> imports = importReceiptService.findAll();
            if (imports != null) {
                for (ImportReceipt r : imports) {
                    if (r == null || r.getTime() == null) continue;
                    LocalDate d = r.getTime().toLocalDate();
                    if (d.isBefore(start) || d.isAfter(today)) continue;
                    double v = r.getTotal() == null ? 0d : r.getTotal();
                    double[] arr = map.get(d);
                    if (arr != null) arr[0] += v;
                }
            }
        } catch (Exception ignored) {
            // keep zeros
        }

        try {
            List<ExportReceipt> exports = exportReceiptService.findAll();
            if (exports != null) {
                for (ExportReceipt r : exports) {
                    if (r == null || r.getTime() == null) continue;
                    LocalDate d = r.getTime().toLocalDate();
                    if (d.isBefore(start) || d.isAfter(today)) continue;
                    double v = r.getTotal() == null ? 0d : r.getTotal();
                    double[] arr = map.get(d);
                    if (arr != null) arr[1] += v;
                }
            }
        } catch (Exception ignored) {
            // keep zeros
        }

        java.util.List<RevenueDay> out = new java.util.ArrayList<>(map.size());
        for (var e : map.entrySet()) {
            double[] v = e.getValue();
            out.add(new RevenueDay(e.getKey(), v[0], v[1]));
        }
        return out;
    }

    /**
     * Revenue aggregation by year (inclusive range).
     *
     * - Cost is approximated from ImportReceipts total.
     * - Revenue is approximated from ExportReceipts total.
     * - Only receipts with status=1 are counted (best-effort).
     */
    public List<RevenueYear> revenueByYear(Integer fromYear, Integer toYear) {
        int current = LocalDate.now().getYear();
        int yFrom = fromYear == null ? (current - 5) : fromYear;
        int yTo = toYear == null ? current : toYear;
        if (yFrom > yTo) {
            int tmp = yFrom;
            yFrom = yTo;
            yTo = tmp;
        }

        Map<Integer, double[]> map = new HashMap<>();
        for (int y = yFrom; y <= yTo; y++) {
            map.put(y, new double[] {0d, 0d});
        }

        try {
            List<ImportReceipt> imports = importReceiptService.findAll();
            if (imports != null) {
                for (ImportReceipt r : imports) {
                    if (r == null || r.getTime() == null) continue;
                    Integer st = r.getStatus();
                    if (st != null && st != 1) continue;
                    int y = r.getTime().getYear();
                    if (y < yFrom || y > yTo) continue;
                    double v = r.getTotal() == null ? 0d : r.getTotal();
                    double[] arr = map.get(y);
                    if (arr != null) arr[0] += v;
                }
            }
        } catch (Exception ignored) {
            // keep zeros
        }

        try {
            List<ExportReceipt> exports = exportReceiptService.findAll();
            if (exports != null) {
                for (ExportReceipt r : exports) {
                    if (r == null || r.getTime() == null) continue;
                    Integer st = r.getStatus();
                    if (st != null && st != 1) continue;
                    int y = r.getTime().getYear();
                    if (y < yFrom || y > yTo) continue;
                    double v = r.getTotal() == null ? 0d : r.getTotal();
                    double[] arr = map.get(y);
                    if (arr != null) arr[1] += v;
                }
            }
        } catch (Exception ignored) {
            // keep zeros
        }

        List<RevenueYear> out = new ArrayList<>();
        for (int y = yFrom; y <= yTo; y++) {
            double[] v = map.get(y);
            if (v == null) v = new double[] {0d, 0d};
            out.add(new RevenueYear(y, v[0], v[1]));
        }
        return out;
    }

    /**
     * Revenue aggregation by month for a specific year (months 1..12).
     * Returns list of RevenueYear where `year` holds the month number (1..12).
     */
    public List<RevenueYear> revenueByMonth(Integer year) {
        int y = year == null ? LocalDate.now().getYear() : year;
        Map<Integer, double[]> map = new HashMap<>();
        for (int m = 1; m <= 12; m++) map.put(m, new double[] {0d, 0d});

        try {
            List<ImportReceipt> imports = importReceiptService.findAll();
            if (imports != null) {
                for (ImportReceipt r : imports) {
                    if (r == null || r.getTime() == null) continue;
                    Integer st = r.getStatus();
                    if (st != null && st != 1) continue;
                    int ry = r.getTime().getYear();
                    if (ry != y) continue;
                    int m = r.getTime().getMonthValue();
                    double v = r.getTotal() == null ? 0d : r.getTotal();
                    double[] arr = map.get(m);
                    if (arr != null) arr[0] += v;
                }
            }
        } catch (Exception ignored) {}

        try {
            List<ExportReceipt> exports = exportReceiptService.findAll();
            if (exports != null) {
                for (ExportReceipt r : exports) {
                    if (r == null || r.getTime() == null) continue;
                    Integer st = r.getStatus();
                    if (st != null && st != 1) continue;
                    int ry = r.getTime().getYear();
                    if (ry != y) continue;
                    int m = r.getTime().getMonthValue();
                    double v = r.getTotal() == null ? 0d : r.getTotal();
                    double[] arr = map.get(m);
                    if (arr != null) arr[1] += v;
                }
            }
        } catch (Exception ignored) {}

        List<RevenueYear> out = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            double[] v = map.get(m);
            if (v == null) v = new double[] {0d, 0d};
            out.add(new RevenueYear(m, v[0], v[1]));
        }
        return out;
    }

    /**
     * Import receipts summary grouped by supplier.
     * - importCount: number of import receipts in range (status=1)
     * - totalAmount: sum of tongtien in range
     */
    public List<SupplierImportRow> supplierImportSummary(LocalDate from, LocalDate to, String keyword) {
        LocalDate today = LocalDate.now();
        LocalDate toDate = (to == null) ? today : to;
        LocalDate fromDate = (from == null) ? toDate.minusDays(30) : from;
        if (fromDate.isAfter(toDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        Map<Long, long[]> counts = new HashMap<>();
        Map<Long, Double> totals = new HashMap<>();
        Map<Long, String> names = new HashMap<>();

        try {
            List<ImportReceipt> imports = importReceiptService.findAll();
            if (imports != null) {
                for (ImportReceipt r : imports) {
                    if (r == null || r.getTime() == null) continue;
                    Integer st = r.getStatus();
                    if (st != null && st != 1) continue;

                    LocalDate d = r.getTime().toLocalDate();
                    if (d.isBefore(fromDate) || d.isAfter(toDate)) continue;

                    Long sid = r.getSupplierId();
                    if (sid == null || sid <= 0) continue;

                    String sname = r.getSupplierName();
                    if (sname != null && !sname.isBlank()) {
                        names.putIfAbsent(sid, sname);
                    }

                    // keyword filter (by id or name)
                    if (!kw.isBlank()) {
                        String idStr = String.valueOf(sid);
                        String nameStr = (sname == null ? "" : sname).toLowerCase(Locale.ROOT);
                        if (!idStr.contains(kw) && !nameStr.contains(kw)) continue;
                    }

                    counts.computeIfAbsent(sid, k -> new long[] {0L})[0] += 1L;
                    double total = r.getTotal() == null ? 0d : r.getTotal();
                    totals.put(sid, totals.getOrDefault(sid, 0d) + total);
                }
            }
        } catch (Exception ignored) {
        }

        // Best-effort: fill missing supplier names from SupplierService
        try {
            SupplierService supplierService = new SupplierServiceImpl();
            var all = supplierService.findAll();
            if (all != null) {
                for (var s : all) {
                    if (s == null) continue;
                    long sid = s.getId();
                    if (sid <= 0) continue;
                    if (!counts.containsKey(sid)) continue;
                    String nm = s.getName();
                    if (nm != null && !nm.isBlank()) names.putIfAbsent(sid, nm);
                }
            }
        } catch (Exception ignored) {
        }

        List<SupplierImportRow> out = new ArrayList<>();
        for (var e : counts.entrySet()) {
            long sid = e.getKey();
            long cnt = e.getValue() == null ? 0L : e.getValue()[0];
            double sum = totals.getOrDefault(sid, 0d);
            String nm = names.getOrDefault(sid, "");
            out.add(new SupplierImportRow(sid, nm, cnt, sum));
        }

        out.sort((a, b) -> Double.compare(b.getTotalAmount(), a.getTotalAmount()));
        return out;
    }

    /**
     * Export receipts summary grouped by customer.
     * - receiptCount: number of export receipts in range (status=1)
     * - totalAmount: sum of tongtien in range
     */
    public List<CustomerExportRow> customerExportSummary(LocalDate from, LocalDate to, String keyword) {
        LocalDate today = LocalDate.now();
        LocalDate toDate = (to == null) ? today : to;
        LocalDate fromDate = (from == null) ? toDate.minusDays(30) : from;
        if (fromDate.isAfter(toDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        Map<Long, long[]> counts = new HashMap<>();
        Map<Long, Double> totals = new HashMap<>();
        Map<Long, String> names = new HashMap<>();

        try {
            List<ExportReceipt> exports = exportReceiptService.findAll();
            if (exports != null) {
                for (ExportReceipt r : exports) {
                    if (r == null || r.getTime() == null) continue;
                    Integer st = r.getStatus();
                    if (st != null && st != 1) continue;

                    LocalDate d = r.getTime().toLocalDate();
                    if (d.isBefore(fromDate) || d.isAfter(toDate)) continue;

                    Long cidObj = r.getCustomerId();
                    if (cidObj == null || cidObj <= 0) continue;
                    long cid = cidObj;

                    String cname = r.getCustomerName();
                    if (cname != null && !cname.isBlank()) {
                        names.putIfAbsent(cid, cname);
                    }

                    if (!kw.isBlank()) {
                        String idStr = String.valueOf(cid);
                        String nameStr = (cname == null ? "" : cname).toLowerCase(Locale.ROOT);
                        if (!idStr.contains(kw) && !nameStr.contains(kw)) continue;
                    }

                    counts.computeIfAbsent(cid, k -> new long[] {0L})[0] += 1L;
                    double total = r.getTotal() == null ? 0d : r.getTotal();
                    totals.put(cid, totals.getOrDefault(cid, 0d) + total);
                }
            }
        } catch (Exception ignored) {
        }

        // Fill missing customer names from CustomerService
        try {
            var all = customerService.findAll();
            if (all != null) {
                for (var c : all) {
                    if (c == null) continue;
                    long cid = c.getId();
                    if (cid <= 0) continue;
                    if (!counts.containsKey(cid)) continue;
                    String nm = c.getName();
                    if (nm != null && !nm.isBlank()) names.putIfAbsent(cid, nm);
                }
            }
        } catch (Exception ignored) {
        }

        List<CustomerExportRow> out = new ArrayList<>();
        for (var e : counts.entrySet()) {
            long cid = e.getKey();
            long cnt = e.getValue() == null ? 0L : e.getValue()[0];
            double sum = totals.getOrDefault(cid, 0d);
            String nm = names.getOrDefault(cid, "");
            out.add(new CustomerExportRow(cid, nm, cnt, sum));
        }
        out.sort((a, b) -> Double.compare(b.getTotalAmount(), a.getTotalAmount()));
        return out;
    }

    public Snapshot snapshot() {
        List<Product> products = productService.findAll();

        int totalProducts = products == null ? 0 : products.size();
        int activeProducts = 0;
        long totalStock = 0;

        if (products != null) {
            for (Product p : products) {
                Integer status = p.getStatus();
                if (status != null && status == 1) {
                    activeProducts++;
                }
                Integer stock = p.getStock();
                if (stock != null && stock > 0) {
                    totalStock += stock;
                }
            }
        }

        int totalZones = 0;
        try {
            var zones = zoneService.findAll();
            totalZones = zones == null ? 0 : zones.size();
        } catch (Exception ignored) {
            totalZones = 0;
        }

        int totalAttributes = 0;
        for (AttributeType t : AttributeType.values()) {
            try {
                var items = attributeService.findByType(t);
                totalAttributes += items == null ? 0 : items.size();
            } catch (Exception ignored) {
                // ignore
            }
        }

        return new Snapshot(totalProducts, activeProducts, totalStock, totalZones, totalAttributes);
    }

    /**
     * Inventory movement per product for a date range.
     *
     * Opening/closing are computed from current stock and line movements:
     * - closing(at end of toDate) = currentStock - (importsAfterTo - exportsAfterTo)
     * - opening(at start of fromDate) = closing - (importsInRange - exportsInRange)
     */
    public List<InventoryRow> inventoryMovement(LocalDate from, LocalDate to, String keyword) {
        LocalDate today = LocalDate.now();
        LocalDate toDate = (to == null) ? today : to;
        LocalDate fromDate = (from == null) ? toDate.minusDays(7) : from;
        if (fromDate.isAfter(toDate)) {
            LocalDate tmp = fromDate;
            fromDate = toDate;
            toDate = tmp;
        }

        List<Product> products;
        try {
            products = productService.findAll();
        } catch (Exception e) {
            products = new ArrayList<>();
        }

        String kw = keyword == null ? "" : keyword.trim();
        if (!kw.isBlank()) {
            String low = kw.toLowerCase(Locale.ROOT);
            List<Product> filtered = new ArrayList<>();
            for (Product p : products) {
                if (p == null) continue;
                String idStr = String.valueOf(p.getId());
                String name = p.getName() == null ? "" : p.getName();
                if (idStr.contains(low) || name.toLowerCase(Locale.ROOT).contains(low)) {
                    filtered.add(p);
                }
            }
            products = filtered;
        }

        LocalDateTime fromStart = fromDate.atStartOfDay();
        LocalDateTime toExclusive = toDate.plusDays(1).atStartOfDay();

        Map<Long, Long> inRange = sumImportQtyByProduct(fromStart, toExclusive);
        Map<Long, Long> outRange = sumExportQtyByProduct(fromStart, toExclusive);
        Map<Long, Long> inAfter = sumImportQtyByProduct(toExclusive, null);
        Map<Long, Long> outAfter = sumExportQtyByProduct(toExclusive, null);

        List<InventoryRow> out = new ArrayList<>();
        for (Product p : products) {
            if (p == null) continue;
            long productId = p.getId();
            String name = p.getName() == null ? "" : p.getName();

            long stockNow = p.getStock() == null ? 0 : p.getStock();
            long importsAfter = inAfter.getOrDefault(productId, 0L);
            long exportsAfter = outAfter.getOrDefault(productId, 0L);
            long closing = stockNow - importsAfter + exportsAfter;

            long imports = inRange.getOrDefault(productId, 0L);
            long exports = outRange.getOrDefault(productId, 0L);
            long opening = closing - (imports - exports);

            out.add(new InventoryRow(productId, name, opening, imports, exports, closing));
        }
        return out;
    }

    private Map<Long, Long> sumImportQtyByProduct(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        // Table: ctphieunhap + phieunhap
        final String lineTable = "ctphieunhap";
        final String receiptTable = "phieunhap";
        final String colReceiptId = "maphieunhap";
        final String colTime = "thoigian";
        final String colStatus = "trangthai";
        final String colQty = "soluong";

        if (!JDBCUtil.hasColumn(lineTable, colReceiptId) || !JDBCUtil.hasColumn(lineTable, colQty)) {
            return new HashMap<>();
        }
        if (!JDBCUtil.hasColumn(receiptTable, colReceiptId) || !JDBCUtil.hasColumn(receiptTable, colTime)) {
            return new HashMap<>();
        }

        boolean variantSchema = JDBCUtil.hasColumn(lineTable, "maphienbansp");
        String productCol = JDBCUtil.hasColumn(lineTable, "masp") ? "masp" : (JDBCUtil.hasColumn(lineTable, "masanpham") ? "masanpham" : null);
        if (!variantSchema && productCol == null) {
            return new HashMap<>();
        }

        StringBuilder sql = new StringBuilder();
        if (variantSchema) {
            sql.append("SELECT pb.masp AS product_id, SUM(ct.").append(colQty).append(") AS qty ")
                .append("FROM ").append(lineTable).append(" ct ")
                .append("JOIN ").append(receiptTable).append(" pn ON ct.").append(colReceiptId).append(" = pn.").append(colReceiptId).append(" ")
                .append("LEFT JOIN phienbansanpham pb ON ct.maphienbansp = pb.maphienbansp ")
                .append("WHERE pn.").append(colStatus).append("=1 ");
        } else {
            sql.append("SELECT ct.").append(productCol).append(" AS product_id, SUM(ct.").append(colQty).append(") AS qty ")
                .append("FROM ").append(lineTable).append(" ct ")
                .append("JOIN ").append(receiptTable).append(" pn ON ct.").append(colReceiptId).append(" = pn.").append(colReceiptId).append(" ")
                .append("WHERE pn.").append(colStatus).append("=1 ");
        }

        if (startInclusive != null) {
            sql.append("AND pn.").append(colTime).append(" >= ? ");
        }
        if (endExclusive != null) {
            sql.append("AND pn.").append(colTime).append(" < ? ");
        }
        sql.append("GROUP BY product_id");

        Map<Long, Long> out = new HashMap<>();
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql.toString())) {
            if (c == null) return out;
            int idx = 1;
            if (startInclusive != null) ps.setObject(idx++, startInclusive);
            if (endExclusive != null) ps.setObject(idx++, endExclusive);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long pid = rs.getLong("product_id");
                    if (rs.wasNull() || pid <= 0) continue;
                    long qty = rs.getLong("qty");
                    if (rs.wasNull()) qty = 0;
                    out.put(pid, qty);
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private Map<Long, Long> sumExportQtyByProduct(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        // Table: ctphieuxuat + phieuxuat
        final String lineTable = "ctphieuxuat";
        final String receiptTable = "phieuxuat";
        final String colReceiptId = "maphieuxuat";
        final String colTime = "thoigian";
        final String colStatus = "trangthai";
        final String colQty = "soluong";

        if (!JDBCUtil.hasColumn(lineTable, colReceiptId) || !JDBCUtil.hasColumn(lineTable, colQty)) {
            return new HashMap<>();
        }
        if (!JDBCUtil.hasColumn(receiptTable, colReceiptId) || !JDBCUtil.hasColumn(receiptTable, colTime)) {
            return new HashMap<>();
        }

        boolean variantSchema = JDBCUtil.hasColumn(lineTable, "maphienbansp");
        String productCol = JDBCUtil.hasColumn(lineTable, "masp") ? "masp" : (JDBCUtil.hasColumn(lineTable, "masanpham") ? "masanpham" : null);
        if (!variantSchema && productCol == null) {
            return new HashMap<>();
        }

        StringBuilder sql = new StringBuilder();
        if (variantSchema) {
            sql.append("SELECT pb.masp AS product_id, SUM(ct.").append(colQty).append(") AS qty ")
                .append("FROM ").append(lineTable).append(" ct ")
                .append("JOIN ").append(receiptTable).append(" px ON ct.").append(colReceiptId).append(" = px.").append(colReceiptId).append(" ")
                .append("LEFT JOIN phienbansanpham pb ON ct.maphienbansp = pb.maphienbansp ")
                .append("WHERE px.").append(colStatus).append("=1 ");
        } else {
            sql.append("SELECT ct.").append(productCol).append(" AS product_id, SUM(ct.").append(colQty).append(") AS qty ")
                .append("FROM ").append(lineTable).append(" ct ")
                .append("JOIN ").append(receiptTable).append(" px ON ct.").append(colReceiptId).append(" = px.").append(colReceiptId).append(" ")
                .append("WHERE px.").append(colStatus).append("=1 ");
        }

        if (startInclusive != null) {
            sql.append("AND px.").append(colTime).append(" >= ? ");
        }
        if (endExclusive != null) {
            sql.append("AND px.").append(colTime).append(" < ? ");
        }
        sql.append("GROUP BY product_id");

        Map<Long, Long> out = new HashMap<>();
        try (Connection c = JDBCUtil.getConnectionSilent();
             PreparedStatement ps = c == null ? null : c.prepareStatement(sql.toString())) {
            if (c == null) return out;
            int idx = 1;
            if (startInclusive != null) ps.setObject(idx++, startInclusive);
            if (endExclusive != null) ps.setObject(idx++, endExclusive);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long pid = rs.getLong("product_id");
                    if (rs.wasNull() || pid <= 0) continue;
                    long qty = rs.getLong("qty");
                    if (rs.wasNull()) qty = 0;
                    out.put(pid, qty);
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }
}
