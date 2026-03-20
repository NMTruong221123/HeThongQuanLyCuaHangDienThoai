package com.phonestore.ui.product;

import com.phonestore.controller.AttributeController;
import com.phonestore.controller.ProductController;
import com.phonestore.controller.ProductVariantController;
import com.phonestore.controller.WarehouseZoneController;
import com.phonestore.model.AttributeItem;
import com.phonestore.model.AttributeType;
import com.phonestore.model.Product;
import com.phonestore.model.ProductVariant;
import com.phonestore.model.WarehouseZone;
import com.phonestore.ui.common.images.ImageLoader;
import com.phonestore.ui.common.toast.Toast;

import javax.swing.*;
import javax.swing.DefaultListCellRenderer;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class ProductFormDialog extends JDialog {

    private final ProductController productController = new ProductController();
    private final ProductVariantController variantController = new ProductVariantController();

    private final AttributeController attributeController = new AttributeController();
    private final WarehouseZoneController zoneController = new WarehouseZoneController();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    private final JLabel lblImagePreview = new JLabel();
    private final JTextField txtImagePath = new JTextField();
    private final JButton btnChooseImage = new JButton("Chọn ảnh...");

    private final JTextField txtName = new JTextField();
    private final JComboBox<AttributeItem> cbOrigin = new JComboBox<>();
    private final JTextField txtChip = new JTextField();
    private final JTextField txtBattery = new JTextField();

    private final JTextField txtScreen = new JTextField();
    private final JTextField txtRearCamera = new JTextField();
    private final JTextField txtFrontCamera = new JTextField();
    private final JComboBox<AttributeItem> cbOperatingSystem = new JComboBox<>();

    private final JTextField txtOsVersion = new JTextField();
    private final JTextField txtWarranty = new JTextField();
    private final JComboBox<AttributeItem> cbBrand = new JComboBox<>();
    private final JComboBox<WarehouseZone> cbZone = new JComboBox<>();

    // Variant config UI
    private final JComboBox<AttributeItem> cbRom = new JComboBox<>();
    private final JComboBox<AttributeItem> cbRam = new JComboBox<>();
    private final JComboBox<AttributeItem> cbColor = new JComboBox<>();
    private final JTextField txtImportPrice = new JTextField();
    private final JTextField txtExportPrice = new JTextField();

    private final VariantTableModel variantTableModel = new VariantTableModel();
    private final JTable tblVariants = new JTable(variantTableModel);

    private Product currentProduct;
    private final boolean addMode;
    private boolean readOnly;
    private boolean changed;

    private JPanel infoActionsWrap;
    private JLabel lblInfoHeader;
    private JLabel lblConfigHeader;
    // staging area when creating a new product + its variants before final confirmation
    private Product pendingProduct;
    private final java.util.List<ProductVariant> stagedVariants = new java.util.ArrayList<>();
    private JPanel configActionsWrap;

    public ProductFormDialog(Window owner, Product editing) {
        super(owner, editing == null ? "Thêm sản phẩm" : "Chỉnh sửa sản phẩm", ModalityType.APPLICATION_MODAL);
        this.addMode = (editing == null);
        this.currentProduct = editing;
        initUi();
        loadComboData();
        if (editing != null) {
            bindInfo(editing);
        }
    }

    public boolean isChanged() {
        return changed;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        applyReadOnlyState();
        refreshInfoActions();
        refreshConfigActions();
    }

    private void refreshConfigActions() {
        if (configActionsWrap == null) return;
        configActionsWrap.removeAll();
        configActionsWrap.add(buildConfigActions(), BorderLayout.CENTER);
        configActionsWrap.revalidate();
        configActionsWrap.repaint();
    }

    private void initUi() {
        setSize(1280, 640);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(0, 0));

        cards.add(buildInfoCard(), "INFO");
        cards.add(buildConfigCard(), "CONFIG");
        add(cards, BorderLayout.CENTER);

        configureCombos();
        applyReadOnlyState();
    }

    private void applyReadOnlyState() {
        boolean enabled = !readOnly;

        txtName.setEnabled(enabled);
        cbOrigin.setEnabled(enabled);
        txtChip.setEnabled(enabled);
        txtBattery.setEnabled(enabled);
        txtScreen.setEnabled(enabled);
        txtRearCamera.setEnabled(enabled);
        txtFrontCamera.setEnabled(enabled);
        cbOperatingSystem.setEnabled(enabled);
        txtOsVersion.setEnabled(enabled);
        txtWarranty.setEnabled(enabled);
        cbBrand.setEnabled(enabled);
        cbZone.setEnabled(enabled);
        txtImagePath.setEnabled(enabled);
        btnChooseImage.setEnabled(enabled);

        cbRom.setEnabled(enabled);
        cbRam.setEnabled(enabled);
        cbColor.setEnabled(enabled);
        txtImportPrice.setEnabled(enabled);
        txtExportPrice.setEnabled(enabled);
    }

    private JPanel buildInfoCard() {
        JPanel root = new JPanel(new BorderLayout(0, 0));

        JPanel header = buildInfoHeader(addMode ? "THÊM SẢN PHẨM" : "CHỈNH SỬA SẢN PHẨM");
        root.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(12, 12));
        body.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        body.add(buildImagePanel(), BorderLayout.WEST);
        body.add(buildInfoFormPanel(), BorderLayout.CENTER);

        root.add(body, BorderLayout.CENTER);

        infoActionsWrap = new JPanel(new BorderLayout());
        infoActionsWrap.add(buildInfoActions(), BorderLayout.CENTER);
        root.add(infoActionsWrap, BorderLayout.SOUTH);
        return root;
    }

    private void refreshInfoActions() {
        if (infoActionsWrap == null) return;
        infoActionsWrap.removeAll();
        infoActionsWrap.add(buildInfoActions(), BorderLayout.CENTER);
        infoActionsWrap.revalidate();
        infoActionsWrap.repaint();
    }

    private JPanel buildConfigCard() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        JPanel header = buildConfigHeader("SỬA CẤU HÌNH");
        root.add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(12, 12));
        body.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        body.add(buildVariantEditorPanel(), BorderLayout.NORTH);
        body.add(buildVariantTablePanel(), BorderLayout.CENTER);
        configActionsWrap = new JPanel(new BorderLayout());
        configActionsWrap.add(buildConfigActions(), BorderLayout.CENTER);
        body.add(configActionsWrap, BorderLayout.SOUTH);

        root.add(body, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildHeader(String title) {
        JPanel header = new JPanel(new BorderLayout());
        Color accent = UIManager.getColor("Component.accentColor");
        Color bg = accent != null ? accent : UIManager.getColor("TableHeader.background");
        if (bg != null) {
            header.setBackground(bg);
            header.setOpaque(true);
        }

        JLabel lbl = new JLabel(title, SwingConstants.CENTER);
        lbl.setForeground(UIManager.getColor("Label.foreground"));
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, lbl.getFont().getSize2D() + 6f));
        lbl.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        header.add(lbl, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildInfoHeader(String title) {
        JPanel header = new JPanel(new BorderLayout());
        Color accent = UIManager.getColor("Component.accentColor");
        Color bg = accent != null ? accent : UIManager.getColor("TableHeader.background");
        if (bg != null) {
            header.setBackground(bg);
            header.setOpaque(true);
        }

        lblInfoHeader = new JLabel(title, SwingConstants.CENTER);
        lblInfoHeader.setForeground(UIManager.getColor("Label.foreground"));
        lblInfoHeader.setFont(lblInfoHeader.getFont().deriveFont(Font.BOLD, lblInfoHeader.getFont().getSize2D() + 6f));
        lblInfoHeader.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        header.add(lblInfoHeader, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildConfigHeader(String title) {
        JPanel header = new JPanel(new BorderLayout());
        Color accent = UIManager.getColor("Component.accentColor");
        Color bg = accent != null ? accent : UIManager.getColor("TableHeader.background");
        if (bg != null) {
            header.setBackground(bg);
            header.setOpaque(true);
        }

        lblConfigHeader = new JLabel(title, SwingConstants.CENTER);
        lblConfigHeader.setForeground(UIManager.getColor("Label.foreground"));
        lblConfigHeader.setFont(lblConfigHeader.getFont().deriveFont(Font.BOLD, lblConfigHeader.getFont().getSize2D() + 6f));
        lblConfigHeader.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        header.add(lblConfigHeader, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildImagePanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setPreferredSize(new Dimension(340, 0));

        lblImagePreview.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagePreview.setVerticalAlignment(SwingConstants.CENTER);
        lblImagePreview.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1));
        lblImagePreview.setPreferredSize(new Dimension(320, 320));
        updateImagePreview(null);

        JPanel picker = new JPanel(new BorderLayout(6, 6));
        txtImagePath.setEditable(false);
        txtImagePath.setPreferredSize(new Dimension(0, 34));
        btnChooseImage.addActionListener(e -> onChooseImage());
        picker.add(txtImagePath, BorderLayout.CENTER);
        picker.add(btnChooseImage, BorderLayout.EAST);

        p.add(lblImagePreview, BorderLayout.CENTER);
        p.add(picker, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildInfoFormPanel() {
        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        int row = 0;

        // Row 1: name, origin, chip, battery
        addGridField(grid, gbc, 0, row, "Tên sản phẩm", txtName);
        addGridField(grid, gbc, 1, row, "Xuất xứ", cbOrigin);
        addGridField(grid, gbc, 2, row, "Chip xử lý", txtChip);
        addGridField(grid, gbc, 3, row, "Dung lượng pin", txtBattery);
        row++;

        // Row 2: screen, rear, front, os
        addGridField(grid, gbc, 0, row, "Kích thước màn", txtScreen);
        addGridField(grid, gbc, 1, row, "Camera sau", txtRearCamera);
        addGridField(grid, gbc, 2, row, "Camera trước", txtFrontCamera);
        addGridField(grid, gbc, 3, row, "Hệ điều hành", cbOperatingSystem);
        row++;

        // Row 3: os version, warranty, brand, zone
        addGridField(grid, gbc, 0, row, "Phiên bản HĐH", txtOsVersion);
        addGridField(grid, gbc, 1, row, "Thời gian bảo hành", txtWarranty);
        addGridField(grid, gbc, 2, row, "Hãng", cbBrand);
        addGridField(grid, gbc, 3, row, "Khu vực kho", cbZone);

        return grid;
    }

    private void addGridField(JPanel grid, GridBagConstraints gbc, int col, int row, String label, JComponent field) {
        gbc.gridx = col;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 1;
        grid.add(fieldBlock(label, field), gbc);
    }

    private JComponent fieldBlock(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        p.add(lbl, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        field.setPreferredSize(new Dimension(0, 34));
        return p;
    }

    private JPanel buildInfoActions() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 12));

        if (readOnly) {
            JButton btnViewConfig = new JButton("Xem cấu hình sản phẩm");
            JButton btnCancelView = new JButton("Hủy bỏ");
            // style view-config button
            btnViewConfig.setPreferredSize(new Dimension(180, 36));
            btnViewConfig.setBackground(new Color(23, 162, 184));
            btnViewConfig.setForeground(Color.WHITE);
            btnViewConfig.setOpaque(true);
            btnViewConfig.setBorderPainted(false);
            btnViewConfig.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnViewConfig.addActionListener(e -> showConfig());

            // style cancel button (replaces 'Đóng')
            btnCancelView.setPreferredSize(new Dimension(120, 36));
            btnCancelView.setBackground(new Color(229, 57, 53));
            btnCancelView.setForeground(Color.WHITE);
            btnCancelView.setOpaque(true);
            btnCancelView.setBorderPainted(false);
            btnCancelView.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnCancelView.addActionListener(e -> dispose());

            actions.add(btnViewConfig);
            actions.add(btnCancelView);
            return actions;
        }

        if (addMode) {
            JButton btnAddConfig = new JButton("Thêm cấu hình");
            JButton btnCancel = new JButton("Hủy bỏ");

            btnAddConfig.addActionListener(e -> onCreateThenConfig());
            btnCancel.addActionListener(e -> {
                // discard any staged data and close
                pendingProduct = null;
                stagedVariants.clear();
                dispose();
            });

            // style buttons for add mode
            btnAddConfig.setPreferredSize(new Dimension(150, 38));
            btnAddConfig.setBackground(new Color(40, 167, 69));
            btnAddConfig.setForeground(Color.WHITE);
            btnAddConfig.setOpaque(true);
            btnAddConfig.setBorderPainted(false);
            btnAddConfig.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            btnCancel.setPreferredSize(new Dimension(140, 38));
            btnCancel.setBackground(new Color(229, 57, 53));
            btnCancel.setForeground(Color.WHITE);
            btnCancel.setOpaque(true);
            btnCancel.setBorderPainted(false);
            btnCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            actions.add(btnAddConfig);
            actions.add(btnCancel);
            getRootPane().setDefaultButton(btnAddConfig);
            return actions;
        }

        JButton btnSaveInfo = new JButton("Lưu thông tin");
        JButton btnEditConfig = new JButton("Sửa cấu hình");
        JButton btnCancel = new JButton("Hủy bỏ");

        btnSaveInfo.addActionListener(e -> onUpdateInfo());
        btnEditConfig.addActionListener(e -> showConfig());
        btnCancel.addActionListener(e -> dispose());

        // style info-mode buttons
        btnSaveInfo.setPreferredSize(new Dimension(150, 38));
        btnSaveInfo.setBackground(new Color(31, 119, 204));
        btnSaveInfo.setForeground(Color.WHITE);
        btnSaveInfo.setOpaque(true);
        btnSaveInfo.setBorderPainted(false);
        btnSaveInfo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnEditConfig.setPreferredSize(new Dimension(130, 38));
        btnEditConfig.setBackground(new Color(255, 152, 0));
        btnEditConfig.setForeground(Color.WHITE);
        btnEditConfig.setOpaque(true);
        btnEditConfig.setBorderPainted(false);
        btnEditConfig.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnCancel.setPreferredSize(new Dimension(140, 38));
        btnCancel.setBackground(new Color(229, 57, 53));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setOpaque(true);
        btnCancel.setBorderPainted(false);
        btnCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        actions.add(btnSaveInfo);
        actions.add(btnEditConfig);
        actions.add(btnCancel);
        getRootPane().setDefaultButton(btnSaveInfo);
        return actions;
    }

    private JPanel buildVariantEditorPanel() {
        JPanel wrap = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        addGridField(wrap, gbc, 0, 0, "ROM", cbRom);
        addGridField(wrap, gbc, 1, 0, "RAM", cbRam);
        addGridField(wrap, gbc, 2, 0, "Màu sắc", cbColor);
        addGridField(wrap, gbc, 3, 0, "Giá nhập", txtImportPrice);
        addGridField(wrap, gbc, 4, 0, "Giá xuất", txtExportPrice);

        return wrap;
    }

    private JPanel buildVariantTablePanel() {
        JPanel p = new JPanel(new BorderLayout());

        tblVariants.setFillsViewportHeight(true);
        tblVariants.setRowHeight(34);
        tblVariants.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblVariants.setDefaultEditor(Object.class, null);
        if (tblVariants.getTableHeader() != null) {
            tblVariants.getTableHeader().setReorderingAllowed(false);
        }

        tblVariants.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            onVariantSelected();
        });

        // center-align all columns for better readability
        javax.swing.table.DefaultTableCellRenderer centerRenderer = new javax.swing.table.DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < variantTableModel.getColumnCount(); i++) {
            tblVariants.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        if (variantTableModel.getColumnCount() > 1) {
            tblVariants.getColumnModel().getColumn(1).setPreferredWidth(120);
            tblVariants.getColumnModel().getColumn(2).setPreferredWidth(80);
            tblVariants.getColumnModel().getColumn(3).setPreferredWidth(120);
        }

        JScrollPane sp = new JScrollPane(tblVariants);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildConfigActions() {
        JPanel actions = new JPanel(new BorderLayout());

        if (readOnly) {
            JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 10));
            JButton btnBack = new JButton("Quay lại");
            JButton btnCancel = new JButton("Hủy bỏ");
            btnBack.setPreferredSize(new Dimension(140, 36));
            btnBack.setBackground(new Color(23, 162, 184));
            btnBack.setForeground(Color.WHITE);
            btnBack.setOpaque(true);
            btnBack.setBorderPainted(false);
            btnBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnBack.addActionListener(e -> showInfo());

            btnCancel.setPreferredSize(new Dimension(140, 36));
            btnCancel.setBackground(new Color(229, 57, 53));
            btnCancel.setForeground(Color.WHITE);
            btnCancel.setOpaque(true);
            btnCancel.setBorderPainted(false);
            btnCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnCancel.addActionListener(e -> dispose());

            nav.add(btnBack);
            nav.add(btnCancel);
            actions.add(nav, BorderLayout.SOUTH);
            return actions;
        }

        JPanel crud = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        JButton btnAdd = new JButton("Thêm");
        JButton btnEdit = new JButton("Sửa");
        JButton btnDelete = new JButton("Xóa cấu hình");
        JButton btnRefresh = new JButton("Làm mới");

        btnAdd.addActionListener(e -> onVariantCreate());
        btnEdit.addActionListener(e -> onVariantUpdate());
        btnDelete.addActionListener(e -> onVariantDelete());
        btnRefresh.addActionListener(e -> loadVariants());

        crud.add(btnAdd);
        crud.add(btnEdit);
        crud.add(btnDelete);
        crud.add(btnRefresh);
        // style CRUD buttons
        btnAdd.setPreferredSize(new Dimension(110, 34));
        btnAdd.setBackground(new Color(40, 167, 69));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setOpaque(true);
        btnAdd.setBorderPainted(false);
        btnAdd.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnEdit.setPreferredSize(new Dimension(110, 34));
        btnEdit.setBackground(new Color(255, 152, 0));
        btnEdit.setForeground(Color.WHITE);
        btnEdit.setOpaque(true);
        btnEdit.setBorderPainted(false);
        btnEdit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnDelete.setPreferredSize(new Dimension(130, 34));
        btnDelete.setBackground(new Color(229, 57, 53));
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setOpaque(true);
        btnDelete.setBorderPainted(false);
        btnDelete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnRefresh.setPreferredSize(new Dimension(110, 34));
        btnRefresh.setBackground(new Color(108, 117, 125));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setOpaque(true);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 10));
        JButton btnConfirm = new JButton("Xác nhận thêm");
        JButton btnBack = new JButton("Quay lại trang");
        JButton btnCancel = new JButton("Hủy bỏ");
        btnConfirm.addActionListener(e -> {
            // If there's a pending (staged) product, persist it first along with staged variants
            if (pendingProduct != null && pendingProduct.getId() <= 0) {
                try {
                    Product created = productController.create(pendingProduct);
                    // persist staged variants
                    for (ProductVariant sv : new ArrayList<>(stagedVariants)) {
                        sv.setProductId((int) created.getId());
                        variantController.create(sv);
                    }
                    currentProduct = created;
                    pendingProduct = null;
                    stagedVariants.clear();
                    loadVariants();
                    Toast.info(this, "Đã thêm sản phẩm");
                } catch (Throwable ex) {
                    Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
                    return;
                }
            }
            if (lblInfoHeader != null) lblInfoHeader.setText("THÊM SẢN PHẨM");
            if (lblConfigHeader != null) lblConfigHeader.setText("SỬA CẤU HÌNH");
            changed = true;
            // close dialog after confirming add
            dispose();
        });
        btnBack.addActionListener(e -> showInfo());
        // style confirm button
        btnConfirm.setPreferredSize(new Dimension(140, 36));
        btnConfirm.setBackground(new Color(31, 119, 204));
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setOpaque(true);
        btnConfirm.setBorderPainted(false);
        btnConfirm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnBack.setPreferredSize(new Dimension(140, 36));
        btnBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnCancel.addActionListener(e -> {
            // cancel while in config: discard staged data and close
            pendingProduct = null;
            stagedVariants.clear();
            dispose();
        });
        btnCancel.setPreferredSize(new Dimension(120, 36));
        btnCancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nav.add(btnConfirm);
        nav.add(btnBack);
        nav.add(btnCancel);

        actions.add(crud, BorderLayout.NORTH);
        actions.add(nav, BorderLayout.SOUTH);
        return actions;
    }

    private void bindInfo(Product p) {
        if (p == null) {
            clearInfoFields();
            return;
        }
        txtName.setText(value(p.getName()));
        txtImagePath.setText(value(p.getImagePath()));
        updateImagePreview(p.getImagePath());

        selectAttribute(cbOrigin, p.getOriginId());
        txtChip.setText(value(p.getChipProcessor()));
        txtBattery.setText(p.getBatteryCapacity() == null ? "" : String.valueOf(p.getBatteryCapacity()));
        txtScreen.setText(p.getScreenSize() == null ? "" : String.valueOf(p.getScreenSize()));
        txtRearCamera.setText(value(p.getRearCamera()));
        txtFrontCamera.setText(value(p.getFrontCamera()));
        selectAttribute(cbOperatingSystem, p.getOperatingSystemId());
        txtOsVersion.setText(p.getOsVersion() == null ? "" : String.valueOf(p.getOsVersion()));
        txtWarranty.setText(p.getWarrantyMonths() == null ? "" : String.valueOf(p.getWarrantyMonths()));
        selectAttribute(cbBrand, p.getBrandId());
        selectZone(cbZone, p.getZoneId());
    }

    private void clearInfoFields() {
        txtName.setText("");
        txtImagePath.setText("");
        updateImagePreview(null);
        cbOrigin.setSelectedIndex(-1);
        txtChip.setText("");
        txtBattery.setText("");
        txtScreen.setText("");
        txtRearCamera.setText("");
        txtFrontCamera.setText("");
        cbOperatingSystem.setSelectedIndex(-1);
        txtOsVersion.setText("");
        txtWarranty.setText("");
        cbBrand.setSelectedIndex(-1);
        cbZone.setSelectedIndex(-1);
    }

    private void showConfig() {
        if (currentProduct == null && pendingProduct == null) {
            Toast.warn(this, "Vui lòng tạo sản phẩm trước");
            return;
        }
        // ensure actions reflect readOnly state
        loadVariants();
        refreshConfigActions();
        cardLayout.show(cards, "CONFIG");
    }

    private void showInfo() {
        cardLayout.show(cards, "INFO");
        // bind staged product into info form if present
        if (pendingProduct != null) {
            bindInfo(pendingProduct);
        } else if (currentProduct != null && currentProduct.getId() > 0) {
            bindInfo(currentProduct);
        } else {
            clearInfoFields();
        }
    }

    private void onChooseImage() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int res = fc.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (f == null) return;
        txtImagePath.setText(f.getAbsolutePath());
        updateImagePreview(f.getAbsolutePath());
    }

    private void updateImagePreview(String path) {
        Icon icon = null;
        String p = trim(path);
        if (!p.isBlank()) {
            icon = ImageLoader.loadImageFromDisk(p, 320, 320);
        }
        if (icon == null) {
            lblImagePreview.setIcon(null);
            lblImagePreview.setText("(Chưa có ảnh)");
        } else {
            lblImagePreview.setText("");
            lblImagePreview.setIcon(icon);
        }
    }

    private void onCreateThenConfig() {
        Product p;
        try {
            p = buildProductFromInfo(true);
        } catch (IllegalArgumentException ex) {
            Toast.warn(this, ex.getMessage());
            return;
        }

        // stage product without persisting yet
        pendingProduct = p;
        currentProduct = pendingProduct;
        stagedVariants.clear();
        changed = true;
        Toast.info(this, "Chuẩn bị thêm cấu hình");
        showConfig();
    }

    private void onUpdateInfo() {
        Product p;
        try {
            p = buildProductFromInfo(false);
        } catch (IllegalArgumentException ex) {
            Toast.warn(this, ex.getMessage());
            return;
        }

        try {
            productController.update(p);
            currentProduct = p;
            changed = true;
            Toast.info(this, "Đã cập nhật sản phẩm");
        } catch (Throwable ex) {
            Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    private Product buildProductFromInfo(boolean creating) {
        String name = trim(txtName.getText());
        if (name.isBlank()) throw new IllegalArgumentException("Tên sản phẩm là bắt buộc");

        AttributeItem origin = (AttributeItem) cbOrigin.getSelectedItem();
        if (origin == null || origin.getId() <= 0) throw new IllegalArgumentException("Vui lòng chọn Xuất xứ");

        String chip = trim(txtChip.getText());
        if (chip.isBlank()) throw new IllegalArgumentException("Chip xử lý là bắt buộc");

        Integer battery = parseRequiredInt(txtBattery.getText(), "Dung lượng pin");
        Double screen = parseRequiredDouble(txtScreen.getText(), "Kích thước màn");

        String rear = trim(txtRearCamera.getText());
        if (rear.isBlank()) throw new IllegalArgumentException("Camera sau là bắt buộc");

        String front = trim(txtFrontCamera.getText());
        if (front.isBlank()) throw new IllegalArgumentException("Camera trước là bắt buộc");

        AttributeItem os = (AttributeItem) cbOperatingSystem.getSelectedItem();
        if (os == null || os.getId() <= 0) throw new IllegalArgumentException("Vui lòng chọn Hệ điều hành");

        Integer osVersion = parseRequiredInt(txtOsVersion.getText(), "Phiên bản HĐH");
        Integer warranty = parseRequiredInt(txtWarranty.getText(), "Thời gian bảo hành");

        AttributeItem brand = (AttributeItem) cbBrand.getSelectedItem();
        if (brand == null || brand.getId() <= 0) throw new IllegalArgumentException("Vui lòng chọn Hãng");

        WarehouseZone zone = (WarehouseZone) cbZone.getSelectedItem();
        if (zone == null || zone.getId() <= 0) throw new IllegalArgumentException("Vui lòng chọn Khu vực kho");

        Product p = new Product();
        if (!creating) {
            if (currentProduct == null || currentProduct.getId() <= 0) {
                throw new IllegalArgumentException("Không tìm thấy sản phẩm để cập nhật");
            }
            p.setId(currentProduct.getId());
            p.setStock(currentProduct.getStock() == null ? 0 : currentProduct.getStock());
            p.setStatus(currentProduct.getStatus() == null ? 1 : currentProduct.getStatus());
        } else {
            p.setStock(0);
            p.setStatus(1);
        }

        p.setName(name);
        p.setOriginId((int) origin.getId());
        p.setChipProcessor(chip);
        p.setBatteryCapacity(battery);
        p.setScreenSize(screen);
        p.setRearCamera(rear);
        p.setFrontCamera(front);
        p.setOperatingSystemId((int) os.getId());
        p.setOsVersion(osVersion);
        p.setWarrantyMonths(warranty);
        p.setBrandId((int) brand.getId());
        p.setZoneId((int) zone.getId());
        String img = trim(txtImagePath.getText());
        p.setImagePath(img.isBlank() ? null : img);
        return p;
    }

    private void loadVariants() {
        // If we're staging a new product (not persisted yet), show staged variants
        if (pendingProduct != null && pendingProduct.getId() <= 0) {
            variantTableModel.setRows(new ArrayList<>(stagedVariants));
            return;
        }
        if (currentProduct == null || currentProduct.getId() <= 0) {
            variantTableModel.setRows(new ArrayList<>());
            return;
        }
        try {
            List<ProductVariant> list = variantController.findByProductId(currentProduct.getId());
            variantTableModel.setRows(list);
        } catch (Throwable ex) {
            Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
            variantTableModel.setRows(new ArrayList<>());
        }
    }

    private void onVariantSelected() {
        ProductVariant v = getSelectedVariant();
        if (v == null) return;
        selectAttribute(cbRom, v.getRomId());
        selectAttribute(cbRam, v.getRamId());
        selectAttribute(cbColor, v.getColorId());
        txtImportPrice.setText(v.getImportPrice() == null ? "" : String.valueOf(v.getImportPrice()));
        txtExportPrice.setText(v.getExportPrice() == null ? "" : String.valueOf(v.getExportPrice()));
    }

    private ProductVariant getSelectedVariant() {
        int row = tblVariants.getSelectedRow();
        if (row < 0) return null;
        return variantTableModel.getAt(row);
    }

    private void onVariantCreate() {
        // preserve user inputs
        AttributeItem romSel = (AttributeItem) cbRom.getSelectedItem();
        AttributeItem ramSel = (AttributeItem) cbRam.getSelectedItem();
        AttributeItem colorSel = (AttributeItem) cbColor.getSelectedItem();
        String impVal = txtImportPrice.getText();
        String expVal = txtExportPrice.getText();

        ProductVariant v;
        try {
            v = buildVariantFromInputs(false);
        } catch (IllegalArgumentException ex) {
            Toast.warn(this, ex.getMessage());
            return;
        }

        // If staging a new product, add variant to staged list instead of persisting
        if (pendingProduct != null && pendingProduct.getId() <= 0) {
            // ensure id is unique in staged list (use decreasing negative temporary ids)
            long tempId = -1L;
            for (ProductVariant sv : stagedVariants) {
                if (sv.getId() < tempId) tempId = sv.getId();
            }
            v.setId(tempId - 1L);
            stagedVariants.add(v);
            changed = true;
            loadVariants();
            // restore inputs
            if (romSel != null) cbRom.setSelectedItem(romSel);
            if (ramSel != null) cbRam.setSelectedItem(ramSel);
            if (colorSel != null) cbColor.setSelectedItem(colorSel);
            txtImportPrice.setText(impVal);
            txtExportPrice.setText(expVal);
            Toast.info(this, "Đã thêm cấu hình (tạm)");
            return;
        }

        try {
            variantController.create(v);
            changed = true;
            loadVariants();
            // restore inputs after persisted add
            if (romSel != null) cbRom.setSelectedItem(romSel);
            if (ramSel != null) cbRam.setSelectedItem(ramSel);
            if (colorSel != null) cbColor.setSelectedItem(colorSel);
            txtImportPrice.setText(impVal);
            txtExportPrice.setText(expVal);
            Toast.info(this, "Đã thêm cấu hình");
        } catch (Throwable ex) {
            Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    private void onVariantUpdate() {
        ProductVariant selected = getSelectedVariant();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 cấu hình");
            return;
        }
        // preserve inputs
        AttributeItem romSel = (AttributeItem) cbRom.getSelectedItem();
        AttributeItem ramSel = (AttributeItem) cbRam.getSelectedItem();
        AttributeItem colorSel = (AttributeItem) cbColor.getSelectedItem();
        String impVal = txtImportPrice.getText();
        String expVal = txtExportPrice.getText();

        ProductVariant v;
        try {
            v = buildVariantFromInputs(true);
            v.setId(selected.getId());
        } catch (IllegalArgumentException ex) {
            Toast.warn(this, ex.getMessage());
            return;
        }
        // If staging, update staged list
        if (pendingProduct != null && pendingProduct.getId() <= 0) {
            int row = tblVariants.getSelectedRow();
            if (row >= 0 && row < stagedVariants.size()) {
                stagedVariants.set(row, v);
                changed = true;
                loadVariants();
                // restore inputs
                if (romSel != null) cbRom.setSelectedItem(romSel);
                if (ramSel != null) cbRam.setSelectedItem(ramSel);
                if (colorSel != null) cbColor.setSelectedItem(colorSel);
                txtImportPrice.setText(impVal);
                txtExportPrice.setText(expVal);
                Toast.info(this, "Đã cập nhật cấu hình (tạm)");
                return;
            }
            Toast.warn(this, "Không tìm thấy cấu hình tạm để cập nhật");
            return;
        }

        try {
            variantController.update(v);
            changed = true;
            loadVariants();
            Toast.info(this, "Đã cập nhật cấu hình");
        } catch (Throwable ex) {
            Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    private void onVariantDelete() {
        ProductVariant selected = getSelectedVariant();
        if (selected == null) {
            Toast.warn(this, "Vui lòng chọn 1 cấu hình");
            return;
        }
        // preserve inputs
        AttributeItem romSel = (AttributeItem) cbRom.getSelectedItem();
        AttributeItem ramSel = (AttributeItem) cbRam.getSelectedItem();
        AttributeItem colorSel = (AttributeItem) cbColor.getSelectedItem();
        String impVal = txtImportPrice.getText();
        String expVal = txtExportPrice.getText();

        // If staging, remove from staged list
        if (pendingProduct != null && pendingProduct.getId() <= 0) {
            int row = tblVariants.getSelectedRow();
            if (row >= 0 && row < stagedVariants.size()) {
                stagedVariants.remove(row);
                changed = true;
                loadVariants();
                // restore inputs
                if (romSel != null) cbRom.setSelectedItem(romSel);
                if (ramSel != null) cbRam.setSelectedItem(ramSel);
                if (colorSel != null) cbColor.setSelectedItem(colorSel);
                txtImportPrice.setText(impVal);
                txtExportPrice.setText(expVal);
                Toast.info(this, "Đã xóa cấu hình (tạm)");
                return;
            }
            Toast.warn(this, "Không tìm thấy cấu hình tạm để xóa");
            return;
        }

        try {
            variantController.delete(selected.getId());
            changed = true;
            loadVariants();
            Toast.info(this, "Đã xóa cấu hình");
        } catch (Throwable ex) {
            Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    private ProductVariant buildVariantFromInputs(boolean updating) {
        AttributeItem rom = (AttributeItem) cbRom.getSelectedItem();
        if (rom == null || rom.getId() <= 0) throw new IllegalArgumentException("Vui lòng chọn ROM");
        AttributeItem ram = (AttributeItem) cbRam.getSelectedItem();
        if (ram == null || ram.getId() <= 0) throw new IllegalArgumentException("Vui lòng chọn RAM");
        AttributeItem color = (AttributeItem) cbColor.getSelectedItem();
        if (color == null || color.getId() <= 0) throw new IllegalArgumentException("Vui lòng chọn Màu sắc");

        Integer importPrice = parseRequiredInt(txtImportPrice.getText(), "Giá nhập");
        Integer exportPrice = parseRequiredInt(txtExportPrice.getText(), "Giá xuất");

        ProductVariant v = new ProductVariant();
        int pid = 0;
        if (currentProduct != null && currentProduct.getId() > 0) {
            pid = (int) currentProduct.getId();
        }
        v.setProductId(pid);
        v.setRomId((int) rom.getId());
        v.setRomName(rom.getName());
        v.setRamId((int) ram.getId());
        v.setRamName(ram.getName());
        v.setColorId((int) color.getId());
        v.setColorName(color.getName());
        v.setImportPrice(importPrice);
        v.setExportPrice(exportPrice);

        if (!updating) {
            v.setStock(0);
            v.setStatus(1);
        }
        return v;
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private String value(String s) {
        return s == null ? "" : s;
    }

    private Integer parseRequiredInt(String s, String label) {
        String v = trim(s);
        if (v.isBlank()) throw new IllegalArgumentException(label + " là bắt buộc");
        try {
            return Integer.parseInt(v);
        } catch (Exception e) {
            throw new IllegalArgumentException(label + " không hợp lệ");
        }
    }

    private Double parseRequiredDouble(String s, String label) {
        String v = trim(s);
        if (v.isBlank()) throw new IllegalArgumentException(label + " là bắt buộc");
        try {
            return Double.parseDouble(v);
        } catch (Exception e) {
            throw new IllegalArgumentException(label + " không hợp lệ");
        }
    }

    private void configureCombos() {
        DefaultListCellRenderer nameOnly = new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AttributeItem a) {
                    setText(a.getName() == null ? "" : a.getName());
                }
                if (value instanceof WarehouseZone z) {
                    setText(z.getName() == null ? "" : z.getName());
                }
                return this;
            }
        };

        cbOrigin.setRenderer(nameOnly);
        cbBrand.setRenderer(nameOnly);
        cbOperatingSystem.setRenderer(nameOnly);
        cbRom.setRenderer(nameOnly);
        cbRam.setRenderer(nameOnly);
        cbColor.setRenderer(nameOnly);
        cbZone.setRenderer(nameOnly);
    }

    private void loadComboData() {
        try {
            List<AttributeItem> origins = attributeController.findByType(AttributeType.ORIGIN);
            cbOrigin.setModel(new DefaultComboBoxModel<>(origins.toArray(new AttributeItem[0])));

            List<AttributeItem> osList = attributeController.findByType(AttributeType.OPERATING_SYSTEM);
            cbOperatingSystem.setModel(new DefaultComboBoxModel<>(osList.toArray(new AttributeItem[0])));

            List<AttributeItem> brands = attributeController.findByType(AttributeType.BRAND);
            cbBrand.setModel(new DefaultComboBoxModel<>(brands.toArray(new AttributeItem[0])));

            List<AttributeItem> roms = attributeController.findByType(AttributeType.ROM);
            cbRom.setModel(new DefaultComboBoxModel<>(roms.toArray(new AttributeItem[0])));

            List<AttributeItem> rams = attributeController.findByType(AttributeType.RAM);
            cbRam.setModel(new DefaultComboBoxModel<>(rams.toArray(new AttributeItem[0])));

            List<AttributeItem> colors = attributeController.findByType(AttributeType.COLOR);
            cbColor.setModel(new DefaultComboBoxModel<>(colors.toArray(new AttributeItem[0])));

            List<WarehouseZone> zones = zoneController.findAll();
            // Only include active zones (trangthai == 1) for product zone selection
            java.util.List<WarehouseZone> activeZones = new java.util.ArrayList<>();
            if (zones != null) {
                for (WarehouseZone z : zones) {
                    if (z != null && z.getStatus() != null && z.getStatus() == 1) activeZones.add(z);
                }
            }
            cbZone.setModel(new DefaultComboBoxModel<>(activeZones.toArray(new WarehouseZone[0])));
        } catch (Throwable ex) {
            Toast.error(this, ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }
    }

    private void selectAttribute(JComboBox<AttributeItem> cb, Integer id) {
        if (id == null) return;
        ComboBoxModel<AttributeItem> model = cb.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            AttributeItem it = model.getElementAt(i);
            if (it != null && it.getId() == id.longValue()) {
                cb.setSelectedItem(it);
                return;
            }
        }
    }

    private void selectZone(JComboBox<WarehouseZone> cb, Integer id) {
        if (id == null) return;
        ComboBoxModel<WarehouseZone> model = cb.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            WarehouseZone it = model.getElementAt(i);
            if (it != null && it.getId() == id.longValue()) {
                cb.setSelectedItem(it);
                return;
            }
        }
    }

    private static final class VariantTableModel extends AbstractTableModel {
        private final String[] cols = {"ID", "ROM", "RAM", "Màu", "Giá nhập", "Giá xuất", "Tồn", "Trạng thái"};
        private List<ProductVariant> rows = new ArrayList<>();

        public void setRows(List<ProductVariant> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        public ProductVariant getAt(int row) {
            if (row < 0 || row >= rows.size()) return null;
            return rows.get(row);
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
            ProductVariant v = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> v.getId() > 0 ? v.getId() : (rowIndex + 1);
                case 1 -> v.getRomName() == null ? "" : v.getRomName();
                case 2 -> v.getRamName() == null ? "" : v.getRamName();
                case 3 -> v.getColorName() == null ? "" : v.getColorName();
                case 4 -> v.getImportPrice();
                case 5 -> v.getExportPrice();
                case 6 -> v.getStock();
                case 7 -> v.getStatus();
                default -> "";
            };
        }
    }

}
