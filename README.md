# Source Code
This source code is AI generated.

---

# ZipBomb Evaluator

A standalone utility for detecting zip bomb attacks in document files processed by Apache POI as a standalone CLI tool.

## Features

- Detects zip bombs across multiple file formats
- **Recursive inspection** of nested archives (zip containing xlsx, zip within zip, etc.)
- Checks compression ratios, entry sizes, and total archive sizes
- Supports: XLSX, DOCX, PPTX, XLS, DOC, PPT, PDF, ZIP, JAR, WAR
- CLI tool with formatted output and exit codes for scripting
- Standalone fat JAR (no runtime dependencies needed)
- Logging via Commons Logging (java.util.logging backend)

## Quick Start

```bash
# 1. Clone and enter the repo
git clone https://github.com/BechtelCanDoIt/AI-Apache-POI-ZipBomb-Checker.git
cd AI-Apache-POI-ZipBomb-Checker

# 2. Run setup (creates dirs, builds project)
chmod +x setup.sh
bash setup.sh

# 3. Test it
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar --help

# 4. Evaluate files in a folder
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar ./testfiles/*
```

See [QUICK_START.md](QUICK_START.md) for detailed step-by-step instructions.

## Prerequisites

- Java 17 or later
- Maven 3.8.1 or later
- Internet connection (for initial Maven dependency download)

## Usage

### Command Line

```bash
# Single file
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar document.xlsx

# Multiple files
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar file1.xlsx file2.docx archive.zip

# All test files
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar ./testfiles/*

# Help
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar --help

# Version
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar --version
```

### Exit Codes

- `0` - All files passed security validation
- `1` - One or more files flagged as security threats

### Programmatic Usage

```java
import org.wso2.carbon.apimgt.impl.indexing.util.ZipBombEvaluator;
import org.wso2.carbon.apimgt.impl.indexing.util.ZipBombEvaluator.ZipBombEvaluationResult;

ZipBombEvaluator evaluator = new ZipBombEvaluator();
ZipBombEvaluationResult result = evaluator.evaluateFile("path/to/file.xlsx");

if (result.isZipBomb()) {
    System.err.println("Threat detected: " + result.getDetails());
} else {
    System.out.println("File is safe: " + result.getStatus());
}
```

### Byte Array Validation (for integration)

```java
import org.wso2.carbon.apimgt.impl.indexing.indexer.DocumentIndexerSecurityFilter;

DocumentIndexerSecurityFilter filter = new DocumentIndexerSecurityFilter();
try {
    filter.validateDocumentBytes(fileBytes, "document.xlsx");
    // Safe to process
} catch (SecurityException e) {
    // Zip bomb detected - block processing
}
```

## Security Thresholds

| Check | Default Limit |
|-------|---------------|
| Compression Ratio | 100:1 |
| Max Entry Size | 1 GB |
| Max Total Uncompressed Size | 10 GB |
| Max Recursion Depth | 10 nesting levels |
| Max Extraction Size | 100 MB per nested entry |

## What It Detects

- Excessive compression ratios (classic zip bombs)
- **Nested zip bombs** (e.g., a zip containing a malicious xlsx)
- Oversized individual archive entries
- Total archive size violations
- Malformed or corrupted zip structures
- Corrupted Office documents that trigger POI exceptions
- Format mismatches (e.g., DOCX file with .xls extension)
- Excessive archive nesting depth (recursive zip bombs)

## Supported File Formats

| Format | Type | Detection Method |
|--------|------|------------------|
| XLSX | Microsoft Excel 2007+ | POI + zip structure analysis |
| DOCX | Microsoft Word 2007+ | POI + zip structure analysis |
| PPTX | Microsoft PowerPoint 2007+ | POI + zip structure analysis |
| XLS | Microsoft Excel 2003 | POI OLE2 analysis |
| DOC | Microsoft Word 2003 | POI OLE2 analysis |
| PPT | Microsoft PowerPoint 2003 | POI OLE2 analysis |
| PDF | Adobe PDF | PDFBox 3.x analysis |
| ZIP | Standard archive | Zip structure + recursive content analysis |
| JAR | Java archive | Zip structure + recursive content analysis |
| WAR | Web archive | Zip structure + recursive content analysis |

## Project Structure

```
AI-Apache-POI-ZipBomb-Checker/
├── pom.xml                                  # Maven build config
├── setup.sh                                 # Linux/macOS setup
├── setup.bat                                # Windows setup
├── LICENSE                                  # Apache License 2.0
├── .gitignore
├── testfiles/                               # Sample files for testing
│   ├── test-zipbomb.xlsx                    # Direct zip bomb (335:1 ratio)
│   ├── testzipbomb.zip                      # Nested zip bomb (xlsx inside zip)
│   └── admin-PizzaShackAPI-1.0.0.zip       # Clean file (should pass)
├── src/
│   ├── main/java/.../indexing/
│   │   ├── util/
│   │   │   └── ZipBombEvaluator.java        # Core detection engine (recursive)
│   │   ├── ZipBombEvaluatorMain.java        # CLI entry point
│   │   └── indexer/
│   │       └── DocumentIndexerSecurityFilter.java  # Integration helper
│   ├── main/resources/
│   │   └── commons-logging.properties       # Logging config (JUL backend)
│   └── test/java/.../indexing/util/
│       └── ZipBombEvaluatorTest.java        # Tests & examples
├── README.md                                # This file
├── QUICK_START.md                           # 5-minute setup guide
├── SETUP_GUIDE.md                           # Detailed setup & troubleshooting
└── FILE_MANIFEST.md                         # Complete file inventory
```

## Building

```bash
# Compile only
mvn clean compile

# Build with tests
mvn clean package

# Build, skip tests
mvn clean package -DskipTests

# Install to local repo
mvn clean install
```

## Sample Output
```
java -jar target/zipbomb-evaluator-1.0.0-standalone.jar ./testfiles/*

╔════════════════════════════════════════════════════════════════╗
║          ZipBomb Evaluator v1.0.0
║   Apache POI Zip Bomb Detection and Analysis Utility
║   Timestamp: 2026-02-12 17:03:47
╚════════════════════════════════════════════════════════════════╝

Starting file evaluation...
---
Feb 12, 2026 5:03:47 PM org.wso2.carbon.apimgt.impl.indexing.util.ZipBombEvaluator evaluateFileInternal
INFO: Evaluating file: admin-PizzaShackAPI-1.0.0.zip (size: 3,356 bytes)
Feb 12, 2026 5:03:47 PM org.wso2.carbon.apimgt.impl.indexing.util.ZipBombEvaluator checkZipStructure
INFO: ZIP structure validation successful: admin-PizzaShackAPI-1.0.0.zip
Feb 12, 2026 5:03:47 PM org.wso2.carbon.apimgt.impl.indexing.util.ZipBombEvaluator evaluateZIP
INFO: ZIP evaluation passed (including nested contents): admin-PizzaShackAPI-1.0.0.zip
✓ admin-PizzaShackAPI-1.0.0.zip (3.28 KB) - PASSED
Feb 12, 2026 5:03:47 PM org.wso2.carbon.apimgt.impl.indexing.util.ZipBombEvaluator evaluateFileInternal
INFO: Evaluating file: test-zipbomb.xlsx (size: 7,572 bytes)

⚠️  ZIP BOMB DETECTED
  Filename:  test-zipbomb.xlsx
  Extension: zip
  Size:      7.39 KB
  Status:    EXCESSIVE_COMPRESSION_RATIO
  Details:   Entry 'xl/styles.xml': suspicious compression ratio of 335:1 (compressed: 7,448 bytes, uncompressed: 2,500,158 bytes)

Feb 12, 2026 5:03:47 PM org.wso2.carbon.apimgt.impl.indexing.util.ZipBombEvaluator evaluateFileInternal
INFO: Evaluating file: testzipbomb.zip (size: 503 bytes)
Feb 12, 2026 5:03:47 PM org.wso2.carbon.apimgt.impl.indexing.util.ZipBombEvaluator checkZipStructure
INFO: ZIP structure validation successful: testzipbomb.zip
Feb 12, 2026 5:03:47 PM org.wso2.carbon.apimgt.impl.indexing.util.ZipBombEvaluator evaluateZipContentsRecursively
INFO:   [depth=0] Inspecting nested entry: testzipbomb.zip -> test-zipbomb.xlsx
Feb 12, 2026 5:03:47 PM org.wso2.carbon.apimgt.impl.indexing.util.ZipBombEvaluator evaluateFileInternal
INFO:   [depth=1] Evaluating nested: testzipbomb.zip -> zbeval_2627965063114260687.xlsx (size: 7,572 bytes)
Feb 12, 2026 5:03:47 PM org.wso2.carbon.apimgt.impl.indexing.util.ZipBombEvaluator evaluateZipContentsRecursively
WARNING: Nested zip bomb found: testzipbomb.zip -> zbeval_2627965063114260687.xlsx

⚠️  ZIP BOMB DETECTED
  Filename:  testzipbomb.zip -> zbeval_2627965063114260687.xlsx
  Extension: zip
  Size:      7.39 KB
  Status:    EXCESSIVE_COMPRESSION_RATIO
  Details:   Entry 'xl/styles.xml': suspicious compression ratio of 335:1 (compressed: 7,448 bytes, uncompressed: 2,500,158 bytes)


---
SUMMARY REPORT
---
Total Files Scanned:    3
Passed Security Check:  1
Flagged as Threats:     2
Total Data Scanned:     18.07 KB
Completion Time:        2026-02-12 17:03:47

⚠️  SECURITY ALERT: 2 file(s) failed validation
```

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Apache POI | 5.2.5 | Office document parsing |
| Apache PDFBox | 3.0.1 | PDF processing |
| Commons IO | 2.15.1 | File utilities |
| Commons Lang3 | 3.14.0 | String utilities |
| Commons Logging | 1.2 | Logging facade (JUL backend) |
| JUnit 4 | 4.13.2 | Testing |

## Performance

- Evaluation time: 5-50ms per file (varies by format and size)
- Standalone JAR size: ~27 MB (includes all dependencies)
- Build time: 30-60 seconds

## License

Apache License 2.0

----
----
----
## WSO2 API Manager Integration - UNTESTED
THIS MAYBE WORKS. I HAVEN'T TESTED IT

To add zip bomb protection to your WSO2 API Manager DocumentIndexer:

1. Copy `ZipBombEvaluator.java` to your WSO2 project's `indexing/util/` package
2. Copy `DocumentIndexerSecurityFilter.java` to your WSO2 project
3. Modify `DocumentIndexer.getIndexedDocument()` to add the security check
4. Build and deploy
