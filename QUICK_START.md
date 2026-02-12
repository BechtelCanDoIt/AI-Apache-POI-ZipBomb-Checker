# ZipBomb Evaluator - Quick Start Guide

## TL;DR - Get Running in 5 Minutes

### On Linux/macOS:
```bash
# 1. Create and navigate to project directory
mkdir zipbomb-evaluator && cd zipbomb-evaluator

# 2. Copy all provided files here:
#    - pom.xml
#    - setup.sh
#    - All Java files

# 3. Run setup
chmod +x setup.sh
bash setup.sh

# 4. Test
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar --help
```

### On Windows:
```bash
# 1. Create and navigate to project directory
mkdir zipbomb-evaluator && cd zipbomb-evaluator

# 2. Copy all provided files here:
#    - pom.xml
#    - setup.bat
#    - All Java files

# 3. Run setup
setup.bat

# 4. Test
java -jar target\zipbomb-evaluator-1.0.0-standalone.jar --help
```

---

## Step-by-Step Instructions

### Step 1: Download and Organize Files

You should have received these files:

| File | Purpose |
|------|---------|
| `pom.xml` | Maven configuration (ROOT) |
| `setup.sh` | Auto-setup for Linux/macOS |
| `setup.bat` | Auto-setup for Windows |
| `ZipBombEvaluator.java` | Core utility |
| `ZipBombEvaluatorMain.java` | Executable entry point |
| `DocumentIndexerSecurityFilter.java` | Integration helper |
| `ZipBombEvaluatorTest_Corrected.java` | Test suite |
| `DocumentIndexer_Modified_Example.java` | Reference (WSO2 integration) |
| `SETUP_GUIDE.md` | Detailed setup instructions |
| `README.md` | Full documentation |

### Step 2: Create Project Directory

```bash
# Create a clean directory for your project
mkdir -p ~/projects/zipbomb-evaluator
cd ~/projects/zipbomb-evaluator
```

### Step 3: Place All Files

Copy/move all the files from your downloads into this directory. After this, your directory should look like:

```
zipbomb-evaluator/
├── pom.xml
├── setup.sh (or setup.bat on Windows)
├── ZipBombEvaluator.java
├── ZipBombEvaluatorMain.java
├── DocumentIndexerSecurityFilter.java
├── ZipBombEvaluatorTest_Corrected.java
├── DocumentIndexer_Modified_Example.java
├── SETUP_GUIDE.md
└── README.md
```

### Step 4: Run Automated Setup

The setup script will:
1. ✓ Check Java version (17+)
2. ✓ Check Maven version (3.8.1+)
3. ✓ Create all directories
4. ✓ Ask you to confirm files are in place
5. ✓ Build the project
6. ✓ Create JAR files

#### Linux/macOS:
```bash
chmod +x setup.sh
bash setup.sh
```

#### Windows:
```bash
setup.bat
```

### Step 5: Verify Installation

After setup completes, test the application:

```bash
# Show help
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar --help

# Show version
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar --version
```

### Step 6: Test with Sample File

```bash
# Create a test file
echo "test content" > test.txt

# Evaluate it
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar test.txt

# Expected output:
# ✓ test.txt (14 bytes) - PASSED
```

---

## Fixing "22 Issues" in VSCode

The issues in your test file are likely due to incorrect package/directory structure. Here's how to fix:

### The Problem
- Test file has wrong package declaration
- OR file is in wrong directory
- OR imports are failing

### The Solution

**Option A: Run Setup Script (Recommended)**
```bash
bash setup.sh  # or setup.bat on Windows
```

**Option B: Manual Fix**

1. Delete your current project structure
2. Create fresh directories exactly as shown in SETUP_GUIDE.md
3. Copy files to exact locations
4. Package declarations must match directory paths:

For example:
- **File with**: `package org.wso2.carbon.apimgt.impl.indexing.util;`
- **Must be at**: `src/main/java/org/wso2/carbon/apimgt/impl/indexing/util/`

5. In VSCode:
   - Ctrl+Shift+P
   - Java: Clean Language Server Workspace
   - Restart VSCode

6. Run build:
```bash
mvn clean compile
```

---

## Running the Application

### Method 1: Standalone JAR (Easiest)

```bash
# Single file
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar document.xlsx

# Multiple files
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar file1.xlsx file2.docx file3.zip

# Show help
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar --help
```

### Method 2: Maven Exec

```bash
mvn exec:java -Dexec.mainClass="org.wso2.carbon.apimgt.impl.indexing.ZipBombEvaluatorMain" \
              -Dexec.args="document.xlsx"
```

### Method 3: Direct Classpath

```bash
java -cp "target/classes:$HOME/.m2/repository/org/apache/poi/poi/5.2.5/*" \
     org.wso2.carbon.apimgt.impl.indexing.ZipBombEvaluatorMain document.xlsx
```

### Method 4: Run Tests

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=ZipBombEvaluatorTest
```

---

## Common Issues & Solutions

### "Command not found: mvn"

**Problem**: Maven not installed or not in PATH

**Solution**:
```bash
# Check if installed
which mvn
mvn -version

# If not found, install:
# macOS: brew install maven
# Linux: sudo apt-get install maven
# Windows: Download from maven.apache.org

# After install, verify:
mvn -v
```

### "Maven version is too old"

**Problem**: Maven 3.8.0 or older

**Solution**: 
```bash
# Upgrade Maven
brew upgrade maven  # macOS
sudo apt-get upgrade maven  # Linux

# Download from: https://maven.apache.org/download.cgi
# Extract and add to PATH
```

### "Java version is too old"

**Problem**: Java 11, 8, or older

**Solution**:
```bash
# Check version
java -version

# Install Java 17+
# macOS: brew install openjdk@17
# Linux: sudo apt-get install openjdk-17-jdk
# Windows: Download from oracle.com or use chocolatey

# Verify installation
java -version  # Should show 17 or higher
```

### "Could not find artifact"

**Problem**: Maven can't download dependencies

**Solution**:
```bash
# Clear Maven cache
rm -rf ~/.m2/repository

# Try clean rebuild
mvn clean compile

# If still failing, check internet connection and proxy settings
```

### "Package does not exist"

**Problem**: Files in wrong directories

**Solution**:
```bash
# Check directory structure
find . -name "*.java" -type f

# Should show paths matching package names
# E.g.: ./src/main/java/org/wso2/carbon/apimgt/impl/indexing/util/ZipBombEvaluator.java

# If not, move files to correct locations per SETUP_GUIDE.md
```

### "Symbol cannot be resolved"

**Problem**: IDE caching issue

**Solution**:
```bash
# Build in Maven first
mvn clean compile

# Then refresh IDE:
# VSCode: Ctrl+Shift+P > Java: Clean Language Server Workspace
# IntelliJ: File > Invalidate Caches > Restart
```

---

## Quick Command Reference

```bash
# Project Setup
mvn clean compile          # Clean build and compile
mvn clean package          # Build and package JAR
mvn clean install          # Install to local Maven repo

# Running Application
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar file.xlsx
java -jar target/*.jar file.xlsx

# Testing
mvn test                   # Run all tests
mvn test -Dtest=Test      # Run specific test

# IDE Integration
mvn idea:idea             # Generate IntelliJ project files
mvn eclipse:eclipse       # Generate Eclipse project files

# Dependencies
mvn dependency:tree       # Show dependency tree
mvn dependency:resolve    # Resolve all dependencies

# Cleanup
mvn clean                 # Clean target directory
rm -rf ~/.m2/repository   # Clear Maven cache
```

---

## What Gets Built?

After running `mvn package`, you'll have:

```
target/
├── zipbomb-evaluator-1.0.0.jar              ← Standard JAR
├── zipbomb-evaluator-1.0.0-standalone.jar   ← Fat JAR (use this!)
├── classes/                                 ← Compiled classes
└── test-classes/                            ← Test classes
```

**Use the standalone JAR**: It includes all dependencies and works everywhere.

---

## Integration with WSO2

To integrate into your WSO2 API Manager:

1. Copy `ZipBombEvaluator.java` to your WSO2 codebase
2. Copy `DocumentIndexerSecurityFilter.java` to your WSO2 codebase
3. Modify your `DocumentIndexer.java` per `DocumentIndexer_Modified_Example.java`
4. Add the security check in `getIndexedDocument()` method
5. Deploy and test

See `DocumentIndexer_Modified_Example.java` for exact code changes.

---

## Next Steps

1. ✓ Run setup script
2. ✓ Verify with `java -jar ... --help`
3. ✓ Test with sample files
4. ✓ Integrate into WSO2 (if needed)
5. ✓ Review README.md for full documentation

---

## Getting Help

If you get stuck:

1. **Read**: SETUP_GUIDE.md (detailed troubleshooting)
2. **Check**: README.md (comprehensive documentation)
3. **Verify**: 
   - Java 17+: `java -version`
   - Maven 3.8.1+: `mvn -version`
   - File locations match package structure
4. **Build**: `mvn clean compile` and check output for errors
5. **Run**: `mvn test` to see if tests pass

---

## File Sizes & Build Time

- Setup time: ~2-5 minutes (depends on Maven downloads)
- JAR size: ~15-20 MB (standalone with all dependencies)
- Build time: ~30-60 seconds
- Test time: ~10-20 seconds

---

## What This Does

The evaluator:
- ✓ Detects zip bomb attacks
- ✓ Checks compression ratios
- ✓ Validates file sizes
- ✓ Analyzes Office documents (XLSX, DOCX, PPTX, XLS, DOC, PPT)
- ✓ Analyzes PDFs and archives
- ✓ Provides detailed diagnostics
- ✓ Safe resource cleanup
- ✓ Production-ready logging

---

## Support Files

You also have:
- **README.md** - Full API documentation
- **SETUP_GUIDE.md** - Detailed setup instructions
- **DocumentIndexer_Modified_Example.java** - How to integrate with WSO2
- **ZipBombEvaluatorTest_Corrected.java** - Test examples

---

Good luck! You're all set to run the ZipBomb Evaluator. 🚀
