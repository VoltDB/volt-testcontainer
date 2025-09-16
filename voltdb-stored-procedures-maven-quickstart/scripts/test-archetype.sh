#!/bin/bash
# scripts/test-archetype.sh

###############################################################################
# Archetype Generation & Test Script
#
# Prerequisites:
# 1. First install the parent project:
#    cd .. && mvn clean install
# 2. Then run this script to generate the archetype and run its build and tests
#    Be sure to set VOLTDB_LICENSE or copy your license file to ~/license.xml
#
# Usage:
#   ./test-archetype.sh
#
# The generated test project will be created in: scripts/test-generated-project/
# This directory is .gitignore'd and will be cleaned up on subsequent runs.
# If the test fails, the generated project remains for inspection.
###############################################################################

set -e  # Exit on any error

ARCH_GROUP_ID=org.voltdb
ARCH_ARTIFACT_ID=voltdb-stored-procedures-maven-quickstart
PROJECT_GROUP_ID=org.example.test
PROJECT_ARTIFACT_ID=test-generated-project
PROJECT_VERSION=1.0.0-SNAPSHOT
GENERATED_PROJECT_DIR=$PROJECT_ARTIFACT_ID

# Clean up previous test run
if [ -d "$GENERATED_PROJECT_DIR" ]; then
    echo "Cleaning up previous test project..."
    rm -rf $PROJECT_ARTIFACT_ID
fi

# Verify archetype is built
if [ ! -f "../target/classes/META-INF/maven/archetype-metadata.xml" ]; then
    echo "ERROR: Archetype not built. Please run first:"
    echo "  cd .. && mvn clean package"
    echo "Then run this script again."
    exit 1
fi

# Set VOLTDB_LICENSE if not already set, validate file exists
if [ -z "${VOLTDB_LICENSE:-}" ]; then
    VOLTDB_LICENSE="$HOME/license.xml"
fi
if [ ! -f "$VOLTDB_LICENSE" ]; then
    echo "ERROR: VoltDB license file not found: $VOLTDB_LICENSE"
    echo "Set VOLTDB_LICENSE environment variable or copy license file to $HOME/license.xml"
    exit 1
fi
export VOLTDB_LICENSE
echo "Using VoltDB license: $VOLTDB_LICENSE"

# Get the current version from the archetype POM
VERSION=$(cd ".." && mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "Testing archetype version: $VERSION"
echo

mvn archetype:generate \
    -DarchetypeGroupId=$ARCH_GROUP_ID \
    -DarchetypeArtifactId=$ARCH_ARTIFACT_ID \
    -DarchetypeVersion=$VERSION \
    -DarchetypeCatalog=local \
    -DgroupId=$PROJECT_GROUP_ID \
    -DartifactId=$PROJECT_ARTIFACT_ID \
    -Dversion=$PROJECT_VERSION \
    -DinteractiveMode=false


cd $PROJECT_ARTIFACT_ID
mvn -DskipTests=true clean package
mvn test
cd ..
