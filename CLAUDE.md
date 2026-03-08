# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 4.0 application demonstrating validation concepts, built with Java 25 and using Spring Modulith architecture. The application models a course/competence validation system with PostgreSQL persistence.

## Core Architecture

### Domain Structure
The application follows a modular structure with two main bounded contexts:
- `course/` - Contains `Course` and `Competence` domain models
- `session/` - Contains `CourseSession` and `Instructor` models

### Key Domain Models
- `Course` - Record containing name, required competences, and acquired competence
- `Competence` - Sealed interface with implementations: JumpMasterCompetence, MedicalCompetence, SlackLineCompetence, HACompetence
- `CourseSession` - Simple record for course sessions
- `Instructor` - Currently empty record placeholder

### Technology Stack
- **Spring Boot 4.0** with Spring Modulith for modular architecture
- **Java 25** with Records and Sealed Interfaces
- **PostgreSQL** with Spring Data JDBC, JOOQ, and Flyway migrations
- **Lombok** for annotation processing
- **Testcontainers** for integration testing
- **Maven** for build management

## Development Commands

### Build and Run
```bash
# Build the application
./mvnw clean compile

# Run the application
./mvnw spring-boot:run

# Run application with test containers for development
./mvnw test-compile exec:java -Dexec.mainClass="learning.validationpoc.TestValidationPocApplication"

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=ValidationPocApplicationTests

# Create native image
./mvnw spring-boot:build-image
```

### Database Operations
The application uses Docker Compose for local PostgreSQL:
```bash
# Start PostgreSQL container (automatic with spring-boot-docker-compose)
docker-compose up -d

# Database migrations are handled by Flyway automatically
```

### Testing Strategy
- **Testcontainers Integration**: `TestcontainersConfiguration` provides PostgreSQL container for tests
- **Test Application**: `TestValidationPocApplication` runs the app with test containers
- **Spring Modulith** testing support for module boundary validation
- All tests use `@Import(TestcontainersConfiguration.class)` for database access

### Test Naming Convention
Follow this naming pattern for all test methods:
- **Method name**: `given__[condition]__when__[action]__then__[expected_result]`
- **Display name**: Clean readable text with spaces (no underscores)

Example:
```java
@Test
@DisplayName("given acquired competence not in required competences when validate then pass")
void given__acquired_competence_not_in_required_competences__when__validate__then__pass() {
    // Test implementation
}
```

This convention ensures:
- Clear separation of test conditions, actions, and expectations
- Readable test reports with proper display names
- Consistent test structure across the codebase

## Key Configuration
- Application name: `validationPOC`
- Database: PostgreSQL (configured in `compose.yaml`)
- Native image support enabled with Paketo buildpacks
- Maven compiler configured for Lombok annotation processing