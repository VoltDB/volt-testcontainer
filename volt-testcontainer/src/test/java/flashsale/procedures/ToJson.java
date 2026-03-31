/*
 * Copyright (C) 2024-2026 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package flashsale.procedures;

import com.google.gson.Gson;
import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;

import java.util.Collections;

/**
 * Wraps a string in a JSON object using Gson and stores the input/result pair.
 * Used to verify that external JARs are loaded into VoltDB via
 * {@code VoltDBCluster}'s extraJarsDir constructor argument.
 */
public class ToJson extends VoltProcedure {

    private static final Gson GSON = new Gson();

    public final SQLStmt insertStmt = new SQLStmt(
            "INSERT INTO string_transform (input, result) VALUES (?, ?);");

    public long run(String input) {
        String json = GSON.toJson(Collections.singletonMap("input", input));
        voltQueueSQL(insertStmt, input, json);
        voltExecuteSQL(true);
        return json.length();
    }
}
