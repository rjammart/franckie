# GitHub Actions Workflows

This directory contains CI/CD workflows for the Franckie Validation Framework.

## Workflows

### 1. CI (`ci.yml`)

**Triggers:**
- Push to `main`, `master`, `develop`, or `custom-predicate` branches
- Pull requests to `main`, `master`, or `develop`

**What it does:**
- ✅ Builds the project with Maven
- ✅ Runs all tests
- ✅ Generates test reports
- ✅ Uploads test results as artifacts
- ✅ Publishes test report (on ubuntu-latest only)
- ✅ Caches Maven dependencies for faster builds

**Configuration:**
- Java: 21 (Temurin distribution)
- OS: Ubuntu Latest (configurable for multi-OS testing)
- Maven: Uses cached dependencies

**Customization:**
To test on multiple operating systems, uncomment lines in the matrix:
```yaml
matrix:
  os: [ ubuntu-latest, macos-latest, windows-latest ]
```

### 2. Pull Request Checks (`pull-request.yml`)

**Triggers:**
- Pull requests to `main`, `master`, or `develop`

**What it does:**
- ✅ Validates PR can be built
- ✅ Runs all tests
- ✅ Checks test coverage
- ✅ Comments test results directly on the PR
- ❌ Fails if no tests are found

**Use case:** Ensures PRs meet quality standards before merging.

### 3. Release (`release.yml`)

**Triggers:**
- Push of version tags (e.g., `v1.0.0`, `v0.1.0`)

**What it does:**
- ✅ Updates Maven version to match tag
- ✅ Builds and packages JARs
- ✅ Runs tests
- ✅ Creates GitHub Release (draft)
- ✅ Attaches JAR artifacts to release
- ✅ Auto-generates release notes with documentation links

**How to use:**
```bash
# Create and push a version tag
git tag v1.0.0
git push origin v1.0.0

# GitHub will automatically:
# 1. Build the project
# 2. Run tests
# 3. Create a draft release
# 4. Attach JARs
```

Then manually:
- Edit the draft release on GitHub
- Update the changelog/release notes
- Publish the release

### 4. CodeQL Security Analysis (`codeql.yml`)

**Triggers:**
- Push to `main`, `master`, or `develop`
- Pull requests to `main`, `master`, or `develop`
- Weekly schedule (Mondays at 9:00 AM UTC)

**What it does:**
- 🔒 Scans code for security vulnerabilities
- 🔍 Performs quality analysis
- 📊 Reports findings in GitHub Security tab

**Use case:** Automated security scanning to catch vulnerabilities early.

## Workflow Status Badges

Add these badges to your `README.md`:

```markdown
![CI](https://github.com/YOUR_USERNAME/franckie/workflows/CI/badge.svg)
![CodeQL](https://github.com/YOUR_USERNAME/franckie/workflows/CodeQL%20Security%20Analysis/badge.svg)
```

Replace `YOUR_USERNAME` with your GitHub username/organization.

## Local Testing

Test your workflows locally using [act](https://github.com/nektos/act):

```bash
# Install act
brew install act  # macOS
# or
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash  # Linux

# Run CI workflow
act push

# Run PR workflow
act pull_request

# List available workflows
act -l
```

## Secrets Required

### For Release Workflow

- `GITHUB_TOKEN` - Auto-provided by GitHub Actions, no setup needed

### For Maven Central Publishing (Future)

If you plan to publish to Maven Central, add these secrets in GitHub:
- `OSSRH_USERNAME` - Sonatype OSSRH username
- `OSSRH_TOKEN` - Sonatype OSSRH password
- `GPG_PRIVATE_KEY` - GPG private key for signing
- `GPG_PASSPHRASE` - GPG key passphrase

## Troubleshooting

### Build fails with "Java version not supported"

Ensure Java 21 is specified in all workflows:
```yaml
- uses: actions/setup-java@v4
  with:
    java-version: '21'
    distribution: 'temurin'
```

### Tests not found

Check that Maven Surefire is configured in `pom.xml`:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
</plugin>
```

### Cache not working

Maven cache is automatic with `actions/setup-java@v4` and `cache: 'maven'`.
If issues persist, clear cache manually in GitHub Actions settings.

### CodeQL fails on annotation processors

This is expected for projects with annotation processors. CodeQL will still
analyze the generated code. No action needed.

## Performance Tips

1. **Use cache**: Already enabled for Maven dependencies
2. **Parallel tests**: Add to `pom.xml`:
   ```xml
   <configuration>
       <parallel>methods</parallel>
       <threadCount>4</threadCount>
   </configuration>
   ```
3. **Skip unnecessary modules**: Use `-pl` flag to test specific modules only

## Customization

### Add test coverage reporting

Install JaCoCo plugin and add to CI:

```yaml
- name: Generate coverage report
  run: mvn jacoco:report

- name: Upload coverage to Codecov
  uses: codecov/codecov-action@v3
  with:
    files: ./validation-core/target/site/jacoco/jacoco.xml
```

### Add dependency vulnerability scanning

Add Dependabot configuration in `.github/dependabot.yml`:

```yaml
version: 2
updates:
  - package-ecosystem: "maven"
    directory: "/"
    schedule:
      interval: "weekly"
```

### Matrix build with multiple Java versions

```yaml
strategy:
  matrix:
    java: [ '21', '22' ]
```

## Contributing

When adding new workflows:
1. Test locally with `act` first
2. Document the workflow in this README
3. Add appropriate status badges to main README
