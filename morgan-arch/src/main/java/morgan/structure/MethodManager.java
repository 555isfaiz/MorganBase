package morgan.structure;

import java.util.HashMap;
import java.util.Map;

public class MethodManager {

    private Worker _owner;

    private Map<String, Method> _methods = new HashMap<>();

    public MethodManager(Worker owner){
        _owner = owner;
    }

    public void registMethod(String name, Object f, Class<?>... args){
        Method m = new Method(name, f, args);
        _methods.put(name, m);
    }

    public Method getFunc(String name){
        return _methods.get(name);
    }

    public Map<String, Method> getMethods(){
        return _methods;
    }
}
