package whifyh;

import burp.api.montoya.MontoyaApi;
import socketsleuth.intruder.executors.Sniper;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QiangZhanStatus {

    public static MontoyaApi api;
    public static List<Player> players;
    public static String nowRoomId;
    public static JPanel playerPanel;
    public static JTextField roomIdTextField;
    public static List<Integer> selectedPlayerIdList = new ArrayList<>();
    public static Boolean controlRunningStatus = false;
    public static List<String> sendMessageListCache = null;
    public static Sniper executor;
    public static Map<String, String> functionMap = new HashMap<>();

    public static class Player {
        public String name;
        public Integer id;
        public Integer camp;
        public String openId;

        public Player(String name, Integer id, Integer camp, String openId) {
            this.name = name;
            this.id = id;
            this.camp = camp;
            this.openId = openId;
        }
    }

    public static void refuse() {
        roomIdTextField.setText(nowRoomId);

        selectedPlayerIdList.clear();
        controlRunningStatus = false;
        playerPanel.removeAll();
        sendMessageListCache = null;
        executor = new Sniper(api, null, null);

        playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.Y_AXIS));
        for (Player player : players) {

            JCheckBox jCheckBox = new JCheckBox("ID:[" + player.id + "] Team:[" + (player.camp.equals(-1) ? "Red]" : "Blue]"));
            jCheckBox.addActionListener(e -> {
                sendMessageListCache = null;
                if (jCheckBox.isSelected()) {
                    selectedPlayerIdList.add(player.id);
                } else {
                    selectedPlayerIdList.remove(player.id);
                }
            });
            playerPanel.add(jCheckBox);
        }
        playerPanel.setVisible(true);
        playerPanel.revalidate();
        playerPanel.repaint();
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
