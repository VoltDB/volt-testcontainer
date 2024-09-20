# volt-testcontainer

## Overview
`volt-testcontainer` is a project designed to integrate and test with VoltDB using containerized environments. This project includes various integration tests and procedures to ensure reliable and efficient interaction with the VoltDB database.

## Key Features
- **Integration Tests**: A set of integration tests to validate the functionalities of VoltDB in a containerized setup.
- **Database Procedures**: Custom procedures for initializing and interacting with the VoltDB database.
- **Schema Management**: Schema definitions and management tailored for VoltDB.
- **Utility Classes**: Helper classes for generating data and managing test cases.

## Installation and Setup
### Prerequisites
- **Java Development Kit (JDK) 17**
- **Maven**: For managing project dependencies and builds.
- **Docker**: Required for containerizing the VoltDB instances.

### Steps
1. Clone the repository:
    ```sh
    git clone <repository-url>
    cd volt-testcontainer
    ```

2. Build the project:
    ```sh
    mvn -DskipTests clean package
    ```

3. Running Tests: To run these tests you must have a VoltDB license to be placed as /tmp/voltdb-license.xml
    ```sh
    mvn test
    ```

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

These procedures can be found in the `src/main/java/voter/procedures/` directory.

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