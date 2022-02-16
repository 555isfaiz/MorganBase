package morgan.connection;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class HttpChannelInitializer  extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline()
                .addLast(new HttpServerCodec())
                .addLast(new HttpObjectAggregator(50*1024*1024))
                .addLast(new WebSocketServerProtocolHandler("/firearmswbs"))
                .addLast(new HttpServerHandler());
    }
}
