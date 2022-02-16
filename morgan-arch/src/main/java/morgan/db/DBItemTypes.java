package morgan.db;

import java.sql.Types;

public enum DBItemTypes {
    TINYINT("tinyint"),
    SMALLINT("smallint"),
    INT("int"),
    BIGINT("bigint"),
    FLOAT("float"),
    DOUBLE("double"),
    VARCHAR("varchar"),
    BLOB("blob"),
    ;

    public String name;

    DBItemTypes(String name) { this.name = name; }

    public static DBItemTypes getEnumFromType(int sqlType) {
        switch (sqlType) {
            case Types.TINYINT:
                return TINYINT;

            case Types.SMALLINT:
                return SMALLINT;

            case Types.BIGINT:
                return BIGINT;

            case Types.REAL:
                return FLOAT;

            case Types.FLOAT:
            case Types.DOUBLE:
                return DOUBLE;

            case Types.VARCHAR:
                return VARCHAR;

            case Types.BLOB:
                return BLOB;

            case Types.INTEGER:
            default:
                return INT;
        }
    }

    public static DBItemTypes getEnumFromJavaClz(Class<?> type) {
		if (int.class.equals(type) || Integer.class.equals(type)) {
			return INT;
		}

		else if (long.class.equals(type) || Long.class.equals(type)) {
			return BIGINT;
		}

		else if (String.class.equals(type)) {
			return VARCHAR;
		}

		else if (Float.class.equals(type) || float.class.equals(type)) {
			return FLOAT;
		}

		else if (Double.class.equals(type) || double.class.equals(type)) {
			return DOUBLE;
		}

		else if (byte[].class.equals(type)) {
			return BLOB;
		}

		else if (boolean.class.equals(type) || Boolean.class.equals(type)) {
			return TINYINT;
		}

		throw new IllegalArgumentException("can't convert " + type.getName() + " to sql type");
	}

	public static DBItemTypes getEnumFromString(String type) {
		switch (type) {
			case "tinyint":
			case "boolean":
				return TINYINT;
			case "smallint":
				return SMALLINT;
			case "int":
				return INT;
			case "bigint":
				return BIGINT;
			case "float":
				return FLOAT;
			case "double":
				return DOUBLE;
			case "blob":
				return BLOB;
			default:
				if (type.startsWith("varchar"))
					return VARCHAR;
				else
					throw new IllegalArgumentException("can't convert " + type + " to sql type");
		}
	}

	public static String tryAppendLength(DBItemTypes type, int length) {
    	if (type == VARCHAR)
    		return type.name + "(" + (length == 0 ? 255 : length) + ")";
    	else
    		return type.name;
	}
}
