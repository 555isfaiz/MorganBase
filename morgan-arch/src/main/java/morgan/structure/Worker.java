package morgan.structure;

import morgan.support.Log;
import morgan.support.Time;
import morgan.support.functions.Function0;
import morgan.support.functions.Function1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class Worker {

    private static ThreadLocal<Worker> _cur_Worker = new ThreadLocal<>();

    protected Node _node;

    protected String _name;

    protected MethodManager _rpcMethodManager;

    private ConcurrentLinkedQueue<Call> _call_queue = new ConcurrentLinkedQueue<>();

    private ConcurrentHashMap<Integer, RPCListen> _result_listen_queue = new ConcurrentHashMap<>();

    private ConcurrentHashMap<Long, Function0> scheduler_ = new ConcurrentHashMap<>();

    private static long pulseTick_ = 0L;

    private int frequency_ = 50;

    private long nextExecute_ = 0L;

    /*call sent in current pulse*/
    private int _call_sent_id;

    private boolean isStandAlone_ = false;

    private boolean inited = false;

    private ConcurrentLinkedQueue<Event> events_ = new ConcurrentLinkedQueue<>();

    protected MethodManager eventsMethod_;

    private Worker(){}

    public Worker(Node node, String name){
        this._name = name;
        this._node = node;
        this._rpcMethodManager = new MethodManager(this);
        this.eventsMethod_ = new MethodManager(this);
        registMethods();
        registEventMethods();
    }

    public void init() {
        initOverride();
    }

    public void onStart(){
        _cur_Worker.set(this);
        if (!inited) {
            inited = true;
            init();
        }
    }

    public void pulse(){
        pulseTick_ = System.currentTimeMillis();
        pulseScheduler();
        handleCall();
        handleEvent();
        pulseOverride();
        clearTimeOutListen();
    }

    public void onEnd(){
        _cur_Worker.set(null);
    }

    public void pulseOverride() {}

    public void initOverride() {}

    public void addCallQueue(Call call){
        _call_queue.add(call);
    }

    public void setWorkerFrequency(int frequency) { frequency_ = frequency; }

    private void pulseScheduler() {
        for (var e : scheduler_.entrySet()) {
            if (e.getKey() > System.currentTimeMillis()) {
                continue;
            }

            try {
                var f = scheduler_.remove(e.getKey());
                f.apply();
                break;
            } catch (Exception ex) {
                Log.worker.error("error during scheduling, e:{}", ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void handleCall(){
        int size = _call_queue.size();
        for (int i = 0; i < size; i++) {
            Call c = _call_queue.peek();
            if (c == null){
                break;
            }

            if (c.callType == Call.CALL_TYPE_RPC)
                processRPC(c.method.split("&")[1], c.parameters);
            else if (c.callType == Call.CALL_TYPE_RPC_RETURN)
                onRPCReturn(c.id, c.flag == Call.CALL_FLAG_ERROR, c.parameters);

            _call_queue.poll();
        }
    }

    public void handleEvent() {
        int size = events_.size();
        for (int i = 0; i < size; i++) {
            var e = events_.peek();
            if (e == null){
                break;
            }

            var func = eventsMethod_.getFunc(e.key);
            if (func != null)
                func.apply(e.parameters);

            events_.poll();
        }
    }

    private void clearTimeOutListen(){
        for (var e : _result_listen_queue.entrySet()){
            if (e.getValue().timeOut <= System.currentTimeMillis()) {
                _result_listen_queue.remove(e.getKey());
                Log.worker.error("listen time out, id:{}", e.getKey());
            }
            break;
        }
    }

    private void processRPC(String methodName, Object[] args){
        Method m = _rpcMethodManager.getFunc(methodName);
        if (m == null){
            return;
        }
        try {
            m.apply(args);
        } catch (Exception e){
            Log.worker.error("Error handling call! method name:{}", methodName, e);
        }
    }

    private void onRPCReturn(int id, boolean isError, Object[] val){
        RPCListen rl = _result_listen_queue.get(id);
        if (rl == null)
            return;
        Result result = rl.createResult(isError, val);
        try {
            rl.callBack.apply(result);
        } catch (Exception e){
            Log.worker.error("error on handle listen result, isError:{}", isError);
            e.printStackTrace();
        } finally {
            _result_listen_queue.remove(id);
        }
    }

    private void sendCall(Call call){
        _node.sendCall(call);
    }

    protected void CallWithNode(String node, String methodName, String workerName, Object... parameters){
        Call call = new Call();
        call.callType = Call.CALL_TYPE_RPC;
        call.method = workerName + "&" + methodName;
        call.from = _node.getName();
        call.caller = _name;
        call.parameters = parameters;
        call.dest = node;
        call.id = call.hashCode();

        _call_sent_id = call.id;
        sendCall(call);
    }

    public void Call(String methodName, String workerName, Object... parameters){
        Call call = new Call();
        call.callType = Call.CALL_TYPE_RPC;
        call.method = workerName + "&" + methodName;
        call.from = _node.getName();
        call.caller = _name;
        call.parameters = parameters;
        call.id = call.hashCode();
        call.dest = _node.getReceiver(call.method);

        _call_sent_id = call.id;
        sendCall(call);
    }

    protected void returns(Call call, Object...values){
        returnsCall(call, Call.CALL_FLAG_NORMAL, values);
    }

    protected void returns(Object...values){
        returns(getCurrentCall(), values);
    }

    private void returnsCall(Call call, int flag, Object...values){
        Call ret = new Call();
        ret.from = _node.getName();
        ret.dest = call.from;
        ret.callType = Call.CALL_TYPE_RPC_RETURN;
        ret.parameters = values;
        ret.method = call.caller;
        ret.flag = flag;
        ret.id = call.id;

        sendCall(ret);
    }

    public void Listen(Function1<Result> f, Object...contexts){
        RPCListen rl = new RPCListen();
        rl.callBack = f;
        rl.contexts = contexts;

        _result_listen_queue.put(_call_sent_id, rl);
    }

    public static void Listen_(Function1<Result> f, Object...contexts) {
        var current = getCurrentWorker();
        if (current == null) {
            Log.node.error("call not in worker!");
            return;
        }

        current.Listen(f, contexts);
    }

    protected Call getCurrentCall(){
        return _call_queue.peek();
    }

    protected void deleteMe() {
        _node.removeWorker(this);
    }

    public void schedule(long delay, Function0 f) {
        long time = System.currentTimeMillis() + delay;
        scheduler_.put(time, f);
    }

    public String getName(){
        return _name;
    }

    public Node getNode(){
        return _node;
    }

    public boolean isStandAlone() {
        return isStandAlone_;
    }

    public void setIsStandAlone(boolean isStandAlone) {
        isStandAlone_ = isStandAlone;
    }

    public List<String> getMethods(){
        Map<String, Method> map = _rpcMethodManager.getMethods();
        List<String> list = new ArrayList<>();
        for (var name : map.keySet()){
            list.add(_name + "&" + name);
        }
        return list;
    }

    public boolean dispatchable() {
        long now = System.currentTimeMillis();
        var flag = nextExecute_ == 0L || now >= nextExecute_;
        nextExecute_ = flag ? now + (long)(((float) 1 / frequency_) * Time.SEC) : nextExecute_;
        return flag;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Worker> T getCurrentWorker(){
        return (T) _cur_Worker.get();
    }

    public static void CallWithStack(Object... args) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StackTraceElement caller = null;
        for (int i = 0; i < stack.length; i++) {
            if (stack[i].getMethodName().equals("CallWithStack")) {
                caller = stack[i + 1];
                break;
            }
        }
        if (caller == null) {
            Log.node.error("wrong caller!");
            return;
        }
        var tmp = caller.getClassName().split("\\.");
        String className = tmp[tmp.length - 1].replaceAll("Abstract", "");
        var t = caller.getMethodName();
        String method = t.substring(0, t.length() - 1);
        StaticCall(className, method, args);
    }

    public static void CallWithStack0(int workerId, Object... args) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StackTraceElement caller = null;
        for (int i = 0; i < stack.length; i++) {
            if (stack[i].getMethodName().equals("CallWithStack0")) {
                caller = stack[i + 1];
                break;
            }
        }
        if (caller == null) {
            Log.node.error("wrong caller!");
            return;
        }
        var tmp = caller.getClassName().split("\\.");
        String className = workerId == -1 ? tmp[tmp.length - 1] : tmp[tmp.length - 1] + "-" + workerId;
        var t = caller.getMethodName();
        String method = t.substring(0, t.length() - 1);
        StaticCall(className.replaceAll("Abstract", ""), method, args);
    }

    private static void StaticCall(String worker, String method, Object... args) {
        var current = getCurrentWorker();
        if (current == null) {
            Log.node.error("call not in worker! calling:{}, {}", worker, method);
            return;
        }
        current.Call(method, worker, args);
    }

    public void postEvent(String event, Object...args) {
        var e = new Event(event, args);
        _node.postEvent(e);
    }

    public void addEvent(Event event) {
        events_.add(event);
    }

    public static long getTime() { return pulseTick_; }

    public abstract void registMethods();

    public void registEventMethods() {}
}
