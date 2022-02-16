/*
 * Usage:Get loggers
 */
package morgan.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * 
 * @author Mark D</a>
 * @version 1.0, 2019.3.4
 */
public class Log {
    public static Logger worker = LoggerFactory.getLogger("worker");
    public static Logger node = LoggerFactory.getLogger("node");
    public static Logger db = LoggerFactory.getLogger("db");
    public static Logger remoteNode = LoggerFactory.getLogger("remoteNode");
    public static Logger msghandler = LoggerFactory.getLogger("msghandler");
    public static Logger connection = LoggerFactory.getLogger("connection");
    public static Logger global = LoggerFactory.getLogger("global");
    public static Logger lobby = LoggerFactory.getLogger("lobby");
    public static Logger gameMngr = LoggerFactory.getLogger("gameMngr");
    public static Logger game = LoggerFactory.getLogger("game");
    public static Logger ntvLog = LoggerFactory.getLogger("ntvLog");
    public static Logger http = LoggerFactory.getLogger("morgan.http");
    public static Logger common = LoggerFactory.getLogger("common");
}
