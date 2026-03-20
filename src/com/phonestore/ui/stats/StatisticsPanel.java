package com.phonestore.ui.stats;

import com.phonestore.controller.StatisticsController;
import com.phonestore.ui.common.images.ImageLoader;
import com.phonestore.ui.common.toast.Toast;
import com.phonestore.util.MoneyVND;
import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.optionalusertools.DateChangeListener;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StatisticsPanel extends JPanel {

    private final StatisticsController statisticsController = new StatisticsController();

    private final JLabel lblProductsInStock = new JLabel("0");
    private final JLabel lblTotalCustomers = new JLabel("0");
    private final JLabel lblActiveEmployees = new JLabel("0");

    private final RevenueChartPanel chartPanel = new RevenueChartPanel();
    private final RevenueTableModel tableModel = new RevenueTableModel();
    private final JTable tbl = new JTable(tableModel);

    // Revenue tab (year)
    private final JTextField txtRevFromYear = new JTextField();
    private final JTextField txtRevToYear = new JTextField();
    private final JTextField txtRevYear = new JTextField();
    private final JButton btnRevStat = new JButton("Thống kê");
    private final JButton btnRevRefresh = new JButton("Làm mới");
    // Excel export feature removed
    private final RevenueYearChartPanel revYearChart = new RevenueYearChartPanel();
    private final RevenueYearTableModel revYearModel = new RevenueYearTableModel();
    private final JTable tblRevYear = new JTable(revYearModel);
    private final JLabel lblRevFrom = new JLabel("Từ năm");
    private final JLabel lblRevTo = new JLabel("Đến năm");
    private final JLabel lblRevYear = new JLabel("Năm");

    // Inventory tab
    private final JTextField txtInvSearch = new JTextField();
    private final DatePicker dpInvFrom = createDatePicker();
    private final DatePicker dpInvTo = createDatePicker();
    // Excel export feature removed
    private final JButton btnInvRefresh = new JButton("Làm mới");
    private final InventoryTableModel invModel = new InventoryTableModel();
    private final JTable tblInv = new JTable(invModel);
    private volatile boolean inventoryLoadedOnce;

    // Supplier tab
    private final JTextField txtSupSearch = new JTextField();
    private final DatePicker dpSupFrom = createDatePicker();
    private final DatePicker dpSupTo = createDatePicker();
    // Excel export feature removed
    private final JButton btnSupRefresh = new JButton("Làm mới");
    private final SupplierImportTableModel supModel = new SupplierImportTableModel();
    private final JTable tblSup = new JTable(supModel);
    private volatile boolean supplierLoadedOnce;

    // Customer tab
    private final JTextField txtCusSearch = new JTextField();
    private final DatePicker dpCusFrom = createDatePicker();
    private final DatePicker dpCusTo = createDatePicker();
    // Excel export feature removed
    private final JButton btnCusRefresh = new JButton("Làm mới");
    private final CustomerExportTableModel cusModel = new CustomerExportTableModel();
    private final JTable tblCus = new JTable(cusModel);
    private volatile boolean customerLoadedOnce;

    // Avoid duplicate reloads when setting default dates programmatically
    private volatile boolean suppressSupplierDateReload;
    private volatile boolean suppressCustomerDateReload;

    private final CardLayout cardsLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardsLayout);

    private final Color backgroundColor = new Color(245, 247, 250);
    private final Color cardBorder = new Color(220, 220, 220);
    private final Color textPrimary = new Color(33, 33, 33);
    private final Color textSubtle = new Color(120, 144, 156);

    // Match screenshot legend colors: Vốn (orange), Doanh thu (blue), Lợi nhuận (purple)
    private final Color seriesCost = new Color(244, 81, 30);
    private final Color seriesRevenue = new Color(25, 118, 210);
    private final Color seriesProfit = new Color(103, 58, 183);

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public StatisticsPanel() {
        setBackground(backgroundColor);
        setLayout(new BorderLayout(12, 12));
        add(buildTopTabs(), BorderLayout.NORTH);
        cards.setOpaque(false);
        cards.add(buildOverviewBody(), "overview");
        cards.add(buildInventoryBody(), "inventory");
        cards.add(buildRevenueBody(), "revenue");
        cards.add(buildSupplierBody(), "supplier");
        cards.add(buildCustomerBody(), "customer");
        add(cards, BorderLayout.CENTER);
        add(buildPadding(BorderLayout.WEST), BorderLayout.WEST);
        add(buildPadding(BorderLayout.EAST), BorderLayout.EAST);
        add(buildPadding(BorderLayout.SOUTH), BorderLayout.SOUTH);

        configureMetricLabel(lblProductsInStock);
        configureMetricLabel(lblTotalCustomers);
        configureMetricLabel(lblActiveEmployees);

        configureRevenueTable(tbl);
        configureRevenueTable(tblRevYear);
        configureInventoryTable(tblInv);
        configureRevenueTable(tblSup);
        configureRevenueTable(tblCus);

        reload();
        reloadRevenueByYear(null, null);
        // default: year overview should show all data — hide month/year filter inputs
        lblRevFrom.setVisible(false);
        txtRevFromYear.setVisible(false);
        lblRevTo.setVisible(false);
        txtRevToYear.setVisible(false);
        lblRevYear.setVisible(false);
        txtRevYear.setVisible(false);
    }

    private JComponent buildTopTabs() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(backgroundColor);

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 0));
        tabs.setBackground(Color.white);
        tabs.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, cardBorder),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        ButtonGroup bg = new ButtonGroup();
        JToggleButton t1 = tab("Tổng quan");
        JToggleButton t2 = tab("Tồn kho");
        JToggleButton t3 = tab("Doanh thu");
        JToggleButton t4 = tab("Nhà cung cấp");
        JToggleButton t5 = tab("Khách hàng");
        bg.add(t1);
        bg.add(t2);
        bg.add(t3);
        bg.add(t4);
        bg.add(t5);
        t1.setSelected(true);

        t1.addActionListener(e -> cardsLayout.show(cards, "overview"));
        t2.addActionListener(e -> {
            cardsLayout.show(cards, "inventory");
            if (!inventoryLoadedOnce) {
                inventoryLoadedOnce = true;
                // sensible defaults
                if (dpInvTo.getDate() == null) dpInvTo.setDate(LocalDate.now());
                if (dpInvFrom.getDate() == null) dpInvFrom.setDate(LocalDate.now().minusDays(7));
                reloadInventory();
            }
        });
        t3.addActionListener(e -> cardsLayout.show(cards, "revenue"));
        t4.addActionListener(e -> {
            cardsLayout.show(cards, "supplier");
            supplierLoadedOnce = true;
            suppressSupplierDateReload = true;
            try {
                if (dpSupTo.getDate() == null) dpSupTo.setDate(LocalDate.now());
                if (dpSupFrom.getDate() == null) dpSupFrom.setDate(LocalDate.now().minusDays(30));
            } finally {
                suppressSupplierDateReload = false;
            }
            reloadSupplier();
        });
        t5.addActionListener(e -> {
            cardsLayout.show(cards, "customer");
            customerLoadedOnce = true;
            suppressCustomerDateReload = true;
            try {
                if (dpCusTo.getDate() == null) dpCusTo.setDate(LocalDate.now());
                if (dpCusFrom.getDate() == null) dpCusFrom.setDate(LocalDate.now().minusDays(30));
            } finally {
                suppressCustomerDateReload = false;
            }
            reloadCustomer();
        });

        // Keep UX minimal: tabs are visual like screenshot.
        tabs.add(t1);
        tabs.add(t2);
        tabs.add(t3);
        tabs.add(t4);
        tabs.add(t5);

        wrap.add(tabs, BorderLayout.CENTER);
        return wrap;
    }

    private JComponent buildCustomerBody() {
        JPanel body = new JPanel(new BorderLayout(12, 12));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(12, 10, 10, 10));

        JPanel left = new JPanel();
        left.setBackground(Color.white);
        left.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(cardBorder),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        left.setPreferredSize(new Dimension(280, 0));
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        left.add(fieldBlock("Tìm kiếm khách hàng", txtCusSearch));
        left.add(Box.createVerticalStrut(10));
        left.add(fieldBlock("Từ ngày", dpCusFrom));
        left.add(Box.createVerticalStrut(10));
        left.add(fieldBlock("Đến ngày", dpCusTo));
        left.add(Box.createVerticalStrut(14));

        JPanel btns = new JPanel(new GridLayout(1, 1, 10, 0));
        btns.setOpaque(false);
        btns.add(btnCusRefresh);
        left.add(btns);

        btnCusRefresh.addActionListener(e -> reloadCustomer());
        txtCusSearch.addActionListener(e -> reloadCustomer());

        DateChangeListener dl = e -> {
            if (!suppressCustomerDateReload) reloadCustomer();
        };
        dpCusFrom.addDateChangeListener(dl);
        dpCusTo.addDateChangeListener(dl);

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Color.white);
        right.setBorder(BorderFactory.createLineBorder(cardBorder));

        JScrollPane sp = new JScrollPane(tblCus);
        sp.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        right.add(sp, BorderLayout.CENTER);

        body.add(left, BorderLayout.WEST);
        body.add(right, BorderLayout.CENTER);
        return body;
    }

    private void reloadCustomer() {
        LocalDate from = dpCusFrom.getDate();
        LocalDate to = dpCusTo.getDate();
        String kw = txtCusSearch.getText();

        new SwingWorker<List<StatisticsController.CustomerExportRow>, Void>() {
            @Override
            protected List<StatisticsController.CustomerExportRow> doInBackground() {
                return statisticsController.customerExportSummary(from, to, kw);
            }

            @Override
            protected void done() {
                try {
                    cusModel.setRows(get());
                } catch (Exception e) {
                    cusModel.setRows(new ArrayList<>());
                    Toast.error(StatisticsPanel.this, e.getMessage() == null ? e.toString() : e.getMessage());
                }
            }
        }.execute();
    }

    private void exportCustomer() {
        String[] headers = {"stt", "ma_kh", "ten_kh", "so_luong_phieu", "tong_so_tien"};
        List<String[]> data = new ArrayList<>();
        List<StatisticsController.CustomerExportRow> rows = cusModel.getRows();
        for (int i = 0; i < rows.size(); i++) {
            var r = rows.get(i);
            data.add(new String[] {
                String.valueOf(i + 1),
                String.valueOf(r.getCustomerId()),
                r.getCustomerName() == null ? "" : r.getCustomerName(),
                String.valueOf(r.getReceiptCount()),
                String.valueOf(Math.round(r.getTotalAmount()))
            });
        }
        // Excel export removed
    }

    private JComponent buildSupplierBody() {
        JPanel body = new JPanel(new BorderLayout(12, 12));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(12, 10, 10, 10));

        JPanel left = new JPanel();
        left.setBackground(Color.white);
        left.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(cardBorder),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        left.setPreferredSize(new Dimension(280, 0));
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        left.add(fieldBlock("Tìm kiếm nhà cung cấp", txtSupSearch));
        left.add(Box.createVerticalStrut(10));
        left.add(fieldBlock("Từ ngày", dpSupFrom));
        left.add(Box.createVerticalStrut(10));
        left.add(fieldBlock("Đến ngày", dpSupTo));
        left.add(Box.createVerticalStrut(14));

        JPanel btns = new JPanel(new GridLayout(1, 1, 10, 0));
        btns.setOpaque(false);
        btns.add(btnSupRefresh);
        left.add(btns);

        btnSupRefresh.addActionListener(e -> reloadSupplier());
        txtSupSearch.addActionListener(e -> reloadSupplier());

        DateChangeListener dl = e -> {
            if (!suppressSupplierDateReload) reloadSupplier();
        };
        dpSupFrom.addDateChangeListener(dl);
        dpSupTo.addDateChangeListener(dl);

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Color.white);
        right.setBorder(BorderFactory.createLineBorder(cardBorder));

        JScrollPane sp = new JScrollPane(tblSup);
        sp.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        right.add(sp, BorderLayout.CENTER);

        body.add(left, BorderLayout.WEST);
        body.add(right, BorderLayout.CENTER);
        return body;
    }

    private void reloadSupplier() {
        LocalDate from = dpSupFrom.getDate();
        LocalDate to = dpSupTo.getDate();
        String kw = txtSupSearch.getText();

        new SwingWorker<List<StatisticsController.SupplierImportRow>, Void>() {
            @Override
            protected List<StatisticsController.SupplierImportRow> doInBackground() {
                return statisticsController.supplierImportSummary(from, to, kw);
            }

            @Override
            protected void done() {
                try {
                    supModel.setRows(get());
                } catch (Exception e) {
                    supModel.setRows(new ArrayList<>());
                    Toast.error(StatisticsPanel.this, e.getMessage() == null ? e.toString() : e.getMessage());
                }
            }
        }.execute();
    }

    private void exportSupplier() {
        String[] headers = {"stt", "ma_ncc", "ten_ncc", "so_luong_nhap", "tong_so_tien"};
        List<String[]> data = new ArrayList<>();
        List<StatisticsController.SupplierImportRow> rows = supModel.getRows();
        for (int i = 0; i < rows.size(); i++) {
            var r = rows.get(i);
            data.add(new String[] {
                String.valueOf(i + 1),
                String.valueOf(r.getSupplierId()),
                r.getSupplierName() == null ? "" : r.getSupplierName(),
                String.valueOf(r.getImportCount()),
                String.valueOf(Math.round(r.getTotalAmount()))
            });
        }
        // Excel export removed
    }

    private JComponent buildRevenueBody() {
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(12, 10, 10, 10));

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.white);
        card.setBorder(BorderFactory.createLineBorder(cardBorder));

        JPanel head = new JPanel(new BorderLayout(0, 8));
        head.setOpaque(false);
        head.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        head.add(buildRevenueSubTabs(), BorderLayout.NORTH);
        head.add(buildRevenueYearFilterBar(), BorderLayout.CENTER);

        card.add(head, BorderLayout.NORTH);

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(0, 12, 10, 12));
        revYearChart.setPreferredSize(new Dimension(0, 340));
        top.add(revYearChart, BorderLayout.CENTER);
        top.add(buildLegend(), BorderLayout.SOUTH);
        card.add(top, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);

        JSeparator sep = new JSeparator();
        sep.setForeground(cardBorder);
        bottom.add(sep, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(tblRevYear);
        sp.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        sp.setPreferredSize(new Dimension(0, 240));
        bottom.add(sp, BorderLayout.CENTER);

        card.add(bottom, BorderLayout.SOUTH);

        body.add(card, BorderLayout.CENTER);
        wireRevenueYearActions();
        return body;
    }

    private JComponent buildRevenueSubTabs() {
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        tabs.setOpaque(false);

        ButtonGroup bg = new ButtonGroup();
        JToggleButton y = subTab("Thống kê theo năm");
        JToggleButton m = subTab("Thống kê từng tháng trong năm");
        bg.add(y);
        bg.add(m);
        y.setSelected(true);

        // Only year and month views required. Wire actions to reload appropriate series.
        y.addActionListener(e -> {
            // year view: label prefix "Năm"
            revYearModel.setLabelPrefix("Năm");
            revYearChart.setXLabelPrefix("Năm ");
            // update filter labels
            lblRevFrom.setText("Từ năm");
            lblRevTo.setText("Đến năm");
            // hide filter inputs for full-year overview
            lblRevFrom.setVisible(false);
            txtRevFromYear.setVisible(false);
            lblRevTo.setVisible(false);
            txtRevToYear.setVisible(false);
            lblRevYear.setVisible(false);
            txtRevYear.setVisible(false);
            reloadRevenueByYear(null, null);
            revalidate();
            repaint();
        });
        m.addActionListener(e -> {
            // month view: label prefix "Tháng" and update filter labels
            revYearModel.setLabelPrefix("Tháng");
            revYearChart.setXLabelPrefix("Tháng ");
            lblRevFrom.setText("Từ tháng");
            lblRevTo.setText("Đến tháng");
            // show filter inputs for month-range queries
            lblRevFrom.setVisible(true);
            txtRevFromYear.setVisible(true);
            lblRevTo.setVisible(true);
            txtRevToYear.setVisible(true);
            lblRevYear.setVisible(true);
            txtRevYear.setVisible(true);
            String ytxt = txtRevYear.getText();
            reloadRevenueByMonth(ytxt);
            revalidate();
            repaint();
        });

        tabs.add(y);
        tabs.add(m);
        return tabs;
    }

    private JToggleButton subTab(String text) {
        JToggleButton b = new JToggleButton(text);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setForeground(new Color(69, 90, 100));
        b.setFont(b.getFont().deriveFont(Font.PLAIN, 12.5f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addChangeListener(e -> {
            if (b.isSelected()) {
                b.setFont(b.getFont().deriveFont(Font.BOLD, 12.5f));
                b.setForeground(textPrimary);
            } else {
                b.setFont(b.getFont().deriveFont(Font.PLAIN, 12.5f));
                b.setForeground(new Color(69, 90, 100));
            }
        });
        return b;
    }

    private JComponent buildRevenueYearFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        bar.setOpaque(false);
        lblRevFrom.setForeground(textSubtle);
        lblRevTo.setForeground(textSubtle);

        txtRevFromYear.setPreferredSize(new Dimension(70, 28));
        txtRevToYear.setPreferredSize(new Dimension(70, 28));
        txtRevYear.setPreferredSize(new Dimension(70, 28));

        bar.add(lblRevFrom);
        bar.add(txtRevFromYear);
        bar.add(lblRevTo);
        bar.add(txtRevToYear);
        bar.add(lblRevYear);
        bar.add(txtRevYear);
        bar.add(btnRevStat);
        bar.add(btnRevRefresh);

        return bar;
    }

    private void wireRevenueYearActions() {
        // Ensure listeners only added once
        if (btnRevStat.getActionListeners().length == 0) {
                btnRevStat.addActionListener(e -> {
                    // If month tab is selected, treat as month query (single year)
                    btnRevStat.setEnabled(false);
                    try {
                                // decide based on revYearModel label prefix
                                if (revYearModel.getLabelPrefix() != null && revYearModel.getLabelPrefix().equalsIgnoreCase("Tháng")) {
                                    // use year from txtRevYear and months from txtRevFromYear/txtRevToYear
                                    reloadRevenueByMonth(txtRevYear.getText());
                                } else {
                                    reloadRevenueByYear(txtRevFromYear.getText(), txtRevToYear.getText());
                                }
                    } finally {
                        btnRevStat.setEnabled(true);
                    }
                });
                // Excel export removed: export button and actions disabled
                btnRevRefresh.addActionListener(e -> {
                    txtRevFromYear.setText("");
                    txtRevToYear.setText("");
                    // refresh depending on mode
                    if (revYearModel.getLabelPrefix() != null && revYearModel.getLabelPrefix().equalsIgnoreCase("Tháng")) {
                        reloadRevenueByMonth(null);
                    } else {
                        reloadRevenueByYear(null, null);
                    }
                });
            txtRevFromYear.addActionListener(e -> {
                if (revYearModel.getLabelPrefix() != null && revYearModel.getLabelPrefix().equalsIgnoreCase("Tháng")) {
                    reloadRevenueByMonth(txtRevYear.getText());
                } else {
                    reloadRevenueByYear(txtRevFromYear.getText(), txtRevToYear.getText());
                }
            });
            txtRevYear.addActionListener(e -> {
                if (revYearModel.getLabelPrefix() != null && revYearModel.getLabelPrefix().equalsIgnoreCase("Tháng")) {
                    reloadRevenueByMonth(txtRevYear.getText());
                }
            });
            txtRevToYear.addActionListener(e -> reloadRevenueByYear(txtRevFromYear.getText(), txtRevToYear.getText()));
        }
    }

    private void reloadRevenueByYear(String fromYearText, String toYearText) {
        Integer fromY = parseYearOrNull(fromYearText);
        Integer toY = parseYearOrNull(toYearText);
        if (fromYearText != null && !fromYearText.isBlank() && fromY == null) {
            Toast.error(this, "Từ năm không hợp lệ");
            return;
        }
        if (toYearText != null && !toYearText.isBlank() && toY == null) {
            Toast.error(this, "Đến năm không hợp lệ");
            return;
        }

        new SwingWorker<List<StatisticsController.RevenueYear>, Void>() {
            @Override
            protected List<StatisticsController.RevenueYear> doInBackground() {
                return statisticsController.revenueByYear(fromY, toY);
            }

            @Override
            protected void done() {
                try {
                    List<StatisticsController.RevenueYear> rows = get();
                    // If we're in month mode (label prefix == "Tháng"), apply month range filter;
                    // otherwise (year mode) show all returned rows (years).
                    if (revYearModel.getLabelPrefix() != null && revYearModel.getLabelPrefix().equalsIgnoreCase("Tháng")) {
                        List<StatisticsController.RevenueYear> filtered = new ArrayList<>();
                        int from = parseMonthOrNull(txtRevFromYear.getText(), 1);
                        int to = parseMonthOrNull(txtRevToYear.getText(), 12);
                        if (from < 1) from = 1;
                        if (to < 1) to = 12;
                        if (from > to) { int t = from; from = to; to = t; }
                        for (StatisticsController.RevenueYear r : rows) {
                            if (r == null) continue;
                            int m = r.getYear();
                            if (m >= from && m <= to) filtered.add(r);
                        }
                        revYearModel.setRows(filtered);
                        revYearChart.setSeries(filtered);
                    } else {
                        revYearModel.setRows(rows);
                        revYearChart.setSeries(rows);
                    }
                } catch (Exception e) {
                    revYearModel.setRows(new ArrayList<>());
                    revYearChart.setSeries(new ArrayList<>());
                    Toast.error(StatisticsPanel.this, e.getMessage() == null ? e.toString() : e.getMessage());
                }
            }
        }.execute();
    }

    private void reloadRevenueByMonth(String yearText) {
        Integer y = null;
        if (yearText != null && !yearText.isBlank()) {
            try { y = Integer.parseInt(yearText.trim()); } catch (Exception e) { y = null; }
        }
        final Integer fy = y == null ? LocalDate.now().getYear() : y;
        new SwingWorker<List<StatisticsController.RevenueYear>, Void>() {
            @Override
            protected List<StatisticsController.RevenueYear> doInBackground() {
                return statisticsController.revenueByMonth(fy);
            }

            @Override
            protected void done() {
                try {
                    List<StatisticsController.RevenueYear> rows = get();
                    // apply optional month range filter via txtRevFromYear/txtRevToYear
                    List<StatisticsController.RevenueYear> filtered = new ArrayList<>();
                    int from = parseMonthOrNull(txtRevFromYear.getText(), 1);
                    int to = parseMonthOrNull(txtRevToYear.getText(), 12);
                    if (from < 1) from = 1;
                    if (to < 1) to = 12;
                    if (from > to) { int t = from; from = to; to = t; }
                    for (StatisticsController.RevenueYear r : rows) {
                        if (r == null) continue;
                        int m = r.getYear();
                        if (m >= from && m <= to) filtered.add(r);
                    }
                    revYearModel.setRows(filtered);
                    revYearChart.setSeries(filtered);
                } catch (Exception e) {
                    revYearModel.setRows(new ArrayList<>());
                    revYearChart.setSeries(new ArrayList<>());
                    Toast.error(StatisticsPanel.this, e.getMessage() == null ? e.toString() : e.getMessage());
                }
            }
        }.execute();
    }

    private int parseMonthOrNull(String s, int def) {
        if (s == null) return def;
        String t = s.trim();
        if (t.isBlank()) return def;
        try {
            int v = Integer.parseInt(t);
            if (v < 1 || v > 12) return def;
            return v;
        } catch (Exception e) {
            return def;
        }
    }

    private Integer parseYearOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        try {
            int y = Integer.parseInt(t);
            if (y < 1900 || y > 3000) return null;
            return y;
        } catch (Exception e) {
            return null;
        }
    }

    private void exportRevenueByYear() {
        String[] headers = {"nam", "von", "doanh_thu", "loi_nhuan"};
        List<String[]> data = new ArrayList<>();
        List<StatisticsController.RevenueYear> rows = revYearModel.getRows();
        for (StatisticsController.RevenueYear r : rows) {
            long cost = Math.round(r.getCost());
            long rev = Math.round(r.getRevenue());
            long prof = Math.round(r.getProfit());
            data.add(new String[] {
                String.valueOf(r.getYear()),
                String.valueOf(cost),
                String.valueOf(rev),
                String.valueOf(prof)
            });
        }
        // Excel export removed
    }

    private void exportRevenueByMonth() {
        String[] headers = {"thang", "von", "doanh_thu", "loi_nhuan"};
        List<String[]> data = new ArrayList<>();
        List<StatisticsController.RevenueYear> rows = revYearModel.getRows();
        // rows already reflect any month-range filtering
        for (StatisticsController.RevenueYear r : rows) {
            long cost = Math.round(r.getCost());
            long rev = Math.round(r.getRevenue());
            long prof = Math.round(r.getProfit());
            data.add(new String[] {
                String.valueOf(r.getYear()),
                String.valueOf(cost),
                String.valueOf(rev),
                String.valueOf(prof)
            });
        }
        // Excel export removed
    }

    private JToggleButton tab(String text) {
        JToggleButton b = new JToggleButton(text);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setForeground(new Color(69, 90, 100));
        b.setFont(b.getFont().deriveFont(Font.PLAIN, 13f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addChangeListener(e -> {
            if (b.isSelected()) {
                b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
                b.setForeground(textPrimary);
            } else {
                b.setFont(b.getFont().deriveFont(Font.PLAIN, 13f));
                b.setForeground(new Color(69, 90, 100));
            }
        });
        return b;
    }

    private JComponent buildOverviewBody() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BorderLayout(0, 12));
        body.setBorder(BorderFactory.createEmptyBorder(12, 10, 10, 10));

        body.add(buildCardsRow(), BorderLayout.NORTH);
        body.add(buildChartAndTableCard(), BorderLayout.CENTER);
        return body;
    }

    private JComponent buildInventoryBody() {
        JPanel body = new JPanel(new BorderLayout(12, 12));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(12, 10, 10, 10));

        JPanel left = new JPanel();
        left.setBackground(Color.white);
        left.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(cardBorder),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        left.setPreferredSize(new Dimension(280, 0));
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Bộ lọc");
        title.setForeground(textPrimary);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        left.add(title);
        left.add(Box.createVerticalStrut(10));

        left.add(fieldBlock("Tìm sản phẩm", txtInvSearch));
        left.add(Box.createVerticalStrut(10));

        left.add(fieldBlock("Từ ngày", dpInvFrom));
        left.add(Box.createVerticalStrut(10));
        left.add(fieldBlock("Đến ngày", dpInvTo));
        left.add(Box.createVerticalStrut(14));

        JPanel btns = new JPanel(new GridLayout(1, 1, 10, 0));
        btns.setOpaque(false);
        btns.add(btnInvRefresh);
        left.add(btns);

        btnInvRefresh.addActionListener(e -> reloadInventory());
        txtInvSearch.addActionListener(e -> reloadInventory());

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Color.white);
        right.setBorder(BorderFactory.createLineBorder(cardBorder));

        JLabel rtitle = new JLabel("Báo cáo tồn kho");
        rtitle.setBorder(BorderFactory.createEmptyBorder(12, 12, 10, 12));
        rtitle.setForeground(textPrimary);
        rtitle.setFont(rtitle.getFont().deriveFont(Font.PLAIN, 16f));
        right.add(rtitle, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(tblInv);
        sp.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        right.add(sp, BorderLayout.CENTER);

        body.add(left, BorderLayout.WEST);
        body.add(right, BorderLayout.CENTER);
        return body;
    }

    private JComponent fieldBlock(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setForeground(textSubtle);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        if (field instanceof JTextField tf) {
            tf.setPreferredSize(new Dimension(0, 34));
        }
        return p;
    }

    private static DatePicker createDatePicker() {
        DatePickerSettings settings = new DatePickerSettings(new Locale("vi", "VN"));
        settings.setAllowKeyboardEditing(false);
        settings.setFormatForDatesCommonEra("yyyy-MM-dd");
        DatePicker dp = new DatePicker(settings);
        dp.setPreferredSize(new Dimension(0, 34));
        return dp;
    }

    private JComponent buildCardsRow() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 12, 12));
        panel.setOpaque(false);
        panel.add(metric("Sản phẩm hiện có trong kho", lblProductsInStock, "product.svg"));
        panel.add(metric("Khách từ trước đến nay", lblTotalCustomers, "customer.svg"));
        panel.add(metric("Nhân viên đang hoạt động", lblActiveEmployees, "staff.svg"));
        return panel;
    }

    private JComponent metric(String title, JLabel valueLabel, String icon) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setBackground(Color.white);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(cardBorder),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel ico = new JLabel(ImageLoader.loadIcon(icon, 40, 40));
        ico.setVerticalAlignment(SwingConstants.TOP);
        p.add(ico, BorderLayout.WEST);

        JPanel text = new JPanel(new BorderLayout(0, 6));
        text.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setForeground(textSubtle);
        text.add(t, BorderLayout.NORTH);
        text.add(valueLabel, BorderLayout.CENTER);
        p.add(text, BorderLayout.CENTER);
        return p;
    }

    private JComponent buildChartAndTableCard() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Color.white);
        outer.setBorder(BorderFactory.createLineBorder(cardBorder));

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(14, 14, 10, 14));

        JLabel title = new JLabel("Thống kê doanh thu 8 ngày gần nhất");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setForeground(textPrimary);
        title.setFont(title.getFont().deriveFont(Font.PLAIN, 16f));
        top.add(title, BorderLayout.NORTH);

        chartPanel.setPreferredSize(new Dimension(0, 320));
        top.add(chartPanel, BorderLayout.CENTER);
        top.add(buildLegend(), BorderLayout.SOUTH);

        outer.add(top, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);

        JSeparator sep = new JSeparator();
        sep.setForeground(cardBorder);
        bottom.add(sep, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        sp.setPreferredSize(new Dimension(0, 230));
        bottom.add(sp, BorderLayout.CENTER);

        outer.add(bottom, BorderLayout.CENTER);

        return outer;
    }

    private JComponent buildLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 4));
        legend.setOpaque(false);
        legend.add(legendItem(seriesCost, "Vốn"));
        legend.add(legendItem(seriesRevenue, "Doanh thu"));
        legend.add(legendItem(seriesProfit, "Lợi nhuận"));
        return legend;
    }

    private JComponent legendItem(Color color, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        JLabel dot = new JLabel("\u25CF");
        dot.setForeground(color);
        JLabel t = new JLabel(text);
        t.setForeground(textSubtle);
        p.add(dot);
        p.add(t);
        return p;
    }

    private void configureMetricLabel(JLabel lbl) {
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 24f));
        lbl.setForeground(textPrimary);
    }

    private void configureRevenueTable(JTable t) {
        t.setFillsViewportHeight(true);
        t.setRowHeight(34);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setDefaultEditor(Object.class, null);
        if (t.getTableHeader() != null) {
            t.getTableHeader().setReorderingAllowed(false);
            t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD, 12f));
        }
    }

    private void configureInventoryTable(JTable t) {
        t.setFillsViewportHeight(true);
        t.setRowHeight(34);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setDefaultEditor(Object.class, null);
        if (t.getTableHeader() != null) {
            t.getTableHeader().setReorderingAllowed(false);
            t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD, 12f));
        }
    }

    private void reloadInventory() {
        LocalDate from = dpInvFrom.getDate();
        LocalDate to = dpInvTo.getDate();
        String kw = txtInvSearch.getText();

        new SwingWorker<List<StatisticsController.InventoryRow>, Void>() {
            @Override
            protected List<StatisticsController.InventoryRow> doInBackground() {
                return statisticsController.inventoryMovement(from, to, kw);
            }

            @Override
            protected void done() {
                try {
                    invModel.setRows(get());
                } catch (Exception e) {
                    invModel.setRows(new ArrayList<>());
                    Toast.error(StatisticsPanel.this, e.getMessage() == null ? e.toString() : e.getMessage());
                }
            }
        }.execute();
    }

    private void exportInventory() {
        String[] headers = {"stt", "masp", "tensp", "ton_dau_ky", "nhap_trong_ky", "xuat_trong_ky", "ton_cuoi_ky"};
        List<String[]> data = new ArrayList<>();
        List<StatisticsController.InventoryRow> rows = invModel.getRows();
        for (int i = 0; i < rows.size(); i++) {
            StatisticsController.InventoryRow r = rows.get(i);
            data.add(new String[] {
                String.valueOf(i + 1),
                String.valueOf(r.getProductId()),
                r.getProductName() == null ? "" : r.getProductName(),
                String.valueOf(r.getOpening()),
                String.valueOf(r.getInPeriod()),
                String.valueOf(r.getOutPeriod()),
                String.valueOf(r.getClosing())
            });
        }
        // Excel export removed
    }

    private JComponent buildPadding(String pos) {
        JPanel p = new JPanel();
        p.setBackground(backgroundColor);
        return switch (pos) {
            case BorderLayout.WEST, BorderLayout.EAST -> {
                p.setPreferredSize(new Dimension(10, 0));
                yield p;
            }
            case BorderLayout.SOUTH -> {
                p.setPreferredSize(new Dimension(0, 10));
                yield p;
            }
            default -> p;
        };
    }

    private void reload() {
        // Avoid blocking UI thread if JDBC is slow.
        new SwingWorker<ReloadData, Void>() {
            @Override
            protected ReloadData doInBackground() {
                StatisticsController.OverviewSnapshot o = statisticsController.overview();
                List<StatisticsController.RevenueDay> days = statisticsController.revenueLastDays(8);
                return new ReloadData(o, days);
            }

            @Override
            protected void done() {
                try {
                    ReloadData d = get();
                    if (d.overview != null) {
                        lblProductsInStock.setText(String.valueOf(d.overview.getProductsInStock()));
                        lblTotalCustomers.setText(String.valueOf(d.overview.getTotalCustomers()));
                        lblActiveEmployees.setText(String.valueOf(d.overview.getActiveEmployees()));
                    }

                    tableModel.setRows(d.days);
                    chartPanel.setSeries(d.days);
                } catch (Exception e) {
                    // Keep zero values if error
                }
            }
        }.execute();
    }

    private static final class ReloadData {
        private final StatisticsController.OverviewSnapshot overview;
        private final List<StatisticsController.RevenueDay> days;

        private ReloadData(StatisticsController.OverviewSnapshot overview, List<StatisticsController.RevenueDay> days) {
            this.overview = overview;
            this.days = days == null ? new ArrayList<>() : days;
        }
    }

    private static final class InventoryTableModel extends AbstractTableModel {
        private final String[] cols = {"STT", "Mã SP", "Tên sản phẩm", "Tồn đầu kỳ", "Nhập trong kỳ", "Xuất trong kỳ", "Tồn cuối kỳ"};
        private List<StatisticsController.InventoryRow> rows = new ArrayList<>();

        public void setRows(List<StatisticsController.InventoryRow> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public List<StatisticsController.InventoryRow> getRows() {
            return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            StatisticsController.InventoryRow r = rows.get(rowIndex);
            if (r == null) return "";
            return switch (columnIndex) {
                case 0 -> rowIndex + 1;
                case 1 -> r.getProductId();
                case 2 -> r.getProductName() == null ? "" : r.getProductName();
                case 3 -> r.getOpening();
                case 4 -> r.getInPeriod();
                case 5 -> r.getOutPeriod();
                case 6 -> r.getClosing();
                default -> "";
            };
        }
    }

    private static final class RevenueYearTableModel extends AbstractTableModel {
        private final String[] cols = {"Năm", "Vốn", "Doanh thu", "Lợi nhuận"};
        private String labelPrefix = "Năm";
        private List<StatisticsController.RevenueYear> rows = new ArrayList<>();

        public void setRows(List<StatisticsController.RevenueYear> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public void setLabelPrefix(String p) {
            this.labelPrefix = p == null ? "" : p;
            fireTableStructureChanged();
        }

        public String getLabelPrefix() { return labelPrefix; }

        public List<StatisticsController.RevenueYear> getRows() {
            return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return column == 0 ? labelPrefix : cols[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            StatisticsController.RevenueYear r = rows.get(rowIndex);
            if (r == null) return "";
            return switch (columnIndex) {
                case 0 -> r.getYear();
                case 1 -> MoneyVND.format(Math.round(r.getCost()));
                case 2 -> MoneyVND.format(Math.round(r.getRevenue()));
                case 3 -> MoneyVND.format(Math.round(Math.max(0d, r.getProfit())));
                default -> "";
            };
        }
    }

    private static final class SupplierImportTableModel extends AbstractTableModel {
        private final String[] cols = {"STT", "Mã nhà cung cấp", "Tên nhà cung cấp", "Số lượng nhập", "Tổng số tiền"};
        private List<StatisticsController.SupplierImportRow> rows = new ArrayList<>();

        public void setRows(List<StatisticsController.SupplierImportRow> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public List<StatisticsController.SupplierImportRow> getRows() {
            return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            StatisticsController.SupplierImportRow r = rows.get(rowIndex);
            if (r == null) return "";
            return switch (columnIndex) {
                case 0 -> rowIndex + 1;
                case 1 -> r.getSupplierId();
                case 2 -> r.getSupplierName() == null ? "" : r.getSupplierName();
                case 3 -> r.getImportCount();
                case 4 -> MoneyVND.format(Math.round(r.getTotalAmount()));
                default -> "";
            };
        }
    }

    private static final class CustomerExportTableModel extends AbstractTableModel {
        private final String[] cols = {"STT", "Mã khách hàng", "Tên khách hàng", "Số lượng phiếu", "Tổng số tiền"};
        private List<StatisticsController.CustomerExportRow> rows = new ArrayList<>();

        public void setRows(List<StatisticsController.CustomerExportRow> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public List<StatisticsController.CustomerExportRow> getRows() {
            return rows == null ? new ArrayList<>() : new ArrayList<>(rows);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            StatisticsController.CustomerExportRow r = rows.get(rowIndex);
            if (r == null) return "";
            return switch (columnIndex) {
                case 0 -> rowIndex + 1;
                case 1 -> r.getCustomerId();
                case 2 -> r.getCustomerName() == null ? "" : r.getCustomerName();
                case 3 -> r.getReceiptCount();
                case 4 -> MoneyVND.format(Math.round(r.getTotalAmount()));
                default -> "";
            };
        }
    }

    private final class RevenueTableModel extends AbstractTableModel {

        private final String[] cols = {"Ngày", "Vốn", "Doanh thu", "Lợi nhuận"};
        private List<StatisticsController.RevenueDay> rows = new ArrayList<>();

        public void setRows(List<StatisticsController.RevenueDay> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int column) {
            return cols[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            StatisticsController.RevenueDay r = rows.get(rowIndex);
            if (r == null) return "";
            return switch (columnIndex) {
                case 0 -> r.getDate() == null ? "" : dateFmt.format(r.getDate());
                case 1 -> MoneyVND.format(Math.round(r.getCost()));
                case 2 -> MoneyVND.format(Math.round(r.getRevenue()));
                case 3 -> MoneyVND.format(Math.round(r.getProfit()));
                default -> "";
            };
        }
    }

    private final class RevenueChartPanel extends JPanel {

        private List<StatisticsController.RevenueDay> days = new ArrayList<>();

        private RevenueChartPanel() {
            setOpaque(false);
        }

        public void setSeries(List<StatisticsController.RevenueDay> days) {
            this.days = days == null ? new ArrayList<>() : new ArrayList<>(days);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                if (w <= 20 || h <= 20) return;

                int padRight = 18;
                int padTop = 10;
                int padBottom = 44;

                g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
                FontMetrics fm = g2.getFontMetrics();

                // Find Y range
                double min = 0d;
                double max = 0d;
                boolean seen = false;
                for (var d : days) {
                    if (d == null) continue;
                    double c = d.getCost();
                    double r = d.getRevenue();
                    double p = d.getProfit();
                    if (!seen) {
                        min = Math.min(c, Math.min(r, p));
                        max = Math.max(c, Math.max(r, p));
                        seen = true;
                    } else {
                        min = Math.min(min, Math.min(c, Math.min(r, p)));
                        max = Math.max(max, Math.max(c, Math.max(r, p)));
                    }
                }
                if (!seen) {
                    min = 0d;
                    max = 1d;
                }
                if (max == min) {
                    if (max == 0d) {
                        max = 1d;
                    } else {
                        max = max * 1.1d;
                        min = min * 0.9d;
                    }
                }

                // Nice ticks
                int grid = 5;
                double range = max - min;
                if (range <= 0d) range = 1d;
                double step = niceStep(range / grid);
                double yMax = Math.ceil(max / step) * step;
                double yMin = Math.floor(min / step) * step;
                if (yMax == yMin) yMax = yMin + step;

                // Compute left padding based on widest label
                int maxLabelW = 0;
                for (int i = 0; i <= grid; i++) {
                    double v = yMax - i * ((yMax - yMin) / grid);
                    String label = formatAxisVnd(v);
                    maxLabelW = Math.max(maxLabelW, fm.stringWidth(label));
                }
                int padLeft = Math.max(56, maxLabelW + 16);

                int cx = padLeft;
                int cy = padTop;
                int cw = Math.max(1, w - padLeft - padRight);
                int ch = Math.max(1, h - padTop - padBottom);

                // Background
                g2.setColor(Color.white);
                g2.fillRect(0, 0, w, h);

                // Grid + axis
                g2.setColor(new Color(236, 239, 241));
                for (int i = 0; i <= grid; i++) {
                    int y = cy + (int) Math.round((ch * (i / (double) grid)));
                    g2.drawLine(cx, y, cx + cw, y);
                }

                g2.setColor(new Color(180, 180, 180));
                g2.drawLine(cx, cy, cx, cy + ch);
                g2.drawLine(cx, cy + ch, cx + cw, cy + ch);

                // Y labels
                g2.setColor(new Color(120, 144, 156));
                for (int i = 0; i <= grid; i++) {
                    double v = yMax - i * ((yMax - yMin) / grid);
                    String label = formatAxisVnd(v);
                    int y = cy + (int) Math.round(ch * (i / (double) grid));
                    int x = Math.max(6, cx - 10 - fm.stringWidth(label));
                    g2.drawString(label, x, y + 4);
                }

                if (days == null || days.size() < 2) return;
                int n = days.size();

                // X labels (dates)
                g2.setColor(new Color(144, 164, 174));
                for (int i = 0; i < n; i++) {
                    StatisticsController.RevenueDay d = days.get(i);
                    LocalDate dt = d == null ? null : d.getDate();
                    String label = dt == null ? "" : dateFmt.format(dt);
                    int x = cx + (int) Math.round(i * (cw / (double) (n - 1)));
                    if (i == 0 || i == n - 1 || n <= 8) {
                        int sw = fm.stringWidth(label);
                        g2.drawString(label, Math.max(0, x - (sw / 2)), cy + ch + 22);
                    }
                }

                // Series lines
                drawSeries(g2, cx, cy, cw, ch, yMin, yMax, days, seriesCost, s -> s.getCost());
                drawSeries(g2, cx, cy, cw, ch, yMin, yMax, days, seriesRevenue, s -> s.getRevenue());
                drawSeries(g2, cx, cy, cw, ch, yMin, yMax, days, seriesProfit, s -> s.getProfit());
            } finally {
                g2.dispose();
            }
        }

        private double niceStep(double rawStep) {
            if (rawStep <= 0d) return 1d;
            double exp = Math.floor(Math.log10(rawStep));
            double base = Math.pow(10, exp);
            double f = rawStep / base;
            double nf;
            if (f <= 1d) nf = 1d;
            else if (f <= 2d) nf = 2d;
            else if (f <= 5d) nf = 5d;
            else nf = 10d;
            return nf * base;
        }

        private String formatAxisVnd(double value) {
            double v = value;
            boolean neg = v < 0;
            v = Math.abs(v);

            String s;
            if (v >= 1_000_000_000d) {
                s = fmt1(v / 1_000_000_000d) + " tỷ";
            } else if (v >= 1_000_000d) {
                s = fmt1(v / 1_000_000d) + " tr";
            } else if (v >= 1_000d) {
                s = fmt1(v / 1_000d) + " k";
            } else {
                s = String.valueOf(Math.round(v)) + " đ";
            }
            return neg ? ("-" + s) : s;
        }

        private String fmt1(double x) {
            long r = Math.round(x * 10d);
            double one = r / 10d;
            if (Math.abs(one - Math.round(one)) < 1e-9) {
                return String.valueOf((long) Math.round(one));
            }
            // use dot for compact display
            return String.valueOf(one);
        }

        private interface ValueFn {
            double get(StatisticsController.RevenueDay d);
        }

        private void drawSeries(
                Graphics2D g2,
                int cx,
                int cy,
                int cw,
                int ch,
                double min,
                double max,
                List<StatisticsController.RevenueDay> days,
                Color color,
                ValueFn fn
        ) {
            int n = days.size();
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f));
            int prevX = -1, prevY = -1;
            double denom = (max - min);
            if (denom == 0d) denom = 1d;
            for (int i = 0; i < n; i++) {
                StatisticsController.RevenueDay d = days.get(i);
                double v = d == null ? 0d : fn.get(d);
                int x = cx + (int) Math.round(i * (cw / (double) (n - 1)));
                double t = (v - min) / denom;
                int y = cy + (int) Math.round((1d - t) * ch);
                if (prevX >= 0) g2.drawLine(prevX, prevY, x, y);
                prevX = x;
                prevY = y;
            }
        }
    }

    private final class RevenueYearChartPanel extends JPanel {

        private List<StatisticsController.RevenueYear> rows = new ArrayList<>();
        private String xLabelPrefix = "Năm ";

        private RevenueYearChartPanel() {
            setOpaque(false);
        }

        public void setSeries(List<StatisticsController.RevenueYear> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            repaint();
        }

        public void setXLabelPrefix(String p) {
            this.xLabelPrefix = p == null ? "" : p;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                if (w <= 20 || h <= 20) return;

                int padRight = 18;
                int padTop = 10;
                int padBottom = 38;

                g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
                FontMetrics fm = g2.getFontMetrics();

                // Determine y range across cost/revenue/profit
                double min = 0d;
                double max = 0d;
                boolean seen = false;
                for (var r : rows) {
                    if (r == null) continue;
                    double c = Math.max(0d, r.getCost());
                    double rv = Math.max(0d, r.getRevenue());
                    double p = Math.max(0d, r.getProfit());
                    if (!seen) {
                        min = Math.min(c, Math.min(rv, p));
                        max = Math.max(c, Math.max(rv, p));
                        seen = true;
                    } else {
                        min = Math.min(min, Math.min(c, Math.min(rv, p)));
                        max = Math.max(max, Math.max(c, Math.max(rv, p)));
                    }
                }
                if (!seen) {
                    min = 0d;
                    max = 1d;
                }

                // Always include baseline 0 to avoid weird bars for zero-years.
                if (min > 0d) min = 0d;
                if (max < 0d) max = 0d;
                if (max == min) {
                    if (max == 0d) max = 1d;
                    else {
                        max = max * 1.1d;
                        min = min * 0.9d;
                    }
                }

                int grid = 5;
                double range = max - min;
                if (range <= 0d) range = 1d;
                double step = niceStep(range / grid);
                double yMax = Math.ceil(max / step) * step;
                double yMin = Math.floor(min / step) * step;
                if (yMax == yMin) yMax = yMin + step;

                int maxLabelW = 0;
                for (int i = 0; i <= grid; i++) {
                    double v = yMax - i * ((yMax - yMin) / grid);
                    String label = formatAxisVnd(v);
                    maxLabelW = Math.max(maxLabelW, fm.stringWidth(label));
                }
                int padLeft = Math.max(56, maxLabelW + 16);

                int cx = padLeft;
                int cy = padTop;
                int cw = Math.max(1, w - padLeft - padRight);
                int ch = Math.max(1, h - padTop - padBottom);

                // Background
                g2.setColor(Color.white);
                g2.fillRect(0, 0, w, h);

                // Grid
                g2.setColor(new Color(236, 239, 241));
                for (int i = 0; i <= grid; i++) {
                    int y = cy + (int) Math.round((ch * (i / (double) grid)));
                    g2.drawLine(cx, y, cx + cw, y);
                }

                // Axis
                g2.setColor(new Color(180, 180, 180));
                g2.drawLine(cx, cy, cx, cy + ch);
                g2.drawLine(cx, cy + ch, cx + cw, cy + ch);

                // Y labels
                g2.setColor(new Color(120, 144, 156));
                for (int i = 0; i <= grid; i++) {
                    double v = yMax - i * ((yMax - yMin) / grid);
                    String label = formatAxisVnd(v);
                    int y = cy + (int) Math.round(ch * (i / (double) grid));
                    int x = Math.max(6, cx - 10 - fm.stringWidth(label));
                    g2.drawString(label, x, y + 4);
                }

                if (rows == null || rows.isEmpty()) return;
                int n = rows.size();

                // Baseline (for negative values)
                double denom = (yMax - yMin);
                if (denom == 0d) denom = 1d;
                double t0 = (0d - yMin) / denom;
                int y0 = cy + (int) Math.round((1d - t0) * ch);
                if (y0 < cy) y0 = cy;
                if (y0 > cy + ch) y0 = cy + ch;

                int groupW = Math.max(1, (int) Math.floor(cw / (double) n));
                int barW = Math.max(4, Math.min(14, (int) Math.floor(groupW / 4d)));
                int gap = Math.max(2, (int) Math.floor(barW / 2d));
                int totalBarsW = barW * 3 + gap * 2;

                for (int i = 0; i < n; i++) {
                    StatisticsController.RevenueYear r = rows.get(i);
                    int gx = cx + i * groupW + (groupW - totalBarsW) / 2;

                    paintBar(g2, gx, cy, ch, yMin, yMax, r == null ? 0d : Math.max(0d, r.getCost()), seriesCost, y0, barW);
                    paintBar(g2, gx + barW + gap, cy, ch, yMin, yMax, r == null ? 0d : Math.max(0d, r.getRevenue()), seriesRevenue, y0, barW);
                    paintBar(g2, gx + (barW + gap) * 2, cy, ch, yMin, yMax, r == null ? 0d : Math.max(0d, r.getProfit()), seriesProfit, y0, barW);

                    // X label
                    String label = r == null ? "" : (xLabelPrefix + r.getYear());
                    int centerX = cx + i * groupW + groupW / 2;
                    int sw = fm.stringWidth(label);
                    g2.setColor(new Color(144, 164, 174));
                    g2.drawString(label, Math.max(0, centerX - (sw / 2)), cy + ch + 22);
                }

            } finally {
                g2.dispose();
            }
        }

        private void paintBar(
                Graphics2D g2,
                int x,
                int cy,
                int ch,
                double yMin,
                double yMax,
                double value,
                Color color,
                int y0,
                int barW
        ) {
            if (Math.abs(value) < 1e-9) return;

            double denom = (yMax - yMin);
            if (denom == 0d) denom = 1d;
            double t = (value - yMin) / denom;
            int y = cy + (int) Math.round((1d - t) * ch);

            if (y < cy) y = cy;
            if (y > cy + ch) y = cy + ch;

            int top = Math.min(y0, y);
            int height = Math.abs(y - y0);
            if (height < 1) height = 1;

            g2.setColor(color);
            g2.fillRect(x, top, barW, height);
        }

        private double niceStep(double rawStep) {
            if (rawStep <= 0d) return 1d;
            double exp = Math.floor(Math.log10(rawStep));
            double base = Math.pow(10, exp);
            double f = rawStep / base;
            double nf;
            if (f <= 1d) nf = 1d;
            else if (f <= 2d) nf = 2d;
            else if (f <= 5d) nf = 5d;
            else nf = 10d;
            return nf * base;
        }

        private String formatAxisVnd(double value) {
            // Smart formatting: display in tỷ (billion) or triệu (million) when large.
            double v = Math.round(value);
            if (v <= 0) return "0";
            double abs = Math.abs(v);
            java.text.DecimalFormat df0 = new java.text.DecimalFormat("#,##0");
            java.text.DecimalFormat df1 = new java.text.DecimalFormat("#,##0.#");
            if (abs >= 1_000_000_000d) {
                double t = v / 1_000_000_000d;
                return df1.format(t) + " tỷ";
            }
            if (abs >= 1_000_000d) {
                double m = v / 1_000_000d;
                return df1.format(m) + " triệu";
            }
            return df0.format((long) v);
        }

        private String fmt1(double x) {
            long r = Math.round(x * 10d);
            double one = r / 10d;
            if (Math.abs(one - Math.round(one)) < 1e-9) {
                return String.valueOf((long) Math.round(one));
            }
            return String.valueOf(one);
        }
    }
}
