package morgan.connection;

import java.util.concurrent.atomic.AtomicInteger;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.CharsetUtil;
import morgan.support.Factory;
import morgan.support.Log;

public class HttpServerHandler extends ChannelInboundHandlerAdapter {

    public static final AtomicInteger idAllocate = new AtomicInteger();

    private AbstractConnection conn = null;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof TextWebSocketFrame) {
            handleWebSocketRequest(ctx, (TextWebSocketFrame) msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (conn != null) {
            conn.closeConnection();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        var body = httpRequest.content().toString(CharsetUtil.UTF_8);
        Object[] args;
        if (!body.isBlank()) {
            var json = JSONObject.parseObject(body);
            args = new Object[json.size() + 1];
            args[0] = ctx;
            int i = 1;
            for (var v : json.values()) {
                args[i] = v;
                i++;
            }
        } else {
            args = new Object[] {ctx};
        }

        int uriEnd = 0;
        for (int i = 0; i < httpRequest.uri().length(); i++) {
            if (httpRequest.uri().charAt(i) == '/' && i != 0) {
                uriEnd = i;
                break;
            }
        }

        var uri = uriEnd == 0 || httpRequest.method().name().equals("GET") ? httpRequest.uri()
                : httpRequest.uri().substring(0, uriEnd);
        Factory.httpMappingInstance().handle(uri, httpRequest.method().name(), args);
    }

    void handleWebSocketRequest(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
        if (conn != null) {
            ConnStarter.connNode.anyWorker().schedule(10, () -> {
                conn.handleMsg(msg.text().getBytes());
            });
            return;
        }

        try {
            var jo = JSONObject.parseObject(msg.text());
            String sessionId = jo.getString("sessionId");
            Log.http.info("webSocket:{}", jo.toJSONString());
            ConnStarter.connNode.anyWorker().schedule(10, () -> {
                int id = idAllocate.getAndIncrement();
                SessionManager.updateConnection_(sessionId, id);
                SessionManager.Listen_(r -> {
                    boolean valid = r.getResult();
                    if (!valid) {
                        var j = new JSONObject();
                        j.put("message", "bad session");
                        ctx.channel().writeAndFlush(j.toJSONString());
                        ctx.close();
                        Log.http.info("bad session connection");
                        return;
                    }

                    conn = Factory.newConnectionInstance(ConnStarter.connNode, ctx.channel(), id);
                    ConnStarter.connNode.addWorker(conn);
                    Log.http.info("connection established! connectionId:{}", id);
                });
            });
        } catch (Exception e) {
            ctx.close();
        }
    }
}
