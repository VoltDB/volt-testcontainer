package ${package};

import org.apache.commons.text.WordUtils;
import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;


// This procedure uses WordUtils as an example of a procedure having a dependency
public class CapitalizeAndPut extends VoltProcedure {

    // Insert a key and value*
    public final SQLStmt insertKey = new SQLStmt("INSERT INTO KEYVALUE (KEYNAME, VALUE) VALUES (?, ?);");

    public String capitalize(String input) {
        return WordUtils.capitalizeFully(input);
    }

    public VoltTable[] run(int key, String value) {

        String capitalizedValue = capitalize(value);
        
        voltQueueSQL(insertKey, key, capitalizedValue);
        VoltTable result[] = voltExecuteSQL();
        return result;
    }
}
