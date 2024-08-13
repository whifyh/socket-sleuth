package whifyh;

import burp.api.montoya.proxy.websocket.ProxyWebSocket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataStatusManager {

    public static Map<Integer, SocketMe> proxyList = new HashMap<>();

    public static Map<String, Boolean> booleanStatusMap = new HashMap<>();

    public static List<SocketMe> getAllActiveSockets() {
        return proxyList.values().stream().filter(x -> x.active).toList();
    }

    public static class SocketMe {
        public ProxyWebSocket socket;
        public Boolean active = false;

        public SocketMe(ProxyWebSocket socket, Boolean active) {
            this.socket = socket;
            this.active = active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }


    }

}
