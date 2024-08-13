import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import socketsleuth.intruder.executors.Sniper;
import socketsleuth.intruder.payloads.models.IPayloadModel;
import whifyh.DataStatusManager;
import whifyh.QiangZhanStatus;

import javax.swing.*;
import java.awt.*;
import java.util.Iterator;
import static burp.api.montoya.websocket.Direction.CLIENT_TO_SERVER;

public class QiangZhan {
    private JPanel jpanel;
    private Sniper executor;

    private JButton redButton;
    private JButton quitButton;
    private JButton blueButton;

    private JTextField redTextField;
    private JTextField quitTextField;
    private JTextField blueTextField;

    private JCheckBox startRecordCheckBox;

    private JTextField roomIdTextField;
    private JPanel playerPanel;

    private JButton startControl;
    private JButton stopControl;

    private JCheckBox moveDownCheckBox;
    private JCheckBox moveLeftCheckBox;
    private JCheckBox moverRightCheckBox;
    private JCheckBox jumpActioncheckBox;
    private JCheckBox meleeAttackActioncheckBox;
    private JCheckBox grenadeAttackActioncheckBox;
    private JCheckBox voice1CheckBox;
    private JCheckBox voice2CheckBox;
    private JCheckBox stopActioncheckBox;

    private IPayloadModel onceList = () -> new Iterator<>() {
        int i = 0;

        @Override
        public boolean hasNext() {
            return i == 0;
        }

        @Override
        public String next() {
            i = 1;
            return "";
        }
    };

    public JPanel getJpanel() {
        return jpanel;
    }

    public QiangZhan() {
        redButton.addActionListener(e -> executor.start(null, 0, onceList, redTextField.getText(), CLIENT_TO_SERVER, false, false, true));
        quitButton.addActionListener(e -> executor.start(null, 0, onceList, quitTextField.getText(), CLIENT_TO_SERVER, false, false, true));
        blueButton.addActionListener(e -> executor.start(null, 0, onceList, blueTextField.getText(), CLIENT_TO_SERVER, false, false, true));

        startRecordCheckBox.addActionListener(e -> {
            if (startRecordCheckBox.isSelected()) {
                DataStatusManager.booleanStatusMap.put("qiangzhan:startRecord", true);
            } else {
                DataStatusManager.booleanStatusMap.put("qiangzhan:startRecord", false);
            }
        });

        moveDownCheckBox.addActionListener(e -> {
            QiangZhanStatus.sendMessageListCache = null;
            if (moveDownCheckBox.isSelected()) {
                QiangZhanStatus.functionMap.put("down", "[1,204,4,[\"h\",\"%s\",%s,1,0]]");
            } else {
                QiangZhanStatus.functionMap.remove("down");
            }
        });

        moveLeftCheckBox.addActionListener(e -> {
            QiangZhanStatus.sendMessageListCache = null;
            if (moveLeftCheckBox.isSelected()) {
                QiangZhanStatus.functionMap.put("left", "[1,204,4,[\"h\",\"%s\",%s,1,1]]");
            } else {
                QiangZhanStatus.functionMap.remove("left");
            }
        });

        moverRightCheckBox.addActionListener(e -> {
            QiangZhanStatus.sendMessageListCache = null;
            if (moverRightCheckBox.isSelected()) {
                QiangZhanStatus.functionMap.put("right", "[1,204,4,[\"h\",\"%s\",%s,1,2]]");
            } else {
                QiangZhanStatus.functionMap.remove("right");
            }
        });

        stopActioncheckBox.addActionListener(e -> {
            QiangZhanStatus.sendMessageListCache = null;
            if (stopActioncheckBox.isSelected()) {
                QiangZhanStatus.functionMap.put("stop", "[1,204,4,[\"h\",\"%s\",%s,2]]");
            } else {
                QiangZhanStatus.functionMap.remove("stop");
            }
        });

        jumpActioncheckBox.addActionListener(e -> {
            QiangZhanStatus.sendMessageListCache = null;
            if (jumpActioncheckBox.isSelected()) {
                QiangZhanStatus.functionMap.put("jump1", "[1,204,4,[\"h\",\"%s\",%s,7]]");
                QiangZhanStatus.functionMap.put("jump2", "[1,204,4,[\"h\",\"%s\",%s,8]]");
            } else {
                QiangZhanStatus.functionMap.remove("jump1");
                QiangZhanStatus.functionMap.remove("jump2");
            }
        });

        meleeAttackActioncheckBox.addActionListener(e -> {
            QiangZhanStatus.sendMessageListCache = null;
            if (meleeAttackActioncheckBox.isSelected()) {
                QiangZhanStatus.functionMap.put("melee", "[1,204,4,[\"h\",\"%s\",%s,11]]");
            } else {
                QiangZhanStatus.functionMap.remove("melee");
            }
        });

        grenadeAttackActioncheckBox.addActionListener(e -> {
            QiangZhanStatus.sendMessageListCache = null;
            if (grenadeAttackActioncheckBox.isSelected()) {
                QiangZhanStatus.functionMap.put("grenade1", "[1,204,4,[\"h\",\"%s\",%s,5]]");
                QiangZhanStatus.functionMap.put("grenade2", "[1,204,4,[\"h\",\"%s\",%s,6]]");
            } else {
                QiangZhanStatus.functionMap.remove("grenade1");
                QiangZhanStatus.functionMap.remove("grenade2");
            }
        });

        voice1CheckBox.addActionListener(e -> {
            QiangZhanStatus.sendMessageListCache = null;
            if (voice1CheckBox.isSelected()) {
                QiangZhanStatus.functionMap.put("voice1", "[1,204,4,[\"h\",\"%s\",%s,10,4]]");
            } else {
                QiangZhanStatus.functionMap.remove("voice1");
            }
        });

        voice2CheckBox.addActionListener(e -> {
            QiangZhanStatus.sendMessageListCache = null;
            if (voice2CheckBox.isSelected()) {
                QiangZhanStatus.functionMap.put("voice2", "[1,204,4,[\"h\",\"%s\",%s,10,7]]");
            } else {
                QiangZhanStatus.functionMap.remove("voice2");
            }
        });

        startControl.addActionListener(e -> {
            QiangZhanStatus.controlRunningStatus = true;
            QiangZhanStatus.executor.qiangZhanKeepControl();
        });

        stopControl.addActionListener(e -> {
            QiangZhanStatus.controlRunningStatus = false;
        });


        QiangZhanStatus.roomIdTextField = roomIdTextField;
        QiangZhanStatus.playerPanel = playerPanel;
    }

    public void setExecutor(Sniper executor) {
        this.executor = executor;
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
        jpanel = new JPanel();
        jpanel.setLayout(new GridLayoutManager(13, 5, new Insets(0, 0, 0, 0), -1, -1));
        quitButton = new JButton();
        quitButton.setText("Quit");
        jpanel.add(quitButton, new GridConstraints(9, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        blueButton = new JButton();
        blueButton.setText("Blue");
        jpanel.add(blueButton, new GridConstraints(9, 3, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        redButton = new JButton();
        redButton.setText("Red");
        jpanel.add(redButton, new GridConstraints(9, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        quitTextField = new JTextField();
        quitTextField.setText("[1,368,4,[\"e\",\"11-3nkpMNF\",45427832,0]]");
        jpanel.add(quitTextField, new GridConstraints(10, 2, 3, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        blueTextField = new JTextField();
        blueTextField.setText("[1,368,4,[\"e\",\"11-3nkpMNF\",45427832,1]]");
        jpanel.add(blueTextField, new GridConstraints(10, 3, 3, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        redTextField = new JTextField();
        redTextField.setText("[1,368,4,[\"e\",\"11-3nkpMNF\",45427832,-1]]");
        jpanel.add(redTextField, new GridConstraints(10, 0, 3, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer1 = new Spacer();
        jpanel.add(spacer1, new GridConstraints(8, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("RoomId:");
        jpanel.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startRecordCheckBox = new JCheckBox();
        startRecordCheckBox.setText("StartRecord");
        jpanel.add(startRecordCheckBox, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        roomIdTextField = new JTextField();
        jpanel.add(roomIdTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        playerPanel = new JPanel();
        playerPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpanel.add(playerPanel, new GridConstraints(3, 0, 4, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("RoomPlayer:");
        jpanel.add(label2, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        startControl = new JButton();
        startControl.setText("startControl");
        jpanel.add(startControl, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        moveDownCheckBox = new JCheckBox();
        moveDownCheckBox.setText("move down");
        jpanel.add(moveDownCheckBox, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        moveLeftCheckBox = new JCheckBox();
        moveLeftCheckBox.setText("move left");
        jpanel.add(moveLeftCheckBox, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        moverRightCheckBox = new JCheckBox();
        moverRightCheckBox.setText("move right");
        jpanel.add(moverRightCheckBox, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jumpActioncheckBox = new JCheckBox();
        jumpActioncheckBox.setText("jump action");
        jpanel.add(jumpActioncheckBox, new GridConstraints(6, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        meleeAttackActioncheckBox = new JCheckBox();
        meleeAttackActioncheckBox.setText("melee attack action");
        jpanel.add(meleeAttackActioncheckBox, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        stopControl = new JButton();
        stopControl.setText("stopControl");
        jpanel.add(stopControl, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        voice2CheckBox = new JCheckBox();
        voice2CheckBox.setText("voice2");
        jpanel.add(voice2CheckBox, new GridConstraints(6, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        voice1CheckBox = new JCheckBox();
        voice1CheckBox.setText("voice1");
        jpanel.add(voice1CheckBox, new GridConstraints(5, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        grenadeAttackActioncheckBox = new JCheckBox();
        grenadeAttackActioncheckBox.setText("grenade attack action");
        jpanel.add(grenadeAttackActioncheckBox, new GridConstraints(4, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        stopActioncheckBox = new JCheckBox();
        stopActioncheckBox.setText("stop action");
        jpanel.add(stopActioncheckBox, new GridConstraints(7, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return jpanel;
    }

}
