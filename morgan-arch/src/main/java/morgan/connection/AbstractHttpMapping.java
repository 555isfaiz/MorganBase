package morgan.connection;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import morgan.support.Factory;
import morgan.support.Log;
import morgan.support.MimeTypeEnum;
import morgan.support.Utils;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractHttpMapping {
    private Map<String, Method> get_mapping = new HashMap<>();
    private Map<String, Method> post_mapping = new HashMap<>();

    public AbstractHttpMapping() {
        init();
    }

    public void init() {
        for (var m : this.getClass().getDeclaredMethods()) {
            var anno = m.getAnnotation(HttpPath.class);
            if (anno == null)
                continue;

            if (anno.method().equals("GET"))
                get_mapping.put(anno.path(), m);
            else if (anno.method().equals("POST"))
                post_mapping.put(anno.path(), m);
        }
    }

    public void handle(String path, String method, Object ... args) {
        Map<String, Method> mapping = null;
        if (method.equals("GET"))
            mapping = get_mapping;
        else if (method.equals("POST"))
            mapping = post_mapping;
        else
            mapping = new HashMap<>();

        var m = mapping.get(path);
        if (m == null) {
            returnsFile((ChannelHandlerContext)args[0], path.substring(1));
            return;
        }
            ConnStarter.connNode.anyWorker().schedule(100, () -> {
                try {
                    m.invoke(Factory.httpMappingInstance(), args);
                } catch (Exception e) {
                    Log.http.error("error invoking http method:{}", m.getName());
                    e.printStackTrace();
                }
            });
    }

    protected void returnsFile(ChannelHandlerContext ctx, String fileUri) {
        var bytes = Utils.readFileFromResource(fileUri);
        if (bytes == null) {
            bytes = Utils.readFile("./" + fileUri);
            if (bytes == null) {
                send(ctx, "", HttpResponseStatus.BAD_REQUEST);
                return;
            }
        }

        var s = fileUri.split("\\.");
        send(ctx, bytes, MimeTypeEnum.getContentType(s[s.length - 1]));
    }

    protected void send(ChannelHandlerContext ctx, String content) {
        send(ctx, content, HttpResponseStatus.OK, "text/plain; charset=UTF-8");
    }

    protected void send(ChannelHandlerContext ctx, String content, HttpResponseStatus status) {
        send(ctx, content, status, "text/plain; charset=UTF-8");
    }

    protected void send(ChannelHandlerContext ctx, String content, String contentType) {
        send(ctx, content, HttpResponseStatus.OK, contentType);
    }

    protected void send(ChannelHandlerContext ctx, String content, HttpResponseStatus status, String contentType) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    protected void send(ChannelHandlerContext ctx, byte[] content) {
        send(ctx, content, HttpResponseStatus.OK, "text/plain; charset=UTF-8");
    }

    protected void send(ChannelHandlerContext ctx, byte[] content, HttpResponseStatus status) {
        send(ctx, content, status, "text/plain; charset=UTF-8");
    }

    protected void send(ChannelHandlerContext ctx, byte[] content, String contentType) {
        send(ctx, content, HttpResponseStatus.OK, contentType);
    }

    protected void send(ChannelHandlerContext ctx, byte[] content, HttpResponseStatus status, String contentType) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(content));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
