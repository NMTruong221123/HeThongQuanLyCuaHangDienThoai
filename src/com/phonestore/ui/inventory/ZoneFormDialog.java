package com.phonestore.ui.inventory;

import com.phonestore.model.WarehouseZone;
import com.phonestore.ui.common.toast.Toast;

import javax.swing.*;
import java.awt.*;

public class ZoneFormDialog extends JDialog {

    private final JTextField txtName = new JTextField();
    private final JTextArea txtNote = new JTextArea();
    private final JComboBox<StatusItem> cboStatus = new JComboBox<>(new StatusItem[] {new StatusItem("Hoạt động", 1), new StatusItem("Ngừng", 0)});

    private WarehouseZone result;
    private final WarehouseZone editing;
    private final boolean readOnly;
    private JButton btnSave;

    public ZoneFormDialog(Window owner, WarehouseZone editing) {
        this(owner, editing, false);
    }

    public ZoneFormDialog(Window owner, WarehouseZone editing, boolean readOnly) {
        super(owner, editing == null ? "Thêm khu vực kho" : (readOnly ? "Chi tiết khu vực kho" : "Sửa khu vực kho"), ModalityType.APPLICATION_MODAL);
        this.editing = editing;
        this.readOnly = readOnly;
        initUi();
        if (editing != null) {
            txtName.setText(editing.getName());
            txtNote.setText(editing.getNote() == null ? "" : editing.getNote());
            if (editing.getStatus() != null && editing.getStatus() == 1) {
                cboStatus.setSelectedItem(new StatusItem("Hoạt động", 1));
            } else {
                cboStatus.setSelectedItem(new StatusItem("Ngừng", 0));
            }
        } else {
            cboStatus.setSelectedItem(new StatusItem("Hoạt động", 1));
        }
    }

    public WarehouseZone getResult() {
        return result;
    }

    private void initUi() {
        setSize(520, 320);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0, 123, 192));
        header.setPreferredSize(new Dimension(0, 72));
        String headerText = (editing == null) ? "THÊM KHU VỰC KHO" : (readOnly ? "CHI TIẾT KHU VỰC KHO" : "SỬA KHU VỰC KHO");
        JLabel lblHeader = new JLabel(headerText, SwingConstants.CENTER);
        lblHeader.setForeground(Color.white);
        lblHeader.setFont(lblHeader.getFont().deriveFont(Font.BOLD, 18f));
        header.add(lblHeader, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // Form body
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        form.add(new JLabel("Tên khu vực kho"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        txtName.setPreferredSize(new Dimension(0, 40));
        form.add(txtName, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        form.add(new JLabel("Trạng thái"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        cboStatus.setPreferredSize(new Dimension(160, 28));
        form.add(cboStatus, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        form.add(new JLabel("Ghi chú"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        txtNote.setLineWrap(true);
        txtNote.setWrapStyleWord(true);
        JScrollPane noteScroll = new JScrollPane(txtNote);
        noteScroll.setPreferredSize(new Dimension(0, 160));
        form.add(noteScroll, gbc);

        add(form, BorderLayout.CENTER);

        // Action buttons (colored)
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 12));
        actions.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        btnSave = new JButton(editing == null ? "Thêm khu vực kho" : "Cập nhật khu vực");
        JButton btnCancel = new JButton("Hủy bỏ");
        // unified style: confirm = green (add) or blue (edit), cancel = red; sizes 180x40 / 140x40
        Color addColor = new Color(40, 167, 69);
        Color editColor = new Color(23, 142, 201);
        Color danger = new Color(229, 57, 53);
        Color okColor = (editing == null) ? addColor : editColor;
        btnSave.setBackground(okColor);
        btnSave.setForeground(Color.white);
        btnSave.setFocusPainted(false);
        btnSave.setBorderPainted(false);
        btnSave.setOpaque(true);
        btnSave.setPreferredSize(new Dimension(180, 40));

        btnCancel.setBackground(danger);
        btnCancel.setForeground(Color.white);
        btnCancel.setFocusPainted(false);
        btnCancel.setBorderPainted(false);
        btnCancel.setOpaque(true);
        btnCancel.setPreferredSize(new Dimension(140, 40));

        btnCancel.addActionListener(e -> {
            result = null;
            dispose();
        });
        btnSave.addActionListener(e -> onSave());
        if (!readOnly) {
            getRootPane().setDefaultButton(btnSave);
        }

        if (!readOnly) {
            actions.add(btnSave);
        }
        actions.add(btnCancel);
        add(actions, BorderLayout.SOUTH);

        // If read-only, disable inputs and hide save
        if (readOnly) {
            txtName.setEnabled(false);
            txtNote.setEnabled(false);
            cboStatus.setEnabled(false);
            if (btnSave != null) btnSave.setVisible(false);
        }
    }

    private void onSave() {
        String name = txtName.getText() == null ? "" : txtName.getText().trim();
        String note = txtNote.getText() == null ? "" : txtNote.getText().trim();

        if (name.isBlank()) {
            Toast.warn(this, "Tên là bắt buộc");
            return;
        }

        WarehouseZone z = new WarehouseZone();
        if (editing != null) {
            z.setId(editing.getId());
        }
        z.setName(name);
        z.setNote(note.isBlank() ? null : note);
        StatusItem selItem = (StatusItem) cboStatus.getSelectedItem();
        z.setStatus(selItem == null ? 1 : selItem.value);

        result = z;
        dispose();
    }

    private static final class StatusItem {
        private final String label;
        private final int value;

        StatusItem(String label, int value) {
            this.label = label;
            this.value = value;
        }

        @Override
        public String toString() {
            return label;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StatusItem)) return false;
            StatusItem s = (StatusItem) o;
            return s.value == value && (label == null ? s.label == null : label.equals(s.label));
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value) * 31 + (label == null ? 0 : label.hashCode());
        }
    }
}
