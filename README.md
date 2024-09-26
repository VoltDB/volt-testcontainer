# volt-testcontainer

## Overview
`volt-testcontainer` is a project designed to integrate and test with VoltDB using containerized environments. This project includes various integration tests and procedures to ensure reliable and efficient interaction with the VoltDB database.

## Key Features
- **VoltDB testcontainer API**: Set of APIs to use a given VoltDB image and load your procedures and test using Junit.
- **Sample Database Procedures**: Custom procedures for initializing and interacting with the VoltDB database.
- **Integration Tests**: A set of integration tests to validate the functionalities of VoltDB in a containerized setup.

## Installation and Setup
### Prerequisites
- **Minimum Java Development Kit (JDK) 8**
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
    mvn clean install javadoc:javadoc
    ```
   This will also run the tests provided you have a license in /tmp/voltdb-license.xml if you want to specify alternate license location point VOLTDB_LICENSE environment variable to a license file.

## Usage
### Running VoltDB in a Container
Use the provided `VoltDBContainer` class to start and manage a VoltDB instance in a Docker container.

### Procedures
The project includes several procedures for operations on VoltDB, such as:
- `ContestantWinningStates`
- `Initialize`
- `JodaTimeInsert`
- `Results`
- `Vote`

These procedures can be found in the `volt-voter-procedures/src/main/java/voter/procedures/` directory.

### Tests
Integration tests like `IntegrationVoltDBClusterTest`, `IntegrationVoterTest`, etc., demonstrate how to use the framework and validate different scenarios.

## Contribution
1. Fork the repository.
2. Create a new branch for your feature or bug fix:
    ```sh
    git checkout -b feature/your-feature-name
    ```
3. Commit your changes:
    ```sh
    git commit -m 'Add some feature'
    ```
4. Push to the branch:
    ```sh
    git push origin feature/your-feature-name
    ```
5. Create a new Pull Request.

## License
MIT