package morgan.db;

import morgan.db.tasks.DBTask;
import morgan.structure.Call;
import morgan.structure.Node;
import morgan.structure.Worker;
import morgan.support.Config;
import morgan.support.Factory;
import morgan.support.Log;
import morgan.support.functions.Function1;
import morgan.support.functions.Function2;
import morgan.support.functions.Function3;
import morgan.support.functions.Function4;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

public class DBWorker extends Worker {
    private Map<String, DBTable> tables_ = new HashMap<>();
    private Map<String, LinkedBlockingQueue<DBTask>> tasks_ = new HashMap<>();
    private Connection dbconn_;
    private boolean dbInited = false;

    public DBWorker(Node node, String name) {
        super(node, name);
    }

    @Override
    public void pulseOverride() {
        // process tasks
        for (var q : tasks_.values()) {
            DBTask t = q.poll();
            if (t == null) {
                continue;
            }
            t.process(dbconn_);
//            Log.db.debug("task executed! task:{}, sql:{}", t.getClass().getName(), t.sql_);
        }

        for (var table : tables_.values()) {
            table.pulse();
        }
    }

    public void init(List<String> tables) {
        if (dbInited)
            return;

        try {
            dbconn_ = DriverManager.getConnection(Factory.configInstance().DB_URL, Factory.configInstance().DB_USER, Factory.configInstance().DB_PASSWORD);
            DatabaseMetaData meta = dbconn_.getMetaData();
            Statement stat = dbconn_.createStatement();
            for (var table : tables) {
                ResultSet indexInfo = meta.getIndexInfo(null, null, table, false, false);
                ResultSet columns = stat.executeQuery("show full columns from " + table + ";");
                loadTable(table, indexInfo, columns);
            }
            dbInited = true;
        } catch (Exception e) {
            Log.db.error("init db worker failed, e:{}", e.getMessage());
            e.printStackTrace();
        }
        Log.db.info("{} inited! tables:{}", _name, tables_.keySet());
    }

    public void init(Map<String, DBTable> tables) {
		if (dbInited)
			return;

		try {
			dbconn_ = DriverManager.getConnection(Factory.configInstance().DB_URL, Factory.configInstance().DB_USER, Factory.configInstance().DB_PASSWORD);
			tables_ = tables;
			dbInited = true;
		} catch (Exception e) {
			Log.db.error("init db worker failed, e:{}", e.getMessage());
			e.printStackTrace();
		}
	}

    public void addTask(String table, DBTask task) {
        if (task.taskType_ == DBTask.TASK_QUERY) {
            task.queryCallBack_ = this::returnQuery;
        }

        if (tasks_.containsKey(table))
            tasks_.get(table).add(task);
        else {
            var q = new LinkedBlockingQueue<DBTask>();
            q.add(task);
            tasks_.put(table, q);
        }
    }

    private void loadTable(String table, ResultSet tableInfo, ResultSet columns) {
        if (tables_.containsKey(table))
            return;

        DBTable t = new DBTable(this, table, tableInfo, columns);

        tables_.put(table, t);
    }

    public void insert(String table, Map<String, Object> values) {
        var t = tables_.get(table);
        if (t == null)
            return;

        if (values == null || values.size() == 0)
            return;

        returns("code", t.insert(values));
    }

    public static void insert_(int workerId, String table, Map<String, Object> values) {
    	CallWithStack0(workerId, table, values);
	}

    public void selectById(String table, long cid) {
        var t = tables_.get(table);
        if (t == null)
            return;

        t.query(getCurrentCall(), cid);
    }

    public static void selectById_(int workerId, String table, long cid) {
    	CallWithStack0(workerId, table, cid);
	}

    public void selectByBinds(String table, String label, Object value) {
        var t = tables_.get(table);
        if (t == null)
            return;

        t.query(getCurrentCall(), label, value);
    }

    public static void selectByBinds_(int workerId, String table, String label, Object value) {
    	CallWithStack0(workerId, table, label, value);
	}

	public void selectAll(String table) {
        var t = tables_.get(table);
        if (t == null)
            return;

        t.queryAll(getCurrentCall());
    }

    public static void selectAll_(int workerId, String table) {
        CallWithStack0(workerId, table);
    }

    public void update(String table, long cid, List<String> labels, List<Object> values) {
        var t = tables_.get(table);
        if (t == null)
            return;

        t.update(cid, labels, values);
    }

    public static void update_(int workerId, String table, long cid, List<String> labels, List<Object> values) {
    	CallWithStack0(workerId, table, cid, labels, values);
	}

    public void remove(String table, long cid) {
        var t = tables_.get(table);
        if (t == null)
            return;

        t.remove(cid);
    }

    public static void remove_(int workerId, String table, long cid) {
    	CallWithStack0(workerId, table, cid);
	}

    private void returnQuery(List<Record> resultSet, Call call) {
        returns(call, "result", resultSet);
    }

    public void execRawStatement(String sql) {
        try{
            //need a WAF!!!!!!
            Statement stat = dbconn_.createStatement();
            if (sql.startsWith("SELECT") || sql.startsWith("select")) {
                ResultSet rs = stat.executeQuery(sql);
                List<Record> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new Record(rs));
                }
                returnQuery(result, getCurrentCall());
            } else {
                stat.execute(sql);
            }
        } catch (Exception e) {
            Log.db.error("DB Exception when executing: {} \n {}", sql, e);
            e.printStackTrace();
        }
    }

    public static void execRawStatement_(int workerId, String sql) {
    	CallWithStack0(workerId, sql);
	}

	public void free(String table, long cid) {
		var t = tables_.get(table);
		if (t == null)
			return;

		t.free(cid);
	}

	public static void free_(int workerId, String table, long cid) {
    	CallWithStack0(workerId, table, cid);
	}

    @Override
    public void registMethods() {
        _rpcMethodManager.registMethod("insert", (Function2<String, Map<String, Object>>)this::insert, String.class, Map.class);
        _rpcMethodManager.registMethod("selectById", (Function2<String, Long>)this::selectById, String.class, Long.class);
        _rpcMethodManager.registMethod("selectByBinds", (Function3<String, String, Object>)this::selectByBinds, String.class, String.class, Object.class);
        _rpcMethodManager.registMethod("selectAll", (Function1<String>)this::selectAll, String.class);
        _rpcMethodManager.registMethod("update", (Function4<String, Long, List<String>, List<Object>>)this::update, String.class, Long.class, List.class, List.class);
        _rpcMethodManager.registMethod("remove", (Function2<String, Long>)this::remove, String.class, Long.class);
        _rpcMethodManager.registMethod("execRawStatement", (Function1<String>)this::execRawStatement, String.class);
        _rpcMethodManager.registMethod("free", (Function2<String, Long>)this::free, String.class, Long.class);
    }
}
