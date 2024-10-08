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
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.proxy.websocket.*;
import burp.api.montoya.websocket.Direction;
import org.json.JSONArray;
import org.json.JSONObject;
import socketsleuth.WebSocketInterceptionRulesTableModel;
import websocket.MessageProvider;
import whifyh.DataStatusManager;
import whifyh.QiangZhanStatus;
import javax.swing.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

class WebSocketMessageHandler implements ProxyMessageHandler {

    private final MessageProvider socketProvider;
    MontoyaApi api;
    int socketId;
    Logging logger;

    WebSocketStreamTableModel streamModel;

    JTable streamTable;

    WebSocketInterceptionRulesTableModel interceptionRules;

    WebSocketMatchReplaceRulesTableModel matchReplaceRules;

    SocketCloseCallback socketCloseCallback;
    JSONRPCResponseMonitor responseMonitor;
    WebSocketAutoRepeater webSocketAutoRepeater;
    // qiangzhan
    HttpRequest request;
    boolean gunfight = false;

    public WebSocketMessageHandler(
            MontoyaApi api,
            int socketId,
            WebSocketStreamTableModel streamModel,
            JTable streamTable,
            WebSocketInterceptionRulesTableModel interceptionRules,
            WebSocketMatchReplaceRulesTableModel matchReplaceRules,
            SocketCloseCallback socketCloseCallback,
            JSONRPCResponseMonitor responseMonitor,
            WebSocketAutoRepeater webSocketAutoRepeater,
            MessageProvider socketProvider,
            HttpRequest request) {
        this.api = api;
        this.socketId = socketId;
        this.logger = api.logging();
        this.streamModel = streamModel;
        this.streamTable = streamTable;
        this.interceptionRules = interceptionRules;
        this.matchReplaceRules = matchReplaceRules;
        this.socketCloseCallback = socketCloseCallback;
        this.responseMonitor = responseMonitor;
        this.webSocketAutoRepeater =  webSocketAutoRepeater;
        this.socketProvider = socketProvider;
        this.request = request;
        if (request.url().contains("gunfight")) {
            gunfight = true;
        }
    }

    @Override
    public TextMessageReceivedAction handleTextMessageReceived(InterceptedTextMessage interceptedTextMessage) {
        // Pass received message to socket message provider
        this.socketProvider.handleTextMessage(this.socketId, interceptedTextMessage);

        // Handle message detections
        this.responseMonitor.processResponse(interceptedTextMessage.payload());


        int selectedRowIndex = streamTable.getSelectedRow();
        ListSelectionModel selectionModel = streamTable.getSelectionModel();
        boolean isSelectionEmpty = selectionModel.isSelectionEmpty();
        WebSocketStream stream=new WebSocketStream(
                streamModel.getRowCount(),
                interceptedTextMessage,
                LocalDateTime.now(),
                ""
        );
        // Handle autorepeater rules
        this.webSocketAutoRepeater.onMessageReceived(this.socketId, stream.getInterceptedMessage());

        streamModel.addStream(stream);

        // Restore the selection if there was a previous selection
        if (!isSelectionEmpty) {
            selectionModel.setSelectionInterval(selectedRowIndex, selectedRowIndex);
        } else {
            selectionModel.clearSelection();
        }

        if (shouldInterceptMessage(this.interceptionRules, interceptedTextMessage)) {
            // This is the in the API example but seems to break WSs :/
            //interceptedTextMessage.annotations().setHighlightColor(HighlightColor.RED);
            return TextMessageReceivedAction.intercept(interceptedTextMessage);
        }

        if (shouldDropMessage(this.matchReplaceRules, stream.getInterceptedMessage())) {
            return TextMessageReceivedAction.drop();
        }

        interceptedTextMessage = (InterceptedTextMessage) handleMatchAndReplace(
                this.matchReplaceRules,
                stream.getInterceptedMessage()
        );
        // qiangzhan
        if (gunfight && DataStatusManager.booleanStatusMap.get("qiangzhan:startRecord")) {
            if (interceptedTextMessage.direction().equals(Direction.SERVER_TO_CLIENT)) {
                setGunfight(interceptedTextMessage.payload());
            }
        }
        return TextMessageReceivedAction.continueWith(interceptedTextMessage);
    }

    private void setGunfight(String payload) {
        try {
            JSONArray jsonObject = new JSONArray(payload);
            // 封包:2,1
            if (jsonObject.getInt(0) == 2 && jsonObject.getInt(1) == 1) {
                JSONObject index2 = jsonObject.getJSONObject(2);
                // 开始游戏
                if (index2.has("roomData")) {
                   JSONObject roomData = index2.getJSONObject("roomData");
                   String id = roomData.getString("id");
                   JSONArray players = roomData.getJSONArray("players");
                   QiangZhanStatus.nowRoomId = id;
                   QiangZhanStatus.players = new ArrayList<>();
                   for (Object player : players) {
                       QiangZhanStatus.players.add(new QiangZhanStatus.Player(
                               ((JSONObject) player).getString("name"),
                               ((JSONObject) player).getInt("id"),
                               ((JSONObject) player).getInt("camp"),
                               ((JSONObject) player).getInt("rankLevel"),
                               ((JSONObject) player).getInt("rankScore")
                       ));
                   }
                   QiangZhanStatus.refuse();
                   api.logging().raiseInfoEvent("[开始游戏]:" + "[房间号]:" + QiangZhanStatus.nowRoomId + "[Payload]:" + payload);
                   return;
                }
                // 用户信息
                if (index2.has("userInfo")) {
                    QiangZhanStatus.controlRunningStatus = false;
                    JSONObject userInfo = index2.getJSONObject("userInfo");
                    QiangZhanStatus.userId = userInfo.getInt("id");
                    api.logging().raiseInfoEvent("[用户信息]: [ID]:" + QiangZhanStatus.userId + "[Payload]:" + payload);
                    return;
                }
            }

//            if (jsonObject.getInt(0) == 0 && jsonObject.getInt(1) == 0) {
//                Object object = jsonObject.get(2);
//                if (object instanceof JSONArray) {
//                    // 掉线重连0,a
//                    JSONArray stringArray = jsonObject.getJSONArray(2);
//                    if ("0".equals(stringArray.getString(0)) && "a".equals(stringArray.getString(1))) {
//                        JSONObject info = jsonObject.getJSONArray(2).getJSONArray(2).getJSONObject(2);
//                        HashMap<String, Object> index2Map = new HashMap<>();
//                        HashMap<String, Object> roomDataMap = new HashMap<>();
//                        index2Map.put("roomData", roomDataMap);
//                        roomDataMap.put("id", info.getString("id"));
//                        roomDataMap.put("players", info.getJSONArray("players"));
//                        QiangZhanStatus.controlRunningStatus = false;
//                        Thread.sleep(1000);
//                        ArrayList<Object> fixPayload = new ArrayList<>();
//                        fixPayload.add(2);
//                        fixPayload.add(1);
//                        fixPayload.add(index2Map);
//                        JSONArray newPayload = new JSONArray(fixPayload);
//                        api.logging().raiseInfoEvent("正在读取对局[fixPayload]:" + newPayload);
//                        setGunfight(newPayload.toString());
//                    }
//                }
//            }

        } catch (Exception e) {
            api.logging().raiseInfoEvent("[解析失败]:" + payload + " [报错原因]:" + e.getMessage() + e.getStackTrace()[e.getStackTrace().length - 1]);
            QiangZhanStatus.controlRunningStatus = false;
        }
    }

    private boolean shouldDropMessage(
            WebSocketMatchReplaceRulesTableModel matchReplaceRules,
            InterceptedMessageFacade interceptedMessageFacade
    ) {
        for (int i = 0; i < matchReplaceRules.getRowCount(); i++) {
            boolean enabled = (boolean) matchReplaceRules.getValueAt(i, 0); // ENABLED

            if (!enabled) {
                continue;
            }

            WebSocketMatchReplaceRulesTableModel.MatchType matchType =
                    (WebSocketMatchReplaceRulesTableModel.MatchType) matchReplaceRules.getValueAt(i, 1);

            WebSocketMatchReplaceRulesTableModel.Direction direction =
                    (WebSocketMatchReplaceRulesTableModel.Direction) matchReplaceRules.getValueAt(i, 2);

            String strMatch = (String) matchReplaceRules.getValueAt(i, 3);

            if (matchType != WebSocketMatchReplaceRulesTableModel.MatchType.DROP) {
                continue;
            }

            if (direction == WebSocketMatchReplaceRulesTableModel.Direction.CLIENT_TO_SERVER) {
                if (!interceptedMessageFacade.direction().toString().equals("CLIENT_TO_SERVER")) continue;
            }

            if (direction == WebSocketMatchReplaceRulesTableModel.Direction.SERVER_TO_CLIENT) {
                if (!interceptedMessageFacade.direction().toString().equals("SERVER_TO_CLIENT")) continue;
            }

            // Find / Match is in Hex
            if (Utils.isHexString(strMatch)) {
                return Utils.byteArrayContains(
                        interceptedMessageFacade.binaryPayload(),
                        Utils.hexStringToByteArray(strMatch)
                );
            // Otherwise normal string
            } else {
                return interceptedMessageFacade.stringPayload().contains(strMatch);
            }
        }
        return false;
    }

    private Object handleMatchAndReplace(
            WebSocketMatchReplaceRulesTableModel matchReplaceRules,
            InterceptedMessageFacade interceptedMessageFacade
    ) {
        for (int i = 0; i < matchReplaceRules.getRowCount(); i++) {
            boolean enabled = (boolean) matchReplaceRules.getValueAt(i, 0); // ENABLED

            if (!enabled) {
                continue;
            }
            WebSocketMatchReplaceRulesTableModel.MatchType matchType =
                    (WebSocketMatchReplaceRulesTableModel.MatchType) matchReplaceRules.getValueAt(i, 1);

            WebSocketMatchReplaceRulesTableModel.Direction direction =
                    (WebSocketMatchReplaceRulesTableModel.Direction) matchReplaceRules.getValueAt(i, 2);

            String strMatch = (String) matchReplaceRules.getValueAt(i, 3);

            String strReplace = (String) matchReplaceRules.getValueAt(i, 4);

            if (matchType != WebSocketMatchReplaceRulesTableModel.MatchType.REPLACE) {
                // dropping messages needs to return a different function from the handler
                // handle this separately inside handleTextMessageReceived + handleBinaryMessageReceived
                continue;
            }

            if (direction == WebSocketMatchReplaceRulesTableModel.Direction.CLIENT_TO_SERVER) {
                if (!interceptedMessageFacade.direction().toString().equals("CLIENT_TO_SERVER")) continue;
            }

            if (direction == WebSocketMatchReplaceRulesTableModel.Direction.SERVER_TO_CLIENT) {
                if (!interceptedMessageFacade.direction().toString().equals("SERVER_TO_CLIENT")) continue;
            }

            // Are we match / replacing a hex pattern?
            if (Utils.isHexString(strMatch)) {
                byte[] bytesMatch = Utils.hexStringToByteArray(strMatch);
                byte[] bytesReplace = Utils.isHexString(strReplace) ? Utils.hexStringToByteArray(strReplace) : strReplace.getBytes();
                byte[] inputBytes = interceptedMessageFacade.binaryPayload();
                byte[] modified = Utils.replace(inputBytes, bytesMatch, bytesReplace);
                interceptedMessageFacade.setBytesPayload(modified);
                return interceptedMessageFacade.getInterceptedMessage();
            } else {        // it's a normal string or regex replacement
                // Do the replacement and see if there is changes
                String newStr = Utils.replace(interceptedMessageFacade.stringPayload(), strMatch, strReplace);
                if (!newStr.equals(interceptedMessageFacade.stringPayload())) {
                    interceptedMessageFacade.setStringPayload(newStr);
                    return interceptedMessageFacade.getInterceptedMessage();
                }
            }
        }
        return interceptedMessageFacade.getInterceptedMessage();
    }

    private boolean shouldInterceptMessage(
            WebSocketInterceptionRulesTableModel interceptionRules,
            InterceptedTextMessage interceptedTextMessage
    ) {
        for (int i = 0; i < interceptionRules.getRowCount(); i++) {
            boolean enabled = (boolean) interceptionRules.getValueAt(i, 0);

            WebSocketInterceptionRulesTableModel.MatchType matchType
                    = (WebSocketInterceptionRulesTableModel.MatchType) interceptionRules.getValueAt(i, 1);

            WebSocketInterceptionRulesTableModel.Direction direction
                    = (WebSocketInterceptionRulesTableModel.Direction) interceptionRules.getValueAt(i, 2);


            String condition = (String) interceptionRules.getValueAt(i, 3);

            // ignore disabled rules
            if (enabled) {
                boolean shouldIntercept = false;

                // check direction before condition - probably a better way to test this
                if (direction == WebSocketInterceptionRulesTableModel.Direction.CLIENT_TO_SERVER) {
                    if (!interceptedTextMessage.direction().toString().equals("CLIENT_TO_SERVER")) continue;
                }

                if (direction == WebSocketInterceptionRulesTableModel.Direction.SERVER_TO_CLIENT) {
                    if (!interceptedTextMessage.direction().toString().equals("SERVER_TO_CLIENT")) continue;
                }

                api.logging().raiseInfoEvent("direction rule: " + direction + " - message direction" + interceptedTextMessage.direction().toString());

                switch (matchType) {
                    case CONTAINS: {
                        shouldIntercept = interceptedTextMessage.payload().contains(condition);
                        break;
                    }
                    case DOES_NOT_CONTAIN: {
                        shouldIntercept = !interceptedTextMessage.payload().contains(condition);
                        break;
                    }
                    case EXACT_MATCH: {
                        shouldIntercept = interceptedTextMessage.payload().equals(condition);
                       break;
                    }
                    default: {
                        api.logging().logToOutput("Unknown interceptor match type detected!");
                        break;
                    }
                }

                // Don't need to test additional rules if we already matched
                if (shouldIntercept) return true;
            }
        }

        return false;
    }

    @Override
    public TextMessageToBeSentAction handleTextMessageToBeSent(InterceptedTextMessage interceptedTextMessage) {
        return TextMessageToBeSentAction.continueWith(interceptedTextMessage);
    }

    @Override
    public BinaryMessageReceivedAction handleBinaryMessageReceived(InterceptedBinaryMessage interceptedBinaryMessage) {
        int selectedRowIndex = streamTable.getSelectedRow();
        ListSelectionModel selectionModel = streamTable.getSelectionModel();
        boolean isSelectionEmpty = selectionModel.isSelectionEmpty();

        streamModel.addStream(new WebSocketStream(
                streamModel.getRowCount(),
                interceptedBinaryMessage,
                LocalDateTime.now(),
                ""
        ));

        // Restore the selection if there was a previous selection
        if (!isSelectionEmpty) {
            selectionModel.setSelectionInterval(selectedRowIndex, selectedRowIndex);
        } else {
            selectionModel.clearSelection();
        }
        return BinaryMessageReceivedAction.continueWith(interceptedBinaryMessage);
    }

    @Override
    public BinaryMessageToBeSentAction handleBinaryMessageToBeSent(InterceptedBinaryMessage interceptedBinaryMessage) {
        return BinaryMessageToBeSentAction.continueWith(interceptedBinaryMessage);
    }

    @Override
    public void onClose() {
        ProxyMessageHandler.super.onClose();
        api.logging().logToOutput("socket is closing");
        this.socketCloseCallback.handleConnectionClosed();
    }
}
