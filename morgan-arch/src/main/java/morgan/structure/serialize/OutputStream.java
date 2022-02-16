package morgan.structure.serialize;

import morgan.messages.MessageBase;
import morgan.structure.Call;
import morgan.structure.Worker;
import morgan.support.Factory;
import morgan.support.Utils;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OutputStream extends StreamBase{

    public OutputStream(){
        this(DEFAULT_BUFFER_SIZE);
    }

    public OutputStream(int size){
        setBufferSize(size);
    }

    private byte[] resolveNumber(long i){
        if (i == 0)
            return new byte[1];
        long absI = Math.abs(i);
        long div = ~Long.MAX_VALUE;
        int index = 0;

        while ((absI & div) == 0){
            div = div >> 1;
            if (index == 0)
                div = div ^ (~Long.MAX_VALUE);
            index++;
        }

        int len = 8 - (index % 8 == 0 ? (index / 8 - 1) : (index / 8));
        byte[] res = new byte[len];
        for (int j = 0; j < len; j++){
            res[j] = (byte) (absI >> (8 * (len - j - 1)));
        }
        if (i < 0){
            res[0] |= (byte)128;
        }
        return res;
    }

    public void write(Object obj){
        try {
            writeOut(obj);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void writeOut(Object obj) throws Exception{
        if (obj == null){
            write(new byte[]{(byte)TYPE_NULL}); return;
        }

        Class<?> clz = obj.getClass();
        if (clz == int.class || clz == Integer.class){

            byte[] bytes = resolveNumber((int)obj);
            writeCompressedTL(TYPE_INT, bytes.length);
            write(bytes);

        } else if (clz == long.class || clz == Long.class){

            byte[] bytes = resolveNumber((long)obj);
            writeCompressedTL(TYPE_LONG, bytes.length);
            write(bytes);

        } else if (clz == byte.class || clz == Byte.class){

            write(new byte[]{(byte) TYPE_BYTE, (byte)obj});

        } else if (clz == boolean.class || clz == Boolean.class){

            write(new byte[]{(byte) ((((boolean)obj ? 1 : 0) << 4) | TYPE_BOOL)});

        } else if (clz == float.class || clz == Float.class){

            write(new byte[]{(byte) TYPE_FLOAT});
            write(Utils.intToBytes(Float.floatToIntBits((float)obj)));

        } else if (clz == double.class || clz == Double.class){

            write(new byte[]{(byte) TYPE_DOUBLE});
            write(Utils.longToBytes(Double.doubleToLongBits((double)obj)));

        } else if (clz == String.class){

            byte[] bytes = ((String)obj).getBytes(StandardCharsets.UTF_8);
            byte[] bNum = resolveNumber(bytes.length);
            writeTandVL(TYPE_STRING, bNum.length, bNum);
            write(bytes);

        } else if (obj instanceof Collection){

            Collection<?> l = (Collection<?>) obj;
            byte[] bNum = resolveNumber(l.size());

            if (obj instanceof List)
                writeTandVL(TYPE_LIST, bNum.length, bNum);
            else if (obj instanceof Set)
                writeTandVL(TYPE_SET, bNum.length, bNum);

            for (var v : l) {
                writeOut(v);
            }

        } else if (obj instanceof Map){

            Map<?, ?> m = (Map<?, ?>)obj;
            byte[] bNum = resolveNumber(m.size() * 2);
            writeTandVL(TYPE_MAP, bNum.length, bNum);
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                writeOut(entry.getKey());
                writeOut(entry.getValue());
            }

        } else if (clz == int[].class){

            int[] array = (int[])obj;
            write(new byte[]{(byte)((TYPE_INT << 4) | TYPE_ARRAY)});
            write(Utils.intToBytes(array.length));
            for (int i = 0; i < array.length; i++)
                writeOut(array[i]);

        } else if (clz == long[].class){

            long[] array = (long[])obj;
            write(new byte[]{(byte)((TYPE_LONG << 4) | TYPE_ARRAY)});
            write(Utils.intToBytes(array.length));
            for (int i = 0; i < array.length; i++)
                writeOut(array[i]);

        } else if (clz == float[].class){

            float[] array = (float[])obj;
            write(new byte[]{(byte)((TYPE_FLOAT << 4) | TYPE_ARRAY)});
            write(Utils.intToBytes(array.length));
            for (int i = 0; i < array.length; i++)
                writeOut(array[i]);

        } else if (clz == double[].class){

            double[] array = (double[])obj;
            write(new byte[]{(byte)((TYPE_DOUBLE << 4) | TYPE_ARRAY)});
            write(Utils.intToBytes(array.length));
            for (int i = 0; i < array.length; i++)
                writeOut(array[i]);

        } else if (clz == boolean[].class){

            boolean[] array = (boolean[])obj;
            write(new byte[]{(byte)((TYPE_BOOL << 4) | TYPE_ARRAY)});
            write(Utils.intToBytes(array.length));
            for (int i = 0; i < array.length; i++)
                writeOut(array[i]);

        } else if (clz == byte[].class){

            byte[] array = (byte[])obj;
            write(new byte[]{(byte)((TYPE_BYTE << 4) | TYPE_ARRAY)});
            write(Utils.intToBytes(array.length));
            write(array);

        } else if (clz == Object[].class){

            Object[] array = (Object[])obj;
            write(new byte[]{(byte)((TYPE_OBJECT << 4) | TYPE_ARRAY)});
            write(Utils.intToBytes(array.length));
            for (int i = 0; i < array.length; i++)
                writeOut(array[i]);

        } else if (obj instanceof Call){

            write(new byte[]{(byte)TYPE_CALL});
            ((Call)obj).writeOut(this);

        } else if (obj instanceof Serializable){

            writeTandId(TYPE_DISTRCLASS, Factory.distrClassInstance().getDistrClassId((Serializable) obj));
            ((Serializable)obj).writeOut(this);

        } else if (obj instanceof MessageBase){

            writeTandId(TYPE_MESSAGE, Factory.messageMapInstance().getMessageId((MessageBase)obj));
            ((MessageBase)obj).writeOut(this);

        } else {
            throw new UnsupportedOperationException("Unserializable class: " + clz.getName());
        }
    }

    private void writeCompressedTL(int tag, int length){
        write(new byte[]{(byte) ((length << 4) | tag)});
    }

    /*
    * Message, DistrClass
    */
    private void writeTandId(int tag, int id){
        byte[] bytes = new byte[5];
        bytes[0] = (byte)tag;
        byte[] idB = Utils.intToBytes(id);

        for (int i = 1; i < 5; i++)
            bytes[i] = idB[i - 1];

        write(bytes);
    }

    /*
    * list, map, string
    * */
    private void writeTandVL(int tag, int length, byte[] lenBytes){
        writeCompressedTL(tag, length);
        write(lenBytes);
    }

    private void write(byte[] bytes){
        write(actualLen, bytes);
    }

    private void write(int begin, byte[] bytes){
        if (begin + bytes.length >= buffer.length){
            setBufferSize(buffer.length << 1);
        }

        System.arraycopy(bytes, 0, buffer, begin, bytes.length);

        if ((begin + bytes.length) > actualLen)
            actualLen = begin + bytes.length;
    }
}
