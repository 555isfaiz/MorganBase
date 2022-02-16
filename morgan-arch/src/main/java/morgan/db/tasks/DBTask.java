package morgan.db.tasks;

import morgan.db.DBTable;
import morgan.db.Record;
import morgan.structure.Call;
import morgan.support.functions.Function2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public abstract class DBTask {

    public static int TASK_INSERT = 1;

    public static int TASK_REMOVE = 2;

    public static int TASK_UPDATE = 3;

    public static int TASK_QUERY = 4;

    public String tableName_ = null;

    public DBTable table_;

    public long cid_;

    public List<String> labels_ = new ArrayList<>();

    public List<Object> values_ = new ArrayList<>();

    public List<Integer> affectedIndexs_ = new ArrayList<>();

    public String sql_ = null;

    public int taskType_;

    public Call queryCall_;

    public Function2<List<Record>, Call> queryCallBack_;

    public abstract void beforeProcess(DBTable table);

    public abstract void process(Connection conn);

    public abstract DBTask merge(DBTask task);

    protected void prepare(PreparedStatement statement) throws SQLException {
        for (int i = 0; i < values_.size(); i++) {
            var type = values_.get(i).getClass();
            if (int.class.equals(type) || Integer.class.equals(type)) {
                statement.setInt(i + 1, (Integer) values_.get(i));
            }

            else if (long.class.equals(type) || Long.class.equals(type)) {
                statement.setLong(i + 1, (Long) values_.get(i));
            }

            else if (String.class.equals(type)) {
                statement.setString(i + 1, (String) values_.get(i));
            }

            else if (Float.class.equals(type) || float.class.equals(type)) {
                statement.setFloat(i + 1, (Float) values_.get(i));
            }

            else if (Double.class.equals(type) || double.class.equals(type)) {
                statement.setDouble(i + 1, (Double) values_.get(i));
            }

            else if (byte[].class.equals(type)) {
                statement.setBytes(i + 1, (byte[]) values_.get(i));
            }

            else if (boolean.class.equals(type) || Boolean.class.equals(type)) {
                statement.setBoolean(i + 1, (Boolean) values_.get(i));
            }
        }
    }
}
