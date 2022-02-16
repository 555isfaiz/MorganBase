package morgan.structure;

import morgan.support.functions.*;

import java.io.IOException;

public class Method {
    private String name;
    private Object f;
    private Class<?>[] args;

    public Method(String name, Object func, Class<?>... args){
        this.name = name;
        f = func;
        this.args = args;
    }

    public String getName(){
        return name;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void apply(Object...para) {
        Object[] args = castBeforeApply(para);
        switch (args.length){
            case 0:
                ((Function0)f).apply();
                break;
            case 1:
                ((Function1)f).apply(args[0]);
                break;
            case 2:
                ((Function2)f).apply(args[0], args[1]);
                break;
            case 3:
                ((Function3)f).apply(args[0], args[1], args[2]);
                break;
            case 4:
                ((Function4)f).apply(args[0], args[1], args[2], args[3]);
                break;
            case 5:
                ((Function5)f).apply(args[0], args[1], args[2], args[3], args[4]);
                break;
            case 6:
                ((Function6)f).apply(args[0], args[1], args[2], args[3], args[4], args[5]);
                break;
            case 7:
                ((Function7)f).apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
                break;
            case 8:
                ((Function8)f).apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
                break;
            case 9:
                ((Function9)f).apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
                break;
            case 10:
                ((Function10)f).apply(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9]);
                break;
        }
    }

    private Object[] castBeforeApply(Object...args){
        Object[] r = new Object[args.length];
        for (int i = 0; i < args.length; i++){
            if (i < this.args.length) {
                if ((this.args[i] == Integer.class || this.args[i] == int.class) && args[i] instanceof Long) {
                    r[i] = ((Number) args[i]).intValue();
                    continue;
                } else if ((this.args[i] == Long.class || this.args[i] == long.class) && args[i] instanceof Integer) {
                    r[i] = ((Number) args[i]).longValue();
                    continue;
                }
            }
            r[i] = args[i];
        }
        return r;
    }
}
