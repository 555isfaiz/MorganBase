package morgan.connection;

import morgan.support.Factory;
import morgan.support.Log;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractWebSocketHandler {

    protected final Map<String, Method> handlers_ = new HashMap<>();

    public AbstractWebSocketHandler() {
        init();
    }

    void init() {
        for (var m : this.getClass().getDeclaredMethods()) {
            var anno = m.getAnnotation(WebSocketHandler.class);
            if (anno == null)
                continue;

            handlers_.put(anno.name(), m);
        }
    }

    public void handle(String path, Object ... args) {
        var m = handlers_.get(path);
        if (m == null) {
            return;
        }

        ConnStarter.connNode.anyWorker().schedule(100, () -> {
            try {
                m.invoke(Factory.webSocketHandlerInstance(), args);
            } catch (Exception e) {
                Log.http.error("error invoking websocket method:{}", m.getName());
                e.printStackTrace();
            }
        });
    }
}
