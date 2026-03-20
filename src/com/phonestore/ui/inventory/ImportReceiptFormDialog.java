 
package com.phonestore.ui.inventory;

import com.phonestore.controller.ProductController;
import com.phonestore.controller.ProductVariantController;
import com.phonestore.controller.SupplierController;
import com.phonestore.dao.jdbc.ImeiRegistryJdbcDao;
import com.phonestore.dao.jdbc.ImportReceiptLineJdbcDao;
import com.phonestore.model.ImportReceipt;
import com.phonestore.model.ImportReceiptLine;
import com.phonestore.model.Product;
import com.phonestore.model.ProductVariant;
import com.phonestore.model.Supplier;
import com.phonestore.ui.common.session.SessionContext;
import com.phonestore.ui.common.session.UserSession;
import com.phonestore.ui.common.toast.Toast;
import com.phonestore.util.MoneyVND;

import javax.swing.*;
import javax.swing.DefaultListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DocumentFilter;
import javax.swing.text.Highlighter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ImportReceiptFormDialog extends JDialog {

    private static final String METHOD_BATCH = "Nh\u1eadp theo l\u00f4";
    private static final String METHOD_PER_DEVICE = "Nh\u1eadp t\u1eebng m\u00e1y";

    private enum BatchImeiMode { NONE, AUTO, ENTER }

    // Left: product list
    private final JTextField txtProductSearch = new JTextField();
    private final ProductTableModel productTableModel = new ProductTableModel();
    private final JTable tblProducts = new JTable(productTableModel);
    private final JButton btnAddItem = new JButton("Th\u00eam s\u1ea3n ph\u1ea9m");
    private final JButton btnImportExcel = new JButton("Nh\u1eadp Excel");

    // Center: entry form
    private final JTextField txtProductId = new JTextField();
    private final JTextField txtProductName = new JTextField();
    private final JComboBox<ProductVariant> cbVariant = new JComboBox<>();
    private final JTextField txtImportPrice = new JTextField();
    private final JComboBox<String> cbImportMethod = new JComboBox<>(new String[] {METHOD_BATCH, METHOD_PER_DEVICE});
    private final JLabel lblImei = new JLabel("Nh\u1eadp IMEI \u0111i\u1ec7n tho\u1ea1i");
    private final JTextField txtImeiStart = new JTextField();
    private final JButton btnAutoImei = new JButton("T\u1ef1 sinh IMEI");
    private final JButton btnEnterImei = new JButton("Nh\u1eadp IMEI");
    private final JTextArea txtImeiAutoList = new JTextArea();
    private final JTextArea txtImeiManualList = new JTextArea();
    private final JLabel lblImeiCount = new JLabel("0/0");
    private final JTextField txtQty = new JTextField();

    // Bottom: items table
    private final JButton btnEditItem = new JButton("S\u1eeda s\u1ea3n ph\u1ea9m");
    private final JButton btnDeleteItem = new JButton("X\u00f3a s\u1ea3n ph\u1ea9m");
    private final ItemsTableModel itemsTableModel = new ItemsTableModel();
    private final JTable tblItems = new JTable(itemsTableModel);

    // Right: receipt info
    private final JTextField txtReceiptCode = new JTextField();
    private final JTextField txtEmployee = new JTextField();
    private final JComboBox<Supplier> cbSupplier = new JComboBox<>();
    private final JLabel lblTotal = new JLabel("0 \u20ab");
    private final JButton btnSubmit = new JButton("Nh\u1eadp h\u00e0ng");

    private final SupplierController supplierController = new SupplierController();
    private final ProductController productController = new ProductController();
    private final ProductVariantController variantController = new ProductVariantController();
    private final ImeiRegistryJdbcDao imeiDao = new ImeiRegistryJdbcDao();

    private final SecureRandom imeiRandom = new SecureRandom();
    private List<String> pendingImeis = new ArrayList<>();
    private BatchImeiMode batchImeiMode = BatchImeiMode.NONE;

        private final Highlighter.HighlightPainter imeiErrorPainter =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 200, 200));
        private Timer manualValidateTimer;
        private int manualValidateSeq = 0;

            private Timer perDeviceValidateTimer;
            private int perDeviceValidateSeq = 0;
            private Color imeiStartDefaultBg;

    private List<Product> allProducts = new ArrayList<>();

    private ImportReceipt result;
    private final ImportReceipt editing;
    private final boolean viewOnly;

    public ImportReceiptFormDialog(Window owner, ImportReceipt editing) {
        this(owner, editing, false);
    }

    public ImportReceiptFormDialog(Window owner, ImportReceipt editing, boolean viewOnly) {
        super(owner, "Phi\u1ebfu nh\u1eadp", ModalityType.APPLICATION_MODAL);
        this.editing = editing;
        this.viewOnly = viewOnly;
        initUi();
        if (editing != null) bind(editing);
    }

    public ImportReceipt getResult() {
        return result;
    }

    private void initUi() {
        setSize(1180, 740);
        setMinimumSize(new Dimension(1050, 680));
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new BorderLayout(12, 0));
        top.add(buildProductListPanel(), BorderLayout.WEST);
        top.add(buildProductInputPanel(), BorderLayout.CENTER);
        top.add(buildReceiptInfoPanel(), BorderLayout.EAST);

        root.add(top, BorderLayout.CENTER);
        root.add(buildItemsPanel(), BorderLayout.SOUTH);
        add(root, BorderLayout.CENTER);

        configureTable(tblProducts);
        configureItemsTable(tblItems);
        configureCombos();
        loadComboData();
        loadProducts();
        bindHeader();
        wireEvents();

        manualValidateTimer = new Timer(350, e -> runManualImeiLiveValidation());
        manualValidateTimer.setRepeats(false);

        perDeviceValidateTimer = new Timer(350, e -> runPerDeviceImeiLiveValidation());
        perDeviceValidateTimer.setRepeats(false);

        imeiStartDefaultBg = txtImeiStart.getBackground();
        installImei15DigitsFilter(txtImeiStart);

        if (editing != null) {
            loadEditingLines();
            disableItemEntryForEditMode();
            if (viewOnly) {
                applyViewOnlyMode();
            }
        }
    }

    private JComponent buildProductListPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setPreferredSize(new Dimension(380, 0));

        txtProductSearch.putClientProperty("JTextField.placeholderText", "T\u00ean s\u1ea3n ph\u1ea9m, m\u00e3 s\u1ea3n ph\u1ea9m...");
        txtProductSearch.setPreferredSize(new Dimension(0, 34));
        p.add(txtProductSearch, BorderLayout.NORTH);

        p.add(new JScrollPane(tblProducts), BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
        btnAddItem.setPreferredSize(new Dimension(0, 38));
        btnImportExcel.setPreferredSize(new Dimension(0, 38));
        actions.add(btnAddItem);
        actions.add(btnImportExcel);
        p.add(actions, BorderLayout.SOUTH);

        return p;
    }

    private JComponent buildProductInputPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        txtProductId.setEnabled(false);
        txtProductName.setEnabled(false);

        JPanel row1 = new JPanel(new GridLayout(1, 2, 12, 0));
        row1.add(fieldBlock("M\u00e3 s\u1ea3n ph\u1ea9m", txtProductId));
        row1.add(fieldBlock("T\u00ean s\u1ea3n ph\u1ea9m", txtProductName));

        JPanel row2 = new JPanel(new GridLayout(1, 2, 12, 0));
        row2.add(fieldBlock("C\u1ea5u h\u00ecnh", cbVariant));
        row2.add(fieldBlock("Gi\u00e1 nh\u1eadp", txtImportPrice));

        JPanel row3 = new JPanel(new GridLayout(1, 1));
        row3.add(fieldBlock("Ph\u01b0\u01a1ng th\u1ee9c nh\u1eadp", cbImportMethod));

        JPanel row4 = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 0;

        // Row 1: IMEI field + Quantity field (aligned)
        gc.gridy = 0;
        gc.gridx = 0;
        gc.weightx = 0.62;
        gc.insets = new Insets(0, 0, 0, 12);
        row4.add(fieldBlock(lblImei, txtImeiStart), gc);

        gc.gridx = 1;
        gc.weightx = 0.38;
        gc.insets = new Insets(0, 0, 0, 0);
        row4.add(fieldBlock("S\u1ed1 l\u01b0\u1ee3ng", txtQty), gc);

        // Row 2: buttons span across both columns
        JPanel btns = new JPanel(new GridLayout(1, 2, 10, 0));
        btnAutoImei.setPreferredSize(new Dimension(0, 34));
        btnEnterImei.setPreferredSize(new Dimension(0, 34));
        btns.add(btnAutoImei);
        btns.add(btnEnterImei);

        gc.gridy = 1;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.weightx = 1;
        gc.insets = new Insets(10, 0, 0, 0);
        row4.add(btns, gc);

        // Row 3: IMEI areas (Auto-left / Manual-right)
        txtImeiAutoList.setEditable(false);
        txtImeiAutoList.setLineWrap(false);
        txtImeiAutoList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        txtImeiManualList.setEditable(true);
        txtImeiManualList.setLineWrap(false);
        txtImeiManualList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JScrollPane spLeft = new JScrollPane(txtImeiAutoList);
        JScrollPane spRight = new JScrollPane(txtImeiManualList);
        spLeft.setPreferredSize(new Dimension(0, 120));
        spRight.setPreferredSize(new Dimension(0, 120));

        JPanel listHeader = new JPanel(new BorderLayout());
        listHeader.add(lblImeiCount, BorderLayout.EAST);

        JPanel listCenter = new JPanel(new GridLayout(1, 2, 12, 0));
        listCenter.add(spLeft);
        listCenter.add(spRight);

        JPanel listWrap = new JPanel(new BorderLayout(0, 6));
        listWrap.add(listHeader, BorderLayout.NORTH);
        listWrap.add(listCenter, BorderLayout.CENTER);

        gc.gridy = 2;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.weightx = 1;
        gc.weighty = 1;
        gc.insets = new Insets(10, 0, 0, 0);
        row4.add(listWrap, gc);

        p.add(row1);
        p.add(Box.createVerticalStrut(12));
        p.add(row2);
        p.add(Box.createVerticalStrut(12));
        p.add(row3);
        p.add(Box.createVerticalStrut(12));
        p.add(row4);
        p.add(Box.createVerticalGlue());

        return p;
    }

    private JComponent buildReceiptInfoPanel() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setPreferredSize(new Dimension(340, 0));

        txtReceiptCode.setEnabled(false);
        txtEmployee.setEnabled(false);

        p.add(fieldBlock("M\u00e3 phi\u1ebfu nh\u1eadp", txtReceiptCode));
        p.add(Box.createVerticalStrut(12));
        p.add(fieldBlock("Nh\u00e2n vi\u00ean nh\u1eadp", txtEmployee));
        p.add(Box.createVerticalStrut(12));
        p.add(fieldBlock("Nh\u00e0 cung c\u1ea5p", cbSupplier));
        p.add(Box.createVerticalGlue());

        JPanel totalWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        totalWrap.add(new JLabel("T\u1ed4NG TI\u1ec0N:"));
        totalWrap.add(lblTotal);
        p.add(totalWrap);

        p.add(Box.createVerticalStrut(10));
        btnSubmit.setPreferredSize(new Dimension(0, 44));
        p.add(btnSubmit);

        return p;
    }

    private JComponent buildItemsPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setPreferredSize(new Dimension(0, 280));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 0));
        btnEditItem.setEnabled(false);
        btnDeleteItem.setEnabled(false);
        actions.add(btnEditItem);
        actions.add(btnDeleteItem);
        p.add(actions, BorderLayout.NORTH);

        p.add(new JScrollPane(tblItems), BorderLayout.CENTER);
        return p;
    }

    private JComponent fieldBlock(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.add(new JLabel(label), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        field.setPreferredSize(new Dimension(0, 34));
        return p;
    }

    private JComponent fieldBlock(JLabel label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.add(label, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        field.setPreferredSize(new Dimension(0, 34));
        return p;
    }

    private void bind(ImportReceipt r) {
        selectSupplier(r.getSupplierId());
        if (r.getTotal() != null) {
            lblTotal.setText(MoneyVND.format(r.getTotal()));
        }
    }

    private void bindHeader() {
        if (editing != null && editing.getId() > 0) {
            txtReceiptCode.setText("PN" + editing.getId());
        } else {
            txtReceiptCode.setText("T\u1ef1 \u0111\u1ed9ng");
        }

        if (editing != null && editing.getCreatedByName() != null && !editing.getCreatedByName().isBlank()) {
            txtEmployee.setText(editing.getCreatedByName());
            return;
        }

        UserSession s = SessionContext.getSession();
        if (s != null && s.getUsername() != null && !s.getUsername().isBlank()) {
            txtEmployee.setText(s.getUsername());
        } else {
            txtEmployee.setText("");
        }
    }

    private void onSave() {
        Supplier supplier = (Supplier) cbSupplier.getSelectedItem();
        if (supplier == null || supplier.getId() <= 0) {
            Toast.warn(this, "Vui l\u00f2ng ch\u1ecdn Nh\u00e0 cung c\u1ea5p");
            return;
        }

        if (editing == null) {
            if (itemsTableModel.isEmpty()) {
                Toast.warn(this, "Vui l\u00f2ng th\u00eam s\u1ea3n ph\u1ea9m tr\u01b0\u1edbc");
                return;
            }
            try {
                reserveImeisForImportItems();
            } catch (Exception ex) {
                Toast.warn(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
                return;
            }
        }

        Long createdBy = null;
        if (editing != null && editing.getCreatedBy() != null && editing.getCreatedBy() > 0) {
            createdBy = editing.getCreatedBy();
        } else {
            UserSession s = SessionContext.getSession();
            if (s != null) createdBy = s.getUserId();
        }
        if (createdBy == null || createdBy <= 0) {
            Toast.warn(this, "Kh\u00f4ng x\u00e1c \u0111\u1ecbnh \u0111\u01b0\u1ee3c nh\u00e2n vi\u00ean nh\u1eadp (vui l\u00f2ng \u0111\u0103ng nh\u1eadp l\u1ea1i)");
            return;
        }

        double total = itemsTableModel.computeTotal();

        ImportReceipt r = new ImportReceipt();
        if (editing != null) {
            r.setId(editing.getId());
            r.setTime(editing.getTime());
        }
        r.setSupplierId(supplier.getId());
        r.setCreatedBy(createdBy);
        r.setTotal(total);
        r.setStatus(editing == null ? null : editing.getStatus());

        if (editing == null) {
            r.setLines(itemsTableModel.toReceiptLines());
        }
        result = r;
        dispose();
    }

    private void reserveImeisForImportItems() {
        List<String> imeis = new ArrayList<>();
        List<Long> productIds = new ArrayList<>();
        List<Long> variantIds = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (ImportItem it : itemsTableModel.rows) {
            if (it == null) continue;
            if (it.imeis == null || it.imeis.isEmpty()) {
                throw new IllegalStateException("Thi\u1ebfu danh s\u00e1ch IMEI cho s\u1ea3n ph\u1ea9m: " + it.productId);
            }
            if (it.quantity <= 0) {
                throw new IllegalStateException("S\u1ed1 l\u01b0\u1ee3ng kh\u00f4ng h\u1ee3p l\u1ec7 cho s\u1ea3n ph\u1ea9m: " + it.productId);
            }
            if (it.imeis.size() != it.quantity) {
                throw new IllegalStateException("S\u1ed1 IMEI kh\u00f4ng kh\u1edbp s\u1ed1 l\u01b0\u1ee3ng cho s\u1ea3n ph\u1ea9m: " + it.productId);
            }

            Long pid = it.productId <= 0 ? null : it.productId;
            Long vid = it.variantId;
            for (String raw : it.imeis) {
                String imei = raw == null ? "" : raw.trim();
                if (!isValidImei15Digits(imei)) {
                    throw new IllegalStateException("IMEI kh\u00f4ng h\u1ee3p l\u1ec7: " + imei);
                }
                if (!seen.add(imei)) {
                    throw new IllegalStateException("IMEI b\u1ecb tr\u00f9ng trong phi\u1ebfu nh\u1eadp: " + imei);
                }
                imeis.add(imei);
                productIds.add(pid);
                variantIds.add(vid);
            }
        }

        imeiDao.reserveAllDetailed(imeis, productIds, variantIds);
    }

    private void configureCombos() {
        cbSupplier.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Supplier s) {
                    setText(s.getName() == null ? "" : s.getName());
                }
                return this;
            }
        });

        cbVariant.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ProductVariant v) {
                    String ram = v.getRamName() == null ? "" : v.getRamName();
                    String rom = v.getRomName() == null ? "" : v.getRomName();
                    String color = v.getColorName() == null ? "" : v.getColorName();
                    String label = (ram + "/" + rom + (color.isBlank() ? "" : (" - " + color))).trim();
                    setText(label.isBlank() ? ("PB#" + v.getId()) : label);
                }
                return this;
            }
        });
    }

    private void loadComboData() {
        try {
            List<Supplier> suppliers = supplierController.findAll();
            cbSupplier.setModel(new DefaultComboBoxModel<>(suppliers.toArray(new Supplier[0])));
        } catch (Throwable ex) {
            Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
            cbSupplier.setModel(new DefaultComboBoxModel<>(new Supplier[0]));
        }
    }

    private void selectSupplier(Long supplierId) {
        if (supplierId == null) return;
        ComboBoxModel<Supplier> model = cbSupplier.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            Supplier it = model.getElementAt(i);
            if (it != null && it.getId() == supplierId) {
                cbSupplier.setSelectedItem(it);
                return;
            }
        }
    }

    private void loadProducts() {
        try {
            allProducts = productController.findAll();
            productTableModel.setRows(allProducts);
        } catch (Throwable ex) {
            allProducts = new ArrayList<>();
            productTableModel.setRows(allProducts);
            Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    private void wireEvents() {
        btnSubmit.addActionListener(e -> {
            if (viewOnly) {
                result = null;
                dispose();
                return;
            }
            onSave();
        });
        getRootPane().setDefaultButton(btnSubmit);

        btnImportExcel.addActionListener(e -> Toast.info(this, "Nh\u1eadp Excel cho phi\u1ebfu nh\u1eadp: \u0111ang ph\u00e1t tri\u1ec3n"));
        btnAddItem.addActionListener(e -> onAddItem());
        btnEditItem.addActionListener(e -> onEditItem());
        btnDeleteItem.addActionListener(e -> onDeleteItem());

        btnAutoImei.addActionListener(e -> onAutoGenerateImeis());
        btnEnterImei.addActionListener(e -> onEnterImeis());

        tblProducts.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            onSelectProduct();
        });
        cbVariant.addActionListener(e -> {
            syncPriceFromVariant();
            if (!isPerDeviceMode()) resetBatchImeiSelection();
        });
        cbImportMethod.addActionListener(e -> onImportMethodChanged());

        tblItems.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            boolean has = tblItems.getSelectedRow() >= 0;
            btnEditItem.setEnabled(has && editing == null);
            btnDeleteItem.setEnabled(has && editing == null);
            if (has) showSelectedItemDetails();
        });

        txtProductSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyProductSearch(); }
            @Override public void removeUpdate(DocumentEvent e) { applyProductSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { applyProductSearch(); }
        });

        DocumentListener imeiReset = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onImeiInputsChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onImeiInputsChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onImeiInputsChanged(); }
        };
        txtQty.getDocument().addDocumentListener(imeiReset);
        txtImeiStart.getDocument().addDocumentListener(imeiReset);

        txtImeiStart.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onPerDeviceImeiChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onPerDeviceImeiChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onPerDeviceImeiChanged(); }
        });

        txtImeiManualList.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onManualImeisChanged(); }
            @Override public void removeUpdate(DocumentEvent e) { onManualImeisChanged(); }
            @Override public void changedUpdate(DocumentEvent e) { onManualImeisChanged(); }
        });

        onImportMethodChanged();
    }

    private void onImeiInputsChanged() {
        if (!isPerDeviceMode()) resetBatchImeiSelection();
    }

    private void onPerDeviceImeiChanged() {
        if (!isPerDeviceMode()) {
            clearPerDeviceImeiFeedback();
            return;
        }
        schedulePerDeviceImeiLiveValidation();
    }

    private void schedulePerDeviceImeiLiveValidation() {
        perDeviceValidateSeq++;
        if (perDeviceValidateTimer != null) perDeviceValidateTimer.restart();
    }

    private void clearPerDeviceImeiFeedback() {
        if (imeiStartDefaultBg != null) txtImeiStart.setBackground(imeiStartDefaultBg);
        txtImeiStart.setToolTipText(null);
    }

    private void runPerDeviceImeiLiveValidation() {
        if (!isPerDeviceMode()) return;

        String imei = trim(txtImeiStart.getText());
        if (imei.isBlank()) {
            clearPerDeviceImeiFeedback();
            return;
        }

        // Format / length
        if (!isValidImei15Digits(imei)) {
            // Only show error once user starts typing; keep it gentle
            txtImeiStart.setBackground(new Color(255, 200, 200));
            txtImeiStart.setToolTipText("IMEI ph\u1ea3i \u0111\u00fang 15 ch\u1eef s\u1ed1");
            return;
        }

        // Duplicate with existing items (in dialog)
        List<String> single = new ArrayList<>();
        single.add(imei);
        if (itemsTableModel.containsAnyImei(single, -1)) {
            txtImeiStart.setBackground(new Color(255, 200, 200));
            txtImeiStart.setToolTipText("IMEI \u0111\u00e3 t\u1ed3n t\u1ea1i trong danh s\u00e1ch");
            return;
        }

        // Debounced DB check (no reserve)
        final int seqAtStart = perDeviceValidateSeq;
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                try {
                    imeiDao.ensureTable();
                    Set<String> exists = imeiDao.findExisting(single);
                    return exists != null && !exists.isEmpty();
                } catch (Exception ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                if (seqAtStart != perDeviceValidateSeq) return;
                try {
                    Boolean exists = get();
                    if (exists == null) {
                        // avoid toast spam while typing
                        txtImeiStart.setBackground(imeiStartDefaultBg);
                        txtImeiStart.setToolTipText("Kh\u00f4ng th\u1ec3 ki\u1ec3m tra IMEI trong CSDL");
                        return;
                    }
                    if (exists) {
                        txtImeiStart.setBackground(new Color(255, 200, 200));
                        txtImeiStart.setToolTipText("IMEI " + imei + " \u0111\u00e3 c\u00f3 tr\u00ean h\u1ec7 th\u1ed1ng h\u00e3y nh\u1eadp l\u1ea1i IMEI m\u1edbi");
                    } else {
                        clearPerDeviceImeiFeedback();
                    }
                } catch (Exception ignored) {
                    txtImeiStart.setBackground(imeiStartDefaultBg);
                    txtImeiStart.setToolTipText("Kh\u00f4ng th\u1ec3 ki\u1ec3m tra IMEI trong CSDL");
                }
            }
        }.execute();
    }

    private void onManualImeisChanged() {
        if (isPerDeviceMode()) return;
        if (batchImeiMode != BatchImeiMode.ENTER) return;
        // Do not keep stale pending IMEIs while typing
        pendingImeis = new ArrayList<>();
        updateImeiCountLabel();
        scheduleManualImeiLiveValidation();
    }

    private void scheduleManualImeiLiveValidation() {
        manualValidateSeq++;
        if (manualValidateTimer != null) manualValidateTimer.restart();
    }

    private void runManualImeiLiveValidation() {
        if (isPerDeviceMode()) return;
        if (batchImeiMode != BatchImeiMode.ENTER) return;

        int qty = readQtyOrZero();
        JTextArea area = txtImeiManualList;
        try {
            area.getHighlighter().removeAllHighlights();
        } catch (Exception ignored) {
        }

        List<String> lines = splitLinesPreserveOrder(area.getText());
        List<String> imeis = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();

        // Local validation first: format + internal duplicates
        for (int i = 0; i < lines.size(); i++) {
            String raw = trim(lines.get(i));
            if (raw.isBlank()) continue;

            if (!isValidImei15Digits(raw)) {
                highlightLineIndex(area, i, imeiErrorPainter);
                area.setToolTipText("IMEI kh\u00f4ng h\u1ee3p l\u1ec7: " + raw);
                return;
            }
            if (!seen.add(raw)) {
                highlightLineIndex(area, i, imeiErrorPainter);
                area.setToolTipText("IMEI b\u1ecb tr\u00f9ng trong danh s\u00e1ch: " + raw);
                return;
            }
            imeis.add(raw);
        }

        // Duplicate with existing items (in dialog)
        if (!imeis.isEmpty() && itemsTableModel.containsAnyImei(imeis, -1)) {
            String dup = itemsTableModel.findFirstDuplicateWithExisting(imeis);
            if (dup != null) {
                highlightImeiLines(lines, area, dup, imeiErrorPainter);
                area.setToolTipText("IMEI " + dup + " \u0111\u00e3 c\u00f3 trong danh s\u00e1ch s\u1ea3n ph\u1ea9m \u0111\u00e3 th\u00eam");
            } else {
                area.setToolTipText("Danh s\u00e1ch IMEI b\u1ecb tr\u00f9ng v\u1edbi s\u1ea3n ph\u1ea9m \u0111\u00e3 th\u00eam");
            }
            return;
        }

        // Only DB-check when user has entered exactly qty IMEIs (avoid spam while typing)
        if (qty > 0 && imeis.size() == qty) {
            final int seqAtStart = manualValidateSeq;
            area.setToolTipText(null);
            new SwingWorker<Set<String>, Void>() {
                @Override
                protected Set<String> doInBackground() throws Exception {
                    imeiDao.ensureTable();
                    return imeiDao.findExisting(imeis);
                }

                @Override
                protected void done() {
                    if (seqAtStart != manualValidateSeq) return; // outdated
                    try {
                        Set<String> exists = get();
                        if (exists != null && !exists.isEmpty()) {
                            String imeiDup = exists.iterator().next();
                            highlightImeiLines(lines, area, imeiDup, imeiErrorPainter);
                            area.setToolTipText("IMEI " + imeiDup + " \u0111\u00e3 c\u00f3 tr\u00ean h\u1ec7 th\u1ed1ng h\u00e3y nh\u1eadp l\u1ea1i IMEI m\u1edbi");
                        } else {
                            area.setToolTipText(null);
                        }
                    } catch (Exception ex) {
                        // Don't toast while typing; keep UX calm
                        area.setToolTipText("Kh\u00f4ng th\u1ec3 ki\u1ec3m tra IMEI trong CSDL");
                    }
                }
            }.execute();
        } else {
            area.setToolTipText(null);
        }
    }

    private void highlightLineIndex(JTextArea area, int lineIndex, Highlighter.HighlightPainter painter) {
        if (area == null) return;
        try {
            int start = area.getLineStartOffset(lineIndex);
            int end = area.getLineEndOffset(lineIndex);
            area.getHighlighter().addHighlight(start, end, painter);
        } catch (Exception ignored) {
        }
    }

    private boolean isPerDeviceMode() {
        Object sel = cbImportMethod.getSelectedItem();
        String m = sel == null ? "" : String.valueOf(sel);
        return METHOD_PER_DEVICE.equalsIgnoreCase(m);
    }

    private void onImportMethodChanged() {
        boolean perDevice = isPerDeviceMode();
        if (perDevice) {
            txtQty.setText("1");
            txtQty.setEnabled(false);
            lblImei.setText("Nh\u1eadp IMEI \u0111i\u1ec7n tho\u1ea1i");
            txtImeiStart.setEnabled(true);
            btnAutoImei.setEnabled(false);
            btnEnterImei.setEnabled(false);
            txtImeiAutoList.setEnabled(false);
            txtImeiManualList.setEnabled(false);
            lblImeiCount.setVisible(false);
            clearPendingImeis();
            schedulePerDeviceImeiLiveValidation();
        } else {
            txtQty.setEnabled(true);
            lblImei.setText("Nh\u1eadp IMEI \u0111i\u1ec7n tho\u1ea1i");
            txtImeiStart.setEnabled(false);
            txtImeiAutoList.setEnabled(true);
            txtImeiManualList.setEnabled(true);
            lblImeiCount.setVisible(true);
            clearPerDeviceImeiFeedback();
            resetBatchImeiSelection();
        }
    }

    private void installImei15DigitsFilter(JTextField field) {
        if (field == null) return;
        if (!(field.getDocument() instanceof AbstractDocument doc)) return;
        doc.setDocumentFilter(new DocumentFilter() {
            private String normalize(String s) {
                if (s == null || s.isEmpty()) return "";
                StringBuilder sb = new StringBuilder(s.length());
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    if (c >= '0' && c <= '9') sb.append(c);
                }
                return sb.toString();
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string == null) return;
                String digits = normalize(string);
                int currentLen = fb.getDocument().getLength();
                int max = 15;
                int allowed = max - currentLen;
                if (allowed <= 0) return;
                if (digits.length() > allowed) digits = digits.substring(0, allowed);
                super.insertString(fb, offset, digits, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String digits = normalize(text);
                int currentLen = fb.getDocument().getLength();
                int max = 15;
                int newLen = currentLen - length + digits.length();
                if (newLen > max) {
                    int allowed = max - (currentLen - length);
                    if (allowed <= 0) {
                        digits = "";
                    } else if (digits.length() > allowed) {
                        digits = digits.substring(0, allowed);
                    }
                }
                super.replace(fb, offset, length, digits, attrs);
            }
        });
    }

    private void resetBatchImeiSelection() {
        batchImeiMode = BatchImeiMode.NONE;
        clearPendingImeis();
        applyBatchImeiModeUi();
    }

    private void applyBatchImeiModeUi() {
        if (isPerDeviceMode()) return;
        if (batchImeiMode == BatchImeiMode.AUTO) {
            btnAutoImei.setEnabled(true);
            btnEnterImei.setEnabled(false);
            txtImeiAutoList.setEnabled(true);
            txtImeiManualList.setEnabled(false);
            txtImeiManualList.setEditable(false);
            return;
        }
        if (batchImeiMode == BatchImeiMode.ENTER) {
            btnAutoImei.setEnabled(false);
            btnEnterImei.setEnabled(true);
            txtImeiAutoList.setEnabled(false);
            txtImeiManualList.setEnabled(true);
            txtImeiManualList.setEditable(true);
            return;
        }
        btnAutoImei.setEnabled(true);
        btnEnterImei.setEnabled(true);
        txtImeiAutoList.setEnabled(false);
        txtImeiManualList.setEnabled(false);
        txtImeiManualList.setEditable(false);
    }

    private void clearPendingImeis() {
        pendingImeis = new ArrayList<>();
        txtImeiAutoList.setText("");
        txtImeiManualList.setText("");
        try { txtImeiManualList.getHighlighter().removeAllHighlights(); } catch (Exception ignored) {}
        updateImeiCountLabel();
    }

    private void updateImeiCountLabel() {
        int need = readQtyOrZero();
        int have;
        if (isPerDeviceMode()) {
            have = 0;
        } else if (batchImeiMode == BatchImeiMode.AUTO) {
            have = pendingImeis == null ? 0 : pendingImeis.size();
        } else if (batchImeiMode == BatchImeiMode.ENTER) {
            have = countNonBlankLines(txtImeiManualList.getText());
        } else {
            have = 0;
        }
        if (need < 0) need = 0;
        lblImeiCount.setText(have + "/" + need);
    }

    private int countNonBlankLines(String text) {
        if (text == null || text.isEmpty()) return 0;
        String[] lines = text.split("\\R", -1);
        int c = 0;
        for (String raw : lines) {
            if (!trim(raw).isBlank()) c++;
        }
        return c;
    }

    private int readQtyOrZero() {
        int qty;
        try {
            qty = Integer.parseInt(trim(txtQty.getText()));
        } catch (Exception ex) {
            qty = 0;
        }
        return qty;
    }

    private void onAutoGenerateImeis() {
        if (editing != null) return;
        if (isPerDeviceMode()) {
            Toast.warn(this, "Ch\u1ebf \u0111\u1ed9 'Nh\u1eadp t\u1eebng m\u00e1y' kh\u00f4ng d\u00f9ng danh s\u00e1ch IMEI");
            return;
        }

        int qty = readQtyOrZero();
        if (qty <= 0) {
            Toast.warn(this, "S\u1ed1 l\u01b0\u1ee3ng ph\u1ea3i > 0");
            return;
        }

        List<String> out;
        try {
            imeiDao.ensureTable();
            out = buildRandomImeisDbChecked(qty, itemsTableModel.collectAllImeis());
        } catch (Exception ex) {
            Toast.warn(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
            return;
        }

        if (itemsTableModel.containsAnyImei(out, -1)) {
            Toast.warn(this, "Danh s\u00e1ch IMEI b\u1ecb tr\u00f9ng v\u1edbi s\u1ea3n ph\u1ea9m \u0111\u00e3 th\u00eam");
            return;
        }

        pendingImeis = out;
        txtImeiAutoList.setText(String.join("\n", out));
        txtImeiManualList.setText("");
        try { txtImeiManualList.getHighlighter().removeAllHighlights(); } catch (Exception ignored) {}
        updateImeiCountLabel();
        batchImeiMode = BatchImeiMode.AUTO;
        applyBatchImeiModeUi();
    }

    private void onEnterImeis() {
        if (editing != null) return;
        if (isPerDeviceMode()) {
            Toast.warn(this, "Ch\u1ebf \u0111\u1ed9 'Nh\u1eadp t\u1eebng m\u00e1y' nh\u1eadp IMEI tr\u1ef1c ti\u1ebfp \u1edf \u00f4 IMEI");
            return;
        }

        int qty = readQtyOrZero();
        if (qty <= 0) {
            Toast.warn(this, "S\u1ed1 l\u01b0\u1ee3ng ph\u1ea3i > 0");
            return;
        }

        // Inline manual entry: enable right text area, disable left auto area
        pendingImeis = new ArrayList<>();
        txtImeiAutoList.setText("");
        txtImeiManualList.setEditable(true);
        txtImeiManualList.setEnabled(true);
        txtImeiManualList.requestFocusInWindow();
        batchImeiMode = BatchImeiMode.ENTER;
        applyBatchImeiModeUi();
        updateImeiCountLabel();
        scheduleManualImeiLiveValidation();
    }

    private List<String> parseImeisFromMultiline(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        String[] lines = text.split("\\R");
        for (String raw : lines) {
            String s = trim(raw);
            if (s.isBlank()) continue;
            if (!isValidImei15Digits(s)) {
                throw new IllegalArgumentException("IMEI kh\u00f4ng h\u1ee3p l\u1ec7: " + s);
            }
            out.add(s);
        }

        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (String s : out) {
            if (!seen.add(s)) {
                throw new IllegalArgumentException("IMEI b\u1ecb tr\u00f9ng trong danh s\u00e1ch: " + s);
            }
        }
        return out;
    }

    private List<String> splitLinesPreserveOrder(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) return out;
        String[] lines = text.split("\\R", -1);
        for (String s : lines) out.add(s);
        return out;
    }

    private void highlightImeiLines(List<String> lines, JTextArea area, String imei, Highlighter.HighlightPainter painter) {
        if (lines == null || area == null || imei == null || imei.isBlank()) return;
        String target = imei.trim();
        for (int i = 0; i < lines.size(); i++) {
            String s = trim(lines.get(i));
            if (!target.equals(s)) continue;
            try {
                int start = area.getLineStartOffset(i);
                int end = area.getLineEndOffset(i);
                area.getHighlighter().addHighlight(start, end, painter);
            } catch (Exception ignored) {
            }
        }
    }

    private void highlightFirstInvalidLine(List<String> lines, JTextArea area, Highlighter.HighlightPainter painter) {
        if (lines == null || area == null) return;
        for (int i = 0; i < lines.size(); i++) {
            String s = trim(lines.get(i));
            if (s.isBlank()) continue;
            if (!isValidImei15Digits(s)) {
                try {
                    int start = area.getLineStartOffset(i);
                    int end = area.getLineEndOffset(i);
                    area.getHighlighter().addHighlight(start, end, painter);
                } catch (Exception ignored) {
                }
                return;
            }
        }
    }

    private void highlightFromParseError(List<String> lines, JTextArea area, Highlighter.HighlightPainter painter, Exception ex) {
        if (ex == null) {
            highlightFirstInvalidLine(lines, area, painter);
            return;
        }
        String msg = ex.getMessage() == null ? "" : ex.getMessage();
        String prefixInvalid = "IMEI kh\u00f4ng h\u1ee3p l\u1ec7: ";
        String prefixDup = "IMEI b\u1ecb tr\u00f9ng trong danh s\u00e1ch: ";

        if (msg.startsWith(prefixInvalid)) {
            String imei = msg.substring(prefixInvalid.length()).trim();
            if (!imei.isBlank()) {
                highlightImeiLines(lines, area, imei, painter);
                return;
            }
        }
        if (msg.startsWith(prefixDup)) {
            String imei = msg.substring(prefixDup.length()).trim();
            if (!imei.isBlank()) {
                highlightImeiLines(lines, area, imei, painter);
                return;
            }
        }

        // Fallback
        highlightFirstInvalidLine(lines, area, painter);
    }

    private List<String> buildRandomImeisDbChecked(int qty, java.util.Set<String> existingInDialog) {
        HashSet<String> blacklist = new HashSet<>();
        if (existingInDialog != null) blacklist.addAll(existingInDialog);

        List<String> out = new ArrayList<>(qty);
        int maxTries = Math.max(6000, qty * 600);
        int batchSize = 120;

        for (int tries = 0; tries < maxTries && out.size() < qty; tries++) {
            List<String> candidates = new ArrayList<>(batchSize);
            while (candidates.size() < batchSize && out.size() + candidates.size() < qty) {
                long v = Math.floorMod(imeiRandom.nextLong(), 1_000_000_000_000_000L);
                String s = String.format(Locale.ROOT, "%015d", v);
                if (blacklist.contains(s)) continue;
                blacklist.add(s);
                candidates.add(s);
            }

            if (candidates.isEmpty()) continue;

            Set<String> exists = imeiDao.findExisting(candidates);
            for (String s : candidates) {
                if (out.size() >= qty) break;
                if (exists.contains(s)) continue;
                out.add(s);
            }
        }

        if (out.size() != qty) {
            throw new IllegalStateException("Kh\u00f4ng th\u1ec3 sinh \u0111\u1ee7 IMEI ng\u1eabu nhi\u00ean kh\u00f4ng tr\u00f9ng trong CSDL (qty=" + qty + ")");
        }

        // Check again for safety (as requested)
        Set<String> existsAgain = imeiDao.findExisting(out);
        if (!existsAgain.isEmpty()) {
            // Replace any that suddenly exist
            HashSet<String> used = new HashSet<>(blacklist);
            used.addAll(out);
            List<String> fixed = new ArrayList<>();
            for (String s : out) {
                if (!existsAgain.contains(s)) fixed.add(s);
            }
            int need = qty - fixed.size();
            if (need > 0) {
                List<String> more = new ArrayList<>();
                int secondTries = Math.max(4000, need * 800);
                for (int t = 0; t < secondTries && more.size() < need; t++) {
                    long v = Math.floorMod(imeiRandom.nextLong(), 1_000_000_000_000_000L);
                    String s = String.format(Locale.ROOT, "%015d", v);
                    if (!used.add(s)) continue;
                    more.add(s);
                }
                if (more.size() < need) {
                    throw new IllegalStateException("Kh\u00f4ng th\u1ec3 sinh \u0111\u1ee7 IMEI sau khi check l\u1ea1i CSDL");
                }
                // DB-check the new ones, filter until enough
                Set<String> bad = imeiDao.findExisting(more);
                for (String s : more) {
                    if (fixed.size() >= qty) break;
                    if (bad.contains(s)) continue;
                    fixed.add(s);
                }
                if (fixed.size() != qty) {
                    throw new IllegalStateException("Kh\u00f4ng th\u1ec3 sinh \u0111\u1ee7 IMEI sau khi check l\u1ea1i CSDL");
                }
            }
            out = fixed;
        }
        return out;
    }

    private boolean isValidImei15Digits(String text) {
        String s = trim(text);
        if (s.length() != 15) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    private void applyProductSearch() {
        String kw = trim(txtProductSearch.getText()).toLowerCase(Locale.ROOT);
        if (kw.isBlank()) {
            productTableModel.setRows(allProducts);
            return;
        }

        List<Product> out = new ArrayList<>();
        for (Product p : allProducts) {
            if (p == null) continue;
            String id = String.valueOf(p.getId());
            String name = p.getName() == null ? "" : p.getName();
            if (id.toLowerCase(Locale.ROOT).contains(kw) || name.toLowerCase(Locale.ROOT).contains(kw)) {
                out.add(p);
            }
        }
        productTableModel.setRows(out);
    }

    private Product getSelectedProduct() {
        int row = tblProducts.getSelectedRow();
        if (row < 0) return null;
        return productTableModel.getAt(row);
    }

    private void onSelectProduct() {
        Product p = getSelectedProduct();
        if (p == null) return;

        if (!isPerDeviceMode()) resetBatchImeiSelection();

        txtProductId.setText(String.valueOf(p.getId()));
        txtProductName.setText(p.getName() == null ? "" : p.getName());

        try {
            List<ProductVariant> vars = variantController.findByProductId(p.getId());
            cbVariant.setModel(new DefaultComboBoxModel<>(vars.toArray(new ProductVariant[0])));
            if (!vars.isEmpty()) cbVariant.setSelectedIndex(0);
        } catch (Throwable ex) {
            cbVariant.setModel(new DefaultComboBoxModel<>(new ProductVariant[0]));
        }
        syncPriceFromVariant();
    }

    private void syncPriceFromVariant() {
        ProductVariant v = (ProductVariant) cbVariant.getSelectedItem();
        if (v == null) return;
        if (v.getImportPrice() != null && v.getImportPrice() > 0) {
            txtImportPrice.setText(String.valueOf(v.getImportPrice()));
        }
    }

    private void onAddItem() {
        if (editing != null) return;

        Product p = getSelectedProduct();
        if (p == null) {
            Toast.warn(this, "Vui l\u00f2ng ch\u1ecdn s\u1ea3n ph\u1ea9m");
            return;
        }
        ProductVariant v = (ProductVariant) cbVariant.getSelectedItem();
        if (v == null) {
            Toast.warn(this, "Vui l\u00f2ng ch\u1ecdn c\u1ea5u h\u00ecnh");
            return;
        }

        boolean perDevice = isPerDeviceMode();

        int qty = 1;
        if (!perDevice) {
            try {
                qty = Integer.parseInt(trim(txtQty.getText()));
            } catch (Exception ex) {
                qty = 0;
            }
            if (qty <= 0) {
                Toast.warn(this, "S\u1ed1 l\u01b0\u1ee3ng ph\u1ea3i > 0");
                return;
            }
        }

        double price;
        try {
            price = MoneyVND.parseToDouble(trim(txtImportPrice.getText()));
        } catch (Exception ex) {
            price = 0;
        }
        if (price <= 0) {
            Toast.warn(this, "Gi\u00e1 nh\u1eadp ph\u1ea3i > 0");
            return;
        }

        List<String> imeis;
        String imeiDisplay;
        if (perDevice) {
            String imei = trim(txtImeiStart.getText());
            if (!isValidImei15Digits(imei)) {
                Toast.warn(this, "IMEI ph\u1ea3i \u0111\u00fang 15 ch\u1eef s\u1ed1");
                return;
            }
            imeis = new ArrayList<>();
            imeis.add(imei);
            if (itemsTableModel.containsAnyImei(imeis, -1)) {
                Toast.warn(this, "IMEI \u0111\u00e3 t\u1ed3n t\u1ea1i trong danh s\u00e1ch");
                return;
            }
            imeiDisplay = imei;
        } else {
            if (batchImeiMode == BatchImeiMode.AUTO) {
                imeis = pendingImeis == null ? new ArrayList<>() : new ArrayList<>(pendingImeis);
                if (imeis.size() != qty) {
                    Toast.warn(this, "Vui l\u00f2ng 'T\u1ef1 sinh IMEI' ho\u1eb7c 'Nh\u1eadp IMEI' \u0111\u1ee7 s\u1ed1 l\u01b0\u1ee3ng (" + qty + ")");
                    return;
                }
                if (itemsTableModel.containsAnyImei(imeis, -1)) {
                    Toast.warn(this, "Danh s\u00e1ch IMEI b\u1ecb tr\u00f9ng v\u1edbi s\u1ea3n ph\u1ea9m \u0111\u00e3 th\u00eam");
                    return;
                }
            } else if (batchImeiMode == BatchImeiMode.ENTER) {
                List<String> manual = readManualImeisValidated(qty);
                if (manual == null) return;
                imeis = manual;
                pendingImeis = new ArrayList<>(imeis);
            } else {
                Toast.warn(this, "Vui l\u00f2ng ch\u1ecdn 'T\u1ef1 sinh IMEI' ho\u1eb7c 'Nh\u1eadp IMEI' \u0111\u1ee7 s\u1ed1 l\u01b0\u1ee3ng (" + qty + ")");
                return;
            }
            imeiDisplay = buildImeiDisplay(imeis);
        }

        ImportItem newItem = ImportItem.from(p, v, imeiDisplay, imeis, price, qty);

        // Prevent duplicate (receiptId, variantId) inserts later by merging items with same variant/product.
        int existingIndex = itemsTableModel.findIndexByKey(newItem.productId, newItem.variantId);
        if (existingIndex >= 0) {
            ImportItem existing = itemsTableModel.getAt(existingIndex);
            if (existing != null) {
                // If user tries to add same variant with different import price, block (DB can't store 2 different prices for same PK).
                if (Math.round(existing.unitPrice) != Math.round(newItem.unitPrice)) {
                    Toast.warn(this, "Sản phẩm/cấu hình này đã có trong phiếu nhập với đơn giá khác. Vui lòng sửa dòng hiện có thay vì thêm mới.");
                    return;
                }
            }
            itemsTableModel.mergeAt(existingIndex, newItem);
        } else {
            itemsTableModel.add(newItem);
        }
        updateTotalLabel();

        if (perDevice) {
            txtImeiStart.setText("");
            txtImeiStart.requestFocusInWindow();
        } else {
            resetBatchImeiSelection();
        }
    }

    private List<String> readManualImeisValidated(int qty) {
        if (qty <= 0) {
            Toast.warn(this, "S\u1ed1 l\u01b0\u1ee3ng ph\u1ea3i > 0");
            return null;
        }

        JTextArea area = txtImeiManualList;
        Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 200, 200));

        try {
            area.getHighlighter().removeAllHighlights();
        } catch (Exception ignored) {
        }

        List<String> lines = splitLinesPreserveOrder(area.getText());

        List<String> imeis;
        try {
            imeis = parseImeisFromMultiline(area.getText());
        } catch (Exception ex) {
            highlightFromParseError(lines, area, painter, ex);
            Toast.warn(this, ex.getMessage());
            return null;
        }

        if (imeis.size() != qty) {
            Toast.warn(this, "S\u1ed1 IMEI ph\u1ea3i \u0111\u00fang b\u1eb1ng s\u1ed1 l\u01b0\u1ee3ng s\u1ea3n ph\u1ea9m (" + qty + ")");
            return null;
        }

        if (itemsTableModel.containsAnyImei(imeis, -1)) {
            String dup = itemsTableModel.findFirstDuplicateWithExisting(imeis);
            if (dup != null) {
                highlightImeiLines(lines, area, dup, painter);
                Toast.warn(this, "IMEI " + dup + " \u0111\u00e3 c\u00f3 trong danh s\u00e1ch s\u1ea3n ph\u1ea9m \u0111\u00e3 th\u00eam");
            } else {
                Toast.warn(this, "Danh s\u00e1ch IMEI b\u1ecb tr\u00f9ng v\u1edbi s\u1ea3n ph\u1ea9m \u0111\u00e3 th\u00eam");
            }
            return null;
        }

        try {
            imeiDao.ensureTable();
            Set<String> exists = imeiDao.findExisting(imeis);
            if (!exists.isEmpty()) {
                String imeiDup = exists.iterator().next();
                highlightImeiLines(lines, area, imeiDup, painter);
                Toast.warn(this, "IMEI " + imeiDup + " \u0111\u00e3 c\u00f3 tr\u00ean h\u1ec7 th\u1ed1ng h\u00e3y nh\u1eadp l\u1ea1i IMEI m\u1edbi");
                return null;
            }
        } catch (Exception ex) {
            Toast.warn(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
            return null;
        }

        return imeis;
    }

    private void onEditItem() {
        if (editing != null) return;
        int row = tblItems.getSelectedRow();
        if (row < 0) {
            Toast.warn(this, "Vui l\u00f2ng ch\u1ecdn 1 d\u00f2ng");
            return;
        }

        Product p = getSelectedProduct();
        ProductVariant v = (ProductVariant) cbVariant.getSelectedItem();
        if (p == null || v == null) {
            Toast.warn(this, "Vui l\u00f2ng ch\u1ecdn s\u1ea3n ph\u1ea9m/c\u1ea5u h\u00ecnh");
            return;
        }

        boolean perDevice = isPerDeviceMode();

        int qty = 1;
        if (!perDevice) {
            try {
                qty = Integer.parseInt(trim(txtQty.getText()));
            } catch (Exception ex) {
                qty = 0;
            }
            if (qty <= 0) {
                Toast.warn(this, "S\u1ed1 l\u01b0\u1ee3ng ph\u1ea3i > 0");
                return;
            }
        }

        double price;
        try {
            price = MoneyVND.parseToDouble(trim(txtImportPrice.getText()));
        } catch (Exception ex) {
            price = 0;
        }
        if (price <= 0) {
            Toast.warn(this, "Gi\u00e1 nh\u1eadp ph\u1ea3i > 0");
            return;
        }

        List<String> imeis;
        String imeiDisplay;
        if (perDevice) {
            String imei = trim(txtImeiStart.getText());
            if (!isValidImei15Digits(imei)) {
                Toast.warn(this, "IMEI ph\u1ea3i \u0111\u00fang 15 ch\u1eef s\u1ed1");
                return;
            }
            imeis = new ArrayList<>();
            imeis.add(imei);
            if (itemsTableModel.containsAnyImei(imeis, row)) {
                Toast.warn(this, "IMEI \u0111\u00e3 t\u1ed3n t\u1ea1i trong danh s\u00e1ch");
                return;
            }
            imeiDisplay = imei;
        } else {
            imeis = pendingImeis == null ? new ArrayList<>() : new ArrayList<>(pendingImeis);
            if (imeis.size() != qty) {
                Toast.warn(this, "Vui l\u00f2ng 'T\u1ef1 sinh IMEI' ho\u1eb7c 'Nh\u1eadp IMEI' \u0111\u1ee7 s\u1ed1 l\u01b0\u1ee3ng (" + qty + ")");
                return;
            }
            if (itemsTableModel.containsAnyImei(imeis, row)) {
                Toast.warn(this, "Danh s\u00e1ch IMEI b\u1ecb tr\u00f9ng v\u1edbi s\u1ea3n ph\u1ea9m \u0111\u00e3 th\u00eam");
                return;
            }
            imeiDisplay = buildImeiDisplay(imeis);
        }

        itemsTableModel.update(row, ImportItem.from(p, v, imeiDisplay, imeis, price, qty));
        updateTotalLabel();

        if (!perDevice) resetBatchImeiSelection();
    }

    private String buildImeiDisplay(List<String> imeis) {
        if (imeis == null || imeis.isEmpty()) return "";
        if (imeis.size() == 1) return imeis.get(0);

        boolean sequential = true;
        long first;
        try {
            first = Long.parseLong(imeis.get(0));
        } catch (Exception e) {
            sequential = false;
            first = 0;
        }
        if (sequential) {
            for (int i = 1; i < imeis.size(); i++) {
                try {
                    long v = Long.parseLong(imeis.get(i));
                    if (v != first + i) {
                        sequential = false;
                        break;
                    }
                } catch (Exception e) {
                    sequential = false;
                    break;
                }
            }
        }

        if (sequential) {
            return imeis.get(0) + " - " + imeis.get(imeis.size() - 1);
        }
        return String.format(Locale.ROOT, "(IMEI x%d)", imeis.size());
    }

    // keep method removed; exact message is handled in onEnterImeis()

    private void onDeleteItem() {
        if (editing != null) return;
        int row = tblItems.getSelectedRow();
        if (row < 0) {
            Toast.warn(this, "Vui l\u00f2ng ch\u1ecdn 1 d\u00f2ng");
            return;
        }
        itemsTableModel.remove(row);
        updateTotalLabel();
    }

    private void updateTotalLabel() {
        lblTotal.setText(MoneyVND.format(itemsTableModel.computeTotal()));
    }

    private void disableItemEntryForEditMode() {
        txtProductSearch.setEnabled(false);
        tblProducts.setEnabled(false);
        txtImportPrice.setEnabled(false);
        cbVariant.setEnabled(false);
        cbImportMethod.setEnabled(false);
        txtImeiStart.setEnabled(false);
        btnAutoImei.setEnabled(false);
        btnEnterImei.setEnabled(false);
        txtImeiAutoList.setEnabled(false);
        txtImeiManualList.setEnabled(false);
        txtQty.setEnabled(false);
        btnAddItem.setEnabled(false);
        btnImportExcel.setEnabled(false);
        btnEditItem.setEnabled(false);
        btnDeleteItem.setEnabled(false);
    }

    private void applyViewOnlyMode() {
        cbSupplier.setEnabled(false);
        btnSubmit.setText("\u0110\u00f3ng");
    }

    private void loadEditingLines() {
        if (editing == null || editing.getId() <= 0) return;
        try {
            List<ImportReceiptLine> lines = (editing.getLines() != null && !editing.getLines().isEmpty())
                ? editing.getLines()
                : new ImportReceiptLineJdbcDao().findByReceiptId(editing.getId());
            if (lines == null || lines.isEmpty()) return;

            for (ImportReceiptLine l : lines) {
                if (l == null) continue;
                ImportItem it = new ImportItem();
                it.productId = l.getProductId() == null ? 0 : l.getProductId();
                it.productName = l.getProductName() == null ? "" : l.getProductName();
                it.variantId = l.getVariantId();
                it.variantLabel = l.getVariantLabel() == null ? "" : l.getVariantLabel();
                it.unitPrice = l.getUnitPrice() == null ? 0 : l.getUnitPrice();
                it.quantity = l.getQuantity() == null ? 0 : l.getQuantity();

                String[] rrc = parseRamRomColor(it.variantLabel);
                it.ram = rrc[0];
                it.rom = rrc[1];
                it.color = rrc[2];

                List<String> imeis = l.getImeis() == null ? new ArrayList<>() : new ArrayList<>(l.getImeis());
                if (imeis.isEmpty() && editing != null && editing.getId() > 0 && it.quantity > 0) {
                    try {
                        imeis = imeiDao.findImeisByImportReceipt(editing.getId(), it.variantId, it.productId, it.quantity);
                        if (imeis != null && imeis.size() > it.quantity) {
                            imeis = new ArrayList<>(imeis.subList(0, it.quantity));
                        }
                    } catch (Exception ignored) {
                    }
                }
                it.imeis = imeis;
                if (!imeis.isEmpty()) {
                    it.imeiDisplay = buildImeiDisplay(imeis);
                } else if (it.quantity <= 0) {
                    it.imeiDisplay = "";
                } else if (it.quantity == 1) {
                    it.imeiDisplay = "(IMEI x1)";
                } else {
                    it.imeiDisplay = String.format(Locale.ROOT, "(IMEI x%d)", it.quantity);
                }

                itemsTableModel.add(it);
            }
            updateTotalLabel();
        } catch (Exception ex) {
            Toast.warn(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    private String[] parseRamRomColor(String label) {
        String ram = "";
        String rom = "";
        String color = "";
        String s = trim(label);
        if (!s.isBlank()) {
            String[] parts = s.split("/", -1);
            if (parts.length > 0) rom = trim(parts[0]);
            if (parts.length > 1) ram = trim(parts[1]);
            if (parts.length > 2) color = trim(parts[2]);
            if (color.isBlank() && s.contains("-")) {
                String[] p2 = s.split("-", 2);
                if (p2.length > 1) color = trim(p2[1]);
            }
        }
        return new String[] {ram, rom, color};
    }

    private void showSelectedItemDetails() {
        int row = tblItems.getSelectedRow();
        if (row < 0 || row >= itemsTableModel.rows.size()) return;
        ImportItem it = itemsTableModel.rows.get(row);
        if (it == null) return;

        txtProductId.setText(it.productId <= 0 ? "" : String.valueOf(it.productId));
        txtProductName.setText(it.productName == null ? "" : it.productName);
        txtImportPrice.setText(it.unitPrice <= 0 ? "" : String.valueOf(Math.round(it.unitPrice)));
        txtQty.setText(it.quantity <= 0 ? "" : String.valueOf(it.quantity));

        if (it.imeis != null && !it.imeis.isEmpty()) {
            String joined = String.join("\n", it.imeis);
            txtImeiAutoList.setText(joined);
            txtImeiManualList.setText(joined);
        } else {
            txtImeiAutoList.setText(it.imeiDisplay == null ? "" : it.imeiDisplay);
            txtImeiManualList.setText(it.imeiDisplay == null ? "" : it.imeiDisplay);
        }
    }

    private void configureTable(JTable t) {
        t.setFillsViewportHeight(true);
        t.setRowHeight(34);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setDefaultEditor(Object.class, null);
        if (t.getTableHeader() != null) t.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(center);
        }
    }

    private void configureItemsTable(JTable t) {
        configureTable(t);
        t.setRowHeight(38);
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static final class ProductTableModel extends AbstractTableModel {
        private final String[] cols = {"M\u00e3 SP", "T\u00ean s\u1ea3n ph\u1ea9m", "S\u1ed1 l\u01b0\u1ee3ng"};
        private List<Product> rows = new ArrayList<>();

        public void setRows(List<Product> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public Product getAt(int row) {
            if (row < 0 || row >= rows.size()) return null;
            return rows.get(row);
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Product p = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> p.getId();
                case 1 -> p.getName() == null ? "" : p.getName();
                case 2 -> p.getStock() == null ? 0 : p.getStock();
                default -> "";
            };
        }
    }

    private static final class ImportItem {
        long productId;
        String productName;

        Long variantId;
        String variantLabel;

        String imeiDisplay;
        List<String> imeis;
        String ram;
        String rom;
        String color;
        double unitPrice;
        int quantity;

        static ImportItem from(Product p, ProductVariant v, String imeiDisplay, List<String> imeis, double unitPrice, int quantity) {
            ImportItem it = new ImportItem();
            it.productId = p.getId();
            it.productName = p.getName() == null ? "" : p.getName();

            it.variantId = v == null ? null : v.getId();
            it.variantLabel = v == null ? "" : buildVariantLabel(v.getRomName(), v.getRamName(), v.getColorName());

            it.imeiDisplay = imeiDisplay == null ? "" : imeiDisplay;
            it.imeis = imeis == null ? new ArrayList<>() : new ArrayList<>(imeis);
            it.ram = v.getRamName() == null ? "" : v.getRamName();
            it.rom = v.getRomName() == null ? "" : v.getRomName();
            it.color = v.getColorName() == null ? "" : v.getColorName();
            it.unitPrice = unitPrice;
            it.quantity = quantity;
            return it;
        }

        private static String buildVariantLabel(String rom, String ram, String color) {
            StringBuilder sb = new StringBuilder();
            if (rom != null && !rom.isBlank()) sb.append(rom.trim());
            if (ram != null && !ram.isBlank()) {
                if (sb.length() > 0) sb.append("/");
                sb.append(ram.trim());
            }
            if (color != null && !color.isBlank()) {
                if (sb.length() > 0) sb.append("/");
                sb.append(color.trim());
            }
            return sb.toString();
        }
    }

    private static final class ItemsTableModel extends AbstractTableModel {
        private final String[] cols = {"STT", "M\u00e3 SP", "IMEI", "T\u00ean s\u1ea3n ph\u1ea9m", "RAM", "ROM", "M\u00e0u s\u1eafc", "\u0110\u01a1n gi\u00e1", "S\u1ed1 l\u01b0\u1ee3ng"};
        private final List<ImportItem> rows = new ArrayList<>();

        public ImportItem getAt(int index) {
            if (index < 0 || index >= rows.size()) return null;
            return rows.get(index);
        }

        public int findIndexByKey(long productId, Long variantId) {
            for (int i = 0; i < rows.size(); i++) {
                ImportItem it = rows.get(i);
                if (it == null) continue;
                if (it.productId != productId) continue;
                if (!java.util.Objects.equals(it.variantId, variantId)) continue;
                return i;
            }
            return -1;
        }

        public void mergeAt(int index, ImportItem add) {
            if (index < 0 || index >= rows.size() || add == null) return;
            ImportItem base = rows.get(index);
            if (base == null) {
                rows.set(index, add);
                fireTableRowsUpdated(index, index);
                return;
            }

            base.quantity = Math.max(0, base.quantity) + Math.max(0, add.quantity);
            if (base.imeis == null) base.imeis = new ArrayList<>();
            if (add.imeis != null && !add.imeis.isEmpty()) base.imeis.addAll(add.imeis);

            // Update IMEI display based on merged list.
            base.imeiDisplay = buildImeiDisplayLocal(base.imeis, base.quantity);
            fireTableRowsUpdated(index, index);
        }

        private static String buildImeiDisplayLocal(List<String> imeis, int qty) {
            if (imeis == null || imeis.isEmpty()) {
                if (qty <= 0) return "";
                if (qty == 1) return "(IMEI x1)";
                return String.format(Locale.ROOT, "(IMEI x%d)", qty);
            }
            if (imeis.size() == 1) return imeis.get(0);

            boolean sequential = true;
            long first;
            try {
                first = Long.parseLong(imeis.get(0));
            } catch (Exception e) {
                sequential = false;
                first = 0;
            }
            if (sequential) {
                for (int i = 1; i < imeis.size(); i++) {
                    try {
                        long v = Long.parseLong(imeis.get(i));
                        if (v != first + i) {
                            sequential = false;
                            break;
                        }
                    } catch (Exception e) {
                        sequential = false;
                        break;
                    }
                }
            }

            if (sequential) {
                return imeis.get(0) + " - " + imeis.get(imeis.size() - 1);
            }
            return String.format(Locale.ROOT, "(IMEI x%d)", imeis.size());
        }

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int column) { return cols[column]; }

        public void add(ImportItem it) {
            rows.add(it);
            fireTableDataChanged();
        }

        public void update(int index, ImportItem it) {
            if (index < 0 || index >= rows.size()) return;
            rows.set(index, it);
            fireTableRowsUpdated(index, index);
        }

        public void remove(int index) {
            if (index < 0 || index >= rows.size()) return;
            rows.remove(index);
            fireTableDataChanged();
        }

        public boolean isEmpty() {
            return rows.isEmpty();
        }

        public double computeTotal() {
            double sum = 0;
            for (ImportItem it : rows) {
                if (it == null) continue;
                sum += it.unitPrice * it.quantity;
            }
            return sum;
        }

        public java.util.Set<String> collectAllImeis() {
            java.util.HashSet<String> out = new java.util.HashSet<>();
            for (ImportItem it : rows) {
                if (it == null || it.imeis == null) continue;
                out.addAll(it.imeis);
            }
            return out;
        }

        public boolean containsAnyImei(List<String> imeis, int excludeRow) {
            if (imeis == null || imeis.isEmpty()) return false;
            java.util.HashSet<String> existing = new java.util.HashSet<>();
            for (int i = 0; i < rows.size(); i++) {
                if (i == excludeRow) continue;
                ImportItem it = rows.get(i);
                if (it == null || it.imeis == null) continue;
                existing.addAll(it.imeis);
            }
            for (String s : imeis) {
                if (s == null) continue;
                if (existing.contains(s)) return true;
            }
            return false;
        }

        public String findFirstDuplicateWithExisting(List<String> imeis) {
            if (imeis == null || imeis.isEmpty()) return null;
            java.util.HashSet<String> existing = new java.util.HashSet<>();
            for (ImportItem it : rows) {
                if (it == null || it.imeis == null) continue;
                existing.addAll(it.imeis);
            }
            for (String s : imeis) {
                if (s == null) continue;
                if (existing.contains(s)) return s;
            }
            return null;
        }

        public List<String> collectAllImeisValidated() {
            java.util.HashSet<String> seen = new java.util.HashSet<>();
            List<String> out = new ArrayList<>();

            for (ImportItem it : rows) {
                if (it == null) continue;
                if (it.imeis == null || it.imeis.isEmpty()) {
                    throw new IllegalStateException("Thi\u1ebfu danh s\u00e1ch IMEI cho s\u1ea3n ph\u1ea9m: " + it.productId);
                }
                if (it.quantity <= 0) {
                    throw new IllegalStateException("S\u1ed1 l\u01b0\u1ee3ng kh\u00f4ng h\u1ee3p l\u1ec7 cho s\u1ea3n ph\u1ea9m: " + it.productId);
                }
                if (it.imeis.size() != it.quantity) {
                    throw new IllegalStateException("S\u1ed1 IMEI kh\u00f4ng kh\u1edbp s\u1ed1 l\u01b0\u1ee3ng cho s\u1ea3n ph\u1ea9m: " + it.productId);
                }

                for (String imei : it.imeis) {
                    if (!isValidImei15DigitsLocal(imei)) {
                        throw new IllegalStateException("IMEI kh\u00f4ng h\u1ee3p l\u1ec7: " + imei);
                    }
                    if (!seen.add(imei)) {
                        throw new IllegalStateException("IMEI b\u1ecb tr\u00f9ng trong danh s\u00e1ch: " + imei);
                    }
                    out.add(imei);
                }
            }

            return out;
        }

        public List<ImportReceiptLine> toReceiptLines() {
            List<ImportReceiptLine> out = new ArrayList<>();
            for (ImportItem it : rows) {
                if (it == null) continue;

                ImportReceiptLine l = new ImportReceiptLine();
                l.setProductId(it.productId <= 0 ? null : it.productId);
                l.setProductName(it.productName);
                l.setVariantId(it.variantId);
                l.setVariantLabel(it.variantLabel);
                l.setQuantity(it.quantity);
                l.setUnitPrice(Math.round(it.unitPrice));
                l.setImeis(it.imeis == null ? new ArrayList<>() : new ArrayList<>(it.imeis));
                out.add(l);
            }
            return out;
        }

        private static boolean isValidImei15DigitsLocal(String text) {
            if (text == null) return false;
            String s = text.trim();
            if (s.length() != 15) return false;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c < '0' || c > '9') return false;
            }
            return true;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ImportItem it = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> rowIndex + 1;
                case 1 -> it.productId;
                case 2 -> it.imeiDisplay;
                case 3 -> it.productName;
                case 4 -> it.ram;
                case 5 -> it.rom;
                case 6 -> it.color;
                case 7 -> MoneyVND.format(it.unitPrice);
                case 8 -> it.quantity;
                default -> "";
            };
        }
    }
}




