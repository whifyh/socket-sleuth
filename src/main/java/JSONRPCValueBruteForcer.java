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
import burp.api.montoya.ui.editor.WebSocketMessageEditor;
import burp.api.montoya.websocket.Direction;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import socketsleuth.WebSocketInterceptionRulesTableModel;
import socketsleuth.intruder.WSIntruderMessageView;
import socketsleuth.intruder.executors.Sniper;
import socketsleuth.intruder.payloads.models.IPayloadModel;
import socketsleuth.intruder.payloads.payloads.IIntruderPayloadType;
import socketsleuth.intruder.payloads.payloads.Utils;
import websocket.MessageProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class JSONRPCValueBruteForcer {
    private final MessageProvider socketProvider;
    private WebSocketMessageEditor messageEditor;
    private IPayloadModel payloadModel;
    private MontoyaApi api;
    private WSIntruderMessageView messageView;
    private Sniper executor;
    private JPanel container;
    private JPanel payloadContainer;
    private JButton startAttackButton;
    private JTextField msgIdFieldTxt;
    private JButton messageIdSelectButton;
    private JButton messageIdAutoDetectButton;
    private JSpinner minDelaySpinner;
    private JSpinner maxDelaySpinner;

    public JCheckBox getHexModeCheckBox() {
        return hexModeCheckBox;
    }

    public JCheckBox getListModeCheckBox() {
        return listModeCheckBox;
    }

    public JCheckBox getKeepAliveBox() {
        return keepAliveBox;
    }


    public JSONRPCValueBruteForcer(MontoyaApi api, WebSocketMessageEditor messageEditor, MessageProvider socketProvider) {
        this.api = api;
        this.messageEditor = messageEditor;
        this.socketProvider = socketProvider;
        this.minDelaySpinner.getModel().setValue(1000);
        this.maxDelaySpinner.getModel().setValue(2000);

        // Payload insertion point setup
        this.addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int caretPosition = messageEditor.caretPosition();

                String currentContents = messageEditor.getContents().toString();

                String updatedContents = currentContents.substring(0, caretPosition) + "§" + currentContents.substring(caretPosition);

                messageEditor.setContents(ByteArray.byteArray(updatedContents));

                try {
                    java.util.List<String> payloads = Utils.extractPayloadPositions(messageEditor.getContents().toString());
                    payloadPositionCount.setText(String.valueOf(payloads.size()));
                } catch (Exception ex) {
                    payloadPositionCount.setText("Unmatched payload");
                }
            }
        });

        this.clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String currentContents = messageEditor.getContents().toString();
                String updatedContents = currentContents.replaceAll("§", "");
                messageEditor.setContents(ByteArray.byteArray(updatedContents));
                payloadPositionCount.setText("0");
            }
        });

        this.directionCombo.addItem(WebSocketInterceptionRulesTableModel.Direction.CLIENT_TO_SERVER);
        this.directionCombo.addItem(WebSocketInterceptionRulesTableModel.Direction.SERVER_TO_CLIENT);

        // Setup results pane
        this.messageView = new WSIntruderMessageView(api);
        this.resultsTabbedPane.addTab("All messages", this.messageView.getContainer());
    }

    public Direction getSelectedDirection() {
        WebSocketInterceptionRulesTableModel.Direction direction = (WebSocketInterceptionRulesTableModel.Direction) directionCombo.getSelectedItem();
        if (direction == WebSocketInterceptionRulesTableModel.Direction.CLIENT_TO_SERVER) {
            return Direction.CLIENT_TO_SERVER;
        } else {
            return Direction.SERVER_TO_CLIENT;
        }
    }

    public JButton getStartAttackButton() {
        return startAttackButton;
    }

    public JSpinner getMinDelaySpinner() {
        return minDelaySpinner;
    }

    public JSpinner getMaxDelaySpinner() {
        return maxDelaySpinner;
    }

    public Sniper getExecutor() {
        return executor;
    }

    public void setPayloadType(IIntruderPayloadType payloadForm) {
        // TODO: only suports sniper atm - also will have weird behaviour if changed when running
        this.executor = new Sniper(this.api, this.messageView, this.socketProvider);
        this.payloadModel = payloadForm.getPayloadModel();
        this.setPayloadTypeContainerPanel(payloadForm.getContainer());
    }

    public void setPayloadTypeContainerPanel(JPanel payloadContainer) {
        this.payloadTypeContainer.removeAll();
        this.payloadTypeContainer.add(payloadContainer);
        this.payloadTypeContainer.revalidate();
        this.payloadTypeContainer.repaint();
    }

    private JComboBox payloadTypeCombo;
    private JPanel payloadTypeContainer;
    private JTabbedPane resultsTabbedPane;
    private JButton addButton;
    private JButton clearButton;
    private JCheckBox useReqIDForCheckBox;
    private JLabel payloadPositionCount;
    private JComboBox directionCombo;
    private JCheckBox hexModeCheckBox;
    private JCheckBox listModeCheckBox;
    private JCheckBox keepAliveBox;

    public JPanel getContainer() {
        return container;
    }

    public JPanel getPayloadContainer() {
        return payloadContainer;
    }

    public JComboBox getPayloadTypeCombo() {
        return payloadTypeCombo;
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
        container.setLayout(new GridLayoutManager(6, 3, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Specify JSONRPC payload insertion points");
        container.add(label1, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        container.add(spacer1, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        payloadContainer = new JPanel();
        payloadContainer.setLayout(new CardLayout(0, 0));
        container.add(payloadContainer, new GridConstraints(2, 0, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        container.add(spacer2, new GridConstraints(2, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(12, 5, new Insets(0, 0, 0, 0), -1, -1));
        container.add(panel1, new GridConstraints(2, 2, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        startAttackButton = new JButton();
        startAttackButton.setText("Start Attack");
        panel1.add(startAttackButton, new GridConstraints(0, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel1.add(spacer3, new GridConstraints(1, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        panel1.add(spacer4, new GridConstraints(4, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Min delay (ms)");
        panel1.add(label2, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Max delay (ms)");
        panel1.add(label3, new GridConstraints(5, 2, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minDelaySpinner = new JSpinner();
        panel1.add(minDelaySpinner, new GridConstraints(6, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        maxDelaySpinner = new JSpinner();
        panel1.add(maxDelaySpinner, new GridConstraints(6, 2, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        panel1.add(spacer5, new GridConstraints(7, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Payload type:");
        panel1.add(label4, new GridConstraints(9, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        payloadTypeCombo = new JComboBox();
        panel1.add(payloadTypeCombo, new GridConstraints(9, 2, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        payloadTypeContainer = new JPanel();
        payloadTypeContainer.setLayout(new CardLayout(0, 0));
        panel1.add(payloadTypeContainer, new GridConstraints(11, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        addButton = new JButton();
        addButton.setText("Add §");
        panel1.add(addButton, new GridConstraints(2, 2, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        clearButton = new JButton();
        clearButton.setText("Clear §");
        panel1.add(clearButton, new GridConstraints(3, 2, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Payload positions");
        panel1.add(label5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        payloadPositionCount = new JLabel();
        payloadPositionCount.setText("0");
        panel1.add(payloadPositionCount, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Direction");
        panel1.add(label6, new GridConstraints(8, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        directionCombo = new JComboBox();
        panel1.add(directionCombo, new GridConstraints(8, 2, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hexModeCheckBox = new JCheckBox();
        hexModeCheckBox.setText("Hex Mode");
        panel1.add(hexModeCheckBox, new GridConstraints(10, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        listModeCheckBox = new JCheckBox();
        listModeCheckBox.setText("List Mode");
        panel1.add(listModeCheckBox, new GridConstraints(10, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        keepAliveBox = new JCheckBox();
        keepAliveBox.setText("Keep Alive");
        panel1.add(keepAliveBox, new GridConstraints(10, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        container.add(spacer6, new GridConstraints(4, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        resultsTabbedPane = new JTabbedPane();
        container.add(resultsTabbedPane, new GridConstraints(5, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return container;
    }

}
