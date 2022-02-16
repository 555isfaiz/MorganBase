package morgan.connection;

import morgan.structure.Node;
import morgan.structure.Worker;
import morgan.support.Log;
import morgan.support.Time;
import morgan.support.functions.Function1;
import morgan.support.functions.Function2;

import java.util.HashMap;
import java.util.Map;

public class SessionManager extends Worker {

    class Session {
        String sessionId;
        long expire;
        int connId = -1;
    }

    private final Map<String, Session> sessions_ = new HashMap<>();

    public SessionManager(Node node) {
        super(node, "SessionManager");
    }

    @Override
    public void pulseOverride() {
        for (var e : sessions_.entrySet()) {
            if (getTime() <= e.getValue().expire)
                continue;

            if (e.getValue().connId < 0)
                continue;

            sessions_.remove(e.getKey());
            AbstractConnection.closeConnection_(e.getValue().connId);
            break;
        }
    }

    public void createSession(String account, String sessionId) {
        if (sessions_.containsKey(account)) {
            var session = sessions_.get(account);
            if (session.connId != -1)
                AbstractConnection.closeConnection_(session.connId);
            Log.worker.error("duplicate session! account:{}", account);
        }

        var session = new Session();
        session.sessionId = sessionId;
        session.expire = System.currentTimeMillis() + 30 * Time.MIN;
        sessions_.put(account, session);
    }

    public static void createSession_(String account, String sessionId) {
        CallWithStack(account, sessionId);
    }

    public void getSessionId(String account) {
        var session = sessions_.get(account);
        String sessionId = session == null ? "" : session.sessionId;
        returns("sessionId", sessionId);
    }

    public static void getSessionId_(String account) {
        CallWithStack(account);
    }

    public void verifySession(String sessionId) {
        returns(sessions_.values().stream().anyMatch(s -> s.sessionId.equals(sessionId)));
    }

    public static void verifySession_(String sessionId) {
        CallWithStack(sessionId);
    }

    public void updateConnection(String sessionId, int connId) {
        for (var s : sessions_.values()) {
            if (s.sessionId.equals(sessionId) && s.connId < 0) {
                s.connId = connId;
                returns("result", true);
                return;
            }
        }
        returns("result", false);
    }

    public static void updateConnection_(String sessionId, int connId) {
        CallWithStack(sessionId, connId);
    }

    public void renewSession(String sessionId) {
        for (var s : sessions_.values()) {
            if (s.sessionId.equals(sessionId)) {
                s.expire = getTime() + 30 * Time.MIN;
            }
        }
    }

    public static void renewSession_(String sessionId) {
        CallWithStack(sessionId);
    }

    public void getAccountBySession(String sessionId) {
        for (var e : sessions_.entrySet()) {
            if (e.getValue().sessionId.equals(sessionId)) {
                returns("account", e.getKey());
                break;
            }
        }
    }

    public static void getAccountBySession_(String sessionId) {
        CallWithStack(sessionId);
    }

    public void discardSession(int connId) {
        for (var e : sessions_.entrySet()) {
            if (e.getValue().connId == connId) {
                sessions_.remove(e.getKey());
                break;
            }
        }
    }

    public static void discardSession_(int connId) {
        CallWithStack(connId);
    }

    @Override
    public void registMethods() {
        _rpcMethodManager.registMethod("createSession", (Function2<String, String>)this::createSession, String.class, String.class);
        _rpcMethodManager.registMethod("getSessionId", (Function1<String>)this::getSessionId, String.class);
        _rpcMethodManager.registMethod("verifySession", (Function1<String>)this::verifySession, String.class);
        _rpcMethodManager.registMethod("updateConnection", (Function2<String, Integer>)this::updateConnection, String.class, Integer.class);
        _rpcMethodManager.registMethod("renewSession", (Function1<String>)this::renewSession, String.class);
        _rpcMethodManager.registMethod("getAccountBySession", (Function1<String>)this::getAccountBySession, String.class);
        _rpcMethodManager.registMethod("discardSession", (Function1<Integer>)this::discardSession, Integer.class);
    }
}
