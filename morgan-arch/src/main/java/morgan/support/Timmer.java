package morgan.support;

public class Timmer {
    public static final int TYPE_ONCE = 0;
    public static final int TYPE_PERIOD = 0;

    private int type;
    private long startTime;
    private long lastTrigger;
    private long interval;
    private boolean started;

    public Timmer(int type, long interval, boolean delayStart){
        this.type = type;
        this.interval = interval;
        this.started = !delayStart;
        if (!delayStart)
            startUp();
    }

    public void startUp(){
        startTime = System.currentTimeMillis();
        if (type == TYPE_PERIOD)
        {
            lastTrigger = startTime;
        }
        started = true;
    }

    public long nextElapse()
    {
        if (!started)
            return -1L;

        if (type == TYPE_ONCE)
        {
            return startTime + interval - System.currentTimeMillis();
        }
        else
        {
            return lastTrigger + interval - System.currentTimeMillis();
        }
    }

    public boolean isTrigger()
    {
        if (!started)
            return false;

        long now = System.currentTimeMillis();
        boolean flag = (lastTrigger + interval) <= now;

        if (flag && type == TYPE_PERIOD)
        {
            lastTrigger = now;
        }
        return flag;
    }
}
