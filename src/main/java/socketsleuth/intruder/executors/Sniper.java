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

package socketsleuth.intruder.executors;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.proxy.websocket.ProxyWebSocket;
import burp.api.montoya.websocket.Direction;
import burp.api.montoya.websocket.TextMessage;
import socketsleuth.intruder.WSIntruderMessageView;
import socketsleuth.intruder.payloads.models.IPayloadModel;
import websocket.MessageProvider;
import whifyh.DataStatusManager;
import whifyh.QiangZhanStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class WebSocketMessage {
    private String data;
    private Direction direction;

    public WebSocketMessage(String data, Direction direction) {
        this.data = data;
        this.direction = direction;
    }
}

public class Sniper {
    private final MessageProvider socketProvider;
    private MontoyaApi api;
    private WSIntruderMessageView messageView;
    private Thread workerThread;
    private volatile boolean cancelled = false;
    private int minDelay = 100;
    private int maxDelay = 200;
    private List<WebSocketMessage> sentMessages;

    public Sniper(MontoyaApi api, WSIntruderMessageView messageView, MessageProvider socketProvider) {
        this.api = api;
        this.messageView = messageView;
        this.socketProvider = socketProvider;
        this.sentMessages = new ArrayList<WebSocketMessage>();
    }

    public int getMinDelay() {
        return minDelay;
    }

    public void setMinDelay(int minDelay) {
        this.minDelay = minDelay;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public void setMaxDelay(int maxDelay) {
        this.maxDelay = maxDelay;
    }

    public boolean isRunning() {
        if (workerThread == null) {
            return false;
        }

        return workerThread.isAlive();
    }

    public void qiangZhanKeepControl() {
        maxDelay = 100;
        minDelay = 50;
        Random rand = new Random();
        Thread keep = new Thread(() -> {
            while (QiangZhanStatus.controlRunningStatus) {
                for (DataStatusManager.SocketMe socketMe : DataStatusManager.getAllActiveSockets()) {
                    for (String msg : QiangZhanStatus.getSendMessageList()) {
                        sendMessage(socketMe.socket, Direction.CLIENT_TO_SERVER, false, msg, rand);
                    }
                }
                api.logging().raiseInfoEvent("控制中...");
            }
            api.logging().raiseInfoEvent("控制结束");
        });
        keep.start();
    }

    public void start(ProxyWebSocket proxyWebSocket,
                      int socketId,
                      IPayloadModel<String> payloadModel,
                      String baseInput,
                      Direction selectedDirection,
                      Boolean isHexMode,
                      Boolean isListMode,
                      Boolean isKeepAlive) {
        if (workerThread != null && workerThread.isAlive()) {
            api.logging().logToOutput("Intruder action is already running. Wait before new action.");
            return;
        }

        api.logging().logToOutput(
                "Starting sniper payload insertion with Min Delay: "
                        + minDelay
                        + " Max Delay: "
                        + maxDelay
        );

        Consumer<TextMessage> responseSubscriber = textMessage -> {
            if(null != messageView) {
                messageView.getTableModel().addMessage(textMessage.payload(), textMessage.direction());
            }
        };
        if (socketProvider != null) {
            this.socketProvider.subscribeTextMessage(socketId, responseSubscriber);
        }


        Random rand = new Random();
        workerThread = new Thread(() -> {
            api.logging().logToOutput("Sniper execution started");
            for (String payload : payloadModel) {
                List<ProxyWebSocket> sendWebSockets = new ArrayList<ProxyWebSocket>();
                sendWebSockets.add(proxyWebSocket);
                // 是否保持最新的socket
                if (isKeepAlive) {
                    sendWebSockets.clear();
                    for (DataStatusManager.SocketMe allActiveSocket : DataStatusManager.getAllActiveSockets()) {
                        sendWebSockets.add(allActiveSocket.socket);
                    }
                }

                for (ProxyWebSocket now : sendWebSockets) {
                    // 是否List模式
                    if (isListMode) {
                        List<String> lineInput = splitAndTrim(baseInput);
                        for (String input : lineInput) {
                            String newInput = replacePlaceholders(input, payload);
                            sendMessage(now, selectedDirection, isHexMode, newInput, rand);
                        }
                    } else {
                        String newInput = replacePlaceholders(baseInput, payload);
                        sendMessage(now, selectedDirection, isHexMode, newInput, rand);
                    }
                }

            }

            try {
                api.logging().logToOutput("finished - cleaning up");
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            } finally {
                if (socketProvider != null) {
                    this.socketProvider.unsubscribeTextMessage(socketId, responseSubscriber);
                }
                api.logging().logToOutput("clean up complete");
            }
        });

        workerThread.start();
    }

    private void sendMessage(ProxyWebSocket proxyWebSocket, Direction selectedDirection, Boolean isHexMode, String newInput, Random rand) {
        // 是否16进制模式
        if (isHexMode) {
            // 将十六进制字符串转换为字节数组
            byte[] binaryData = hexStringToBytes(newInput);
            api.logging().raiseInfoEvent("[Hex]:" + bytesToHexString(binaryData));
            proxyWebSocket.sendBinaryMessage(ByteArray.byteArray(binaryData), selectedDirection);
            if (messageView != null) {
                messageView.getTableModel().addMessage("[Hex]" + newInput, selectedDirection);
            }
            sentMessages.add(new WebSocketMessage(newInput, selectedDirection));
        } else {
            api.logging().raiseInfoEvent("[String]:" + newInput);
            proxyWebSocket.sendTextMessage(newInput, selectedDirection);
            if (messageView != null) {
               messageView.getTableModel().addMessage(newInput, selectedDirection);
            }
            sentMessages.add(new WebSocketMessage(newInput, selectedDirection));
        }

        int delay = rand.nextInt(maxDelay - minDelay + 1) + minDelay;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private static String replacePlaceholders(String input, String replacement) {
        Pattern pattern = Pattern.compile("§(.*?)§");
        Matcher matcher = pattern.matcher(input);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static byte[] hexStringToByteArray(String hexString) {
        String[] hexBytes = hexString.split("\\s+");
        byte[] byteArray = new byte[hexBytes.length];
        for (int i = 0; i < hexBytes.length; i++) {
            byteArray[i] = (byte) Integer.parseInt(hexBytes[i], 16);
        }
        return byteArray;
    }

    public static String bytesToHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                // 如果是一位的话，前面补0
                hexString.append('0');
            }
            hexString.append(hex);
            if (i < bytes.length - 1) {
                hexString.append(" "); // 在字节间加入空格分隔
            }
        }
        return hexString.toString().toUpperCase(); // 转换成大写
    }

    public static byte[] hexStringToBytes(String hexString) {
        hexString = hexString.replaceAll("\\s", ""); // 移除字符串中的空格
        byte[] bytes = new byte[hexString.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            int value = Integer.parseInt(hexString.substring(index, index + 2), 16);
            bytes[i] = (byte) value;
        }
        return bytes;
    }

    public static List<String> splitAndTrim(String input) {
        List<String> result = new ArrayList<>();

        // 按换行符分割字符串
        String[] lines = input.split("\\r?\\n");

        // 遍历每一行
        for (String line : lines) {
            // 去除行首和行尾的空格
            String trimmedLine = line.trim();

            // 使用正则表达式将多个空格替换为单个空格
            String formattedLine = trimmedLine.replaceAll("\\s+", " ");

            // 如果处理后的行不为空,则添加到结果列表中
            if (!formattedLine.isEmpty()) {
                result.add(formattedLine);
            }
        }

        return result;
    }
}
