package morgan.db;

import morgan.structure.serialize.InputStream;
import morgan.structure.serialize.OutputStream;
import morgan.structure.serialize.Serializable;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.LinkedHashMap;
import java.util.Map;

public class Record implements Serializable {

    public Map<String, Object> values = new LinkedHashMap<>();
    public String table;
    public boolean persisted;

    public Record(String tableName) {
        table = tableName;
    }

    public Record(DBItem item) {
        for (int i = 0; i < item.columnSize(); i++) {
            String label = item.table().getLabelByIndex(i);
            values.put(label, item.getColumn(i));
        }
        table = item.table().name();
        persisted = true;
    }

    public Record(ResultSet rs) {
        try {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String name = meta.getColumnName(i).intern();
                switch (DBItemTypes.getEnumFromType(meta.getColumnType(i))) {
                    case SMALLINT:
                    case INT: {
                        values.put(name, rs.getInt(i));
                        break;
                    }
                    case TINYINT: {
                        values.put(name, rs.getBoolean(i));
                        break;
                    }
                    case BIGINT: {
                        values.put(name, rs.getLong(i));
                        break;
                    }
                    case BLOB: {
                        values.put(name, rs.getBytes(i));
                        break;
                    }
                    case FLOAT: {
                        values.put(name, rs.getFloat(i));
                        break;
                    }
                    case DOUBLE: {
                        values.put(name, rs.getDouble(i));
                        break;
                    }
                    case VARCHAR: {
                        values.put(name, rs.getString(i));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        persisted = true;
    }

    @Override
    public void writeOut(OutputStream out) throws IOException {
        out.write(values);
        out.write(table);
        out.write(persisted);
    }

    @Override
    public void readIn(InputStream in) throws IOException {
        values = in.read();
        table = in.read();
        persisted = in.read();
    }

    public String toString() {
        return values.toString();
    }
}
