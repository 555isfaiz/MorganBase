package morgan.db.tasks;

import morgan.db.DBItem;
import morgan.db.DBTable;
import morgan.db.Record;
import morgan.support.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DBTaskQueryWithBinds extends DBTask {

    public DBTaskQueryWithBinds() {
        taskType_ = TASK_QUERY;
    }

    @Override
    public void beforeProcess(DBTable table) {
    	this.table_ = table;
        sql_ = "SELECT * FROM " + tableName_ + " WHERE ";
        sql_ += labels_.get(0) + " = ?;";
    }

    @Override
    public void process(Connection conn) {
        try {
            PreparedStatement stmt = conn.prepareStatement(sql_);
            prepare(stmt);
            ResultSet result = stmt.executeQuery();
            List<Record> r = new ArrayList<>();
            while (result.next()) {
            	var record = new Record(result);
            	record.table = tableName_;
                r.add(record);
				var item = new DBItem(table_, record);
				table_.addItem(item);
            }
            queryCallBack_.apply(r, queryCall_);
        } catch (Exception e) {
            Log.db.error("SELECT error, sql:{} , cid:{}", sql_, cid_);
            e.printStackTrace();
        }
    }

    @Override
    public DBTask merge(DBTask task) {
        throw new UnsupportedOperationException("Task QueryWithBinds is unmergeable");
    }
}
