package morgan.structure;

import morgan.structure.serialize.InputStream;
import morgan.structure.serialize.OutputStream;
import morgan.structure.serialize.Serializable;

import java.io.IOException;

public class Call implements Serializable {

    public static final int CALL_TYPE_RPC                   = 1;
    public static final int CALL_TYPE_RPC_RETURN            = 2;
    public static final int CALL_TYPE_FORM_CONN             = 3;
    public static final int CALL_TYPE_OFFLINE               = 4;
    public static final int CALL_TYPE_PING                  = 5;
    public static final int CALL_TYPE_COLLECT               = 6;

    public static final int CALL_FLAG_NORMAL                = 0;
    public static final int CALL_FLAG_ERROR                 = 1;
    public static final int CALL_FLAG_TIMEOUT               = 2;

    public int callType;
    public int id;
    public String from;
    public String dest;
    public String caller;
    public String method;
    public Object[] parameters;
    public int flag;

    @Override
    public void writeOut(OutputStream out) throws IOException {
        out.write(callType);
        out.write(id);
        out.write(from);
        out.write(dest);
        out.write(caller);
        out.write(method);
        out.write(parameters);
        out.write(flag);
    }

    @Override
    public void readIn(InputStream in) throws IOException {
        callType = in.read();
        id = in.read();
        from = in.read();
        dest = in.read();
        caller = in.read();
        method = in.read();
        parameters = in.read();
        flag = in.read();
    }
}
