# sbt-openapi-client-http4s

An SBT plugin and Scala core library for generating Scala 3 REST API clients from OpenAPI/Swagger specifications.

## Features

- Generates Scala 3 code compatible with the latest LTS version
- Uses Circe for JSON handling
- Utilizes opaque types for primitive wrappers
- Includes comprehensive test suite with Scalatest

## Project Structure

The project consists of two main components:

1. **Core Library**: A Scala 2.12 library that handles OpenAPI specification parsing and code generation
2. **SBT Plugin**: An SBT plugin that wraps the core library functionality for easy use in SBT projects

## Getting Started

_Note: This project is currently in development._

### Prerequisites

- SBT 1.9.x
- Scala 2.12.x (for development)
- Scala 3.x (for generated client usage)

## Development

This project is structured in phases:

1. Project Setup (core structure, build files)
2. Swagger/OpenAPI Parsing Library
3. Code Generation Library
4. Additional OpenAPI Spec Implementation
5. SBT Plugin Wrapper

## License

This project is licensed under the MIT License - see the LICENSE file for details.
