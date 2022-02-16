package morgan.db.tasks;

import morgan.db.DBTable;
import morgan.db.Record;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

public class DBTaskQueryWithId extends DBTask {

    public Record record = null;

    public DBTaskQueryWithId() {
        taskType_ = TASK_QUERY;
    }

    @Override
    public void beforeProcess(DBTable table) {
		this.table_ = table;
	}

    @Override
    public void process(Connection conn) {
        List<Record> r = new ArrayList<>();
        r.add(record);
        queryCallBack_.apply(r, queryCall_);
    }

    @Override
    public DBTask merge(DBTask task) {
        throw new UnsupportedOperationException("unmergeable");
    }
}
