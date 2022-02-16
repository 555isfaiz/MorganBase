package morgan.connection;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;
import morgan.messages.MessageBase;
import morgan.structure.Node;
import morgan.structure.Worker;
import morgan.structure.serialize.OutputStream;
import morgan.support.Utils;
import morgan.support.functions.Function0;
import morgan.support.functions.Function1;

public abstract class AbstractConnection extends Worker {

	protected Channel _channel;
	protected int _connId;
	protected boolean _started;
	protected LinkedBlockingQueue<byte[]> _recv = new LinkedBlockingQueue<>();
	protected ConcurrentLinkedQueue<byte[]> _send = new ConcurrentLinkedQueue<>();
	protected OutputStream out = new OutputStream();

	public AbstractConnection(Node node, Channel channel, int connId) {
		super(node, "Connection-" + connId);

		_channel = channel;
		_connId = connId;
		_started = true;
	}

	public void pulseOverride(){
		if (!_started)
			return;
		pulseInput();
		pulseOutput();
	}

	private void pulseInput(){
		while (!_recv.isEmpty()){
			handleMsg(_recv.poll());
		}
	}

	private void pulseOutput(){
		boolean sent = false;
		while (_send != null){
			byte[] msg = _send.poll();
			if (msg == null)
				break;

			if (!_channel.isActive())
				return;
			if (!_channel.isWritable())
				return;

			var bytes = beforeWrite(msg);

//            Log.connection.info("msg sent, length:{}", bytes.length);
			_channel.write(bytes);
			sent = true;
//            Log.connection.info("msg sent! playerId:{}, connId:{}", _playerId, _connId);
		}

		if (sent)
			_channel.flush();
	}

	public void recv(byte[] buf){
		try {
			_recv.put(buf);
		} catch (InterruptedException e){
			e.printStackTrace();
		}
	}

	public void sendMsg(MessageBase msg){
		if (msg == null)
			return;
		out.reset();
		out.write(msg);
		sendMsgBytes(out.getBuffer());
	}

	public static void sendMsg_(int workerId, MessageBase msg) {
		CallWithStack0(workerId, msg);
	}

	public void sendMsgBytes(byte[] msg){
		if (msg == null || msg.length == 0)
			return;
		_send.add(msg);
	}

	public static void sendMsgBytes_(int workerId, byte[] msg) {
		CallWithStack0(workerId, (Object) msg);
	}

	public void closeConnection(){
		if (!_started)
			return;

		_started = false;

		onConnectionClosed();

		_channel.close();
		deleteMe();
	}

	public static void closeConnection_(int workerId) {
		CallWithStack0(workerId);
	}

	public int getId(){
		return _connId;
	}

	protected abstract void handleMsg(byte[] msg);

	public void onConnectionClosed() {
		onConnectionClosedOverride();
	}

	protected abstract void onConnectionClosedOverride();

	protected Object beforeWrite(byte[] msg) {
		//fill the first four bytes with the whole length of the buffer.
		var bytes = new byte[msg.length + 4];
		System.arraycopy(msg, 0, bytes, 4, msg.length);
		System.arraycopy(Utils.intToBytes(bytes.length), 0, bytes, 0, 4);
		return Unpooled.wrappedBuffer(bytes, 0, bytes.length);
	}

	@Override
	public void registMethods() {
		_rpcMethodManager.registMethod("sendMsgBytes", (Function1<byte[]>)this::sendMsgBytes, byte[].class);
		_rpcMethodManager.registMethod("sendMsg", (Function1<MessageBase>)this::sendMsg, MessageBase.class);
		_rpcMethodManager.registMethod("closeConnection", (Function0)this::closeConnection);
	}
}
