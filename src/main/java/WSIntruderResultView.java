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
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;

public class WSIntruderResultView {
    private JPanel container;
    private JList discoveredList;
    private JSplitPane resultSplit;
    private JPanel requestEditorPanel;
    private JPanel responseEditorPanel;
    private WebSocketMessageEditor messageEditor;
    private WebSocketMessageEditor responseEditor;

    public WSIntruderResultView(MontoyaApi api) {
        this.messageEditor = api.userInterface().createWebSocketMessageEditor(EditorOptions.READ_ONLY);
        this.responseEditor = api.userInterface().createWebSocketMessageEditor(EditorOptions.READ_ONLY);
        // this.resultSplit.setRightComponent(this.messageEditor.uiComponent());
        requestEditorPanel.add(this.messageEditor.uiComponent());
        responseEditorPanel.add(this.responseEditor.uiComponent());
        discoveredList.setModel(new DefaultListModel<JSONRPCMethodItem>());

        discoveredList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    JSONRPCMethodItem selectedItem = (JSONRPCMethodItem) discoveredList.getSelectedValue();
                    if (selectedItem != null) {
                        messageEditor.setContents(ByteArray.byteArray(selectedItem.getRequest()));
                        responseEditor.setContents(ByteArray.byteArray(selectedItem.getResponse()));
                    } else {
                        // Clear the requestTextArea and responseTextArea if no item is selected
                        messageEditor.setContents(ByteArray.byteArray(""));
                        responseEditor.setContents(ByteArray.byteArray(""));
                    }
                }
            }
        });
    }

    public JPanel getContainer() {
        return container;
    }

    public JList getDiscoveredList() {
        return discoveredList;
    }

    public JSplitPane getResultSplit() {
        return resultSplit;
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
        resultSplit = new JSplitPane();
        container.add(resultSplit, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        resultSplit.setLeftComponent(panel1);
        final JLabel label1 = new JLabel();
        label1.setText("JSONRPC methods discovered");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel1.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        discoveredList = new JList();
        discoveredList.setSelectionMode(0);
        scrollPane1.setViewportView(discoveredList);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        resultSplit.setRightComponent(panel2);
        requestEditorPanel = new JPanel();
        requestEditorPanel.setLayout(new CardLayout(0, 0));
        panel2.add(requestEditorPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        responseEditorPanel = new JPanel();
        responseEditorPanel.setLayout(new CardLayout(0, 0));
        panel2.add(responseEditorPanel, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("WS Request");
        panel2.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("WS Response");
        panel2.add(label3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return container;
    }

}
