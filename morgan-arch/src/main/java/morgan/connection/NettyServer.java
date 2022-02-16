package morgan.connection;

import java.util.concurrent.CountDownLatch;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class NettyServer extends Thread {

    private CountDownLatch stopLatch = new CountDownLatch(1);

    public int port = 0;

    ChannelInitializer<SocketChannel> channelInitializer;

    public NettyServer(ChannelInitializer<SocketChannel> channelInitializer) {
        this.channelInitializer = channelInitializer;
    }

    @Override
    public void run() {
        if (port == 0)
            throw new UnsupportedOperationException("port unset");

        EventLoopGroup bossGroup = null;
        EventLoopGroup workerGroup = null;
        Class<? extends ServerChannel> channelClass = NioServerSocketChannel.class;
        int nThreads = Runtime.getRuntime().availableProcessors();
        if (System.getProperty("os.name").toLowerCase().contains("linux")){
            bossGroup = new EpollEventLoopGroup();
            workerGroup = new EpollEventLoopGroup(nThreads);
            channelClass = EpollServerSocketChannel.class;
        } else {
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup(nThreads);
        }
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(channelClass)
                    .option(ChannelOption.SO_BACKLOG, 10240)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(32 * 1024, 128 * 1024))
                    .childHandler(channelInitializer);
            Channel ch = b.bind(port).sync().channel();
            stopLatch.await();
            ch.closeFuture().sync();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void serverStop(){
        stopLatch.countDown();
    }
}
