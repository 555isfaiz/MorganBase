package morgan.db;

import morgan.structure.Node;
import morgan.structure.Worker;

public class DBStartUp {

    public static void main(String[] args) {
        Node node = new Node("Conn", "tcp://127.0.0.1:3340");
        DBManager.initDB(node);
        node.startUp();
//        node.anyWorker().schedule(1000, ()->{
//			((DBWorker)Worker.getCurrentWorker()).execRawStatement("show full columns from new_table");
//		});
    }
}
