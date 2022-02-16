package morgan.structure.serialize;

import java.io.IOException;

public interface Serializable {
    void writeOut(OutputStream out) throws IOException;
    void readIn(InputStream in) throws IOException;
}
