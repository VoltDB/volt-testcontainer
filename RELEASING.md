# Volt Test Container - Release Instructions

## Release Process

1. Create a release branch (skip if doing a patch release)

    ```sh
    git checkout main
    git pull
    git checkout -b release-1.11.x
    ```

1. Set the version (without - SNAPSHOT)

Since this project has 4 modules and a parent pom.xml, the easiest way is using the `mvn versions` command:

    ```sh
    mvn versions:set -DnewVersion=1.11.0
    mvn versions:commit
    ```

Check if you want to update the versions of any of the dependencies

 - In the top-level pom.xml:
    - set volt-procedure-api.version property to latest published release, e.g. 15.0.0
    - set voltdbclient.version to the latest published release, e.g. 15.0.0

1. Manually build & test (set path to your VoltDB license)

    ```sh
    export VOLTDB_LICENSE=~/license.xml
    mvn clean install
    ```

1. Commit this change and push to the branch.

    ```sh
    git add pom.xml */pom.xml
    git commit -m "Setting version to 1.11.0"
    git push -u origin release-1.11.x
    ```

1. Create the release tag

    ```sh
    git tag -a v1.11.0 -m "Tagging v1.11.0 release"
    git push origin v1.11.0
    ```

1. Run the Release [job](https://voltdb-devs-ci.voltactivedata.com/job/testcontainer/job/Release/) in Jenkins
   - TAG: v1.11.0

1. Login to Sonatype, verify the artifact is Validated, then click Publish


## After Release

To set things up for the next releases:

- In the main branch, set the version to the next planned release, which may be the next major or minor version.

    ```sh
    git checkout main
    mvn versions:set -DnewVersion=1.12.0-SNAPSHOT
    mvn versions:commit
    ```

