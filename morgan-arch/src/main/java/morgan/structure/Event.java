package morgan.structure;

public class Event {

    public String key;
    public Object[] parameters;

    public Event(String key, Object ... args) {
        this.key = key;
        parameters = args;
    }
}
