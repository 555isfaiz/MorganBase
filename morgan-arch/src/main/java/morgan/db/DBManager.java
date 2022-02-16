package morgan.db;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import morgan.structure.Node;
import morgan.support.Factory;
import morgan.support.Log;
import morgan.support.Utils;

public class DBManager {
    /*key: table name, value: DBWorker name*/
    private static final Map<String, String> dbworkers_ = new HashMap<>();

    public static void initDB(Node node) {
    	Log.db.info("DB load start...");
        try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			Connection conn = DriverManager.getConnection(Factory.configInstance().DB_URL, Factory.configInstance().DB_USER, Factory.configInstance().DB_PASSWORD);

			if (Factory.configInstance().DB_LOAD_FROM_META == 1)
				loadFromDBMeta(conn, node);
			else
				loadFromDBConfig(conn, node);

			conn.close();
			Log.db.info("DB load finished!");
        } catch (Exception e) {
			Log.db.error("init DB failed, e:{}", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadFromDBMeta(Connection conn, Node node) throws Exception {
		Log.db.info("DB load from meta.");

		Map<Integer, List<String>> assign = new HashMap<>();

		DatabaseMetaData meta = conn.getMetaData();
		ResultSet rs = meta.getTables(null, null, null, new String[] { "TABLE" });
		while (rs.next()) {
			String tableName = rs.getString("TABLE_NAME");
			int index = assign(tableName);
			dbworkers_.put(tableName, "DBWorker-" + index);
			assign.compute(index, (i, l) -> {
				if (l == null)
					l = new ArrayList<>();
				l.add(tableName);
				return l;
			});
		}

		for (var e : assign.entrySet()) {
			DBWorker w = new DBWorker(node, "DBWorker-" + e.getKey());
			w.init(e.getValue());
			node.addWorkerStandAlone(w);
		}
	}

	public static void loadFromDBConfig(Connection conn, Node node) throws Exception {
		Log.db.info("DB load from config.");

		List<Class<?>> clzzs = Utils.getClassFromPackage(Factory.configInstance().DB_DEF_PATH);
		Map<Integer, Map<String, DBTable>> assign = new HashMap<>();
		Map<String, DBWorker> workers = new HashMap<>();
		for (var clz : clzzs) {
			if (!clz.isAnnotationPresent(Table.class))
				continue;

			Log.db.info("scanned db config class: {}", clz.getName());
			var tableName = clz.getAnnotation(Table.class).tableName();
			int index = assign(tableName);
			dbworkers_.put(tableName, "DBWorker-" + index);
			var tables = assign.get(index);
			if (tables == null) {
				tables = new HashMap<>();
				assign.put(index, tables);
				var worker = new DBWorker(node, "DBWorker-" + index);
				workers.put("DBWorker-" + index, worker);
			}

			if (Factory.configInstance().DB_CREATE_OR_UPDATE_BEFORE_START == 1)
				createOrUpdate(conn, clz);

			tables.put(tableName, createDBTable(workers.get("DBWorker-" + index), tableName, clz));
		}

		for (var e : assign.entrySet()) {
			var worker = workers.get("DBWorker-" + e.getKey());
			worker.init(e.getValue());
			node.addWorkerStandAlone(worker);
		}
	}

	private static void createOrUpdate(Connection conn, Class<?> dbClz) {
    	Map<String, Map<String, Object>> fields = new LinkedHashMap<>();
		List<String> index = new ArrayList<>();

		for (var f : dbClz.getDeclaredFields()) {
			var anno = f.getAnnotation(Column.class);
			if (anno == null)
				continue;

			Map<String, Object> info = new HashMap<>();
			info.put("unique", anno.unique());
			info.put("defaults", anno.defaults());
			info.put("comment", anno.comments());
			info.put("nullable", anno.nullable());
			info.put("isIndex", anno.index());
			info.put("type", DBItemTypes.tryAppendLength(DBItemTypes.getEnumFromJavaClz(f.getType()), anno.length()));
			fields.put(f.getName(), info);
		}

		String tableName = dbClz.getAnnotation(Table.class).tableName();
    	try {
			Statement stat1 = conn.createStatement();
			ResultSet rs = stat1.executeQuery("show full columns from " + tableName + ";");
			DatabaseMetaData meta = conn.getMetaData();
			var ii = meta.getIndexInfo(null, null, tableName, false, true);
			while (ii.next()) {
				index.add(ii.getString("INDEX_NAME"));
			}
			while (rs.next()) {
				// try update this table
				var fieldName = rs.getString("Field");
				var f = fields.remove(fieldName);

				if (f == null) {
					if (!fieldName.equals("id")) {
						// drop column
						String drop = "alter table " + tableName + " drop column `" + fieldName + "`;";
						Log.db.info("drop column from {}, sql:{}", tableName, drop);
						Statement stat2 = conn.createStatement();
						stat2.execute(drop);
					}
					continue;
				}

				// update column
				boolean update = false;

				String nullab = rs.getString("NULL");
				if ((nullab.equals("YES") && !(Boolean)f.get("nullable")) || (nullab.equals("NO") && (Boolean)f.get("nullable")))
					update = true;
				else if (!rs.getString("Type").equals(f.get("type")))
					update = true;
				else if (!f.get("defaults").equals(rs.getString("Default") == null ? "" : rs.getString("Default")))
					update = true;
				else if (!f.get("comment").equals(rs.getString("Comment")))
					update = true;

				if (update) {
					StringBuilder alter = new StringBuilder();
					alter.append("alter table `").append(tableName)
							.append("` modify `").append(fieldName).append("` ")
							.append((String)f.get("type"));

					if (!((String)f.get("defaults")).isBlank())
						alter.append(" default '").append(f.get("defaults")).append("'");

					alter.append((Boolean) f.get("nullable") ? "" : " not null")
							.append(" comment '").append(f.get("comment")).append("';");
					Log.db.info("update column for {}, sql:{}", tableName, alter.toString());
					Statement stat3 = conn.createStatement();
					stat3.execute(alter.toString());
				}

				// update unique key
				StringBuilder uniqueKey = new StringBuilder();
				if (rs.getString("Key").equals("UNI") && !(Boolean)f.get("unique")) {
					uniqueKey.append("alter table `").append(tableName).append("` drop index `unique_key_").append(fieldName).append("`;");
				} else if (!rs.getString("Key").equals("UNI") && (Boolean)f.get("unique")) {
					uniqueKey.append("alter table `").append(tableName).append("` ADD UNIQUE INDEX `unique_key_").append(fieldName).append("` (`").append(fieldName).append("` ASC) VISIBLE;");
				}

				if (uniqueKey.length() != 0) {
					Log.db.info("update unique key for {}, sql:{}", tableName, uniqueKey.toString());
					Statement stat4 = conn.createStatement();
					stat4.execute(uniqueKey.toString());
				}

				// update index
				StringBuilder updateIndex = new StringBuilder();
				if ((Boolean)f.get("isIndex") && !index.contains(fieldName)) {
					updateIndex.append("alter table `").append(tableName).append("` add index `").append(fieldName).append("`(`").append(fieldName).append("` ASC) VISIBLE;");
				} else if (!(Boolean)f.get("isIndex") && index.contains(fieldName)) {
					updateIndex.append("alter table `").append(tableName).append("` drop index `").append(fieldName).append("`;");
				}

				if (updateIndex.length() != 0) {
					Log.db.info("update index for {}, sql:{}", tableName, updateIndex.toString());
					Statement stat5 = conn.createStatement();
					stat5.execute(updateIndex.toString());
				}
			}

			// add columns
			for (var e : fields.entrySet()) {
				var info = e.getValue();
				StringBuilder add = new StringBuilder();
				add.append("alter table `").append(tableName).append("` add column `").append(e.getKey()).append("` ").append(info.get("type"));
				if (!((String)info.get("defaults")).isBlank())
					add.append(" default '").append(info.get("defaults")).append("'");
				add.append(" comment '").append(info.get("comment")).append("'");
				if (!(Boolean)info.get("nullable"))
					add.append(" not null");

				add.append(";");
				Log.db.info("add column for {}, sql:{}", tableName, add.toString());
				Statement stat6 = conn.createStatement();
				stat6.execute(add.toString());

				if ((Boolean)info.get("isIndex")) {
					StringBuilder indexBuild = new StringBuilder();
					indexBuild.append("alter table `").append(tableName).append("` add index `").append(e.getKey()).append("`(`").append(e.getKey()).append("` ASC) VISIBLE;");
					Log.db.info("add index for {}, sql:{}", tableName, indexBuild.toString());
					Statement stat = conn.createStatement();
					stat.execute(indexBuild.toString());
				}

				if ((Boolean)info.get("unique")) {
					StringBuilder uniqueBuild = new StringBuilder();
					uniqueBuild.append("alter table `").append(tableName).append("` ADD UNIQUE INDEX `unique_key_").append(e.getKey()).append("` (`").append(e.getKey()).append("` ASC) VISIBLE;");
					Log.db.info("add unique key for {}, sql:{}", tableName, uniqueBuild.toString());
					Statement stat = conn.createStatement();
					stat.execute(uniqueBuild.toString());
				}
			}
		} catch (Exception e) {
			if (e.getMessage() == null || !e.getMessage().endsWith("doesn't exist")) {
				e.printStackTrace();
			} else {
				// create new table
				try {
					Statement stat = conn.createStatement();
					StringBuilder create = new StringBuilder();
					StringBuilder indexBuild = new StringBuilder();
					StringBuilder uniqueBuild = new StringBuilder();
					create.append("create table `").append(tableName).append("`(");
					create.append("`id` bigint(20) not null,");
					for (var entry : fields.entrySet()) {
						var info = entry.getValue();
						create.append("`").append(entry.getKey()).append("` ").append(info.get("type"));

						if (!((String)info.get("defaults")).isBlank())
							create.append(" default '").append(info.get("defaults")).append("'");
						if (!(Boolean)info.get("nullable"))
							create.append(" not null");
						create.append(" comment '").append(info.get("comments")).append("', ");

						if ((Boolean)info.get("isIndex"))
							indexBuild.append("key `").append(entry.getKey()).append("`(`").append(entry.getKey()).append("`),");

						if ((Boolean)info.get("unique"))
							uniqueBuild.append("unique key `unique_key_").append(entry.getKey()).append("`(`").append(entry.getKey()).append("`),");
					}
					create.append(indexBuild.toString());
					create.append(uniqueBuild.toString());
					create.append("primary key (`id`));");
					Log.db.info("created table, sql:{}", create.toString());
					stat.execute(create.toString());
				} catch (Exception ee) {
					ee.printStackTrace();
				}
			}
		}
	}

	private static DBTable createDBTable(DBWorker worker, String tableName, Class<?> clz) {
		Map<String, DBItemTypes> columns = new LinkedHashMap<>();
		List<Integer> uniqueIndices = new ArrayList<>();
		columns.put("id", DBItemTypes.BIGINT);
		int columnIndex = 1;
		for (var f : clz.getDeclaredFields()) {
			var anno = f.getAnnotation(Column.class);
			if (anno == null)
				continue;

			if (anno.unique())
				uniqueIndices.add(columnIndex);

			columns.put(f.getName(), DBItemTypes.getEnumFromJavaClz(f.getType()));
			columnIndex++;
		}

		return new DBTable(worker, tableName, columns, uniqueIndices);
	}

	public static void tryUpdateDB() {
    	try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			Connection conn = DriverManager.getConnection(Factory.configInstance().DB_URL, Factory.configInstance().DB_USER, Factory.configInstance().DB_PASSWORD);
			List<Class<?>> clzzs = Utils.getClassFromPackage(Factory.configInstance().DB_DEF_PATH);
			for (var clz : clzzs) {
				if (!clz.isAnnotationPresent(Table.class))
					continue;

				createOrUpdate(conn, clz);
			}
		} catch (Exception e) {
    		e.printStackTrace();
		}
	}

	public static int assign(String tableName) {
    	if (tableName == null || tableName.isBlank())
    		return -1;

    	return Math.abs(tableName.hashCode()) % Factory.configInstance().DB_WORKER_NUM;
	}

    public static String getAssignedWorker(String table) {
        String worker = dbworkers_.get(table);
        if (worker != null)
            return worker;
        return "";
    }

    public static int getAssignedWorkerId(String table) {
		String worker = dbworkers_.get(table);
		if (worker != null)
			return Integer.parseInt(worker.split("-")[1]);
		return -99;
	}

	public static void main(String[] args) {
    	tryUpdateDB();
	}
}
