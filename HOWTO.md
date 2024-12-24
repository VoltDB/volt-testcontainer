# VoltDB and Unit Testing

This document gives an overview of how VoltDB stored procedures can be unit tested before deploying to production.
VoltDB Stored Procedures allow developers to write business logic inside a single transaction. Traditionally database layers/applications write this on the application side. Since VoltDB does not have externally managed transactions users/developers need to write it in the stored procedures. Procedures are written in Java and have exactly the same semantics of a transaction.

A typical stored procedure has a run() method with input parameters which are provided by application. The method executes your logic which can comprise of many SQL statements and decides if the transaction is successful by simply returning the results. In case the transaction needs to be aborted the method simply throws a VoltAbortException or return error codes that the application can check.

# Write Stored Procedure:

Let's look at the following procedure for our example and how we can unit test the procedure.   
The procedure:

1. Inserts a key and value and returns a VoltTable  of what was inserted.

```
package com.mycompany.procs;

public class KeyValueInsert extends VoltProcedure {

   // Get the current key inserted*  
   public final SQLStmt getKey \= new SQLStmt(  
           "SELECT * from KEYVALUE WHERE KEY \= ?;");

   // Insert a key and value*  
   public final SQLStmt insertKey \= new SQLStmt(  
           "INSERT INTO KEYVALUE (KEY, VALUE) VALUES (?, ?);");

   public VoltTable\[\] run(int key, int value) {  

       voltQueueSQL(insertKey, key, value);  
       voltQueueSQL(getKey, key);  
       VoltTable result\[\] \= voltExecuteSQL();
       return result;  
   }  
}
```

# Unit Testing above Stored Procedure:

For unit testing we will need following setup:

1. A docker environment.  
2. Access to VoltDB images for your target version.  
3. A developer license.

Once you have above requirements satisfied you will need to add volt-testcontainer as test dependency to your project like below
```
<dependency\>  
   <groupId\>org.voltdb</groupId\>  
   <artifactId\>volt-testcontainer</artifactId\>  
</dependency\>
```

Write your unit test as below unit test assumes that your procedures are compiled and put in a jar file and your schema is accessible.
```
public class KeyValueTest {

   @Test  
   public void testKeyValue() {  
       VoltDBCluster db \= new VoltDBCluster(“path-to-voltdb-license”, "**voltdb/voltdb-enterprise:13.3.0**")  
.withInitialSchema("\<path-to-ddl\>")  
.withInitialClasses("\<path-to-jar\>", "somename");  
       try {  
           db.start();  
           Client client \= db.getClient();  
           ClientResponse response \= client.callProcedure("KeyValueInsert", 10, 10);  
           *// Assert*  
       } catch (Exception e) {  
           fail(e.getMessage());  
       } finally {  
           if (db \!= null) {  
               db.shutdown();  
           }  
       }  
   }  
}
```

# Test your procedures:

Once your code is locally unit tested, integrate with your build to validate and publish your procedures. Flyway or Liquibase support for VoltDB does not exist but for continuous deployment once can easily script loading the schema (if it has changes) and classes using sqlcmd CLI. This way you can promote changes to your production environment.

# References:

The volt-testcontainer and a sample testing code is available here:   
[https://github.com/VoltDB/volt-testcontainer.git](https://github.com/VoltDB/volt-testcontainer.git)

The repository contains a sample procedure project that packages the procedures and a test project that tests the procedures.

The repository also contains advanced use of testing API such as loading external jars that procedure code depends on.

