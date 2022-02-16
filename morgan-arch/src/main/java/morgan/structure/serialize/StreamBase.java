package morgan.structure.serialize;

import java.util.Arrays;

public abstract class StreamBase {

    protected static final int TYPE_OBJECT         = 0;
    protected static final int TYPE_INT            = 1;      //int32
    protected static final int TYPE_LONG           = 2;      //int64
    protected static final int TYPE_FLOAT          = 3;
    protected static final int TYPE_DOUBLE         = 4;
    protected static final int TYPE_BOOL           = 5;
    protected static final int TYPE_BYTE           = 6;
    protected static final int TYPE_STRING         = 7;
    protected static final int TYPE_LIST           = 8;
    protected static final int TYPE_SET            = 9;
    protected static final int TYPE_MAP            = 10;
    protected static final int TYPE_MESSAGE        = 11;
    protected static final int TYPE_DISTRCLASS     = 12;
    protected static final int TYPE_NULL           = 13;
    protected static final int TYPE_ARRAY          = 14;
    protected static final int TYPE_CALL           = 15;

    protected static final int LENGTH_INT          = 4;
    protected static final int LENGTH_LONG         = 8;

    public static final int DEFAULT_BUFFER_SIZE    = 512;
    public static final int MAXIMUM_BUFFER_SIZE    = 50 * 1024;

    protected byte[] buffer;
    protected int actualLen;

    public byte[] getBuffer(){
        byte[] toReturn = new byte[actualLen];
        System.arraycopy(buffer, 0, toReturn, 0, actualLen);
        return toReturn;
    }

    public void setBuffer(byte[] buf){
        buffer = buf;
        actualLen = buf.length;
    }

    public int getBufferSize(){
        return actualLen;
    }

    public void setBufferSize(int size){
        if (size < 0)
            throw new IllegalArgumentException("size can't be negative: " + size);
        if (size > MAXIMUM_BUFFER_SIZE)
            throw new IllegalArgumentException("buffer overflowed! size:" + size);

        if (buffer == null){
            buffer = new byte[size];
            return;
        }

        if (size < getBufferSize()){
            throw new UnsupportedOperationException("can't set a smaller buffer, if you want to reset buffer, call reset()");
        } else if (size > getBufferSize()){
            buffer = Arrays.copyOf(buffer, size);
        }
    }

    public void reset(){
        buffer = new byte[DEFAULT_BUFFER_SIZE];
        actualLen = 0;
    }
}
