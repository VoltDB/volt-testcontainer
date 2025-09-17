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



# Generate a maven project with sample schema, procedures, and Junit tests
```shell
mvn -B -ntp archetype:generate \
    -DarchetypeGroupId=org.voltdb \
    -DarchetypeArtifactId=voltdb-stored-procedures-maven-quickstart \
    -DarchetypeVersion=1.5.0 \
    -DgroupId=org.example.test \
    -DartifactId=voltdb-procedures \
    -Dpackage=org.example.procedures \
    -Dversion=1.0-SNAPSHOT
```
This uses a maven archetype to generate your own project that starts you off with a simple example schema, a few simple procedures, and passing Junit tests.

Then, use maven to install the project and run tests.

```shell
cd voltdb-procedures
mvn -DskipTests=true clean install
mvn test
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


# Add a Unit Test:

For unit testing we will need following setup:

1. A docker environment.
2. Access to VoltDB images for your target version.
3. A developer license.


Once you have above requirements satisfied, you can use the project you generated from the quickstart archetype to develop procedures, build a procedure jar file, and develop and run unit tests.

Write your unit test to call one or more of your procedures, passing in test data, and validating that the procedure functioned as expected. Your test class must extend the TestBase class, which handles configuring the test container (including loading the packaged jar file and schema.ddl file). Different VoltDBCluster constructors can be used to test using a single node or multi-node cluster (See the Javadoc for volt-testcontainer).

```java
public class KeyValueTest extends TestBase {

    @Test
    public void testKeyValue() {
        VoltDBCluster db = new VoltDBCluster(getLicensePath(), "voltdb/voltdb-enterprise:" + getImageVersion());
        try {
            configureTestContainer(db);
            Client client = db.getClient();
            ClientResponse response = client.callProcedure("Put", 10, "Hello");
            assertTrue(response.getStatus() == ClientResponse.SUCCESS);
            response = client.callProcedure("Get", 10);
            VoltTable table = response.getResults()[0];
            // Advance to first row
            table.advanceRow();
            String val = table.getString(0);
            assertTrue(val.equals("Hello"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (db != null) {
                db.shutdown();
            }
        }
    }
}
```

# Test your procedures:

To test the above example, first the jar needs to be built and present in the target directory:
```shell
mvn -DskipTests=true clean install
```
Then, you can run the unit test:
```shell
mvn test
```

With your stored procedure successfully unit tested, you can now integrate this build process to validate and package your procedures for production. 
