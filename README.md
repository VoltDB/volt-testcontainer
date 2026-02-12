# Unit Testing VoltDB Java Stored Procedures

This guide shows how to set up a Maven Java project where you can develop, unit test, and package your stored procedures for VoltDB. This enables you to validate your procedure logic in isolation before deployment.

VoltDB uses Java Stored Procedures to enable fast data-driven decisions, using SQL access to the latest state of data in-memory, and java for safe memory management and familiar syntax for business logic.

VoltDB Stored Procedures implement a run() method with:
- input parameters of various types, including arrays or tables of data
- Java code to transform values, implement business logic, format results, etc.
- SQL statements that can be queued with input parameters and executed in batches
- the option to throw a VoltAbortException to rollback the transaction (or run to completion for commit)
- flexible responses that can return status codes and messages and one or more tables of result data

Testing VoltDB procedures traditionally required a mix of manual and scripted steps to initialize a VoltDB database instance, start the server process, load the schema SQL file and procedure jar, load some data, run some sequence of procedure invocations, and validate the results. Now, tests can be run using Junit in a maven project, leveraging VoltDB test docker containers to run the database and validate the results in seconds. This enables VoltDB users to develop more robust tests with less effort, to catch potential problems quickly without requiring complex setup or infrastructure resources.

# Pre-requisites
- have a valid Volt license.xml file; assuming it is in ~/license.xml - set the env var:
```shell
export VOLTDB_LICENSE=~/license.xml
```

# Generate a maven project with sample schema, procedures, and tests
```shell
mvn -B -ntp archetype:generate \
    -DarchetypeGroupId=org.voltdb \
    -DarchetypeArtifactId=voltdb-stored-procedures-maven-quickstart \
    -DarchetypeVersion=1.6.0 \
    -DgroupId=org.example.test \
    -DartifactId=my-voltdb-procedures \
    -Dpackage=org.example.procedures \
    -Dversion=1.0-SNAPSHOT
```
This uses a maven archetype to generate your own project that starts you off with a simple example schema, a few simple procedures, and passing Junit and Integration tests.

To build the project and run the tests, you can use a single maven command.

```shell
cd my-voltdb-procedures
mvn clean verify
```
You can then replace the schema, procedures, and tests with your own, and expand the project to cover your business requirements.

# Write a Stored Procedure:

Let's look at the following procedure for our example and how we can unit test the procedure.

The procedure:
1. Inserts a key and value
2. Queries the inserted record
3. Returns a set of tables with the results.

```java
package com.mycompany.procs;

public class KeyValueInsert extends VoltProcedure {

   // Get the current key inserted*  
   public final SQLStmt getKey = new SQLStmt(  
           "SELECT * from KEYVALUE WHERE KEY = ?;");

   // Insert a key and value*  
   public final SQLStmt insertKey = new SQLStmt(  
           "INSERT INTO KEYVALUE (KEY, VALUE) VALUES (?, ?);");

   public VoltTable[] run(int key, int value) {

       voltQueueSQL(insertKey, key, value);  
       voltQueueSQL(getKey, key);  
       VoltTable result[] = voltExecuteSQL();
       return result;  
   }  
}
```
For more information about writing stored procedures, see Using VoltDB: [Chapter 5. Designing Stored Procedures](https://docs.voltactivedata.com/UsingVoltDB/DesignProc.php)

The project includes one example unit test, CapitalizeTest.java, which tests the correctness of a helper method used in a procedure. You may write your own unit tests, but they may be very limited in scope since they cannot test SQL execution. 

The vast majority of your tests will be integration tests, since they load the VoltDB test container with the procedure jar file, which maven hasn't generated yet in the (unit) 'test' stage. Integration tests run after package in the maven lifecycle, so they can load the jar file and test any SQL functionality.

# Add an Integration Test:

For integration tests we will need the following setup:

1. A docker environment.
2. Access to VoltDB images for your target version.
3. A developer license.

Once you have the above requirements satisfied, you can use the project you generated from the quickstart archetype to develop procedures, build a procedure jar file, and develop and run integration tests.

Write your integration test to call one or more of your procedures, passing in test data, and validating that the procedure functioned as expected. Your test class must be named *IT (which is a naming convention for integration tests) and must extend the IntegrationTestBase class, which handles configuring the test container (including loading the packaged jar file, additional jar files for any declared dependencies, and the schema/ddl.sql file). Different VoltDBCluster constructors can be used to test using a single node or multi-node cluster (See the Javadoc for volt-testcontainer).

```java
public class KeyValueIT extends IntegrationTestBase {

    @Test
    public void testKeyValue() {
        VoltDBCluster db = new VoltDBCluster(getLicensePath(), "voltdb/voltdb-enterprise:" + getImageVersion(), getExtraLibDirectory());
        try {
            configureTestContainer(db);
            Client client = db.getClient();
            ClientResponse response = client.callProcedure("Put", 10, "Hello");
            assertEquals(ClientResponse.SUCCESS, response.getStatus());
            response = client.callProcedure("Get", 10);
            VoltTable table = response.getResults()[0];
            // Advance to first row
            table.advanceRow();
            String val = table.getString(0);
            assertEquals("Hello", val);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            db.shutdown();
        }
    }
}
```

## License search path

You can provide a path to your VoltDB license file via the constructor. If the path is not provided, the container class will try to load it from the following search path:
- If `VOLTDB_LICENSE` environment variable is set it will use that value.
- `license.xml` in the user home directory if it exists.
- `license.xml` in the system temp directory if it exists.

If the license file cannot be found, the container will throw an exception. The validity of the license if verified
by the actual VoltDB server process upo startup.

# Test your procedures:

Since this is an integration test, the procedure classes are compiled, unit tests are run, and the jar is created, then the integration test runs, since it depends on loading the procedure jar. To run all of these stages, you can use a single maven command.
```shell
mvn clean verify
```

With your stored procedure successfully tested, you can integrate the procedures and schema into your testing environment to prepare for migration to production.
