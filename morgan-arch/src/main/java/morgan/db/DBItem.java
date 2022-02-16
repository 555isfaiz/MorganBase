package morgan.db;

import morgan.db.tasks.*;
import morgan.structure.Call;

import java.util.ArrayList;
import java.util.List;

public class DBItem {
    private List<Object> columns_ = new ArrayList<>();
    private DBTable table_;

    public DBItem() {}

    public DBItem(DBTable table, Record r) {
    	table_ = table;
		columns_.addAll(r.values.values());
	}

    public DBTask onUpdate(List<Integer> indexs, List<Object> values) {
        DBTask task = new DBTaskUpdate();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < indexs.size(); i++) {
            labels.add(table_.getLabelByIndex(indexs.get(i)));
            columns_.set(indexs.get(i), values.get(i));
        }
        task.labels_ = labels;
        task.values_ = new ArrayList<>(values);
        task.cid_ = (long)columns_.get(0);
        return task;
    }

    public DBTask onInsert() {
        DBTask task = new DBTaskInsertItem();
        task.values_.addAll(columns_);
        for (int i = 0; i < columns_.size(); i++) {
            task.affectedIndexs_.add(i);
        }
        return task;
    }

    public DBTask onRemove() {
        DBTask task = new DBTaskRemoveItem();
        task.cid_ = (long)getColumn(0);
        return task;
    }

    public DBTask onQuery(Call queryCall) {
        DBTaskQueryWithId task = new DBTaskQueryWithId();
        task.record = new Record(this);
        task.queryCall_ = queryCall;
        return task;
    }

    public void addColumn(Object column) {
        columns_.add(column);
    }

    public Object getColumn(int index) {
        return columns_.get(index);
    }

    public void table(DBTable table) {
        table_ = table;
    }

    public DBTable table() {
        return table_;
    }

    public int columnSize() {
        return columns_.size();
    }
}
