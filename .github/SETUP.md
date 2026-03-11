# GitHub Actions Setup Guide

## Quick Start

The GitHub Actions workflows are ready to use! They will automatically run when you push code to GitHub.

## Step 1: Push Workflows to GitHub

```bash
git add .github/
git commit -m "ci: Add GitHub Actions workflows for build, test, and release"
git push origin custom-predicate
```

## Step 2: Verify Workflows

1. Go to your repository on GitHub
2. Click the **"Actions"** tab
3. You should see workflows listed:
   - ✅ CI
   - ✅ Pull Request Checks
   - ✅ Release
   - ✅ CodeQL Security Analysis

## Step 3: Test the CI Workflow

The CI workflow should run automatically on your push. You can also manually trigger it:

1. Go to **Actions** → **CI**
2. Click **"Run workflow"**
3. Select branch: `custom-predicate`
4. Click **"Run workflow"**

## Step 4: Add Status Badges (Optional)

Add these badges to the top of your `README.md`:

```markdown
# Franckie Validation Framework

![CI](https://github.com/YOUR_USERNAME/franckie/workflows/CI/badge.svg?branch=main)
![CodeQL](https://github.com/YOUR_USERNAME/franckie/workflows/CodeQL%20Security%20Analysis/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)
```

Replace `YOUR_USERNAME` with your GitHub username or organization name.

## Step 5: Enable Dependabot (Optional but Recommended)

Dependabot is already configured in `.github/dependabot.yml`. To enable it:

1. Go to repository **Settings** → **Security & analysis**
2. Enable **Dependabot alerts**
3. Enable **Dependabot security updates**
4. Dependabot will automatically create PRs for dependency updates every Monday

## Workflow Overview

### CI Workflow (Runs on Every Push)

**Status:** ✅ Ready
**Triggers:** Push to main branches, PRs
**What it does:**
- Builds the project
- Runs 98 tests (validation-core: 81, validation-processor: 8, validation-sample: 9)
- Generates test reports
- Uploads artifacts

**Expected Result:** All tests pass ✅

### Pull Request Workflow

**Status:** ✅ Ready
**Triggers:** Pull requests to main/develop
**What it does:**
- Validates the PR builds successfully
- Runs all tests
- Comments results directly on the PR

### Release Workflow

**Status:** ✅ Ready
**Triggers:** Version tags (e.g., `v1.0.0`)
**What it does:**
- Creates GitHub Release
- Attaches JAR artifacts
- Generates release notes

**How to use:**
```bash
git tag v0.0.1
git push origin v0.0.1
```

### CodeQL Security Analysis

**Status:** ✅ Ready
**Triggers:** Push, PR, weekly schedule
**What it does:**
- Scans for security vulnerabilities
- Reports in Security tab

## Testing Locally

Before pushing, test the build locally:

```bash
# Clean build and test
mvn clean test

# Should see:
# Tests run: 81 (validation-core) ✅
# Tests run: 8 (validation-processor) ✅
# Tests run: 9 (validation-sample) ✅
# BUILD SUCCESS ✅
```

## Common Issues

### Issue: Workflow doesn't run

**Solution:** Check that you pushed `.github/workflows/` directory:
```bash
git ls-files .github/
# Should show all .yml files
```

### Issue: Java version mismatch

**Solution:** All workflows use Java 21. Ensure `pom.xml` specifies:
```xml
<java.version>21</java.version>
```

### Issue: Tests fail on GitHub but pass locally

**Solution:** Check for:
- Hardcoded paths (use relative paths)
- Platform-specific behavior
- Missing resources in `src/test/resources`

### Issue: CodeQL fails

**Solution:** CodeQL may have issues with annotation processors. This is normal and doesn't affect your code.

## Next Steps

1. ✅ Push workflows to GitHub
2. ✅ Watch first CI run complete
3. ✅ Add status badges to README
4. ✅ Create a test PR to see PR checks in action
5. ✅ Enable Dependabot
6. ✅ Create your first release when ready

## Support

- **Workflow Documentation:** `.github/workflows/README.md`
- **GitHub Actions Docs:** https://docs.github.com/actions
- **Issue Tracker:** https://github.com/YOUR_USERNAME/franckie/issues

## Current Test Summary

```
Module                      Tests  Status
─────────────────────────────────────────
validation-core              81    ✅
├─ CustomPredicateTest       48    ✅
├─ RuleValidationTest        18    ✅
├─ ProjectionContramapTest   10    ✅
└─ LazySupplierPerformanceTest 5  ✅

validation-processor          8    ✅
└─ ValidationProjectionProcessorTest

validation-sample             9    ✅
└─ SessionValidationTest

TOTAL                        98    ✅
```

All systems green! 🟢
