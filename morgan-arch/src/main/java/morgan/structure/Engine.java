package morgan.structure;

import morgan.support.functions.Function0;

public class Engine extends Thread {

    public static final int DEFAULT_SLEEP_TIME = 5;

    private Function0 start;
    private Function0 runOnce;
    private Function0 end;

    private boolean isStop = true;
    private int sleepTime = DEFAULT_SLEEP_TIME;

    private String name;

    public Engine(String name){
        this.name = name;
        setName(name);
    }

    public Engine(Function0 start, Function0 runOnce, Function0 end, String name){
        this(name);
        if (start == null && runOnce == null && end == null)
            return;
        this.start = start;
        this.runOnce = runOnce;
        this.end = end;
    }

    public void setStartFunc(Function0 start){
        this.start = start;
    }

    public void setRunOnceFunc(Function0 runOnce){
        this.runOnce = runOnce;
    }

    public void setEndFunc(Function0 end){
        this.end = end;
    }

    public boolean isActive(){
        return !isStop;
    }

    public void stopEngine(){
        isStop = true;
    }

    public void setSleepTime(int sleepTime){
        this.sleepTime = Math.max(sleepTime, DEFAULT_SLEEP_TIME);
    }

    public void start(){
        isStop = false;
        super.start();
    }

    public void run(){
        try {
            if (start != null)
                start.apply();

            while ((!isStop) && (runOnce != null)){
                runOnce.apply();
                Thread.sleep(sleepTime);
            }

            if (end != null)
                end.apply();
            isStop = true;
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public String toString(){
        return name + " isStop:" + isStop;
    }
}
