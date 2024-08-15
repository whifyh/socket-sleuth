package whifyh;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.websocket.Direction;
import socketsleuth.intruder.executors.Sniper;

import javax.swing.*;
import java.util.*;

public class QiangZhanStatus {

    public static MontoyaApi api;
    public static List<Player> players;
    public static String nowRoomId;
    public static JPanel playerPanel;
    public static List<JCheckBox> playerCheckBoxList = new ArrayList<>();
    public static JTextField roomIdTextField;
    public static JCheckBox autoSelectedEnemyPlayerCheckBox;
    public static JCheckBox autoStartControlCheckBox;
    public static JCheckBox disarmEquipmentCheckBox;
    public static JSlider controlPowerSlider;
    public static List<Integer> selectedPlayerIdList = new ArrayList<>();
    public static Boolean controlRunningStatus = false;
    public static List<String> sendMessageListCache = null;
    public static Sniper executor;
    public static Map<String, String> functionMap = new HashMap<>();

    public static Integer userId = null;

    public static List<String> disarmEquipmentMsgList = new ArrayList<>(){{
        add("[1,89,49,{\"objType\":4,\"objId\":4001001,\"pid\":%s}]");
        add("[1,93,49,{\"objType\":4,\"objId\":4005001,\"pid\":%s}]");
        add("[1,96,49,{\"objType\":4,\"objId\":4006001,\"pid\":%s}]");
        add("[1,99,49,{\"objType\":5,\"objId\":5001001,\"pid\":%s}]");
        add("[1,99,49,{\"objType\":6,\"objId\":6001001,\"pid\":%s}]");
        add("[1,99,49,{\"objType\":7,\"objId\":7001001,\"pid\":%s}]");
        add("[1,108,49,{\"objType\":8,\"objId\":8001001,\"pid\":%s}]");
        add("[1,111,49,{\"objType\":9,\"objId\":9001001,\"pid\":%s}]");
        add("[1,114,49,{\"objType\":13,\"objId\":0,\"pid\":%s}]");
        add("[1,117,49,{\"objType\":3,\"objId\":0,\"pid\":%s}]");
    }};

    public static class Player {
        public String name;
        public Integer id;
        public Integer camp;
        public Integer rankLevel;
        public Integer rankScore;

        public Player(String name, Integer id, Integer camp, Integer rankLevel, Integer rankScore) {
            this.name = name;
            this.id = id;
            this.camp = camp;
            this.rankLevel = rankLevel;
            this.rankScore = rankScore;
        }
    }

    public static void refuse() {
        roomIdTextField.setText(nowRoomId);

        selectedPlayerIdList.clear();
        playerCheckBoxList.clear();
        controlRunningStatus = false;
        playerPanel.removeAll();
        sendMessageListCache = null;
        executor = new Sniper(api, null, null);

        playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.Y_AXIS));
        for (Player player : players) {
            JCheckBox jCheckBox = getPlayerCheckBox(player);
            playerPanel.add(jCheckBox);
            playerCheckBoxList.add(jCheckBox);
        }
        playerPanel.setVisible(true);
        playerPanel.revalidate();
        playerPanel.repaint();

        // 自动选敌
        if (autoSelectedEnemyPlayerCheckBox.isSelected()) {
            Player player = players.stream().filter(x -> Objects.equals(x.id, userId)).findFirst().get();
            String userCamp = (player.camp == -1) ? "Red" : "Blue";
            playerCheckBoxList.stream()
                    .filter(x -> !x.getText().contains(userCamp))
                    .forEach(AbstractButton::doClick);
        }

        // 提前开局
        if (disarmEquipmentCheckBox.isSelected()) {
            players.stream().filter(x -> !Objects.equals(x.id, userId)).forEach(player -> {
               for (DataStatusManager.SocketMe socketMe : DataStatusManager.getAllActiveSockets()) {
                    for (String msg : disarmEquipmentMsgList) {
                        executor.sendMessageFast(socketMe.socket, Direction.CLIENT_TO_SERVER, false, String.format(msg, player.id));
                    }
                }
            });
        }

        // 自动启动控制
        if (autoStartControlCheckBox.isSelected()) {
            controlRunningStatus = true;
            executor.qiangZhanKeepControl();
        }
    }

    private static JCheckBox getPlayerCheckBox(Player player) {
        String team = (player.camp == -1) ? "Red" : "Blue";
        String rankScoreFormatted = String.format("%-4d", player.rankScore);
        String rankLevelFormatted = String.format("%-2d", player.rankLevel);
        StringBuilder checkBoxText = new StringBuilder()
                .append("ID:[").append(player.id)
                .append("] Team:[").append(team)
                .append("] RankScore:[").append(rankScoreFormatted)
                .append("] RankLevel:[").append(rankLevelFormatted)
                .append("]");

        if (player.rankScore == 0) {
            checkBoxText.append(" [BOT]");
        }
        JCheckBox jCheckBox = new JCheckBox(checkBoxText.toString());

        jCheckBox.addActionListener(e -> {
            sendMessageListCache = null;
            if (jCheckBox.isSelected()) {
                selectedPlayerIdList.add(player.id);
            } else {
                selectedPlayerIdList.remove(player.id);
            }
        });
        return jCheckBox;
    }

    public static List<String> getSendMessageList() {
        if (sendMessageListCache != null) {
            return sendMessageListCache;
        }
        sendMessageListCache = new ArrayList<>();
        for (Map.Entry<String, String> entry : functionMap.entrySet()) {
            for (Integer id : selectedPlayerIdList) {
                sendMessageListCache.add(String.format(entry.getValue(), nowRoomId, id));
            }
        }
        return sendMessageListCache;
    }

}
