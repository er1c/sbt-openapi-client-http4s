#!/bin/bash

# Script to run the OpenAPI client generator CLI

# Ensure sbt is in the PATH
if ! command -v sbt &> /dev/null
then
    echo "sbt could not be found, please install it or add it to your PATH."
    exit 1
fi

# Base directory of the project (where build.sbt is located)
PROJECT_BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Output directories
PETSTORE_OUTPUT_DIR="${PROJECT_BASE_DIR}/generated/petstore"
COSMOLOGY_OUTPUT_DIR="${PROJECT_BASE_DIR}/generated/cosmology"

# Ensure output directories exist
mkdir -p "${PETSTORE_OUTPUT_DIR}"
mkdir -p "${COSMOLOGY_OUTPUT_DIR}"

# Package name
PACKAGE_NAME="io.github.er1c.generated"

# OpenAPI specification files
PETSTORE_SPEC="${PROJECT_BASE_DIR}/core/src/test/resources/specs/petstore_3.0.4.yml"
COSMOLOGY_SPEC="${PROJECT_BASE_DIR}/core/src/test/resources/specs/cosmology.yml"

# Run for Petstore
echo "Generating client for Petstore..."
sbt "core/run --spec ${PETSTORE_SPEC} --outputDir ${PETSTORE_OUTPUT_DIR} --packageName ${PACKAGE_NAME}.petstore"

# Run for Cosmology
echo "Generating client for Cosmology..."
sbt "core/run --spec ${COSMOLOGY_SPEC} --outputDir ${COSMOLOGY_OUTPUT_DIR} --packageName ${PACKAGE_NAME}.cosmology"

echo "Script finished."

