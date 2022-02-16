package morgan.structure;

import morgan.support.Time;
import morgan.support.functions.Function1;

public class RPCListen {

    public Function1<Result> callBack;
    public Object[] contexts;
    public long timeOut;

    public RPCListen(){
        timeOut = System.currentTimeMillis() + 10 * Time.SEC;
    }

    public Result createResult(boolean isError, Object...results){
        Result r = new Result();
        r.setContexts(contexts);
        r.setResults(results);
        r.setError(isError);
        return r;
    }

}
