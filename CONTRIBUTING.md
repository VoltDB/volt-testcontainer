# volt-testcontainer

## Overview
`volt-testcontainer` is a project designed to integrate and test with VoltDB using containerized environments. This project includes various integration tests and procedures to ensure reliable and efficient interaction with the VoltDB database.

## Modules
- **volt-testcontainer**: a set of classes that enable running tests inside VoltDB test containers.
- **voltdb-stored-procedures-maven-quickstart**: a maven archetype customers can use to generate a fully configured project that includes a small sample schema, stored procedures, a unit test, and integration tests that use volt-testcontainer
- **volt-voter-procedures**: a procedure-only maven project that compiles and packages a set of stored procedures into a jar file.
- **volt-test-container-test**: a test-only module that includes a ddl file that goes with volt-voter-procedures, some client code, and tests.



To build and test you can run a single mvn command:

    mvn install

This will take about 5-6 minutes, mainly for the volt-test-container-test test stage which includes several voter test runs.

## Installation and Setup
### Prerequisites
- **Minimum Java Development Kit (JDK) 17**
- **Maven**: For managing project dependencies and builds.
- **Docker**: Required for containerizing the VoltDB instances. Access to VoltDB images.

### Steps
1. Clone the repository:
    ```sh
    git clone <repository-url>
    cd volt-testcontainer
    ```

2. Build the project:
    ```sh
    mvn clean install
    ```
   This will also run the tests provided you have a license in /tmp/voltdb-license.xml if you want to specify alternate license location point VOLTDB_LICENSE environment variable to a license file.


## Contribution
1. Fork the repository.
2. Create a new branch for your feature or bug fix:
    ```sh
    git checkout -b your-feature-name
    ```
3. Commit your changes:
    ```sh
    git commit -m 'Add some feature'
    ```
4. Push to the branch:
    ```sh
    git push origin your-feature-name
    ```
5. Create a new Pull Request.

## License
MIT
