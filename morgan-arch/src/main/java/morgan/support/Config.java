package morgan.support;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public abstract class Config {
	public static final String MAIN_CONFIG = "./config/main.config";

	public final String DB_URL = null;

	public final String DB_USER = null;

	public final String DB_PASSWORD = null;

	public final Integer DB_MERGE_LIMIT = null;

	public final Integer DB_WORKER_NUM = null;

	public final Integer DB_LOAD_FROM_META = null;

	public final String DB_DEF_PATH = null;

	public final Integer DB_CREATE_OR_UPDATE_BEFORE_START = null;

	public final Integer HTTP_PORT = 0;

	public final Integer TCP_PORT = 0;

	public final Integer SERVER_ID = 0;

	public final Integer DISPATCH_THREADS = 0;

	protected Map<String, String> read(String fileName) {
		Map<String, String> res = new HashMap<>();
		try {
			BufferedReader in = new BufferedReader(new FileReader(fileName));
			while (in.ready()) {
				String s = in.readLine();
				if (s.isBlank() || s.charAt(0) != '$')
					continue;

				s = s.replace("$", "").replace(" ", "");
				int equal = s.indexOf("=");
				res.put(s.substring(0, equal), s.substring(equal + 1));
			}
			in.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return res;
	}

	protected void fillField(Field f, String val) {
		try {
			f.setAccessible(true);
			var type = f.getType();
			if (type == Integer.class)
				f.set(this, Integer.valueOf(val));
			else if (type == Long.class)
				f.set(this, Long.valueOf(val));
			else if (type == Double.class)
				f.set(this, Double.valueOf(val));
			else if (type == Float.class)
				f.set(this, Float.valueOf(val));
			else if (type == Boolean.class)
				f.set(this, Boolean.valueOf(val));
			else
				f.set(this, val);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void fill() {
		var pairs = read(MAIN_CONFIG);
		for (var f : Config.class.getDeclaredFields()) {
			if (pairs.containsKey(f.getName()))
				fillField(f, pairs.get(f.getName()));
		}
		fillOverride();
	}

	protected abstract void fillOverride();
}
