package morgan.connection;

import io.netty.channel.ChannelInboundHandlerAdapter;
import morgan.structure.Node;
import morgan.support.Config;
import morgan.support.Factory;

public class ConnStarter {
    public static Node connNode;
    public static NettyServer nettyThread;

    public static void startUp(Node node){
        connNode = node;
        nettyThread = new NettyServer(new TCPChannelInitializer());
        nettyThread.port = Factory.configInstance().TCP_PORT;
        nettyThread.start();
    }

    public static void httpStartUp() {
        nettyThread = new NettyServer(new HttpChannelInitializer());
        nettyThread.port = Factory.configInstance().HTTP_PORT;
        nettyThread.start();
    }

    public static void stopServer(){
        nettyThread.serverStop();
        nettyThread = null;
    }
}
