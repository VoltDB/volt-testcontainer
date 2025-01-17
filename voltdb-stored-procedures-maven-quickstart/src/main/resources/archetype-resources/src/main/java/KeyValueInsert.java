package ${package};

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class KeyValueInsert extends VoltProcedure {

    // Get the current key inserted*
    public final SQLStmt getKey = new SQLStmt("SELECT * from KEYVALUE WHERE KEYNAME = ?;");

    // Insert a key and value*
    public final SQLStmt insertKey = new SQLStmt("INSERT INTO KEYVALUE (KEYNAME, VALUE) VALUES (?, ?);");

    public VoltTable[] run(int key, String value) {
        voltQueueSQL(insertKey, key, value);
        voltQueueSQL(getKey, key);
        VoltTable result[] = voltExecuteSQL();
        return result;
    }
}