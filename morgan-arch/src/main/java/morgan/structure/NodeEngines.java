package morgan.structure;

import morgan.support.Log;
import morgan.support.functions.Function0;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NodeEngines {
    public static final int MINIMUM_THREAD_NUMBER = 5;
    public static final int WARNING_THREAD_NUMBER = 30;

    private final ArrayList<Engine> engines = new ArrayList<>();

    private final Map<String, Integer> dispatchedWorker_ = new HashMap<>();

    public NodeEngines(int threadNum){
        for (int i = 0; i < Math.max(threadNum, MINIMUM_THREAD_NUMBER); i++)
            addEngine();
    }

    public void setEngineNum(int num){
        int size = engines.size();
        if (num > size){
            for (int i = 0; i < (num - size); i++)
                addEngine();
        } else if (num < size){
            for (int i = 0; i < (size - num); i++){
                for (var e : engines){
                    if (e.isActive())
                        continue;
                    engines.remove(e);
                    break;
                }
            }
        }
    }

    public void addEngine(){
        addEngine(new Engine("Engine-" + engines.size()));
    }

    public void addEngine(Engine e){
        if (e == null)
            return;
        engines.add(e);
        if (engines.size() >= WARNING_THREAD_NUMBER)
            Log.node.warn("engines too many, size:{}", engines.size());
    }

    public void addEngineAndStart(Function0 start, Function0 runOnce, Function0 end){
        Engine e = new Engine(start, runOnce, end, "Engine-" + engines.size());
        addEngine(e);
        e.start();
    }

    public void bindAndRun(Function0 start, Function0 runOnce, Function0 end) {
        bindAndRun(start, runOnce, end, "");
    }

    public void bindAndRun(Function0 start, Function0 runOnce, Function0 end, String worker){
        Engine idle = null;
        int index = 0;
        for (int i = 0; i < engines.size(); i++){
            var e = engines.get(i);
            if (!e.isActive()){
                idle = e;
                index = i;
                break;
            }
        }

        if (idle != null){
            idle.setStartFunc(start);
            idle.setRunOnceFunc(runOnce);
            idle.setEndFunc(end);
        } else {
            idle = new Engine(start, runOnce, end, "Engine-" + engines.size());
            addEngine(idle);
            index = engines.size();
        }

        idle.start();
        if (!worker.isBlank())
            dispatchedWorker_.put(worker, index);
    }

    public void stopAll(){
        for (var e : engines){
            synchronized (e){
                e.stopEngine();
            }
        }
    }

    public void stopStandAlone(String worker) {
        var index = dispatchedWorker_.get(worker);
        if (index == null)
            return;

        engines.get(index).stopEngine();
    }
}
