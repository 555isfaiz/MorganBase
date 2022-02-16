package morgan.messages;

import morgan.structure.serialize.InputStream;
import morgan.structure.serialize.OutputStream;

import java.io.IOException;

public abstract class MessageBase {

    public int msgId = 0;

    public abstract void writeOut(OutputStream out) throws IOException;
    public abstract void readIn(InputStream in) throws IOException;

}
