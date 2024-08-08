package whifyh;

import burp.api.montoya.proxy.websocket.ProxyWebSocket;

import java.util.HashMap;
import java.util.Map;

public class whifyhTT {

    public static Map<Integer, SocketMe> proxyList = new HashMap<>();

    public static SocketMe getActiveSocket() {
        for (Map.Entry<Integer, SocketMe> entry : proxyList.entrySet()) {
            if (entry.getValue().active) {
                return entry.getValue();
            }
        }
        try {
            Thread.sleep(1000);
            return getActiveSocket();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
