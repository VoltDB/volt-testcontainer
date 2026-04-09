/*
 * Copyright (C) 2025-2026 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
import flashsale.common.Constants;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.Client2;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdbtest.testcontainer.VoltDBCluster;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration tests for the flash sale stored procedures against a live VoltDB
 * cluster managed by {@link VoltDBCluster}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Successful purchases that decrement stock correctly</li>
 *   <li>Rejection of purchases for non-existent products</li>
 *   <li>Rejection of purchases when stock is exhausted</li>
 *   <li>Both the legacy {@link Client} and modern {@link Client2} APIs</li>
 *   <li>A 3-node k=1 cluster topology</li>
 * </ul>
 */
public class FlashSaleIT extends TestBase {

    private static final Logger LOG = LoggerFactory.getLogger(FlashSaleIT.class);

    private static final int PRODUCT_ID    = 1;
    private static final int STOCK         = 10;
    private static final long CUSTOMER_ID  = 42L;

    /**
     * Seeds one product and verifies that:
     * <ol>
     *   <li>A valid purchase succeeds and decrements stock.</li>
     *   <li>A purchase for a non-existent product returns ERR_INVALID_PRODUCT.</li>
     *   <li>After exhausting stock, a further purchase returns ERR_OUT_OF_STOCK.</li>
     * </ol>
     */
    @Test
    public void testFlashSaleOnSingleNode() throws Exception {
        VoltDBCluster db = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        db.withLogConsumer(LOG);
        try {
            configureTestContainer(db);
            runFlashSaleAssertions(db);
        } finally {
            db.shutdown();
        }
    }

    /**
     * Same assertions on a 3-node k=1 cluster to verify multi-node topology works.
     */
    @Test
    public void testFlashSaleOnThreeNodeCluster() throws Exception {
        VoltDBCluster db = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE, 3, 1);
        db.withLogConsumer(LOG);
        try {
            configureTestContainer(db);
            runFlashSaleAssertions(db);
        } finally {
            db.shutdown();
        }
    }

    /**
     * Verifies that both the legacy {@link Client} and modern {@link Client2} can
     * issue flash sale procedure calls against the same running cluster.
     */
    @Test
    public void testBothClientTypes() throws Exception {
        VoltDBCluster db = new VoltDBCluster(validLicensePath, VOLTDB_IMAGE);
        db.withLogConsumer(LOG);
        try {
            configureTestContainer(db);

            // Seed product with 4 units
            db.callProcedure("AddProduct", 10, "Dual-client tee", 25.00, 4);

            // Purchase 2 units via Client2 (modern API)
            Client2 client2 = db.getClient2();
            ClientResponse r2 = client2.callProcedureSync("Purchase", CUSTOMER_ID, 10, 2);
            assertEquals(ClientResponse.SUCCESS, r2.getStatus());
            assertEquals(Constants.PURCHASE_SUCCESS, r2.getResults()[0].fetchRow(0).getLong(0));

            // Purchase remaining 2 units via Client (legacy API)
            Client client = db.getClient();
            ClientResponse r1 = client.callProcedure("Purchase", CUSTOMER_ID + 1, 10, 2);
            assertEquals(ClientResponse.SUCCESS, r1.getStatus());
            assertEquals(Constants.PURCHASE_SUCCESS, r1.getResults()[0].fetchRow(0).getLong(0));

            // Stock should now be 0; next purchase must be rejected
            ClientResponse r3 = client2.callProcedureSync("Purchase", CUSTOMER_ID + 2, 10, 1);
            assertEquals(ClientResponse.SUCCESS, r3.getStatus());
            assertEquals(Constants.ERR_OUT_OF_STOCK, r3.getResults()[0].fetchRow(0).getLong(0));
        } finally {
            db.shutdown();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Seeds one product with {@link #STOCK} units and exercises the three
     * core purchase scenarios using {@link VoltDBCluster#callProcedure}.
     */
    private void runFlashSaleAssertions(VoltDBCluster db)
            throws IOException, ProcCallException {

        // Seed inventory
        ClientResponse addResp = db.callProcedure("AddProduct", PRODUCT_ID,
                "Limited-edition hoodie", 79.99, STOCK);
        assertEquals(ClientResponse.SUCCESS, addResp.getStatus());

        // 1. Valid purchase: buy 3 units, verify stock falls to STOCK-3
        ClientResponse buyResp = db.callProcedure("Purchase", CUSTOMER_ID, PRODUCT_ID, 3);
        assertEquals(ClientResponse.SUCCESS, buyResp.getStatus());
        assertEquals(Constants.PURCHASE_SUCCESS,
                buyResp.getResults()[0].fetchRow(0).getLong(0),
                "Valid purchase must return PURCHASE_SUCCESS");

        ClientResponse stockResp = db.callProcedure("GetStock", PRODUCT_ID);
        assertEquals(ClientResponse.SUCCESS, stockResp.getStatus());
        long remaining = stockResp.getResults()[0].fetchRow(0).getLong(0);
        assertEquals(STOCK - 3, remaining, "Stock must have decreased by 3");

        // 2. Invalid product: product 999 does not exist
        ClientResponse invalidResp = db.callProcedure("Purchase", CUSTOMER_ID, 999, 1);
        assertEquals(ClientResponse.SUCCESS, invalidResp.getStatus());
        assertEquals(Constants.ERR_INVALID_PRODUCT,
                invalidResp.getResults()[0].fetchRow(0).getLong(0),
                "Purchase of invalid product must return ERR_INVALID_PRODUCT");

        // 3. Out-of-stock: buy remaining units, then one more
        long toBuy = remaining;
        ClientResponse drainResp = db.callProcedure("Purchase", CUSTOMER_ID, PRODUCT_ID, (int) toBuy);
        assertEquals(ClientResponse.SUCCESS, drainResp.getStatus());
        assertEquals(Constants.PURCHASE_SUCCESS,
                drainResp.getResults()[0].fetchRow(0).getLong(0));

        ClientResponse oosResp = db.callProcedure("Purchase", CUSTOMER_ID, PRODUCT_ID, 1);
        assertEquals(ClientResponse.SUCCESS, oosResp.getStatus());
        assertEquals(Constants.ERR_OUT_OF_STOCK,
                oosResp.getResults()[0].fetchRow(0).getLong(0),
                "Purchase after stock exhausted must return ERR_OUT_OF_STOCK");
    }
}
