# ZipBomb Evaluator - Detailed Setup Guide

## Directory Structure

The Maven project must have this exact structure for compilation:

```
zipbomb-evaluator/
├── pom.xml                                          ← Root (must be here)
├── setup.sh / setup.bat                             ← Run from root
│
├── src/main/java/org/wso2/carbon/apimgt/impl/indexing/
│   ├── ZipBombEvaluatorMain.java                    ← CLI entry point
│   ├── util/
│   │   └── ZipBombEvaluator.java                    ← Core engine
│   └── indexer/
│       └── DocumentIndexerSecurityFilter.java       ← Integration helper
│
├── src/main/resources/
│   └── log4j2.xml                                   ← Logging config
│
├── src/test/java/org/wso2/carbon/apimgt/impl/indexing/util/
│   └── ZipBombEvaluatorTest.java                    ← Tests
│
└── reference/                                       ← NOT compiled
    ├── DocumentIndexer_Modified_Example.java        ← WSO2 integration guide
    └── DocumentIndexerSecurityFilter_WSO2.java      ← WSO2-specific version
```

**Critical**: Java package declarations MUST match directory paths exactly.

## Automated Setup

The recommended approach is to use the setup script:

### Linux/macOS
```bash
cd zipbomb-evaluator
chmod +x setup.sh
bash setup.sh
```

### Windows
```cmd
cd zipbomb-evaluator
setup.bat
```

The script will create all directories, copy files to correct locations, verify prerequisites, and build the project.

## Manual Setup

If you prefer to set up manually:

### Step 1: Create directories
```bash
mkdir -p src/main/java/org/wso2/carbon/apimgt/impl/indexing/util
mkdir -p src/main/java/org/wso2/carbon/apimgt/impl/indexing/indexer
mkdir -p src/test/java/org/wso2/carbon/apimgt/impl/indexing/util
mkdir -p src/main/resources
mkdir -p reference
```

### Step 2: Place files
```bash
cp ZipBombEvaluator.java src/main/java/org/wso2/carbon/apimgt/impl/indexing/util/
cp ZipBombEvaluatorMain.java src/main/java/org/wso2/carbon/apimgt/impl/indexing/
cp DocumentIndexerSecurityFilter.java src/main/java/org/wso2/carbon/apimgt/impl/indexing/indexer/
cp ZipBombEvaluatorTest.java src/test/java/org/wso2/carbon/apimgt/impl/indexing/util/
```

### Step 3: Build
```bash
mvn clean package
```

### Step 4: Verify
```bash
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar --help
```

## IDE Configuration

### VS Code
1. Open the project root folder (where pom.xml is)
2. Install the "Extension Pack for Java" if not installed
3. If you see errors, press Ctrl+Shift+P → "Java: Clean Language Server Workspace"
4. Restart VS Code

### IntelliJ IDEA
1. File → Open → select the project root
2. IntelliJ should auto-detect the Maven project
3. If needed: File → Invalidate Caches → Restart

### Eclipse
1. File → Import → Maven → Existing Maven Projects
2. Select the project root directory
3. Click Finish

## Troubleshooting

### "Package does not exist" errors
Files are in the wrong directory. The package declaration in each file must match its directory path:
- `package org.wso2.carbon.apimgt.impl.indexing.util;` → file must be in `src/main/java/org/wso2/carbon/apimgt/impl/indexing/util/`

### "Cannot resolve symbol" in IDE
Run `mvn clean compile` first to download dependencies, then refresh your IDE.

### Test failures
Ensure `ZipBombEvaluatorTest.java` is in the **test** source tree, not the main source tree.

### Maven download failures
```bash
rm -rf ~/.m2/repository
mvn clean compile
```

### Java version errors
Verify with `java -version`. Must be 17+. If you have multiple Java versions:
```bash
export JAVA_HOME=/path/to/jdk-17
export PATH=$JAVA_HOME/bin:$PATH
```

### Build succeeds but no standalone JAR
The shade plugin runs during the `package` phase. Use `mvn clean package` (not just `compile`).

## Verification Checklist

After setup, verify everything works:

```bash
# 1. Check Java
java -version            # Should show 17+

# 2. Check Maven  
mvn -version             # Should show 3.8.1+

# 3. Compilation
mvn clean compile        # Should succeed with 0 errors

# 4. Tests
mvn test                 # Should run and pass

# 5. JAR exists
ls -lh target/*standalone*.jar   # Should show ~18MB JAR

# 6. CLI works
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar --help

# 7. Evaluate a test file
echo "test" > /tmp/test.txt
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar /tmp/test.txt
```
