/*
 * Copyright (C) 2025-2026 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
import flashsale.common.Constants;
import org.junit.jupiter.api.Test;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdbtest.testcontainer.VoltDBCluster;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that schema and stored-procedure classes can be preloaded at
 * cluster construction time via {@link VoltDBCluster#withInitialSchemaFromHostPath}
 * and {@link VoltDBCluster#withInitialClasses}, so no separate
 * {@code loadClasses} / {@code runDDL} calls are required after {@code start()}.
 */
public class FlashSalePreloadIT extends TestBase {

    /**
     * Schema and classes configured in the constructor chain, before {@code start()}.
     */
    @Test
    public void testPreloadInConstructorChain() throws Exception {
        File schemaFile = getSchemaFile();
        VoltDBCluster db = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE)
                .withInitialSchemaFromHostPath(schemaFile.getAbsolutePath(), "schema.ddl")
                .withInitialClasses(getJars());
        try {
            db.start();
            runBasicFlashSale(db);
        } finally {
            db.shutdown();
        }
    }

    /**
     * Schema and classes set after construction but before {@code start()}.
     */
    @Test
    public void testPreloadAfterConstruction() throws Exception {
        VoltDBCluster db = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        File schemaFile = getSchemaFile();
        db.withInitialSchemaFromHostPath(schemaFile.getAbsolutePath(), "schema.ddl");
        db.withInitialClasses(getJars());
        try {
            db.start();
            runBasicFlashSale(db);
        } finally {
            db.shutdown();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static File getSchemaFile() throws Exception {
        URL resource = FlashSalePreloadIT.class.getClassLoader().getResource("schema.ddl");
        if (resource == null) {
            throw new FileNotFoundException("schema.ddl not found on classpath");
        }
        return new File(resource.toURI());
    }

    private static void runBasicFlashSale(VoltDBCluster db)
            throws IOException, ProcCallException {

        db.callProcedure("AddProduct", 1, "Preload-test item", 9.99, 5);

        ClientResponse buy = db.callProcedure("Purchase", 1L, 1, 2);
        assertEquals(ClientResponse.SUCCESS, buy.getStatus());
        assertEquals(Constants.PURCHASE_SUCCESS,
                buy.getResults()[0].fetchRow(0).getLong(0));
    }
}
