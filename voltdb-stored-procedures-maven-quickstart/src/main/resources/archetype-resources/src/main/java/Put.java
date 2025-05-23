package ${package};

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class Put extends VoltProcedure {

    // Insert a key and value*
    public final SQLStmt insertKey = new SQLStmt("INSERT INTO KEYVALUE (KEYNAME, VALUE) VALUES (?, ?);");

    public VoltTable[] run(int key, String value) {
        voltQueueSQL(insertKey, key, value);
        VoltTable result[] = voltExecuteSQL();
        return result;
    }
}