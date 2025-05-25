# Copilot Instructions

This document provides guidance for assisting in the development of an sbt plugin and Scala core library aimed at generating Scala 3 REST API clients. The project emphasizes compatibility with the latest LTS Scala 3, Circe for JSON handling, opaque types for primitive wrappers, and Scalatest for testing. The code generator itself must be implemented in Scala 2.12 for sbt plugin compatibility.

## Project Overview

The overall project is divided into sequential steps. Clearly indicate the current step when prompting for assistance.

### Step 1: Project Setup *(Completed)*

* **Location:** `core/`
* **Scala Version:** Scala 2.12 (for code generator)
* **Tasks:**

  * Set up project structure including sbt build files, dependencies, and project layout
  * Prepare initial directory structure and necessary configuration

### Step 2: Swagger Parsing Library *(In Progress)*

* **Tasks:**

  * Implement Swagger/OpenAPI specification parsing using `io.swagger.parser.v3:swagger-parser:2.1.28`
  * Verify correctness and completeness of parsed data with initial tests

### Step 3: Code Generation Library

* **OpenAPI Spec:** `core/src/test/resources/specs/petstore_3.0.4.yml`
* **Tasks:**

  * Generate Scala 3 REST API client code using Circe and opaque types for primitives
  * Write thorough unit tests with Scalatest to verify generated client correctness

### Step 4: Additional OpenAPI Spec Implementation

* **OpenAPI Spec:** `core/src/test/resources/specs/cosmology.yml`
* **Tasks:**

  * Extend core generator to handle this specification
  * Implement necessary code adjustments for this spec
  * Write unit tests validating correctness and completeness of the generated clients

### Step 5: SBT Plugin Wrapper

* **Location:** Dedicated sbt plugin module
* **Tasks:**

  * Wrap the existing core generation functionality within an sbt plugin
  * Ensure ease of use and proper integration with sbt workflows
