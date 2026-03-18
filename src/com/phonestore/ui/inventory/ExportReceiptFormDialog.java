package com.phonestore.ui.inventory;

import com.phonestore.controller.CustomerController;
import com.phonestore.controller.ExportReceiptController;
import com.phonestore.controller.ProductController;
import com.phonestore.controller.ProductVariantController;
import com.phonestore.dao.jdbc.ImeiRegistryJdbcDao;
import com.phonestore.model.Customer;
import com.phonestore.model.ExportReceipt;
import com.phonestore.model.ExportReceiptLine;
import com.phonestore.model.Product;
import com.phonestore.model.ProductVariant;
import com.phonestore.ui.common.session.SessionContext;
import com.phonestore.ui.common.session.UserSession;
import com.phonestore.util.invoice.ExportReceiptCodeUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;

public class ExportReceiptFormDialog extends JDialog {
    private final ProductTableModel productTableModel = new ProductTableModel();
    private final ItemsTableModel itemsTableModel = new ItemsTableModel();

    private final JTextField txtSearch = new JTextField();
    private final JTable tblProducts = new JTable();
    private final JButton btnAddProduct = new JButton("Th\u00eam s\u1ea3n ph\u1ea9m");
    private final JButton btnImportExcel = new JButton("Nh\u1eadp Excel");

    private final JTextField txtProductId = new JTextField();
    private final JTextField txtProductName = new JTextField();
    private final JComboBox<Product> cbProductList = new JComboBox<>();
    private final JComboBox<ProductVariant> cbVariant = new JComboBox<>();
    private final JTextField txtExportPrice = new JTextField();
    private final JTextField txtStock = new JTextField();
    private final JButton btnChooseImei = new JButton("Ch\u1ecdn IMEI");
    private final JButton btnScanImei = new JButton("Qu\u00e9t IMEI");
    private final JTextArea txtImeis = new JTextArea();

    private final JButton btnEditItem = new JButton("S\u1eeda s\u1ea3n ph\u1ea9m");
    private final JButton btnDeleteItem = new JButton("X\u00f3a s\u1ea3n ph\u1ea9m");
    private final JButton btnAddItem = new JButton("Thêm sản phẩm");
    private final JTable tblItems = new JTable();

    private final JTextField txtReceiptCode = new JTextField();
    private final JTextField txtEmployee = new JTextField();
    private final JComboBox<Customer> cbCustomer = new JComboBox<>();
    private final JButton btnChooseCustomer = new JButton("...");
    private final JTextArea txtCustomerDetail = new JTextArea();
    private final JLabel lblTotal = new JLabel("0 \u20ab");
    private final JButton btnSubmit = new JButton("Xu\u1ea5t h\u00e0ng");

    private ExportReceipt result;
    private final ExportReceipt editing;

    private final ProductController productController = new ProductController();
    private final ProductVariantController variantController = new ProductVariantController();
    private final CustomerController customerController = new CustomerController();
    private final ExportReceiptController exportReceiptController = new ExportReceiptController();
    private final ImeiRegistryJdbcDao imeiDao = new ImeiRegistryJdbcDao();

    private final List<Product> allProducts = new ArrayList<>();
    private UserSession currentSession;

    public ExportReceiptFormDialog(Window owner, ExportReceipt editing) {
        super(owner, "Phi\u1ebfu xu\u1ea5t", ModalityType.APPLICATION_MODAL);
        this.editing = editing;
        initUi();
        bindSessionEmployee();
        loadProducts();
        loadCustomers();
        wireEvents();

        txtReceiptCode.setText(ExportReceiptCodeUtil.build(LocalDateTime.now(), String.valueOf(estimateNextReceiptId())));
        txtReceiptCode.setEditable(false);
        if (tblProducts.getRowCount() > 0) tblProducts.setRowSelectionInterval(0, 0);
    }

    private long estimateNextReceiptId() {
        try {
            List<ExportReceipt> rows = exportReceiptController.findAll();
            long max = 0;
            if (rows != null) {
                for (ExportReceipt it : rows) {
                    if (it == null) continue;
                    if (it.getId() > max) max = it.getId();
                }
            }
            return max + 1;
        } catch (Exception ignored) {
            return Math.max(1, System.currentTimeMillis() % 100000);
        }
    }

    public ExportReceipt getResult() {
        return result;
    }

    private void initUi() {
        setSize(1180, 740);
        setMinimumSize(new Dimension(1050, 680));
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));

        tblProducts.setModel(productTableModel);
        tblItems.setModel(itemsTableModel);
        txtImeis.setLineWrap(true);
        txtImeis.setWrapStyleWord(true);

        txtProductId.setEditable(false);
        txtProductName.setEditable(false);
        txtStock.setEditable(false);
        txtEmployee.setEditable(false);
        btnImportExcel.setEnabled(false);
        btnScanImei.setEnabled(false);
        cbCustomer.setEditable(true);
        
        txtCustomerDetail.setEditable(false);
        txtCustomerDetail.setLineWrap(true);
        txtCustomerDetail.setWrapStyleWord(true);
        cbCustomer.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Customer c) {
                    String phone = c.getPhone() == null ? "" : c.getPhone().trim();
                    setText(phone.isBlank() ? safe(c.getName()) : safe(c.getName()) + " - " + phone);
                }
                return this;
            }
        });
        cbVariant.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ProductVariant v) {
                    String label = buildVariantLabel(v);
                    setText(label.isBlank() ? ("PB#" + v.getId()) : label);
                }
                return this;
            }
        });
        // product combo renderer: show id - name
        cbProductList.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Product p) {
                    String text = String.valueOf(p.getId()) + " - " + safe(p.getName());
                    setText(text);
                }
                return this;
            }
        });

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(root, BorderLayout.CENTER);

        // POS layout: Left - customer/payment, Center - product entry, Right - order/employee, Bottom - product list + items
        root.add(buildReceiptInfoPanel(), BorderLayout.WEST);
        root.add(buildEntryPanel(), BorderLayout.CENTER);
        root.add(buildOrderEmployeePanel(), BorderLayout.EAST);

        // Bottom: product list on top, items table below. left column reserve removed so left info panel can expand into this area
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 8));
        bottomPanel.setPreferredSize(new Dimension(0, 320));

        JPanel productListHolder = new JPanel(new BorderLayout());
        productListHolder.add(buildProductListPanel(), BorderLayout.CENTER);
        bottomPanel.add(productListHolder, BorderLayout.NORTH);

        // reserve left column space so items table doesn't occupy the area under customer info
        bottomPanel.add(new JScrollPane(tblItems), BorderLayout.CENTER);

        // create a container that holds bottomPanel and a submit area at the very bottom
        JPanel containerBottom = new JPanel(new BorderLayout());
        containerBottom.add(bottomPanel, BorderLayout.CENTER);
        JPanel submitWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 12));
        submitWrap.setPreferredSize(new Dimension(0, 80));
        // enlarge submit button and align left-bottom
        btnSubmit.setPreferredSize(new Dimension(160, 44));
        btnSubmit.setFont(btnSubmit.getFont().deriveFont(Font.BOLD, 16f));
        submitWrap.add(btnSubmit);
        containerBottom.add(submitWrap, BorderLayout.SOUTH);
        root.add(containerBottom, BorderLayout.SOUTH);
        
    }
    

    private JComponent buildProductListPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setPreferredSize(new Dimension(340, 0));

        txtSearch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        txtSearch.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(txtSearch);
        p.add(Box.createVerticalStrut(8));

        JScrollPane spProducts = new JScrollPane(tblProducts);
        spProducts.setAlignmentX(Component.LEFT_ALIGNMENT);
        spProducts.setPreferredSize(new Dimension(340, 220));
        spProducts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        p.add(spProducts);
        p.add(Box.createVerticalStrut(8));
        // show selected product id + name under the search/list
        txtProductId.setEditable(false);
        txtProductName.setEditable(false);
        JPanel showRow = new JPanel(new GridLayout(1, 2, 12, 6));
        showRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        showRow.add(txtProductId);
        showRow.add(txtProductName);
        p.add(showRow);
        p.add(Box.createVerticalStrut(8));

        JPanel actions = new JPanel();
        actions.setLayout(new BoxLayout(actions, BoxLayout.X_AXIS));
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.add(btnAddProduct);
        actions.add(Box.createHorizontalStrut(12));
        actions.add(btnImportExcel);
        p.add(actions);
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JComponent buildEntryPanel() {
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(420, 0));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JPanel row1 = new JPanel(new GridLayout(1, 1, 12, 6));
        row1.add(new JLabel("Danh sách sản phẩm"));
        p.add(row1);
        cbProductList.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        p.add(cbProductList);
        p.add(Box.createVerticalStrut(8));
        p.add(Box.createVerticalStrut(8));

        JPanel row2 = new JPanel(new GridLayout(2, 3, 12, 6));
        row2.add(new JLabel("C\u1ea5u h\u00ecnh"));
        row2.add(new JLabel("Gi\u00e1 xu\u1ea5t"));
        row2.add(new JLabel("S\u1ed1 l\u01b0\u1ee3ng t\u1ed3n"));
        row2.add(cbVariant);
        row2.add(txtExportPrice);
        row2.add(txtStock);
        p.add(row2);
        p.add(Box.createVerticalStrut(8));

        JPanel imeiHeader = new JPanel(new BorderLayout(0, 0));
        imeiHeader.add(new JLabel("Danh s\u00e1ch IMEI \u0111\u00e3 ch\u1ecdn"), BorderLayout.WEST);
        JPanel imeiButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        imeiButtons.add(btnChooseImei);
        imeiButtons.add(btnScanImei);
        imeiHeader.add(imeiButtons, BorderLayout.EAST);
        p.add(imeiHeader);

        JScrollPane sp = new JScrollPane(txtImeis);
        sp.setPreferredSize(new Dimension(0, 180));
        p.add(sp);
        // keep product id/name fields for display but non-editable (moved under product list)
        return p;
    }
    private JComponent buildReceiptInfoPanel() {
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(460, 0));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.add(new JLabel("M\u00e3 phi\u1ebfu xu\u1ea5t"));
        p.add(txtReceiptCode);
        p.add(Box.createVerticalStrut(8));
        p.add(new JLabel("Nh\u00e2n vi\u00ean xu\u1ea5t"));
        p.add(txtEmployee);
        p.add(Box.createVerticalStrut(8));

        // customer info moved to the right-side panel; keep action buttons here
        p.add(Box.createVerticalStrut(8));
        JPanel infoActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        infoActions.setOpaque(false);
        infoActions.add(btnAddItem);
        infoActions.add(btnEditItem);
        infoActions.add(btnDeleteItem);
        p.add(infoActions);

        p.add(Box.createVerticalStrut(12));
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JComponent buildOrderEmployeePanel() {
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(320, 0));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        // Right-side panel now holds the Customer selector and details (moved from left)
        JPanel custTitle = new JPanel(new BorderLayout(8, 0));
        custTitle.add(new JLabel("Kh\u00e1ch h\u00e0ng"), BorderLayout.WEST);
        custTitle.add(btnChooseCustomer, BorderLayout.EAST);
        p.add(custTitle);
        p.add(cbCustomer);
        p.add(Box.createVerticalStrut(8));
        JScrollPane custScroll = new JScrollPane(txtCustomerDetail);
        custScroll.setPreferredSize(new Dimension(0, 260));
        p.add(custScroll);
        p.add(Box.createVerticalStrut(8));
        // Placeholder for future payment / summary controls
        JPanel summary = new JPanel(new GridLayout(0, 1, 6, 6));
        summary.setOpaque(false);
        summary.add(new JLabel("Tổng tiền:"));
        summary.add(lblTotal);
        p.add(summary);
        p.add(Box.createVerticalGlue());
        return p;
    }
    private void bindSessionEmployee() {
        currentSession = SessionContext.getSession();
        String username = currentSession == null ? "" : safe(currentSession.getUsername());
        txtEmployee.setText(username);
        txtEmployee.setEditable(false);
        try {
            imeiDao.ensureTable();
        } catch (Exception ignored) {
        }
    }

    private void loadProducts() {
        allProducts.clear();
        try {
            List<Product> rows = productController.findAll();
            if (rows != null) allProducts.addAll(rows);
        } catch (Exception ignored) {
        }
        if (allProducts.isEmpty()) {
            allProducts.add(makeProduct(2L, "Samsung Galaxy A53 5G", 3));
            allProducts.add(makeProduct(4L, "Vivo Y02s", 14));
            allProducts.add(makeProduct(5L, "Samsung Galaxy A54 5G", 39));
        }
        productTableModel.setRows(allProducts);
        // populate product combo
        try {
            cbProductList.setModel(new DefaultComboBoxModel<>(allProducts.toArray(new Product[0])));
            if (cbProductList.getItemCount() > 0) cbProductList.setSelectedIndex(0);
        } catch (Exception ignored) {}
        itemsTableModel.setRows(new ArrayList<>());
        updateTotal();
    }

    private void loadCustomers() {
        List<Customer> customers = new ArrayList<>();
        try {
            List<Customer> rows = customerController.findAll();
            if (rows != null) customers.addAll(rows);
        } catch (Exception ignored) {
        }

        customers.removeIf(c -> c == null || c.getStatus() == null || c.getStatus() != 1);
        cbCustomer.setModel(new DefaultComboBoxModel<>(customers.toArray(new Customer[0])));
        if (cbCustomer.getItemCount() > 0) {
            cbCustomer.setSelectedIndex(0);
            Object sel = cbCustomer.getSelectedItem();
            if (sel instanceof Customer c) txtCustomerDetail.setText(buildCustomerDetailsText(c));
        }
    }

    private Product makeProduct(Long id, String name, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setStock(stock);
        return p;
    }

    private void wireEvents() {
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyProductFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyProductFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyProductFilter();
            }
        });

        tblProducts.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = tblProducts.getSelectedRow();
            if (row < 0) return;
            Product p = productTableModel.getRowAt(row);
            if (p == null) return;
            txtProductId.setText(String.valueOf(p.getId()));
            txtProductName.setText(safe(p.getName()));
            txtStock.setText(p.getStock() == null ? "" : String.valueOf(p.getStock()));
            txtImeis.setText("");
            loadVariantsByProduct(p.getId());
        });

        cbProductList.addActionListener(e -> {
            Product p = (Product) cbProductList.getSelectedItem();
            if (p == null) return;
            txtProductId.setText(String.valueOf(p.getId()));
            txtProductName.setText(safe(p.getName()));
            txtStock.setText(p.getStock() == null ? "" : String.valueOf(p.getStock()));
            loadVariantsByProduct(p.getId());
        });

        cbVariant.addActionListener(e -> {
            ProductVariant v = (ProductVariant) cbVariant.getSelectedItem();
            if (v == null) return;
            txtExportPrice.setText(v.getExportPrice() == null ? "" : String.valueOf(v.getExportPrice()));
            txtStock.setText(v.getStock() == null ? "" : String.valueOf(v.getStock()));
            txtImeis.setText("");
        });

        btnChooseImei.addActionListener(e -> chooseImeis());
        btnAddProduct.addActionListener(e -> {
            // open product creation dialog; after create, navigate to Products screen
            com.phonestore.ui.product.ProductFormDialog dlg = new com.phonestore.ui.product.ProductFormDialog(SwingUtilities.getWindowAncestor(this), null);
            dlg.setVisible(true);
            try {
                if (dlg.isChanged()) {
                    Window w = SwingUtilities.getWindowAncestor(this);
                    if (w instanceof com.phonestore.ui.common.MainFrame mf) {
                        mf.showScreen("products");
                    }
                }
            } catch (Throwable ignored) {}
        });
        btnAddItem.addActionListener(e -> addCurrentProductToReceipt());
        btnDeleteItem.addActionListener(e -> deleteSelectedItem());
        btnEditItem.addActionListener(e -> editSelectedItem());
        btnChooseCustomer.addActionListener(e -> searchOrCreateCustomer());
        btnSubmit.addActionListener(e -> submitReceipt());

        tblItems.getModel().addTableModelListener(e -> updateTotal());
        tblItems.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int row = tblItems.getSelectedRow();
            if (row < 0) return;
            ExportReceiptLine line = itemsTableModel.getRowAt(row);
            showLine(line);
        });

        cbCustomer.addActionListener(e -> {
            Object sel = cbCustomer.getSelectedItem();
            if (sel instanceof Customer c) {
                cbCustomer.setToolTipText(buildCustomerDetailsText(c));
                txtCustomerDetail.setText(buildCustomerDetailsText(c));
            } else {
                cbCustomer.setToolTipText(null);
                txtCustomerDetail.setText("");
            }
        });
    }

    private void showLine(ExportReceiptLine line) {
        if (line == null) return;
        txtProductId.setText(line.getProductId() == null ? "" : String.valueOf(line.getProductId()));
        txtProductName.setText(safe(line.getProductName()));
        txtExportPrice.setText(line.getUnitPrice() == null ? "" : String.valueOf(line.getUnitPrice()));
        txtStock.setText(line.getQuantity() == null ? "" : String.valueOf(line.getQuantity()));
        if (line.getImeis() != null && !line.getImeis().isEmpty()) txtImeis.setText(String.join("\n", line.getImeis()));
        else txtImeis.setText("");
    }

    private String buildCustomerDetailsText(Customer c) {
        if (c == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("Họ & Tên: ").append(safe(c.getName())).append('\n');
        sb.append("Số điện thoại: ").append(safe(c.getPhone())).append('\n');
        sb.append("Địa chỉ: ").append(safe(c.getAddress())).append('\n');
        sb.append("Email: ").append(safe(c.getEmail())).append('\n');
        return sb.toString();
    }

    private void applyProductFilter() {
        String kw = safe(txtSearch.getText()).toLowerCase(java.util.Locale.ROOT);
        if (kw.isBlank()) {
            productTableModel.setRows(allProducts);
            return;
        }
        List<Product> filtered = new ArrayList<>();
        for (Product p : allProducts) {
            if (p == null) continue;
            String id = String.valueOf(p.getId());
            String name = safe(p.getName()).toLowerCase(java.util.Locale.ROOT);
            if (id.contains(kw) || name.contains(kw)) filtered.add(p);
        }
        productTableModel.setRows(filtered);
    }

    private void loadVariantsByProduct(long productId) {
        try {
            List<ProductVariant> vars = variantController.findByProductId(productId);
            cbVariant.setModel(new DefaultComboBoxModel<>(vars.toArray(new ProductVariant[0])));
            if (!vars.isEmpty()) cbVariant.setSelectedIndex(0);
        } catch (Throwable ex) {
            cbVariant.setModel(new DefaultComboBoxModel<>(new ProductVariant[0]));
        }
    }
    private void chooseImeis() {
        ProductVariant v = (ProductVariant) cbVariant.getSelectedItem();
        if (v == null) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng ch\u1ecdn c\u1ea5u h\u00ecnh tr\u01b0\u1edbc.", "Th\u00f4ng b\u00e1o", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Integer variantStock = v.getStock() == null ? 0 : v.getStock();
        if (variantStock <= 0) {
            JOptionPane.showMessageDialog(this, "Sản phẩm này đã hết hàng (variant).", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> available;
        try {
            available = imeiDao.findAvailableImeisByVariantId(v.getId());
            if (available.isEmpty()) {
                long productId = parseLongSafe(txtProductId.getText(), 0L);
                if (productId > 0) available = imeiDao.findAvailableImeisByProductId(productId);
            }
            if (available.isEmpty()) available = imeiDao.findAllImeis();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Kh\u00f4ng th\u1ec3 t\u1ea3i danh s\u00e1ch IMEI: " + ex.getMessage(), "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Set<String> used = collectImeisInCurrentItems();
        List<String> visible = new ArrayList<>();
        for (String it : available) {
            if (it == null) continue;
            String imei = it.trim();
            if (imei.isBlank() || used.contains(imei)) continue;
            visible.add(imei);
        }
        if (visible.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Kh\u00f4ng c\u00f2n IMEI kh\u1ea3 d\u1ee5ng.", "Th\u00f4ng b\u00e1o", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JList<String> imeiList = new JList<>(visible.toArray(new String[0]));
        imeiList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane pane = new JScrollPane(imeiList);
        pane.setPreferredSize(new Dimension(360, 280));
        int c = JOptionPane.showConfirmDialog(this, pane, "Ch\u1ecdn IMEI", JOptionPane.OK_CANCEL_OPTION);
        if (c != JOptionPane.OK_OPTION) return;

        List<String> selected = imeiList.getSelectedValuesList();
        if (selected == null || selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "B\u1ea1n ch\u01b0a ch\u1ecdn IMEI.", "Th\u00f4ng b\u00e1o", JOptionPane.WARNING_MESSAGE);
            return;
        }
        txtImeis.setText(String.join("\n", selected));
    }
    private Set<String> collectImeisInCurrentItems() {
        Set<String> used = new LinkedHashSet<>();
        for (ExportReceiptLine it : itemsTableModel.getRows()) {
            if (it == null || it.getImeis() == null) continue;
            for (String imei : it.getImeis()) {
                if (imei == null) continue;
                String v = imei.trim();
                if (!v.isBlank()) used.add(v);
            }
        }
        return used;
    }

    private void addCurrentProductToReceipt() {
        ProductVariant v = (ProductVariant) cbVariant.getSelectedItem();
        if (v == null) return;
        if (safe(txtProductId.getText()).isBlank() || safe(txtProductName.getText()).isBlank()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm (Mã và Tên không được để trống).", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int variantStock = v.getStock() == null ? 0 : v.getStock();
        if (variantStock <= 0) {
            JOptionPane.showMessageDialog(this, "Không thể thêm: variant đã hết hàng.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> imeis = parseImeis(txtImeis.getText());
        if (imeis.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng ch\u1ecdn IMEI cho s\u1ea3n ph\u1ea9m.", "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int stock = variantStock;
        if (stock > 0 && imeis.size() > stock) {
            JOptionPane.showMessageDialog(this, "S\u1ed1 IMEI ch\u1ecdn v\u01b0\u1ee3t t\u1ed3n kho.", "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ExportReceiptLine line = new ExportReceiptLine();
        line.setProductId(parseLongSafe(txtProductId.getText(), 0L));
        line.setVariantId(v.getId());
        line.setProductName(txtProductName.getText());
        line.setVariantLabel(buildVariantLabel(v));
        line.setUnitPrice(parseLongSafe(txtExportPrice.getText(), 0L));
        line.setQuantity(imeis.size());
        line.setImeis(new ArrayList<>(imeis));

        itemsTableModel.add(line);
        txtImeis.setText("");
        updateTotal();
        // decrement local stock display
        try {
            long pid = parseLongSafe(txtProductId.getText(), 0L);
            int used = line.getQuantity() == null ? 0 : line.getQuantity();
            // decrement product overall stock
            for (Product p : allProducts) {
                if (p == null) continue;
                if (p.getId() == pid) {
                    Integer s = p.getStock();
                    if (s == null) s = 0;
                    p.setStock(Math.max(0, s - used));
                    break;
                }
            }
            // decrement variant stock
            try {
                int newVStock = Math.max(0, variantStock - used);
                v.setStock(newVStock);
            } catch (Exception ignored) {}

            productTableModel.setRows(allProducts);
            txtStock.setText(String.valueOf(Math.max(0, variantStock - used)));
        } catch (Exception ignored) {}
    }

    private List<String> parseImeis(String text) {
        List<String> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        String[] lines = text == null ? new String[0] : text.split("\\R");
        for (String raw : lines) {
            String s = raw == null ? "" : raw.trim();
            if (s.isBlank()) continue;
            if (!isImei15Digits(s)) {
                JOptionPane.showMessageDialog(this, "IMEI kh\u00f4ng h\u1ee3p l\u1ec7: " + s, "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
                return new ArrayList<>();
            }
            if (!seen.add(s)) {
                JOptionPane.showMessageDialog(this, "IMEI b\u1ecb tr\u00f9ng: " + s, "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
                return new ArrayList<>();
            }
            out.add(s);
        }
        return out;
    }

    private boolean isImei15Digits(String s) {
        if (s == null || s.length() != 15) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    private String buildVariantLabel(ProductVariant v) {
        String ram = safe(v.getRamName());
        String rom = safe(v.getRomName());
        String color = safe(v.getColorName());
        String base = ram + "/" + rom + " - " + color;
        Integer s = v.getStock();
        if (s != null) base = base + " (" + s + ")";
        return base;
    }

    private String buildImeiDisplay(List<String> imeis) {
        if (imeis == null || imeis.isEmpty()) return "";
        if (imeis.size() == 1) return imeis.get(0);
        return String.format(java.util.Locale.ROOT, "(IMEI x%d)", imeis.size());
    }

    private void deleteSelectedItem() {
        int row = tblItems.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng ch\u1ecdn d\u00f2ng c\u1ea7n x\u00f3a.", "Th\u00f4ng b\u00e1o", JOptionPane.WARNING_MESSAGE);
            return;
        }
        itemsTableModel.removeAt(row);
        updateTotal();
    }

    private void editSelectedItem() {
        int row = tblItems.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng ch\u1ecdn d\u00f2ng c\u1ea7n s\u1eeda.", "Th\u00f4ng b\u00e1o", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ExportReceiptLine line = itemsTableModel.getRowAt(row);
        if (line == null) return;
        txtProductId.setText(line.getProductId() == null ? "" : String.valueOf(line.getProductId()));
        txtProductName.setText(safe(line.getProductName()));
        txtExportPrice.setText(line.getUnitPrice() == null ? "" : String.valueOf(line.getUnitPrice()));
        txtImeis.setText(line.getImeis() == null ? "" : String.join("\n", line.getImeis()));
        JOptionPane.showMessageDialog(this, "B\u1ea1n c\u00f3 th\u1ec3 ch\u1ec9nh s\u1eeda IMEI v\u00e0 b\u1ea5m 'Th\u00eam s\u1ea3n ph\u1ea9m' \u0111\u1ec3 t\u1ea1o d\u00f2ng m\u1edbi.", "Th\u00f4ng b\u00e1o", JOptionPane.INFORMATION_MESSAGE);
    }

    private void searchOrCreateCustomer() {
        String keyword = JOptionPane.showInputDialog(this, "Nh\u1eadp t\u00ean/S\u0110T kh\u00e1ch h\u00e0ng:");
        if (keyword == null) return;
        keyword = keyword.trim();
        if (keyword.isBlank()) return;

        List<Customer> found = new ArrayList<>();
        try {
            List<Customer> rows = customerController.search(keyword);
            if (rows != null) found.addAll(rows);
        } catch (Exception ignored) {
        }

        if (!found.isEmpty()) {
            Customer selected = chooseCustomerFromList(found);
            if (selected != null) cbCustomer.setSelectedItem(selected);
            return;
        }

        int c = JOptionPane.showConfirmDialog(this,
                "Kh\u00f4ng t\u00ecm th\u1ea5y kh\u00e1ch h\u00e0ng. Th\u00eam m\u1edbi?",
                "Kh\u00e1ch h\u00e0ng",
                JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;

        JTextField txtName = new JTextField(keyword);
        JTextField txtPhone = new JTextField();
        JTextField txtAddress = new JTextField();
        JTextField txtEmail = new JTextField();
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.add(new JLabel("Họ & Tên"));
        form.add(txtName);
        form.add(new JLabel("Số điện thoại"));
        form.add(txtPhone);
        form.add(new JLabel("\u0110\u1ecba ch\u1ec9"));
        form.add(txtAddress);
        form.add(new JLabel("Email (không bắt buộc)"));
        form.add(txtEmail);

        int ok = JOptionPane.showConfirmDialog(this, form, "Th\u00eam kh\u00e1ch h\u00e0ng", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return;

        Customer newCust = new Customer();
        newCust.setName(safe(txtName.getText()));
        newCust.setPhone(safe(txtPhone.getText()));
        newCust.setAddress(safe(txtAddress.getText()));
        newCust.setEmail(safe(txtEmail.getText()));
        newCust.setStatus(1);
        if (newCust.getName().isBlank() || newCust.getPhone().isBlank() || newCust.getAddress().isBlank()) {
            JOptionPane.showMessageDialog(this, "T\u00ean, S\u0110T, \u0111\u1ecba ch\u1ec9 l\u00e0 b\u1eaft bu\u1ed9c.", "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            Customer created = customerController.create(newCust);
            cbCustomer.addItem(created);
            cbCustomer.setSelectedItem(created);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Kh\u00f4ng th\u1ec3 th\u00eam kh\u00e1ch h\u00e0ng: " + ex.getMessage(), "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void submitReceipt() {
        if (itemsTableModel.getRowCount() <= 0) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng th\u00eam \u00edt nh\u1ea5t 1 s\u1ea3n ph\u1ea9m.", "Thi\u1ebfu d\u1eef li\u1ec7u", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ExportReceipt receipt = new ExportReceipt();
        receipt.setStatus(1);

        if (currentSession != null) {
            receipt.setCreatedBy(currentSession.getUserId());
            receipt.setCreatedByName(safe(currentSession.getUsername()));
        }
        if (receipt.getCreatedByName() == null || receipt.getCreatedByName().isBlank()) {
            receipt.setCreatedByName(safe(txtEmployee.getText()));
        }

        Customer selectedCustomer = resolveOrCreateCustomerFromInput();
        if (selectedCustomer != null) {
            receipt.setCustomerId(selectedCustomer.getId() > 0 ? selectedCustomer.getId() : null);
            receipt.setCustomerName(safe(selectedCustomer.getName()));
        }

        List<ExportReceiptLine> lines = new ArrayList<>(itemsTableModel.getRows());
        receipt.setLines(lines);
        receipt.setTotal((double) calculateTotal());
        receipt.setReceiptCode(safe(txtReceiptCode.getText()));

        this.result = receipt;
        this.dispose();
    }

    private Customer chooseCustomerFromList(List<Customer> customers) {
        if (customers == null || customers.isEmpty()) return null;
        JList<Customer> list = new JList<>(customers.toArray(new Customer[0]));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lst, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(lst, value, index, isSelected, cellHasFocus);
                if (value instanceof Customer c) {
                    setText(safe(c.getName()) + " - " + safe(c.getPhone()));
                }
                return this;
            }
        });
        if (!customers.isEmpty()) list.setSelectedIndex(0);
        JScrollPane pane = new JScrollPane(list);
        pane.setPreferredSize(new Dimension(360, 260));
        int c = JOptionPane.showConfirmDialog(this, pane, "Ch\u1ecdn kh\u00e1ch h\u00e0ng", JOptionPane.OK_CANCEL_OPTION);
        if (c != JOptionPane.OK_OPTION) return null;
        return list.getSelectedValue();
    }

    private Customer resolveOrCreateCustomerFromInput() {
        Object selected = cbCustomer.getSelectedItem();
        if (selected instanceof Customer c) return c;

        String raw = "";
        if (selected != null) raw = safe(String.valueOf(selected));
        if (raw.isBlank() && cbCustomer.isEditable()) {
            Object editorItem = cbCustomer.getEditor().getItem();
            raw = editorItem == null ? "" : safe(String.valueOf(editorItem));
        }
        if (raw.isBlank()) return null;

        try {
            List<Customer> found = customerController.search(raw);
            if (found != null && !found.isEmpty()) {
                Customer c = chooseCustomerFromList(found);
                if (c != null) return c;
            }
        } catch (Exception ignored) {
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Kh\u00f4ng th\u1ea5y kh\u00e1ch h\u00e0ng \"" + raw + "\". Th\u00eam m\u1edbi?",
                "Kh\u00e1ch h\u00e0ng",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return null;

        JTextField txtName = new JTextField(raw);
        JTextField txtPhone = new JTextField();
        JTextField txtAddress = new JTextField();
        JTextField txtEmail = new JTextField();
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 6));
        form.add(new JLabel("T\u00ean kh\u00e1ch h\u00e0ng"));
        form.add(txtName);
        form.add(new JLabel("S\u1ed1 \u0111i\u1ec7n tho\u1ea1i"));
        form.add(txtPhone);
        form.add(new JLabel("\u0110\u1ecba ch\u1ec9"));
        form.add(txtAddress);
        form.add(new JLabel("Email (kh\u00f4ng b\u1eaft bu\u1ed9c)"));
        form.add(txtEmail);

        int ok = JOptionPane.showConfirmDialog(this, form, "Th\u00eam kh\u00e1ch h\u00e0ng", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) return null;

        Customer newCust = new Customer();
        newCust.setName(safe(txtName.getText()));
        newCust.setPhone(safe(txtPhone.getText()));
        newCust.setAddress(safe(txtAddress.getText()));
        newCust.setEmail(safe(txtEmail.getText()));
        newCust.setStatus(1);
        if (newCust.getName().isBlank() || newCust.getPhone().isBlank() || newCust.getAddress().isBlank()) {
            JOptionPane.showMessageDialog(this, "T\u00ean, S\u0110T, \u0111\u1ecba ch\u1ec9 l\u00e0 b\u1eaft bu\u1ed9c.", "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        try {
            Customer created = customerController.create(newCust);
            cbCustomer.addItem(created);
            cbCustomer.setSelectedItem(created);
            return created;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Kh\u00f4ng th\u1ec3 th\u00eam kh\u00e1ch h\u00e0ng: " + ex.getMessage(), "L\u1ed7i", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private long calculateTotal() {
        long total = 0;
        for (ExportReceiptLine l : itemsTableModel.getRows()) {
            if (l.getUnitPrice() != null && l.getQuantity() != null) total += l.getUnitPrice() * l.getQuantity();
        }
        return total;
    }

    private void updateTotal() {
        lblTotal.setText(String.format("%,d \u20ab", calculateTotal()));
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private long parseLongSafe(String text, long fallback) {
        try {
            String v = safe(text).replaceAll("[^0-9]", "");
            if (v.isBlank()) return fallback;
            return Long.parseLong(v);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private int parseIntSafe(String text, int fallback) {
        try {
            String v = safe(text).replaceAll("[^0-9]", "");
            if (v.isBlank()) return fallback;
            return Integer.parseInt(v);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private static class ProductTableModel extends AbstractTableModel {
        private final String[] cols = {"M\u00e3 SP", "T\u00ean s\u1ea3n ph\u1ea9m", "S\u1ed1 l\u01b0\u1ee3ng"};
        private List<Product> rows = new ArrayList<>();

        public void setRows(List<Product> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public Product getRowAt(int row) {
            return row < 0 || row >= rows.size() ? null : rows.get(row);
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
            Product p = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> p.getId();
                case 1 -> p.getName();
                case 2 -> p.getStock();
                default -> "";
            };
        }
    }

    private static class ItemsTableModel extends AbstractTableModel {
        private final String[] cols = {"STT", "M\u00e3 SP", "IMEI", "T\u00ean s\u1ea3n ph\u1ea9m", "RAM", "ROM", "M\u00e0u s\u1eafc", "\u0110\u01a1n gi\u00e1", "S\u1ed1 l\u01b0\u1ee3ng"};
        private List<ExportReceiptLine> rows = new ArrayList<>();

        public void setRows(List<ExportReceiptLine> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public void add(ExportReceiptLine line) {
            rows.add(line);
            fireTableDataChanged();
        }

        public void removeAt(int row) {
            if (row < 0 || row >= rows.size()) return;
            rows.remove(row);
            fireTableDataChanged();
        }

        public List<ExportReceiptLine> getRows() {
            return rows;
        }

        public ExportReceiptLine getRowAt(int row) {
            return row < 0 || row >= rows.size() ? null : rows.get(row);
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
            ExportReceiptLine it = rows.get(rowIndex);
            String ram = "";
            String rom = "";
            String color = "";
            String imeiDisplay = "";
            String label = it.getVariantLabel() == null ? "" : it.getVariantLabel();
            if (!label.isBlank()) {
                String[] parts = label.split("-", 2);
                String left = parts[0].trim();
                String right = parts.length > 1 ? parts[1].trim() : "";
                String[] rr = left.split("/", -1);
                if (rr.length > 0) ram = rr[0].trim();
                if (rr.length > 1) rom = rr[1].trim();
                color = right;
            }
            if (it.getImeis() != null && !it.getImeis().isEmpty()) {
                if (it.getImeis().size() == 1) imeiDisplay = it.getImeis().get(0);
                else imeiDisplay = String.format(java.util.Locale.ROOT, "(IMEI x%d)", it.getImeis().size());
            }
            long unit = it.getUnitPrice() == null ? 0 : it.getUnitPrice();
            int qty = it.getQuantity() == null ? 0 : it.getQuantity();
            return switch (columnIndex) {
                case 0 -> rowIndex + 1;
                case 1 -> it.getProductId();
                case 2 -> imeiDisplay;
                case 3 -> it.getProductName();
                case 4 -> ram;
                case 5 -> rom;
                case 6 -> color;
                case 7 -> unit <= 0 ? "" : String.format("%,d \u20ab", unit);
                case 8 -> qty;
                default -> "";
            };
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ExportReceiptFormDialog dialog = new ExportReceiptFormDialog(null, null);
            dialog.setVisible(true);
        });
    }
}
