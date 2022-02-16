package morgan.structure.serialize;

import morgan.messages.MessageBase;
import morgan.structure.Call;
import morgan.structure.Worker;
import morgan.support.Factory;
import morgan.support.Utils;

import java.io.IOException;
import java.util.*;

public class InputStream extends StreamBase{

    private int cursor = 0;

    public InputStream(byte[] bytes){
        setBuffer(bytes);
    }

    public void resetCursor(){
        cursor = 0;
    }

    public <T> T read(){
        T res = null;
        try {
            res = readIn();
        } catch (Exception e){
            e.printStackTrace();
        }
        return res;
    }

    @SuppressWarnings("unchecked")
    private <T> T readIn() throws IOException {
        Object result = null;
        byte firstByte = read(1)[0];
        int tag = resolveTag(firstByte);

        if (tag == TYPE_NULL){

        } else if (tag == TYPE_INT){

            int len = (firstByte & 0xF0) >> 4;
            result = (int)resolveNum(len);

        } else if (tag == TYPE_LONG){

            int len = (firstByte & 0xF0) >> 4;
            result = resolveNum(len);

        } else if (tag == TYPE_FLOAT){

            byte[] value = read(LENGTH_INT);
            result =  Float.intBitsToFloat(Utils.bytesToInt(value));

        } else if (tag == TYPE_DOUBLE){

            byte[] value = read(LENGTH_LONG);
            result =  Double.longBitsToDouble(Utils.bytesToLong(value));

        } else if (tag == TYPE_BOOL){

            result = (((firstByte & 0xF0) >> 4) & 1) == 1;

        } else if (tag == TYPE_BYTE){

            result = read(1)[0];

        } else if (tag == TYPE_STRING){

            int lenOflen = (firstByte & 0xF0) >> 4;
            int length = (int) resolveNum(lenOflen);

            byte[] str = read(length);
            result = new String(str);

        } else if (tag == TYPE_LIST){

            int lenOflen = (firstByte & 0xF0) >> 4;
            int length = (int) resolveNum(lenOflen);

            List<?> list = new ArrayList<>();
            for (int i = 0; i < length; i++){
                list.add(readIn());
            }
            result = list;

        } else if (tag == TYPE_SET){

            int lenOflen = (firstByte & 0xF0) >> 4;
            int length = (int) resolveNum(lenOflen);

            Set<?> list = new HashSet<>();
            for (int i = 0; i < length; i++){
                list.add(readIn());
            }
            result = list;

        } else if (tag == TYPE_MAP){

            int lenOflen = (firstByte & 0xF0) >> 4;
            int length = (int) resolveNum(lenOflen);
            Map<?, ?> map = new LinkedHashMap<>();
            for (int i = 0; i < length/2; i++){
                map.put(readIn(), readIn());
            }
            result = map;

        } else if (tag == TYPE_MESSAGE){

            int id = Utils.bytesToInt(read(LENGTH_INT));
            MessageBase m = Factory.messageMapInstance().getEmptyMessageById(id);
            m.readIn(this);
            result = m;

        } else if (tag == TYPE_DISTRCLASS){

            int id = Utils.bytesToInt(read(LENGTH_INT));
            Serializable s = Factory.distrClassInstance().getDistrClassById(id);
            s.readIn(this);
            result = s;

        } else if (tag == TYPE_CALL){

            Call call = new Call();
            call.readIn(this);
            result = call;

        } else if (tag == TYPE_ARRAY){

            int subTag = (firstByte & 0xFF) >> 4;
            if (subTag == TYPE_INT){

                int length = Utils.bytesToInt(read(LENGTH_INT));
                int[] array = new int[length];
                for (int i = 0; i < length; i++)
                    array[i] = readIn();
                result = array;

            } else if (subTag == TYPE_LONG){

                int length = Utils.bytesToInt(read(LENGTH_INT));
                long[] array = new long[length];
                for (int i = 0; i < length; i++)
                    array[i] = readIn();
                result = array;

            } else if (subTag == TYPE_FLOAT){

                int length = Utils.bytesToInt(read(LENGTH_INT));
                float[] array = new float[length];
                for (int i = 0; i < length; i++)
                    array[i] = readIn();
                result = array;

            } else if (subTag == TYPE_DOUBLE){

                int length = Utils.bytesToInt(read(LENGTH_INT));
                double[] array = new double[length];
                for (int i = 0; i < length; i++)
                    array[i] = readIn();
                result = array;

            } else if (subTag == TYPE_BOOL){

                int length = Utils.bytesToInt(read(LENGTH_INT));
                boolean[] array = new boolean[length];
                for (int i = 0; i < length; i++)
                    array[i] = readIn();
                result = array;

            } else if (subTag == TYPE_BYTE){

                int length = Utils.bytesToInt(read(LENGTH_INT));
                result = read(length);

            } else if (subTag == TYPE_OBJECT){

                int length = Utils.bytesToInt(read(LENGTH_INT));
                Object[] array = new Object[length];
                for (int i = 0; i < length; i++)
                    array[i] = readIn();
                result = array;

            }
        }

        return (T) result;
    }

    private int resolveTag(byte b){
        return b & 0xF;
    }

    private long resolveNum(int length){
        byte[] bytes = read(length);

        boolean isNegative = (bytes[0] & (1 << 7)) == 128;
        long num = 0L;
        for (int i = 0; i < length; i++){
            if (i == 0)
                num |= (long)(bytes[i] & 0x7F) << ((length - i - 1) * 8);
            else {
                num |= ((long) bytes[i] & 0xFF) << ((length - i - 1) * 8);
            }
        }
        return isNegative ? -num : num;
    }

    private byte[] read(int length){
        return read(cursor, length);
    }

    private byte[] read(int begin, int length){
        if (begin == actualLen)
            throw new UnsupportedOperationException("end of InputStream!");

        if ((begin + length) > actualLen){
            length = actualLen - begin;
        }

        byte[] toReturn = new byte[length];
        System.arraycopy(buffer, begin, toReturn, 0, length);
        if (begin == cursor)
            cursor += length;
        return toReturn;
    }
}
