#!/bin/bash
set -e  # Exit on error

echo "Building core project first..."
sbt core/compile

rm -rf generated/petstore
echo "Generating Petstore client..."
sbt "core/runMain io.github.er1c.openapi.cli.Main \
  --spec core/src/test/resources/specs/petstore_3.0.4.yml \
  --outputDir generated \
  --packageName io.github.er1c.generated \
  --projectName petstore \
  --fullProject"

echo "Building Petstore client..."
(cd generated/petstore && sbt test)

rm -rf generated/cosmology
echo "Generating Cosmology client..."
sbt "core/runMain io.github.er1c.openapi.cli.Main \
  --spec core/src/test/resources/specs/cosmology.yml \
  --outputDir generated \
  --packageName io.github.er1c.generated \
  --projectName cosmology \
  --fullProject"

echo "Building Cosmology client..."
(cd generated/cosmology && sbt test)

echo "All clients generated and built successfully!"
