package morgan.structure;

import morgan.DBDefs.ArchDB;
import morgan.db.DBManager;
import morgan.db.DBWorker;
import morgan.db.Record;
import morgan.support.Factory;
import morgan.support.Time;

import java.util.List;

public class SystemManager extends Worker{

    private ArchDB archDB;

    public SystemManager(Node node) {
        super(node, "SystemManager");
    }

    @Override
    public void pulseOverride() {
        if (archDB == null)
            return;

        var now = System.currentTimeMillis();
        if (archDB.getZero_reset_time() == 0 || !Time.isSameDay(now, archDB.getZero_reset_time())) {
            postEvent("zeroReset");
            archDB.setZero_reset_time(now);
        }
    }

    @Override
    public void initOverride() {
        DBWorker.selectById_(DBManager.getAssignedWorkerId("m_arch"), "m_arch", Factory.configInstance().SERVER_ID);
        DBWorker.Listen_((r) -> {
            List<Record> rr = r.getResult("result");
            ArchDB db = null;
            if (rr.isEmpty()) {
                db = new ArchDB();
                db.setId(Factory.configInstance().SERVER_ID);
                db.save();
            } else {
                db = new ArchDB(rr.get(0));
            }

            db.setSystem_start_time(System.currentTimeMillis());
            archDB = db;
        });
    }

    @Override
    public void registMethods() {}
}
