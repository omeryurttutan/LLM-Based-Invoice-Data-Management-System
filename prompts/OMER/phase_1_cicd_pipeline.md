# PHASE 1: CI/CD PIPELINE SETUP

## CONTEXT AND BACKGROUND

You are working on "Fatura OCR ve Veri Yönetim Sistemi" (Invoice OCR and Data Management System), a graduation project for a Turkish university. This system automates invoice data extraction from images and e-Invoice XML files using LLM-based analysis.

### Project Overview
- **Project Name**: Fatura OCR ve Veri Yönetim Sistemi
- **Team**: Muhammed Furkan Akdağ (AI/LLM) & Ömer Talha Yurttutan (Web)
- **Architecture**: Hybrid (Modular Monolith + Microservice)
  - Backend: Spring Boot 3.2+ (Java 17)
  - Frontend: Next.js 14+ (TypeScript)
  - Extraction Service: Python 3.11+ FastAPI

### Current State
**Phase 0 has been completed.** The development environment is now ready:
- ✅ Docker Compose configured with all services
- ✅ PostgreSQL, Redis, RabbitMQ running
- ✅ Backend Spring Boot skeleton created
- ✅ Frontend Next.js skeleton created
- ✅ Extraction Service FastAPI skeleton created
- ✅ All services start with `docker-compose up`
- ✅ Git repository initialized with `main` and `develop` branches

### Phase Assignment
- **Assigned To**: ÖMER (Web Developer)
- **Estimated Duration**: 1-2 days

---

## OBJECTIVE

Set up a comprehensive CI/CD pipeline using GitHub Actions that automatically runs linting, testing, and building for all three services (Backend, Frontend, Extraction Service) on every push. The pipeline should enforce code quality standards and prevent broken code from being merged.

---

## DETAILED REQUIREMENTS

### 1. GitHub Actions Workflow Structure

**Purpose**: Create a well-organized workflow that handles all services efficiently.

**Directory Structure**:
```
.github/
├── workflows/
│   ├── ci.yml                 # Main CI pipeline
│   ├── backend.yml            # Backend-specific jobs (optional, can be in ci.yml)
│   ├── frontend.yml           # Frontend-specific jobs (optional, can be in ci.yml)
│   └── extraction.yml         # Extraction service jobs (optional, can be in ci.yml)
├── CODEOWNERS                 # Code ownership rules
└── pull_request_template.md   # PR template
```

**Recommended Approach**: Single `ci.yml` file with multiple jobs for simplicity in Phase 1.

---

### 2. Main CI Workflow (ci.yml)

**File**: `.github/workflows/ci.yml`

**Trigger Events**:
```yaml
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]
```

**Workflow Structure**:
```yaml
name: CI Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

env:
  JAVA_VERSION: '17'
  NODE_VERSION: '20'
  PYTHON_VERSION: '3.11'

jobs:
  # Job 1: Backend
  backend:
    # ...
  
  # Job 2: Frontend
  frontend:
    # ...
  
  # Job 3: Extraction Service
  extraction:
    # ...
  
  # Job 4: Integration check (runs after all)
  integration:
    needs: [backend, frontend, extraction]
    # ...
```

---

### 3. Backend CI Job (Spring Boot)

**Purpose**: Lint, test, and build the Spring Boot backend.

**Job Configuration**:
```yaml
backend:
  name: Backend CI
  runs-on: ubuntu-latest
  defaults:
    run:
      working-directory: ./backend
  
  steps:
    - name: Checkout code
      uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    
    - name: Cache Maven packages
      uses: actions/cache@v4
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        restore-keys: ${{ runner.os }}-m2
    
    - name: Run Checkstyle
      run: mvn checkstyle:check
    
    - name: Run SpotBugs
      run: mvn spotbugs:check
    
    - name: Run Tests
      run: mvn test
    
    - name: Build Application
      run: mvn package -DskipTests
    
    - name: Upload Test Results
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: backend-test-results
        path: backend/target/surefire-reports/
```

**Required Maven Plugins** (add to backend/pom.xml):
```xml
<build>
  <plugins>
    <!-- Checkstyle Plugin -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-checkstyle-plugin</artifactId>
      <version>3.3.1</version>
      <configuration>
        <configLocation>checkstyle.xml</configLocation>
        <consoleOutput>true</consoleOutput>
        <failsOnError>true</failsOnError>
        <violationSeverity>warning</violationSeverity>
      </configuration>
    </plugin>
    
    <!-- SpotBugs Plugin -->
    <plugin>
      <groupId>com.github.spotbugs</groupId>
      <artifactId>spotbugs-maven-plugin</artifactId>
      <version>4.8.3.0</version>
      <configuration>
        <effort>Max</effort>
        <threshold>Medium</threshold>
        <failOnError>true</failOnError>
      </configuration>
    </plugin>
  </plugins>
</build>
```

**Create Checkstyle Configuration** (backend/checkstyle.xml):
```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
  "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
  "https://checkstyle.org/dtds/configuration_1_3.dtd">

<module name="Checker">
  <property name="charset" value="UTF-8"/>
  <property name="severity" value="warning"/>
  
  <module name="TreeWalker">
    <!-- Naming Conventions -->
    <module name="ConstantName"/>
    <module name="LocalVariableName"/>
    <module name="MemberName"/>
    <module name="MethodName"/>
    <module name="PackageName"/>
    <module name="ParameterName"/>
    <module name="TypeName"/>
    
    <!-- Import Rules -->
    <module name="AvoidStarImport"/>
    <module name="UnusedImports"/>
    
    <!-- Code Style -->
    <module name="EmptyBlock"/>
    <module name="LeftCurly"/>
    <module name="RightCurly"/>
    <module name="NeedBraces"/>
    
    <!-- Best Practices -->
    <module name="EqualsHashCode"/>
    <module name="SimplifyBooleanExpression"/>
    <module name="StringLiteralEquality"/>
  </module>
  
  <!-- File Length -->
  <module name="FileLength">
    <property name="max" value="500"/>
  </module>
  
  <!-- Line Length -->
  <module name="LineLength">
    <property name="max" value="120"/>
  </module>
</module>
```

---

### 4. Frontend CI Job (Next.js)

**Purpose**: Lint, type-check, test, and build the Next.js frontend.

**Job Configuration**:
```yaml
frontend:
  name: Frontend CI
  runs-on: ubuntu-latest
  defaults:
    run:
      working-directory: ./frontend
  
  steps:
    - name: Checkout code
      uses: actions/checkout@v4
    
    - name: Set up Node.js
      uses: actions/setup-node@v4
      with:
        node-version: '20'
        cache: 'npm'
        cache-dependency-path: frontend/package-lock.json
    
    - name: Install dependencies
      run: npm ci
    
    - name: Run ESLint
      run: npm run lint
    
    - name: Run TypeScript check
      run: npm run type-check
    
    - name: Run Tests
      run: npm run test --passWithNoTests
    
    - name: Build Application
      run: npm run build
    
    - name: Upload Build Artifacts
      uses: actions/upload-artifact@v4
      with:
        name: frontend-build
        path: frontend/.next/
```

**Required Scripts** (add to frontend/package.json):
```json
{
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "lint": "next lint",
    "lint:fix": "next lint --fix",
    "type-check": "tsc --noEmit",
    "test": "jest",
    "test:watch": "jest --watch",
    "test:coverage": "jest --coverage"
  }
}
```

**ESLint Configuration** (frontend/.eslintrc.json):
```json
{
  "extends": [
    "next/core-web-vitals",
    "plugin:@typescript-eslint/recommended"
  ],
  "parser": "@typescript-eslint/parser",
  "plugins": ["@typescript-eslint"],
  "rules": {
    "@typescript-eslint/no-unused-vars": "error",
    "@typescript-eslint/no-explicit-any": "warn",
    "no-console": ["warn", { "allow": ["warn", "error"] }],
    "prefer-const": "error",
    "react-hooks/rules-of-hooks": "error",
    "react-hooks/exhaustive-deps": "warn"
  },
  "ignorePatterns": [
    "node_modules/",
    ".next/",
    "out/"
  ]
}
```

**TypeScript Configuration Check** (ensure in frontend/tsconfig.json):
```json
{
  "compilerOptions": {
    "strict": true,
    "noEmit": true,
    "esModuleInterop": true,
    "module": "esnext",
    "moduleResolution": "bundler",
    "resolveJsonModule": true,
    "isolatedModules": true,
    "jsx": "preserve",
    "incremental": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"]
    }
  },
  "include": ["next-env.d.ts", "**/*.ts", "**/*.tsx"],
  "exclude": ["node_modules"]
}
```

---

### 5. Extraction Service CI Job (Python FastAPI)

**Purpose**: Lint, type-check, and test the Python extraction service.

**Job Configuration**:
```yaml
extraction:
  name: Extraction Service CI
  runs-on: ubuntu-latest
  defaults:
    run:
      working-directory: ./extraction-service
  
  steps:
    - name: Checkout code
      uses: actions/checkout@v4
    
    - name: Set up Python
      uses: actions/setup-python@v5
      with:
        python-version: '3.11'
        cache: 'pip'
    
    - name: Install dependencies
      run: |
        python -m pip install --upgrade pip
        pip install -r requirements.txt
        pip install -r requirements-dev.txt
    
    - name: Run Ruff (Linter)
      run: ruff check src/ tests/
    
    - name: Run Ruff (Formatter Check)
      run: ruff format --check src/ tests/
    
    - name: Run MyPy (Type Check)
      run: mypy src/
    
    - name: Run Tests
      run: pytest tests/ -v --tb=short
    
    - name: Upload Test Results
      uses: actions/upload-artifact@v4
      if: always()
      with:
        name: extraction-test-results
        path: extraction-service/pytest-results/
```

**Create Development Requirements** (extraction-service/requirements-dev.txt):
```txt
# Linting & Formatting
ruff==0.1.14

# Type Checking
mypy==1.8.0
types-requests==2.31.0.20240125

# Testing
pytest==7.4.4
pytest-asyncio==0.23.3
pytest-cov==4.1.0
httpx==0.26.0

# Development
black==24.1.1
isort==5.13.2
```

**Ruff Configuration** (extraction-service/pyproject.toml):
```toml
[project]
name = "fatura-ocr-extraction"
version = "0.1.0"
requires-python = ">=3.11"

[tool.ruff]
target-version = "py311"
line-length = 100
select = [
    "E",      # pycodestyle errors
    "W",      # pycodestyle warnings
    "F",      # Pyflakes
    "I",      # isort
    "B",      # flake8-bugbear
    "C4",     # flake8-comprehensions
    "UP",     # pyupgrade
    "ARG",    # flake8-unused-arguments
    "SIM",    # flake8-simplify
]
ignore = [
    "E501",   # line too long (handled by formatter)
    "B008",   # do not perform function calls in argument defaults
]

[tool.ruff.per-file-ignores]
"__init__.py" = ["F401"]
"tests/*" = ["ARG001"]

[tool.ruff.isort]
known-first-party = ["src"]

[tool.mypy]
python_version = "3.11"
warn_return_any = true
warn_unused_ignores = true
disallow_untyped_defs = true
ignore_missing_imports = true

[tool.pytest.ini_options]
testpaths = ["tests"]
asyncio_mode = "auto"
addopts = "-v --tb=short"
```

---

### 6. Integration Check Job

**Purpose**: Ensure all services pass before allowing merge.

**Job Configuration**:
```yaml
integration:
  name: Integration Check
  runs-on: ubuntu-latest
  needs: [backend, frontend, extraction]
  
  steps:
    - name: Checkout code
      uses: actions/checkout@v4
    
    - name: All Checks Passed
      run: |
        echo "✅ All CI checks passed successfully!"
        echo "Backend: ${{ needs.backend.result }}"
        echo "Frontend: ${{ needs.frontend.result }}"
        echo "Extraction: ${{ needs.extraction.result }}"
```

---

### 7. Branch Protection Rules

**Purpose**: Enforce code quality through GitHub branch protection.

**Settings for `main` branch**:
- ✅ Require a pull request before merging
- ✅ Require approvals: 1 (can be 0 for small team)
- ✅ Require status checks to pass before merging
  - Required checks: `backend`, `frontend`, `extraction`, `integration`
- ✅ Require branches to be up to date before merging
- ✅ Do not allow bypassing the above settings

**Settings for `develop` branch**:
- ✅ Require status checks to pass before merging
  - Required checks: `backend`, `frontend`, `extraction`
- ❌ Require approvals (optional for develop)

---

### 8. Pull Request Template

**File**: `.github/pull_request_template.md`

```markdown
## Description
<!-- Describe your changes in detail -->

## Type of Change
- [ ] 🐛 Bug fix (non-breaking change that fixes an issue)
- [ ] ✨ New feature (non-breaking change that adds functionality)
- [ ] 💥 Breaking change (fix or feature that would cause existing functionality to change)
- [ ] 📝 Documentation update
- [ ] 🔧 Configuration change
- [ ] ♻️ Refactoring (no functional changes)

## Related Phase
<!-- Which phase does this PR belong to? -->
- Phase: <!-- e.g., Phase 1 -->
- Assigned to: <!-- FURKAN / ÖMER -->

## Checklist
- [ ] My code follows the project's coding standards
- [ ] I have tested my changes locally
- [ ] I have added/updated tests as needed
- [ ] All CI checks pass
- [ ] I have updated documentation as needed

## Screenshots (if applicable)
<!-- Add screenshots here -->

## Additional Notes
<!-- Any additional information -->
```

---

### 9. CODEOWNERS File

**File**: `.github/CODEOWNERS`

```
# Default owners for everything
* @furkan @omer

# Backend (Spring Boot) - Ömer
/backend/ @omer

# Frontend (Next.js) - Shared, but Ömer leads infrastructure
/frontend/ @furkan @omer

# Extraction Service (Python) - Furkan
/extraction-service/ @furkan

# CI/CD - Ömer
/.github/ @omer

# Documentation - Both
/docs/ @furkan @omer
```

---

### 10. Workflow Status Badges

**Purpose**: Display CI status in README.

**Add to README.md**:
```markdown
# Fatura OCR ve Veri Yönetim Sistemi

![CI Pipeline](https://github.com/{username}/{repo}/actions/workflows/ci.yml/badge.svg)
![Backend](https://github.com/{username}/{repo}/actions/workflows/ci.yml/badge.svg?branch=main&event=push&label=Backend)

<!-- Rest of README -->
```

---

## COMPLETE CI.YML FILE

Here is the complete workflow file to create:

**File**: `.github/workflows/ci.yml`

```yaml
name: CI Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

env:
  JAVA_VERSION: '17'
  NODE_VERSION: '20'
  PYTHON_VERSION: '3.11'

jobs:
  # ============================================
  # BACKEND (Spring Boot)
  # ============================================
  backend:
    name: Backend CI
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: ./backend

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK ${{ env.JAVA_VERSION }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: maven

      - name: Cache Maven packages
        uses: actions/cache@v4
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
          restore-keys: ${{ runner.os }}-m2

      - name: Verify Maven Project
        run: mvn verify -DskipTests

      - name: Run Checkstyle
        run: mvn checkstyle:check
        continue-on-error: false

      - name: Run SpotBugs
        run: mvn spotbugs:check
        continue-on-error: false

      - name: Run Unit Tests
        run: mvn test

      - name: Build JAR
        run: mvn package -DskipTests

      - name: Upload Test Results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: backend-test-results
          path: backend/target/surefire-reports/
          retention-days: 7

  # ============================================
  # FRONTEND (Next.js)
  # ============================================
  frontend:
    name: Frontend CI
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: ./frontend

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Node.js ${{ env.NODE_VERSION }}
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        run: npm ci

      - name: Run ESLint
        run: npm run lint

      - name: Run TypeScript Check
        run: npm run type-check

      - name: Run Tests
        run: npm run test -- --passWithNoTests --ci
        continue-on-error: true

      - name: Build Application
        run: npm run build

      - name: Upload Build Artifacts
        uses: actions/upload-artifact@v4
        with:
          name: frontend-build
          path: frontend/.next/
          retention-days: 7

  # ============================================
  # EXTRACTION SERVICE (Python FastAPI)
  # ============================================
  extraction:
    name: Extraction Service CI
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: ./extraction-service

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Python ${{ env.PYTHON_VERSION }}
        uses: actions/setup-python@v5
        with:
          python-version: ${{ env.PYTHON_VERSION }}
          cache: 'pip'
          cache-dependency-path: extraction-service/requirements*.txt

      - name: Install dependencies
        run: |
          python -m pip install --upgrade pip
          pip install -r requirements.txt
          pip install -r requirements-dev.txt

      - name: Run Ruff Linter
        run: ruff check src/ tests/ --output-format=github

      - name: Run Ruff Formatter Check
        run: ruff format --check src/ tests/

      - name: Run MyPy Type Check
        run: mypy src/ --ignore-missing-imports
        continue-on-error: true

      - name: Run Pytest
        run: pytest tests/ -v --tb=short --junitxml=pytest-results/results.xml
        continue-on-error: true

      - name: Upload Test Results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: extraction-test-results
          path: extraction-service/pytest-results/
          retention-days: 7

  # ============================================
  # INTEGRATION CHECK
  # ============================================
  integration:
    name: Integration Check
    runs-on: ubuntu-latest
    needs: [backend, frontend, extraction]
    if: always()

    steps:
      - name: Check Results
        run: |
          echo "## CI Results Summary" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "| Service | Status |" >> $GITHUB_STEP_SUMMARY
          echo "|---------|--------|" >> $GITHUB_STEP_SUMMARY
          echo "| Backend | ${{ needs.backend.result }} |" >> $GITHUB_STEP_SUMMARY
          echo "| Frontend | ${{ needs.frontend.result }} |" >> $GITHUB_STEP_SUMMARY
          echo "| Extraction | ${{ needs.extraction.result }} |" >> $GITHUB_STEP_SUMMARY

      - name: Verify All Passed
        if: |
          needs.backend.result != 'success' ||
          needs.frontend.result != 'success' ||
          needs.extraction.result != 'success'
        run: |
          echo "❌ One or more checks failed!"
          exit 1

      - name: All Checks Passed
        run: echo "✅ All CI checks passed successfully!"
```

---

## TESTING REQUIREMENTS

### Test 1: Local Workflow Validation
```bash
# Install act (GitHub Actions local runner) - optional
# brew install act (macOS) or see https://github.com/nektos/act

# Validate workflow syntax
cd .github/workflows
cat ci.yml | head -50  # Check YAML structure
```

### Test 2: Push to Trigger CI
```bash
# Create a test branch
git checkout -b test/ci-pipeline

# Make a small change
echo "# CI Test" >> README.md

# Commit and push
git add .
git commit -m "[Phase-1] Test CI pipeline"
git push origin test/ci-pipeline

# Open GitHub and check Actions tab
```

### Test 3: Verify Each Job
1. Go to GitHub → Actions tab
2. Click on the running workflow
3. Verify each job:
   - ✅ Backend job completes
   - ✅ Frontend job completes
   - ✅ Extraction job completes
   - ✅ Integration job completes

### Test 4: Test Failure Detection
```bash
# Introduce a linting error in backend
# Add unused import in Java file
# Push and verify CI fails

# Introduce a linting error in frontend
# Add unused variable in TypeScript
# Push and verify CI fails

# Fix errors and verify CI passes
```

---

## VERIFICATION CHECKLIST

After completing this phase, verify all items:

- [ ] `.github/workflows/ci.yml` file exists and is valid YAML
- [ ] CI workflow triggers on push to main and develop
- [ ] CI workflow triggers on pull requests to main and develop
- [ ] Backend job runs Checkstyle successfully
- [ ] Backend job runs SpotBugs successfully
- [ ] Backend job runs tests successfully
- [ ] Backend job builds JAR successfully
- [ ] Frontend job runs ESLint successfully
- [ ] Frontend job runs TypeScript check successfully
- [ ] Frontend job builds successfully
- [ ] Extraction job runs Ruff linter successfully
- [ ] Extraction job runs Ruff formatter check successfully
- [ ] Extraction job runs MyPy successfully
- [ ] Integration job verifies all jobs passed
- [ ] Pull request template is created
- [ ] CODEOWNERS file is created
- [ ] Backend has checkstyle.xml configuration
- [ ] Frontend has .eslintrc.json configuration
- [ ] Extraction service has pyproject.toml configuration
- [ ] Extraction service has requirements-dev.txt
- [ ] GitHub Actions badge is added to README

---

## RESULT FILE REQUIREMENTS

After completing this phase, create a result file at:
`docs/OMER/step_results/faz_1_result.md`

The result file **MUST** include:

### 1. Execution Status
- Overall status: Success / Partial Success / Failed
- Date completed
- Actual time spent vs estimated (1-2 days)

### 2. Completed Tasks
List each task with checkbox:
- [x] Task completed
- [ ] Task not completed (with reason)

### 3. Files Created/Modified
```
.github/
├── workflows/
│   └── ci.yml
├── CODEOWNERS
└── pull_request_template.md

backend/
├── pom.xml (modified - added plugins)
└── checkstyle.xml (created)

frontend/
├── package.json (modified - added scripts)
├── .eslintrc.json (created/modified)
└── tsconfig.json (verified)

extraction-service/
├── requirements-dev.txt (created)
└── pyproject.toml (created)
```

### 4. CI Run Results
Include screenshot or link to successful CI run:
- Workflow run URL: `https://github.com/{user}/{repo}/actions/runs/{id}`
- Total duration: X minutes
- Each job duration

### 5. Test Results Summary
```
Backend:
- Checkstyle: ✅ Passed (0 violations)
- SpotBugs: ✅ Passed (0 bugs)
- Tests: ✅ Passed (X tests)
- Build: ✅ Success

Frontend:
- ESLint: ✅ Passed (0 warnings)
- TypeScript: ✅ Passed
- Build: ✅ Success

Extraction:
- Ruff: ✅ Passed
- MyPy: ✅ Passed
- Pytest: ✅ Passed (X tests)
```

### 6. Screenshots
- GitHub Actions workflow running
- All jobs passed (green checkmarks)
- Pull request with status checks

### 7. Issues Encountered
Document any problems and solutions.

### 8. Next Steps
What needs to be done in Phase 2 (Hexagonal Architecture)

---

## DEPENDENCIES

### Requires (must be completed first)
- **Phase 0**: Development Environment Setup ✅

### Required By (blocks these phases)
- **Phase 2**: Hexagonal Architecture (needs CI to validate structure)
- **Phase 3**: Database Schema (needs CI for migration validation)
- All subsequent phases rely on CI for quality assurance

---

## SUCCESS CRITERIA

This phase is considered **SUCCESSFUL** when:

1. ✅ CI workflow file is valid and triggers correctly
2. ✅ All three service jobs (backend, frontend, extraction) pass
3. ✅ Integration job confirms all checks passed
4. ✅ Linting catches code style violations
5. ✅ Tests run automatically on push
6. ✅ Build artifacts are generated
7. ✅ PR template guides contributors
8. ✅ CODEOWNERS assigns correct reviewers
9. ✅ CI badge shows in README
10. ✅ Result file is created with complete documentation

---

## IMPORTANT NOTES

1. **First CI Run**: First run will be slower due to dependency caching
2. **Secrets**: No secrets needed for Phase 1 (no deployments yet)
3. **Branch Protection**: Configure after first successful CI run
4. **Continue on Error**: Some checks use `continue-on-error` for flexibility
5. **Artifact Retention**: Set to 7 days to save storage
6. **Caching**: Maven, npm, and pip caches speed up subsequent runs

---

## TROUBLESHOOTING

### Issue: Workflow doesn't trigger
```bash
# Check workflow file syntax
# Ensure file is in .github/workflows/
# Check branch names match
```

### Issue: Maven build fails
```bash
# Check Java version matches
# Verify pom.xml is valid
# Check for dependency conflicts
```

### Issue: npm ci fails
```bash
# Ensure package-lock.json exists
# Run npm install locally first
# Commit package-lock.json
```

### Issue: Python tests fail to run
```bash
# Verify pytest is in requirements-dev.txt
# Check tests/ directory exists
# Add __init__.py to test directories
```

---

## COMMANDS REFERENCE

```bash
# Run backend checks locally
cd backend
mvn checkstyle:check
mvn spotbugs:check
mvn test

# Run frontend checks locally
cd frontend
npm run lint
npm run type-check
npm run build

# Run extraction checks locally
cd extraction-service
pip install -r requirements-dev.txt
ruff check src/ tests/
ruff format --check src/ tests/
mypy src/
pytest tests/
```

---

**Phase 1 Completion Target**: Automated quality assurance for all code changes
