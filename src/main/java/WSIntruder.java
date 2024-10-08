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
import burp.api.montoya.proxy.websocket.ProxyWebSocket;
import burp.api.montoya.ui.editor.WebSocketMessageEditor;
import burp.api.montoya.websocket.Direction;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.json.JSONException;
import org.json.JSONObject;
import socketsleuth.intruder.executors.Sniper;
import socketsleuth.intruder.payloads.payloads.ui.NumericListForm;
import websocket.MessageProvider;
import whifyh.QiangZhanStatus;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;

public class WSIntruder implements ContainerProvider {
    private final MessageProvider socketProvider;
    private MontoyaApi api;
    private WebSocketMessageEditor messageEditor;
    private JSONRPCResponseMonitor responseMonitor;
    private WebSocketConnectionTableModel webSocketConnectionTableModel;
    private ProxyWebSocket proxyWebSocket;
    private int socketId;
    private JPanel container;
    private int tabId;

    @Override
    public void handleData(Object data) {
        if (data instanceof byte[]) {
            byte[] byteArray = (byte[]) data;
            if (this.messageEditor == null) {
                return;
            }
            // Not so nice but setContents uses custom ByteArray
            this.messageEditor.setContents(ByteArray.byteArray(new String((byte[]) data)));

            // Also not so nice, it results in two panels being created - this is because the
            // JSONRPC auto detect is run during creation of the panel, and in the first time
            // there is no data in the WS editor. This is a hack to auto detect when added via
            // right click context menu.
            setWsIntruderPanel(constructJSONRPCMethodPanel());
        }
    }

    private JComboBox attackTypeCombo;
    private JPanel wsIntruderPanel;
    private JButton selectWebSocketButton;
    private JLabel selectedSocketLabel;

    private JPanel attackTypePanel;

    public JPanel getContainer() {
        return container;
    }

    public JPanel getAttackTypePanel() {
        return attackTypePanel;
    }

    public JComboBox getAttackTypeCombo() {
        return attackTypeCombo;
    }

    // tabId is always incremented and is used to subscribe to detection events for the correct tab
    // tab index can not be used as it could cause issues when tabs have been removed and new tabs added
    public WSIntruder(int tabId, MontoyaApi api, WebSocketConnectionTableModel connectionTableModel, JSONRPCResponseMonitor responseMonitor, MessageProvider socketProvider) {
        this.tabId = tabId;
        this.api = api;
        this.messageEditor = api.userInterface().createWebSocketMessageEditor();
        this.webSocketConnectionTableModel = connectionTableModel;
        this.responseMonitor = responseMonitor;
        this.socketProvider = socketProvider;

        this.getAttackTypeCombo().setModel(new DefaultComboBoxModel<>(WSIntruderType.values()));
        this.getAttackTypeCombo().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                WSIntruderType selectedOption = (WSIntruderType) getAttackTypeCombo().getSelectedItem();
                switch (selectedOption) {
                    case JSONRPCMETHOD:
                        setWsIntruderPanel(constructJSONRPCMethodPanel());
                        break;
                    case SNIPER:
                        setWsIntruderPanel(constructJSONRPCValueBruteForcerPanel());
                        break;
                    case QiangZhan:
                        setWsIntruderPanel(constructQiangZhanPanel());
                        QiangZhanStatus.api = api;
                        break;
                    default:
                        break;
                }
            }
        });

        this.setWsIntruderPanel(constructJSONRPCMethodPanel());


        this.selectWebSocketButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFrame popup = new JFrame("WebSocket Connection Manager");
                WSConnectionManager connectionManager = new WSConnectionManager(connectionTableModel);
                connectionManager.getCancelButton().addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        popup.dispose();
                    }
                });

                connectionManager.getSelectButton().addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        int selectedRow = connectionManager.getWsConnectionManagerTabel().getSelectedRow();
                        if (selectedRow != -1) {
                            boolean active = connectionTableModel.getConnection(selectedRow).isActive();
                            if (!active) {
                                return;
                            }

                            int webSocketId = connectionTableModel.getConnection(selectedRow).getSocketId();
                            String webSocketUrl = connectionTableModel.getConnection(selectedRow).getUrl();
                            selectedSocketLabel.setText("ID: " + webSocketId + " URL: " + webSocketUrl);
                            proxyWebSocket = connectionTableModel.getConnection(selectedRow).getProxyWebSocket();
                            socketId = webSocketId;
                        }
                        popup.dispose();
                    }
                });

                popup.add(connectionManager.getContainer());
                popup.pack();
                popup.setLocationRelativeTo(api.userInterface().swingUtils().suiteFrame());
                popup.setVisible(true);
            }
        });
    }

    private JPanel constructJSONRPCParamPanel() {
        return new JSONRPCParamBruteIntruder().getContainer();
    }

    private JPanel constructJSONRPCValueBruteForcerPanel() {
        JSONRPCValueBruteForcer bruteForcer = new JSONRPCValueBruteForcer(this.api, this.messageEditor, this.socketProvider);
        bruteForcer.getPayloadContainer().add(this.messageEditor.uiComponent());

        DefaultComboBoxModel<BruteForcePayloadTypeOption> comboBoxModel = new DefaultComboBoxModel<>();
        comboBoxModel.addElement(BruteForcePayloadTypeOption.SIMPLE_LIST);
        comboBoxModel.addElement(BruteForcePayloadTypeOption.NUMBERS);
        bruteForcer.getPayloadTypeCombo().setModel(comboBoxModel);

        // Default payload type
        setBruteForcerPayloadSimpleList(bruteForcer);

        // Handle switching payload type
        bruteForcer.getPayloadTypeCombo().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BruteForcePayloadTypeOption selectedOption = (BruteForcePayloadTypeOption) bruteForcer.getPayloadTypeCombo().getSelectedItem();

                switch (selectedOption) {
                    case SIMPLE_LIST:
                        setBruteForcerPayloadSimpleList(bruteForcer);
                        break;
                    case NUMBERS:
                        setBruteForcerPayloadNumeric(bruteForcer);
                        break;
                    default:
                        break;
                }

            }
        });

        return bruteForcer.getContainer();
    }

    private JPanel constructQiangZhanPanel() {
        QiangZhan qiangZhan = new QiangZhan();
        qiangZhan.setExecutor(new Sniper(this.api, null, null));
        return qiangZhan.getJpanel();
    }



    private void setBruteForcerPayloadNumeric(JSONRPCValueBruteForcer bruteForcer) {
        NumericListForm numericListForm = new NumericListForm(this.api);
        bruteForcer.setPayloadType(numericListForm);

        this.removeActionListeners(bruteForcer.getStartAttackButton());
        bruteForcer.getStartAttackButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isWebSocketSelected()) {
                    JOptionPane.showMessageDialog(
                            api.userInterface().swingUtils().suiteFrame(),
                            "Please select a WebSocket using the button above.",
                            "No WebSocket selected", JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                // Validate form and set values in model
                try {
                    numericListForm.setFormValuesInModel(bruteForcer.getHexModeCheckBox().isSelected());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            api.userInterface().swingUtils().suiteFrame(),
                            "A validation error occurred: " + ex.getMessage(),
                            "Invalid configuration", JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                if (bruteForcer.getExecutor().isRunning()) {
                    api.logging().logToOutput("Its already running running - please wait.");
                    return;
                }

                int minDelay = (int) bruteForcer.getMinDelaySpinner().getModel().getValue();
                int maxDelay = (int) bruteForcer.getMaxDelaySpinner().getModel().getValue();
                bruteForcer.getExecutor().setMinDelay(minDelay);
                bruteForcer.getExecutor().setMaxDelay(maxDelay);
                bruteForcer.getExecutor().start(
                        proxyWebSocket,
                        socketId,
                        numericListForm.getPayloadModel(),
                        messageEditor.getContents().toString(),
                        bruteForcer.getSelectedDirection(),
                        bruteForcer.getHexModeCheckBox().isSelected(),
                        bruteForcer.getListModeCheckBox().isSelected(),
                        bruteForcer.getKeepAliveBox().isSelected()
                );
            }
        });
    }

    // Temporary method to avoid dupe event handlers. Refactor all the code
    // around this and have this handled by a function on the bruteforcerClass
    private void removeActionListeners(JButton startAttackButton) {
        for (ActionListener al : startAttackButton.getActionListeners()) {
            startAttackButton.removeActionListener(al);
        }
    }

    private void setBruteForcerPayloadSimpleList(JSONRPCValueBruteForcer bruteForcer) {
        IntruderPayloadTypeSimpleList simpleList = new IntruderPayloadTypeSimpleList(this.api);
        bruteForcer.setPayloadType(simpleList);

        // This should be moved into the actual bruteforcer class
        this.removeActionListeners(bruteForcer.getStartAttackButton());
        bruteForcer.getStartAttackButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isWebSocketSelected()) {
                    JOptionPane.showMessageDialog(
                            api.userInterface().swingUtils().suiteFrame(),
                            "Please select a WebSocket using the button above.",
                            "No WebSocket selected", JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                if (bruteForcer.getExecutor().isRunning()) {
                    api.logging().logToOutput("Already running running - please wait.");
                    return;
                }

                int minDelay = (int) bruteForcer.getMinDelaySpinner().getModel().getValue();
                int maxDelay = (int) bruteForcer.getMaxDelaySpinner().getModel().getValue();
                bruteForcer.getExecutor().setMinDelay(minDelay);
                bruteForcer.getExecutor().setMaxDelay(maxDelay);
                bruteForcer.getExecutor().start(
                        proxyWebSocket,
                        socketId,
                        simpleList.getPayloadModel(),
                        messageEditor.getContents().toString(),
                        bruteForcer.getSelectedDirection(),
                        bruteForcer.getHexModeCheckBox().isSelected(),
                        bruteForcer.getListModeCheckBox().isSelected(),
                        bruteForcer.getKeepAliveBox().isSelected());
            }
        });


    }

    private JPanel constructJSONRPCMethodPanel() {
        Color lightGreen = new Color(0, 204, 102);
        JSONRPCIntruder jsonrpcIntruder = new JSONRPCIntruder(api);
        jsonrpcIntruder.getPayloadContainer().add(this.messageEditor.uiComponent());
        jsonrpcIntruder.getPayloadContainer().revalidate();
        jsonrpcIntruder.getPayloadContainer().repaint();

        jsonrpcIntruder.getAutoDetectButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jsonrpcIntruder.attemptAutoDetectJSONRPC(messageEditor);
            }
        });


        jsonrpcIntruder.getAddButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ByteArray message = messageEditor.getContents();
                try {
                    JSONObject jsonObj = new JSONObject(new String(message.getBytes()));
                    String method = jsonObj.getString(jsonrpcIntruder.getFieldText().getText());
                    jsonrpcIntruder.getMethodLabel().setText(jsonrpcIntruder.getFieldText().getText());
                    jsonrpcIntruder.getMethodLabel().setForeground(lightGreen);
                } catch (JSONException ex) {
                    // Unable to detect
                    jsonrpcIntruder.getMethodLabel().setText("unable to select");
                    jsonrpcIntruder.getMethodLabel().setForeground(Color.RED);
                }
            }
        });

        // Default wordlist
        DefaultListModel<String> listModel = new DefaultListModel<>();
        jsonrpcIntruder.getMethodWordlist().setModel(listModel);
        populateDefaultWordlist(jsonrpcIntruder.getMethodWordlist());

        // Maybe change this to a custom impl of DefaultListModel and add an event handler...
        updateItemCountTextField(listModel, jsonrpcIntruder.getWordlistCountLabel());

        jsonrpcIntruder.getStartDiscoveryButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isWebSocketSelected()) {
                    JOptionPane.showMessageDialog(
                            api.userInterface().swingUtils().suiteFrame(),
                            "Please select a WebSocket using the button above.",
                            "No WebSocket selected", JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                String basePayload = messageEditor.getContents().toString();
                if (!JsonRpcUtils.isJsonRpcMessage(basePayload)) {
                    JOptionPane.showMessageDialog(
                            api.userInterface().swingUtils().suiteFrame(),
                            "The base payload is not a valid JSONRPC message.",
                            "Invalid JSONRPC", JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }


                Random rand = new Random();
                int minDelay = (int) jsonrpcIntruder.getMinDelaySpinner().getModel().getValue();
                int maxDelay = (int) jsonrpcIntruder.getMaxDelaySpinner().getModel().getValue();

                api.logging().logToOutput(
                        "Starting JSONRPC method discovery with Min Delay: "
                                + minDelay
                                + " Max Delay: "
                                + maxDelay
                                + " and Wordlist Size: "
                                + listModel.size()
                );

                Thread thread = new Thread(() -> {
                    // TODO: Allow this to be specified in the UI
                    int currentId = 10000;
                    for (int i = 0; i < listModel.size(); i++) {
                        // Do discovery
                        String item = listModel.getElementAt(i);
                        JSONObject jsonObject = new JSONObject(basePayload);

                        // Set the properties of the object
                        jsonObject.put("jsonrpc", "2.0");
                        jsonObject.put("id", currentId);
                        jsonObject.put("method", item);
                        jsonObject.put("params", JSONObject.NULL);

                        // Add to response monitor
                        responseMonitor.addRequest(tabId, jsonObject);

                        proxyWebSocket.sendTextMessage(jsonObject.toString(), Direction.CLIENT_TO_SERVER);
                        currentId++;

                        // Delay
                        int delay = rand.nextInt(maxDelay - minDelay + 1) + minDelay;
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                    api.logging().logToOutput("JSONRPC Method discovery request sending complete");
                });

                thread.start();
            }
        });

        jsonrpcIntruder.getRemoveButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedIndices = jsonrpcIntruder.getMethodWordlist().getSelectedIndices();
                for (int i = selectedIndices.length - 1; i >= 0; i--) {
                    listModel.remove(selectedIndices[i]);
                    updateItemCountTextField(listModel, jsonrpcIntruder.getWordlistCountLabel());
                }
            }
        });

        jsonrpcIntruder.getAddWordlistItemButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = jsonrpcIntruder.getWordlistManualText().getText();
                if (text.trim().equals("")) {
                    return;
                }

                listModel.addElement(text);
                jsonrpcIntruder.getWordlistManualText().setText("");
                updateItemCountTextField(listModel, jsonrpcIntruder.getWordlistCountLabel());
            }
        });

        jsonrpcIntruder.getClearButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listModel.removeAllElements();
                updateItemCountTextField(listModel, jsonrpcIntruder.getWordlistCountLabel());
            }
        });

        jsonrpcIntruder.getResetDefaultButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listModel.clear(); // Clear the existing items from the listModel
                populateDefaultWordlist(jsonrpcIntruder.getMethodWordlist());
                updateItemCountTextField(listModel, jsonrpcIntruder.getWordlistCountLabel());
            }
        });

        jsonrpcIntruder.getPasteButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String clipboard = "";
                Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                if (systemClipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                    try {
                        clipboard = (String) systemClipboard.getData(DataFlavor.stringFlavor);
                        String[] lines = clipboard.split("\\r?\\n");
                        listModel.addAll(Arrays.stream(lines).collect(Collectors.toList()));
                    } catch (UnsupportedFlavorException ex) {
                        throw new RuntimeException(ex);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                updateItemCountTextField(listModel, jsonrpcIntruder.getWordlistCountLabel());
            }
        });

        // Bind event listener to jsonrpc results
        this.responseMonitor.addMethodDetectedListener(this.tabId, jsonrpcIntruder);
        this.responseMonitor.addResponseReceivedListener(this.tabId, jsonrpcIntruder);

        jsonrpcIntruder.attemptAutoDetectJSONRPC(this.messageEditor);
        return jsonrpcIntruder.getContainer();
    }

    // Seperate into funciton and also check socket is still alive
    private boolean isWebSocketSelected() {
        return proxyWebSocket != null;
    }

    private void updateItemCountTextField(DefaultListModel listModel, JLabel label) {
        int itemCount = listModel.getSize();
        label.setText("" + itemCount);
    }

    private void populateDefaultWordlist(JList<String> list) {
        DefaultListModel<String> listModel = (DefaultListModel<String>) list.getModel();
        try {
            // Get the InputStream for the embedded resource file
            InputStream is = getClass().getResourceAsStream("/jsonrpc.txt");
            if (is == null) {
                throw new IOException("Resource file not found: /jsonrpc.txt");
            }

            // Create a BufferedReader to read the file line by line
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = reader.readLine()) != null) {
                listModel.addElement(line);
            }

            reader.close(); // Close the BufferedReader
        } catch (IOException ex) {
            // Handle the exception, e.g., show an error message, log the error, etc.
            ex.printStackTrace();
        }
    }

    public void setWsIntruderPanel(JPanel intruderPanel) {
        this.wsIntruderPanel.removeAll();
        this.wsIntruderPanel.add(intruderPanel);
        this.wsIntruderPanel.revalidate();
        this.wsIntruderPanel.repaint();
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
        container.setLayout(new GridLayoutManager(8, 5, new Insets(0, 0, 0, 0), -1, -1));
        container.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label1 = new JLabel();
        Font label1Font = this.$$$getFont$$$(null, Font.BOLD, -1, label1.getFont());
        if (label1Font != null) label1.setFont(label1Font);
        label1.setText("Choose an attack type");
        container.add(label1, new GridConstraints(0, 0, 1, 5, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Attack type:");
        container.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        container.add(spacer1, new GridConstraints(1, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(1, 5), null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        container.add(spacer2, new GridConstraints(6, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(-1, 20), null, null, 0, false));
        attackTypeCombo = new JComboBox();
        container.add(attackTypeCombo, new GridConstraints(2, 1, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        wsIntruderPanel = new JPanel();
        wsIntruderPanel.setLayout(new CardLayout(0, 0));
        container.add(wsIntruderPanel, new GridConstraints(7, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("WebSocket:");
        container.add(label3, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        selectedSocketLabel = new JLabel();
        selectedSocketLabel.setText("none selected");
        container.add(selectedSocketLabel, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        selectWebSocketButton = new JButton();
        selectWebSocketButton.setText("Select WebSocket");
        container.add(selectWebSocketButton, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        Font label4Font = this.$$$getFont$$$(null, Font.BOLD, -1, label4.getFont());
        if (label4Font != null) label4.setFont(label4Font);
        label4.setText("Select WebSocket");
        container.add(label4, new GridConstraints(4, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        container.add(spacer3, new GridConstraints(3, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(-1, 5), null, null, 0, false));
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

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
