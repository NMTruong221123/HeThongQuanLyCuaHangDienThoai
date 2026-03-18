import com.phonestore.controller.StatisticsController;

import java.time.LocalDate;

public class RunStatsSummary {
    public static void main(String[] args) {
        StatisticsController c = new StatisticsController();
        LocalDate from = LocalDate.of(2026, 1, 13);
        LocalDate to = LocalDate.of(2026, 3, 15);

        var sup = c.supplierImportSummary(from, to, "");
        System.out.println("supplierImportSummary size=" + (sup == null ? 0 : sup.size()));
        if (sup != null) {
            for (int i = 0; i < Math.min(5, sup.size()); i++) {
                var r = sup.get(i);
                System.out.println(" - nccId=" + r.getSupplierId() + " | name=" + r.getSupplierName() + " | cnt=" + r.getImportCount() + " | sum=" + r.getTotalAmount());
            }
        }

        var cus = c.customerExportSummary(from, to, "");
        System.out.println("customerExportSummary size=" + (cus == null ? 0 : cus.size()));
        if (cus != null) {
            for (int i = 0; i < Math.min(5, cus.size()); i++) {
                var r = cus.get(i);
                System.out.println(" - cusId=" + r.getCustomerId() + " | name=" + r.getCustomerName() + " | cnt=" + r.getReceiptCount() + " | sum=" + r.getTotalAmount());
            }
        }
    }
}
