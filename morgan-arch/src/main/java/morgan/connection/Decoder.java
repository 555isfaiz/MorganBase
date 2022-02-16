package morgan.connection;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class Decoder extends LengthFieldBasedFrameDecoder {

    public Decoder() {
        super(Integer.MAX_VALUE, 0, 4, 0, 0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
        ByteBuf buffs = (ByteBuf)super.decode(ctx, buf);
        if (buffs == null)
            return null;

        byte[] decoded = new byte[buffs.readableBytes()];
        buffs.readBytes(decoded);
        buffs.release();
        return decoded;
    }
}
