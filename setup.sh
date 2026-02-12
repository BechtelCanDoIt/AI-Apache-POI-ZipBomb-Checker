#!/usr/bin/env bash
# ============================================================================
# ZipBomb Evaluator - Automated Setup Script (Linux/macOS)
# ============================================================================
# This script:
#   1. Checks prerequisites (Java 17+, Maven 3.8.1+)
#   2. Creates the Maven project directory structure
#   3. Places Java source files in correct locations
#   4. Builds the project and creates the standalone JAR
# ============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║          ZipBomb Evaluator - Setup Script v1.0.0              ║"
echo "║   Automated project setup for Linux/macOS                     ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# ---- Step 1: Check prerequisites ----

echo -e "${BLUE}[1/5] Checking prerequisites...${NC}"

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}ERROR: Java is not installed or not in PATH.${NC}"
    echo "Please install Java 17 or later:"
    echo "  macOS:  brew install openjdk@17"
    echo "  Linux:  sudo apt-get install openjdk-17-jdk"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ] 2>/dev/null; then
    echo -e "${RED}ERROR: Java 17+ required, found Java ${JAVA_VERSION}${NC}"
    echo "Please upgrade Java and try again."
    exit 1
fi
echo -e "  ${GREEN}✓ Java ${JAVA_VERSION} detected${NC}"

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}ERROR: Maven is not installed or not in PATH.${NC}"
    echo "Please install Maven 3.8.1 or later:"
    echo "  macOS:  brew install maven"
    echo "  Linux:  sudo apt-get install maven"
    exit 1
fi

MVN_VERSION=$(mvn -version 2>&1 | head -1 | grep -oE '[0-9]+\.[0-9]+\.[0-9]+' | head -1)
echo -e "  ${GREEN}✓ Maven ${MVN_VERSION} detected${NC}"

# ---- Step 2: Create directory structure ----

echo ""
echo -e "${BLUE}[2/5] Creating Maven project structure...${NC}"

MAIN_PKG="src/main/java/org/wso2/carbon/apimgt/impl/indexing"
TEST_PKG="src/test/java/org/wso2/carbon/apimgt/impl/indexing"

mkdir -p "${SCRIPT_DIR}/${MAIN_PKG}/util"
mkdir -p "${SCRIPT_DIR}/${MAIN_PKG}/indexer"
mkdir -p "${SCRIPT_DIR}/${TEST_PKG}/util"
mkdir -p "${SCRIPT_DIR}/src/main/resources"
mkdir -p "${SCRIPT_DIR}/reference"

echo -e "  ${GREEN}✓ Directory structure created${NC}"

# ---- Step 3: Place source files ----

echo ""
echo -e "${BLUE}[3/5] Placing source files...${NC}"

ERRORS=0

place_file() {
    local src="$1"
    local dst="$2"
    local label="$3"
    
    if [ -f "${SCRIPT_DIR}/${src}" ]; then
        cp "${SCRIPT_DIR}/${src}" "${SCRIPT_DIR}/${dst}"
        echo -e "  ${GREEN}✓ ${label}${NC}"
    elif [ -f "${SCRIPT_DIR}/${dst}" ]; then
        echo -e "  ${GREEN}✓ ${label} (already in place)${NC}"
    else
        echo -e "  ${RED}✗ ${label} - FILE NOT FOUND: ${src}${NC}"
        ERRORS=$((ERRORS + 1))
    fi
}

# Core source files
place_file "ZipBombEvaluator.java" \
    "${MAIN_PKG}/util/ZipBombEvaluator.java" \
    "ZipBombEvaluator.java → util/"

place_file "ZipBombEvaluatorMain.java" \
    "${MAIN_PKG}/ZipBombEvaluatorMain.java" \
    "ZipBombEvaluatorMain.java → indexing/"

place_file "DocumentIndexerSecurityFilter.java" \
    "${MAIN_PKG}/indexer/DocumentIndexerSecurityFilter.java" \
    "DocumentIndexerSecurityFilter.java → indexer/"

# Test files
place_file "ZipBombEvaluatorTest.java" \
    "${TEST_PKG}/util/ZipBombEvaluatorTest.java" \
    "ZipBombEvaluatorTest.java → test/util/"

# Also try the _Corrected variant if normal name not found
if [ ! -f "${SCRIPT_DIR}/${TEST_PKG}/util/ZipBombEvaluatorTest.java" ]; then
    place_file "ZipBombEvaluatorTest_Corrected.java" \
        "${TEST_PKG}/util/ZipBombEvaluatorTest.java" \
        "ZipBombEvaluatorTest_Corrected.java → test/util/ (renamed)"
fi

# Reference files (not compiled)
if [ -f "${SCRIPT_DIR}/DocumentIndexer_Modified_Example.java" ]; then
    cp "${SCRIPT_DIR}/DocumentIndexer_Modified_Example.java" "${SCRIPT_DIR}/reference/"
    echo -e "  ${GREEN}✓ DocumentIndexer_Modified_Example.java → reference/${NC}"
fi

if [ $ERRORS -gt 0 ]; then
    echo ""
    echo -e "${RED}ERROR: ${ERRORS} required file(s) not found.${NC}"
    echo "Please ensure all Java files are in the project root directory."
    echo "Required files: ZipBombEvaluator.java, ZipBombEvaluatorMain.java,"
    echo "  DocumentIndexerSecurityFilter.java, ZipBombEvaluatorTest.java"
    exit 1
fi

# ---- Step 4: Verify pom.xml ----

echo ""
echo -e "${BLUE}[4/5] Verifying build configuration...${NC}"

if [ ! -f "${SCRIPT_DIR}/pom.xml" ]; then
    echo -e "${RED}ERROR: pom.xml not found in ${SCRIPT_DIR}${NC}"
    echo "Please ensure pom.xml is in the project root directory."
    exit 1
fi
echo -e "  ${GREEN}✓ pom.xml found${NC}"

# Verify logging config exists
if [ ! -f "${SCRIPT_DIR}/src/main/resources/commons-logging.properties" ]; then
    echo -e "  ${YELLOW}⚠ commons-logging.properties not found, creating default...${NC}"
    cat > "${SCRIPT_DIR}/src/main/resources/commons-logging.properties" << 'PROPEOF'
# Force commons-logging to use java.util.logging (JDK14 logger)
org.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger
PROPEOF
    echo -e "  ${GREEN}✓ Default commons-logging.properties created${NC}"
else
    echo -e "  ${GREEN}✓ commons-logging.properties found${NC}"
fi

# ---- Step 5: Build ----

echo ""
echo -e "${BLUE}[5/5] Building project...${NC}"
echo ""

cd "${SCRIPT_DIR}"
mvn clean package -q -DskipTests=false 2>&1

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}╔════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                    BUILD SUCCESSFUL!                          ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    # Show built artifacts
    echo "Built artifacts:"
    ls -lh target/*.jar 2>/dev/null | while read line; do
        echo "  $line"
    done
    
    echo ""
    echo "Quick test:"
    echo "  java -jar target/zipbomb-evaluator-1.0.0-standalone.jar --help"
    echo ""
    echo "Evaluate a file:"
    echo "  java -jar target/zipbomb-evaluator-1.0.0-standalone.jar <file>"
    echo ""
else
    echo ""
    echo -e "${RED}BUILD FAILED${NC}"
    echo "Run 'mvn clean compile -X' for detailed error output."
    exit 1
fi
