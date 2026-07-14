# GreenMail Copilot Instructions

## Build & Test Commands

### Build from source
```bash
./mvnw clean install -Pdocker
```
- `-Pdocker` includes Docker image build (optional, omit to skip)
- Requires JDK 11+ and Maven 3.9.x
- Default goal is `install`

### Run tests
```bash
# All tests (unit + integration)
./mvnw clean verify

# Skip integration tests for faster feedback
./mvnw clean verify -DskipITs

# Run a specific test class
./mvnw test -Dtest=ImapServerTest

# Run a specific test method
./mvnw test -Dtest=ImapServerTest#testRetrieveSimple

# Run integration tests only
./mvnw verify -DskipTests
```

### Generate site documentation
```bash
mvn site -Psite
```

### Code quality
- Checkstyle is configured and runs during builds
- PMD and FindBugs plugins are available in pom.xml
- Javadoc generation is part of the build

## Architecture Overview

GreenMail is a virtual mail server for testing SMTP, IMAP, and POP3 protocols. It's structured as a multi-module Maven project:

### Core Modules
- **greenmail-core**: Main mail server implementation with protocol handlers (SMTP/IMAP/POP3), user management, mail storage
- **greenmail-junit4/junit5**: Test framework integration (GreenMailRule for JUnit4, extensions for JUnit5)
- **greenmail-spring**: Spring Framework integration
- **greenmail-standalone**: Runnable standalone server
- **greenmail-webapp**: Web UI for managing the server

### Key Packages in greenmail-core
- `smtp/`, `imap/`, `pop3/`: Protocol-specific implementations
- `store/`: Mail storage and retrieval
- `user/`: User and mailbox management
- `configuration/`: Server setup and configuration
- `util/`: Utilities including GreenMailUtil and Retriever for common operations

### Version Alignment
- **2.1.x**: Jakarta EE 10 (JakartaMail 2.1.x), Java 11+
- **2.0.x**: Jakarta EE 9 (JakartaMail 2.0.x), Java 11+
- **1.6.x**: Jakarta EE 8 (JakartaMail 1.6.x), Java 8+

## Key Conventions

### Testing
- **Unit tests**: Named `*Test.java` in `src/test/java`
- **Integration tests**: Named `*IT.java` in `src/test/java`
- **Test fixture**: Use `GreenMailRule` (JUnit4) or `GreenMailExtension` (JUnit5)
- **Assertions**: Use AssertJ (`assertThat()`) for readable assertions
- **Common setup**: `ServerSetupTest` provides pre-configured server setups (SMTP, IMAPS, IMAP, POP3, etc.)

Example (JUnit4):
```java
@Rule
public final GreenMailRule greenMail = new GreenMailRule(
    new ServerSetup[]{ServerSetupTest.SMTP, ServerSetupTest.IMAP}
);

@Test
public void testSendAndRetrieve() throws Exception {
    GreenMailUtil.sendTextEmail("to@test.com", "from@test.com", 
        "subject", "body", greenMail.getSmtp());
    // assertions...
}
```

### Package Organization
Each protocol handler follows a consistent pattern:
- `commands/`: Protocol command implementations
- `commands/parser/`: Command parsing logic
- Protocol state management and protocol handler implementations

### Import Statements
Use Jakarta EE imports (not javax.*):
- `jakarta.mail.*`
- `jakarta.activation.*`
- Angus Mail implementation: `org.eclipse.angus.mail.*`

### Configuration
- Properties-based configuration via `PropertiesBasedGreenMailConfigurationBuilder`
- GreenMailRule handles server startup/teardown
- ServerSetup objects define protocol + port combinations

## Contribution Guidelines

Before implementing changes, review [CONTRIBUTING.md](../../CONTRIBUTING.md):

- **Keep patches focused**: Avoid mixing unrelated changes
- **Include tests**: Every bug fix or feature should include a test case
- **Minimize dependencies**: GreenMail prioritizes being lightweight
- **Backward compatibility**: Be aware that changes may affect multiple versions/branches
- **Avoid cosmetic changes**: Refactoring and reformatting alone won't be accepted without prior discussion
- **Branches**: Active branches are `master` (2.1), `releases/2.0.x`, and `releases/1.6.x`

## CI/CD

The CI pipeline in `.github/workflows/ci.yml`:
- Runs on Java 11, 17, and 21
- Builds with Docker profile enabled
- Runs on push to master, release branches, and all PRs
- Uses Maven caching for faster builds
