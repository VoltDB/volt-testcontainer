# Volt Test Container - Versioning and Release

## Updating the version

Since this project has a parent pom.xml file and 4 separate modules, it is easier to set the version using this maven command:

    ```sh
    mvn versions:set -DnewVersion=1.6.0-SNAPSHOT
    mvn versions:commit
    ```
That updates the version in all the pom.xml files and saves them to disk, and is easier and less error-prone than editing them manually.

## Release

### Pre-Release Checklist:

 - The version should be set to the next release without "-SNAPSHOT", e.g. 1.5.0
 - In the top-level pom.xml:
    - set volt-procedure-api.version property to latest published release, e.g. 15.0.0
    - set voltdbclient.version to the latest published release, e.g. 15.0.0


 - (Optional) Create a release branch:
    ```sh
    git clone git@github.com:VoltDB/volt-testcontainer.git
    cd volt-testcontainer

    git checkout main
    git pull

    git checkout -b release-1.5.x
    git push -u origin release-1.5.x
    ```

 - Manually build & test

    ```sh
    mvn verify
    ```


### Tag & Release


 - Create a release tag

    ```sh
    git tag -a v1.5.0 -m "Tagging v1.5.0 release"
    git push origin v1.5.0
    ```

 - Run the Release job in Jenkins
   - TAG: v1.5.0

 - Login to Sonatype, verify the artifact is Validated, then click Publish


## After Release

To set things up for the next releases:

- In the main branch, set the version to the next planned release, which may be the next major or minor version.

    ```sh
    mvn versions:set -DnewVersion=1.6.0-SNAPSHOT
    mvn versions:commit
    ```

- If you created a release branch, you can use the following command to increment to the next version:
    ```sh
    mvn versions:set -DnextSnapshot
    mvn versions:commit
    ```

   Note: -DnextSnapshot increments the patch version and adds -SNAPSHOT, e.g. it would change 1.5.0 to 1.5.1-SNAPSHOT
