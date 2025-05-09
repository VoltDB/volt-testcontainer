package ${package};

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class Get extends VoltProcedure {

    // Get the current key inserted*
    public final SQLStmt getKey = new SQLStmt("SELECT VALUE from KEYVALUE WHERE KEYNAME = ?;");

    public VoltTable[] run(int key) {
        voltQueueSQL(getKey, key);
        return voltExecuteSQL();
    }
}