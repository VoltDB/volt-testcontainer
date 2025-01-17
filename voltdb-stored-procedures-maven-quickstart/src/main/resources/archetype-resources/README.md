# VoltDB and Unit Testing

This document gives an overview of how VoltDB stored procedures can be unit tested before deploying to production.
VoltDB Stored Procedures allow developers to write business logic inside a single transaction. Traditionally database layers/applications write this on the application side. Since VoltDB does not have externally managed transactions users/developers need to write it in the stored procedures. Procedures are written in Java and have exactly the same semantics of a transaction.

A typical stored procedure has a run() method with input parameters which are provided by application. The method executes your logic which can comprise of many SQL statements and decides if the transaction is successful by simply returning the results. In case the transaction needs to be aborted the method simply throws a VoltAbortException or return error codes that the application can check.

## Generated project structure
Storedprocedures are under src/main/java/${package} <br />
Stored Procedures tests are under src/test/java/${package} <br/>
Schema needed for Procedures are under src/main/resources <br/>

### Unit Testing above Stored Procedure:

### For unit testing we will need following setup:
#### What you need
1. A docker environment.  
2. Access to VoltDB images for your target version.  
3. A VoltDB license.

#### Unit Testing above Stored Procedure:
> export VOLTDB_LICENSE=<path-to-license> </br>
> mvn -DskipTest=true clean install </br>
> mvn test </br>
