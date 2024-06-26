/*
 * © 2023 Snyk Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.ui.editor.EditorOptions;
import burp.api.montoya.ui.editor.WebSocketMessageEditor;
import burp.api.montoya.websocket.Direction;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

public class AutoRepeaterTab implements ContainerProvider {
    private MontoyaApi api;
    private JPanel container;
    private JSplitPane primarySplit;
    private JSplitPane configurationSplit;
    private JTable connectionConfigTable;
    private JButton selectSourceButton;
    private JButton selectTargetButton;
    private JButton activateWSAutoRepeaterButton;
    private JComboBox directionCombo;
    private JTabbedPane requestTabbedPane;
    private JLabel selectedSrcLabel;
    private JLabel selectedDstLabel;
    private TableModel sourceTableModel;
    private TableModel targetTableModel;

    private Integer selectedSocketId;
    private Integer selectedTargetId;

    AutoRepeaterTableModel tableModel;
    WebSocketAutoRepeater webSocketAutoRepeater;
    WebSocketAutoRepeaterStreamTableModel autoRepeaterStreamTableModel;
    int tabId;

    String CLIENT_TO_SERVER = "Client to Server";
    String SERVER_TO_CLIENT = "Server to Client";
    String BIDIRECTIONAL = "Bidirectional";

    public AutoRepeaterTab(int tabID, MontoyaApi api, TableModel tableModel, WebSocketAutoRepeater webSocketAutoRepeater) {
        this.api = api;
        this.tabId = tabID;
        this.tableModel = new AutoRepeaterTableModel((AbstractTableModel) tableModel);
        this.connectionConfigTable.setAutoCreateRowSorter(false);
        this.connectionConfigTable.setModel(tableModel);
        this.webSocketAutoRepeater = webSocketAutoRepeater;

        TableRowSorter<TableModel> rowSorter = new TableRowSorter<TableModel>(this.tableModel);

        primarySplit.setDividerLocation(169);
        configurationSplit.setDividerLocation(1000);

        // Build tabs
        // Tab -> SplitPane: Left (AutoRepeaterMessageTable) Right (MessageEditor)
        JSplitPane originalSocketSplit = new JSplitPane();
        this.autoRepeaterStreamTableModel = new WebSocketAutoRepeaterStreamTableModel();
        AutoRepeaterMessageTable messageTable = new AutoRepeaterMessageTable();
        messageTable.getMessageTable().setModel(this.autoRepeaterStreamTableModel);
        WebSocketMessageEditor messageEditor = this.api.userInterface().createWebSocketMessageEditor(EditorOptions.READ_ONLY);

        messageTable.getMessageTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int selectedViewIndex = messageTable.getMessageTable().getSelectedRow();
                    int selectedRow = messageTable.getMessageTable().convertRowIndexToModel(selectedViewIndex);
                    if (selectedRow != -1) {
                        // TODO: better way to byte[] -> ByteArray
                        messageEditor.setContents(ByteArray.byteArray(new String(autoRepeaterStreamTableModel.getStream(selectedRow).getRawMessage())));
                    }
                }
            }
        });

        // Assign components
        originalSocketSplit.setLeftComponent(messageTable.getContainer());
        originalSocketSplit.setRightComponent(messageEditor.uiComponent());
        originalSocketSplit.setDividerLocation(800);
        requestTabbedPane.addTab("Target Socket", originalSocketSplit);

        DefaultComboBoxModel<String> cbModel = new DefaultComboBoxModel<String>();
        cbModel.addElement(CLIENT_TO_SERVER);
        cbModel.addElement(SERVER_TO_CLIENT);
        cbModel.addElement(BIDIRECTIONAL);
        this.directionCombo.setModel(cbModel);

        setButtonEvents();
    }

    private boolean validateWebsocketSelection(int selectedRow) {
        boolean active = (boolean) connectionConfigTable.getModel().getValueAt(selectedRow, 3);
        if (!active) {
            JOptionPane.showMessageDialog(
                    api.userInterface().swingUtils().suiteFrame(),
                    "WebSocket is no longer active! You must select an active WebSocket",
                    "Inactive WebSocket", JOptionPane.WARNING_MESSAGE
            );
        }
        return active;
    }

    private void toggleSourceTargetButtons(boolean enabled) {
        this.selectSourceButton.setEnabled(enabled);
        this.selectTargetButton.setEnabled(enabled);
        this.directionCombo.setEnabled(enabled);
    }

    private void setButtonEvents() {
        // Activate WS Repeater button
        this.activateWSAutoRepeaterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Is it running and we should stop?
                if (webSocketAutoRepeater.hasRepeaterForTab(tabId)) {
                    webSocketAutoRepeater.removeRepeaterByTabId(tabId);

                    toggleSourceTargetButtons(true);
                    activateWSAutoRepeaterButton.setBackground(new Color(-65536));
                    activateWSAutoRepeaterButton.setForeground(new Color(-12935007));
                    activateWSAutoRepeaterButton.setText("Activate WS Auto Repeater");
                    return;
                }

                // Check configuration
                if (selectedTargetId == null || selectedSocketId == null) {
                    JOptionPane.showMessageDialog(
                            api.userInterface().swingUtils().suiteFrame(),
                            "You must select both source and target sockets.",
                            "Invalid configuration", JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                if (selectedSocketId == selectedTargetId) {
                    JOptionPane.showMessageDialog(
                            api.userInterface().swingUtils().suiteFrame(),
                            "Source and target websockets can not be the same.",
                            "Invalid configuration", JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                Direction direction = null;
                String selectedDirection = directionCombo.getSelectedItem().toString();
                if (selectedDirection == CLIENT_TO_SERVER) {
                    direction = Direction.CLIENT_TO_SERVER;
                }

                if (selectedDirection == SERVER_TO_CLIENT) {
                    direction = Direction.SERVER_TO_CLIENT;
                }

                boolean isSourceAlive = (boolean) connectionConfigTable.getModel().getValueAt(selectedSocketId, 3);
                if (!isSourceAlive) {
                    JOptionPane.showMessageDialog(
                            api.userInterface().swingUtils().suiteFrame(),
                            "Check that configured sockets are still alive.",
                            "Invalid configuration", JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                boolean isTargetAlive = (boolean) connectionConfigTable.getModel().getValueAt(selectedTargetId, 3);
                if (!isTargetAlive) {
                    JOptionPane.showMessageDialog(
                            api.userInterface().swingUtils().suiteFrame(),
                            "Check that configured sockets are still alive.",
                            "Invalid configuration", JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                // Set in autorepeater
                webSocketAutoRepeater.setRepeater(tabId, new AutoRepeaterConfig(
                        selectedSocketId,
                        selectedTargetId,
                        direction,
                        tabId,
                        autoRepeaterStreamTableModel
                ));

                toggleSourceTargetButtons(false);
                activateWSAutoRepeaterButton.setBackground(Color.decode("#008000"));
                activateWSAutoRepeaterButton.setForeground(Color.WHITE);
                activateWSAutoRepeaterButton.setText("Stop WS Auto Repeater");
            }
        });

        // Source socket button
        this.selectSourceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedView = connectionConfigTable.getSelectedRow();
                int selectedRow = connectionConfigTable.convertRowIndexToModel(selectedView);
                TableModel tm = connectionConfigTable.getModel();

                if (selectedRow >= 0) {
                    if (!validateWebsocketSelection(selectedRow)) {
                        return;
                    }

                    int socketId = (int) tm.getValueAt(selectedRow, 0);
                    selectedSrcLabel.setText(Integer.toString(socketId));
                    selectedSocketId = socketId;
                } else {
                    JOptionPane.showMessageDialog(
                            api.userInterface().swingUtils().suiteFrame(),
                            "Please select a WebSocket from the table.",
                            "No row selected", JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        });

        // Target socket button
        this.selectTargetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedView = connectionConfigTable.getSelectedRow();
                int selectedRow = connectionConfigTable.convertRowIndexToModel(selectedView);
                TableModel tm = connectionConfigTable.getModel();

                if (selectedRow >= 0) {
                    if (!validateWebsocketSelection(selectedRow)) {
                        return;
                    }

                    int socketId = (int) tm.getValueAt(selectedRow, 0);
                    selectedDstLabel.setText(Integer.toString(socketId));
                    selectedTargetId = socketId;
                } else {
                    JOptionPane.showMessageDialog(
                            api.userInterface().swingUtils().suiteFrame(),
                            "Please select a WebSocket from the table.",
                            "No row selected", JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        });
    }

    public JPanel getContainer() {
        return container;
    }

    public JSplitPane getPrimarySplit() {
        return primarySplit;
    }

    public JSplitPane getConfigurationSplit() {
        return configurationSplit;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        container = new JPanel();
        container.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        container.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        primarySplit = new JSplitPane();
        primarySplit.setDividerLocation(179);
        primarySplit.setOrientation(0);
        container.add(primarySplit, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        configurationSplit = new JSplitPane();
        configurationSplit.setDividerLocation(174);
        primarySplit.setLeftComponent(configurationSplit);
        final JScrollPane scrollPane1 = new JScrollPane();
        configurationSplit.setLeftComponent(scrollPane1);
        connectionConfigTable = new JTable();
        scrollPane1.setViewportView(connectionConfigTable);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(6, 3, new Insets(0, 0, 0, 0), -1, -1));
        configurationSplit.setRightComponent(panel1);
        final JLabel label1 = new JLabel();
        Font label1Font = this.$$$getFont$$$(null, Font.BOLD, -1, label1.getFont());
        if (label1Font != null) label1.setFont(label1Font);
        label1.setText("Source Socket:");
        panel1.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        selectedSrcLabel = new JLabel();
        selectedSrcLabel.setText("None selected");
        panel1.add(selectedSrcLabel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        selectSourceButton = new JButton();
        selectSourceButton.setText("Select");
        panel1.add(selectSourceButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        Font label2Font = this.$$$getFont$$$(null, Font.BOLD, -1, label2.getFont());
        if (label2Font != null) label2.setFont(label2Font);
        label2.setText("Target Socket:");
        panel1.add(label2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        selectedDstLabel = new JLabel();
        selectedDstLabel.setText("None selected");
        panel1.add(selectedDstLabel, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        selectTargetButton = new JButton();
        selectTargetButton.setText("Select");
        panel1.add(selectTargetButton, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel1.add(spacer2, new GridConstraints(4, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        activateWSAutoRepeaterButton = new JButton();
        activateWSAutoRepeaterButton.setBackground(new Color(-65536));
        activateWSAutoRepeaterButton.setForeground(new Color(-12935007));
        activateWSAutoRepeaterButton.setText("Activate WS Auto Repeater");
        panel1.add(activateWSAutoRepeaterButton, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        Font label3Font = this.$$$getFont$$$(null, Font.BOLD, -1, label3.getFont());
        if (label3Font != null) label3.setFont(label3Font);
        label3.setText("Direction:");
        panel1.add(label3, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        directionCombo = new JComboBox();
        panel1.add(directionCombo, new GridConstraints(5, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        requestTabbedPane = new JTabbedPane();
        primarySplit.setRightComponent(requestTabbedPane);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return container;
    }

}
