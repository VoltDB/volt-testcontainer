package procedures.voltdb;


public class KeyValueInsert extends VoltProcedure {

    // Get the current key inserted*
    public final SQLStmt getKey \= new SQLStmt(
           "SELECT * from KEYVALUE WHERE KEY = ?;");

    // Insert a key and value*
    public final SQLStmt insertKey \= new SQLStmt(
           "INSERT INTO KEYVALUE (KEY, VALUE) VALUES (?, ?);");

    public VoltTable[] run(int key, int value) {

        voltQueueSQL(insertKey, key, value);
        voltQueueSQL(getKey, key);
        VoltTable result[] = voltExecuteSQL();
        return result;
    }
}