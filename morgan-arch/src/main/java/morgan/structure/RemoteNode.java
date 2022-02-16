package morgan.structure;

import morgan.structure.serialize.OutputStream;
import morgan.support.Log;
import morgan.support.Time;
import morgan.support.Timmer;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

public class RemoteNode {
    private Node _owner;

    private String _name;

    private ZMQ.Context _context = ZMQ.context(1);
    private ZMQ.Socket _push = _context.socket(SocketType.PUSH);
    private long _nextPingTime;
    private long _lastPingRecvTime = 0;
    private OutputStream out_ = new OutputStream();

    private Timmer _syncTimer = new Timmer(Timmer.TYPE_PERIOD, 10 * Time.SEC, false);

    public boolean closed = false;

    public RemoteNode(Node owner, String name, String addr){
        _owner = owner;
        _name = name;
        _push.connect(addr);
        _nextPingTime = System.currentTimeMillis() + 3 * Time.SEC;
    }

    public void pulse(){
        long now = System.currentTimeMillis();
        if (_syncTimer.isTrigger()){
            pulseCollect();
        }

        if (now >= _nextPingTime) {
            sendPing();
        }

        if (now >= _lastPingRecvTime + 3 * Time.MIN){
            Log.remoteNode.error("lost contact from {} for 3 minutes, morgan.connection close", _name);
            connClose();
        }
        if (now >= _lastPingRecvTime + 30 * Time.SEC)
            Log.remoteNode.warn("lost contact from {} for 30 seconds", _name);
    }

    private void sendPing(){
        Call call = new Call();
        call.callType = Call.CALL_TYPE_PING;
        call.dest = _name;
        call.from = _owner.getName();

        //other fields are not needed

        sendCall(call);

        _nextPingTime = System.currentTimeMillis() + 3 * Time.SEC;
    }

    public void sendCall(Call call){
        try {
            out_.write(call);
            synchronized (this){
                _push.send(out_.getBuffer());
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            out_.reset();
        }
    }

    public void onPing(){
        _lastPingRecvTime = System.currentTimeMillis();
    }

    private void pulseCollect(){
        Call call = new Call();
        call.from = _owner.getName();
        call.dest = _name;
        call.callType = Call.CALL_TYPE_COLLECT;
        call.parameters = _owner.getLocalMethods().toArray();

        sendCall(call);
    }

    public void connClose(){
        _push.close();
        closed = true;
    }
}
