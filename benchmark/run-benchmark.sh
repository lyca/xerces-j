#!/usr/bin/env bash
set -e
export LC_ALL=C

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

LIB_DIR="${ROOT_DIR}/build/benchmark-libs"
BIN_DIR="${ROOT_DIR}/build/benchmark-classes"
SAMPLE_XML="${ROOT_DIR}/build/benchmark_sample.xml"

XML_FILE="${1:-${SAMPLE_XML}}"
XML_SIZE_MB="${2:-5}"
ITERATIONS="${3:-15}"
WARMUP="${4:-5}"
MODE="${5:-parse}" # 'parse' or 'traverse'

echo "================================================================================"
echo "          XML PARSER BENCHMARK (FasterXML vs JDK vs Xerces 2.12.2 vs Repo)       "
echo "================================================================================"

# 1. Build local repo
echo "[1/4] Building current repository (:xerces-impl)..."
"${ROOT_DIR}/gradlew" -q -p "${ROOT_DIR}" :xerces-impl:jar

REPO_JAR="$(ls "${ROOT_DIR}/xerces-impl/build/libs/xercesImpl-"*.jar | grep -v 'javadoc' | grep -v 'sources' | grep -v 'dtd' | head -n 1)"
if [ ! -f "${REPO_JAR}" ]; then
    echo "Error: Local xercesImpl jar not found!"
    exit 1
fi

# 2. Download external benchmark dependencies if missing
mkdir -p "${LIB_DIR}"
echo "[2/4] Verifying benchmark dependencies..."

download_if_missing() {
    local file="$1"
    local url="$2"
    if [ ! -f "${LIB_DIR}/${file}" ]; then
        echo "  Downloading ${file}..."
        curl -sSL "${url}" -o "${LIB_DIR}/${file}"
    fi
}

download_if_missing "xercesImpl-2.12.2.jar" "https://repo1.maven.org/maven2/xerces/xercesImpl/2.12.2/xercesImpl-2.12.2.jar"
download_if_missing "xml-apis-1.4.01.jar" "https://repo1.maven.org/maven2/xml-apis/xml-apis/1.4.01/xml-apis-1.4.01.jar"
download_if_missing "woodstox-core-7.1.0.jar" "https://repo1.maven.org/maven2/com/fasterxml/woodstox/woodstox-core/7.1.0/woodstox-core-7.1.0.jar"
download_if_missing "stax2-api-4.2.2.jar" "https://repo1.maven.org/maven2/org/codehaus/woodstox/stax2-api/4.2.2/stax2-api-4.2.2.jar"
download_if_missing "aalto-xml-1.3.3.jar" "https://repo1.maven.org/maven2/com/fasterxml/aalto-xml/1.3.3/aalto-xml-1.3.3.jar"

# 3. Compile Benchmark classes
echo "[3/4] Compiling benchmark runners..."
mkdir -p "${BIN_DIR}"
javac -d "${BIN_DIR}" -cp "${LIB_DIR}/xml-apis-1.4.01.jar:${LIB_DIR}/stax2-api-4.2.2.jar:${LIB_DIR}/woodstox-core-7.1.0.jar:${LIB_DIR}/aalto-xml-1.3.3.jar" \
    "${SCRIPT_DIR}/Benchmark.java" "${SCRIPT_DIR}/BenchmarkGenerator.java"

# 4. Generate test XML if file does not exist
if [ ! -f "${XML_FILE}" ]; then
    echo "[4/4] Generating synthetic XML test file (${XML_SIZE_MB} MB at ${XML_FILE})..."
    java -cp "${BIN_DIR}" BenchmarkGenerator "${XML_FILE}" "${XML_SIZE_MB}"
else
    echo "[4/4] Using XML file: ${XML_FILE} ($(du -h "${XML_FILE}" | cut -f1))"
fi

echo ""
echo "Running benchmarks (${ITERATIONS} iterations, ${WARMUP} warmup, mode: ${MODE})..."
echo "--------------------------------------------------------------------------------"

declare -a PARSERS=(
    "aalto-stax|FasterXML Aalto (StAX)|${LIB_DIR}/aalto-xml-1.3.3.jar:${LIB_DIR}/stax2-api-4.2.2.jar"
    "woodstox-stax|FasterXML Woodstox (StAX)|${LIB_DIR}/woodstox-core-7.1.0.jar:${LIB_DIR}/stax2-api-4.2.2.jar"
    "aalto-sax|FasterXML Aalto (SAX)|${LIB_DIR}/aalto-xml-1.3.3.jar:${LIB_DIR}/stax2-api-4.2.2.jar"
    "woodstox-sax|FasterXML Woodstox (SAX)|${LIB_DIR}/woodstox-core-7.1.0.jar:${LIB_DIR}/stax2-api-4.2.2.jar"
    "jdk-sax|JDK Internal (SAX)|"
    "xerces212-sax|Xerces 2.12.2 (SAX)|${LIB_DIR}/xercesImpl-2.12.2.jar:${LIB_DIR}/xml-apis-1.4.01.jar"
    "repo-sax|This Repo (SAX)|${REPO_JAR}:${LIB_DIR}/xml-apis-1.4.01.jar"
    "jdk-dom-defer|JDK Internal (DOM Deferred)|"
    "jdk-dom-nodefer|JDK Internal (DOM Eager)|"
    "xerces212-dom-defer|Xerces 2.12.2 (DOM Deferred)|${LIB_DIR}/xercesImpl-2.12.2.jar:${LIB_DIR}/xml-apis-1.4.01.jar"
    "xerces212-dom-nodefer|Xerces 2.12.2 (DOM Eager)|${LIB_DIR}/xercesImpl-2.12.2.jar:${LIB_DIR}/xml-apis-1.4.01.jar"
    "repo-dom-defer|This Repo (DOM Deferred)|${REPO_JAR}:${LIB_DIR}/xml-apis-1.4.01.jar"
    "repo-dom-nodefer|This Repo (DOM Eager)|${REPO_JAR}:${LIB_DIR}/xml-apis-1.4.01.jar"
)

RAW_RESULTS="${ROOT_DIR}/build/benchmark_results.txt"
rm -f "${RAW_RESULTS}"

for entry in "${PARSERS[@]}"; do
    IFS="|" read -r key label cp <<< "${entry}"
    FULL_CP="${BIN_DIR}"
    if [ -n "${cp}" ]; then
        FULL_CP="${FULL_CP}:${cp}"
    fi

    printf "Testing %-32s ... " "${label}"
    OUT=$(java -cp "${FULL_CP}" Benchmark "${key}" "${XML_FILE}" "${ITERATIONS}" "${WARMUP}" "${MODE}" 2>&1)
    if [[ "${OUT}" =~ RESULT\| ]]; then
        LINE=$(echo "${OUT}" | grep 'RESULT|')
        echo "${label}|${LINE}" >> "${RAW_RESULTS}"
        AVG=$(echo "${LINE}" | cut -d'|' -f3)
        TP=$(echo "${LINE}" | cut -d'|' -f5)
        printf "%8.2f ms   (%7.2f MB/s)\n" "${AVG}" "${TP}"
    else
        echo "FAILED: ${OUT}"
    fi
done

echo ""
echo "================================================================================"
echo "                               BENCHMARK SUMMARY                                "
echo "================================================================================"
printf "| %-32s | %-10s | %-10s | %-14s |\n" "Parser & API" "Avg Time" "Best Time" "Throughput"
printf "|:---------------------------------|-----------:|-----------:|---------------:|\n"

while IFS="|" read -r label prefix key avg min tp size; do
    printf "| %-32s | %7.2f ms | %7.2f ms | %8.2f MB/s |\n" "${label}" "${avg}" "${min}" "${tp}"
done < "${RAW_RESULTS}"

echo "================================================================================"
