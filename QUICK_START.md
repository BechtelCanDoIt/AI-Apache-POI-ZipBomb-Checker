# ZipBomb Evaluator - Quick Start Guide

## TL;DR - Get Running in 5 Minutes

### On Linux/macOS:
```bash
# 1. Clone the repo
git clone https://github.com/<your-user>/AI-Apache-POI-ZipBomb-Checker.git
cd AI-Apache-POI-ZipBomb-Checker

# 2. Run setup
chmod +x setup.sh
bash setup.sh

# 3. Test with included sample files
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar ./testfiles/*
```

### On Windows:
```bash
# 1. Clone the repo
git clone https://github.com/<your-user>/AI-Apache-POI-ZipBomb-Checker.git
cd AI-Apache-POI-ZipBomb-Checker

# 2. Run setup
setup.bat

# 3. Test with included sample files
java -jar target\zipbomb-evaluator-1.0.0-standalone.jar testfiles\*
```

---

## Step-by-Step Instructions

### Step 1: Clone or Download

```bash
git clone https://github.com/<your-user>/AI-Apache-POI-ZipBomb-Checker.git
cd AI-Apache-POI-ZipBomb-Checker
```

Or download and extract the ZIP from GitHub.

### Step 2: Verify What You Have

Your directory should contain:

```
AI-Apache-POI-ZipBomb-Checker/
├── pom.xml
├── setup.sh / setup.bat
├── LICENSE
├── testfiles/
│   ├── test-zipbomb.xlsx
│   ├── testzipbomb.zip
│   └── admin-PizzaShackAPI-1.0.0.zip
├── src/
│   ├── main/java/.../
│   ├── main/resources/commons-logging.properties
│   └── test/java/.../
├── README.md
├── QUICK_START.md
├── SETUP_GUIDE.md
└── FILE_MANIFEST.md
```

### Step 3: Run Automated Setup

The setup script will:
1. ✓ Check Java version (17+)
2. ✓ Check Maven version (3.8.1+)
3. ✓ Verify directory structure
4. ✓ Build the project
5. ✓ Create JAR files

#### Linux/macOS:
```bash
chmod +x setup.sh
bash setup.sh
```

#### Windows:
```bash
setup.bat
```

### Step 4: Verify Installation

```bash
# Show help
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar --help

# Show version
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar --version
```

### Step 5: Test with Included Sample Files

```bash
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar ./testfiles/*
```

Expected results:
- `admin-PizzaShackAPI-1.0.0.zip` → **PASSED** (clean file)
- `test-zipbomb.xlsx` → **ZIP BOMB DETECTED** (335:1 compression ratio)
- `testzipbomb.zip` → **ZIP BOMB DETECTED** (nested - contains test-zipbomb.xlsx inside)

### Step 6: Test with Your Own Files

```bash
# Single file
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar /path/to/your/file.xlsx

# Multiple files
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar file1.xlsx file2.docx archive.zip
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
# Test files included in repo
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar ./testfiles/*

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

# If not, re-clone the repo or move files to correct locations per SETUP_GUIDE.md
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

### `grep: invalid option -- P` on macOS

**Problem**: Old version of `setup.sh` used GNU grep syntax

**Solution**: Pull the latest `setup.sh` — the `grep -P` has been replaced with `grep -oE` which works on both macOS and Linux.

---

## Quick Command Reference

```bash
# Project Setup
mvn clean compile          # Clean build and compile
mvn clean package          # Build and package JAR
mvn clean install          # Install to local Maven repo

# Running Application
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar ./testfiles/*
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar file.xlsx

# Testing
mvn test                   # Run all tests
mvn test -Dtest=ZipBombEvaluatorTest  # Run specific test

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

## Next Steps

1. ✓ Clone or download the repo
2. ✓ Run setup script
3. ✓ Verify with `java -jar ... --help`
4. ✓ Test with `./testfiles/*`
5. ✓ Test with your own files
6. ✓ Review README.md for full documentation

---

## Getting Help

If you get stuck:

1. **Read**: SETUP_GUIDE.md (detailed troubleshooting)
2. **Read**: README.md (comprehensive documentation)
3. **Verify**: 
   - Java 17+: `java -version`
   - Maven 3.8.1+: `mvn -version`
   - File locations match package structure
4. **Build**: `mvn clean compile` and check output for errors
5. **Run**: `mvn test` to see if tests pass

---

## What This Does

The evaluator:
- ✓ Detects zip bomb attacks
- ✓ **Recursively inspects nested archives** (zip inside zip, xlsx inside zip)
- ✓ Checks compression ratios
- ✓ Validates file sizes
- ✓ Analyzes Office documents (XLSX, DOCX, PPTX, XLS, DOC, PPT)
- ✓ Analyzes PDFs and archives
- ✓ Provides detailed diagnostics with nesting path
- ✓ Safe resource cleanup
- ✓ Production-ready logging

---

Good luck! You're all set to run the ZipBomb Evaluator. 🚀
