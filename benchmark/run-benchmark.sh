#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "Executing JMH Microbenchmarks via Gradle..."
"${ROOT_DIR}/gradlew" -p "${ROOT_DIR}" :xerces-benchmarks:jmh "$@"
