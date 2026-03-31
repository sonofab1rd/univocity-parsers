#!/usr/bin/env bash
# build.sh – Compile the Rust CSV parser and print instructions for running
# the benchmark.
#
# Usage:
#   cd rust-csv
#   ./build.sh [release|debug]
#
# After building, run the benchmark with:
#   cd ..
#   LD_LIBRARY_PATH=rust-csv/target/release \
#     mvn test -Dtest=RustVsJavaBenchmarkTest -DfailIfNoTests=false

set -euo pipefail

PROFILE="${1:-release}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(dirname "$SCRIPT_DIR")"

if [[ "$PROFILE" == "release" ]]; then
    cargo build --release --manifest-path "$SCRIPT_DIR/Cargo.toml"
    LIB_DIR="$SCRIPT_DIR/target/release"
else
    cargo build --manifest-path "$SCRIPT_DIR/Cargo.toml"
    LIB_DIR="$SCRIPT_DIR/target/debug"
fi

echo ""
echo "Build complete.  Native library: $LIB_DIR"
echo ""
echo "To run the benchmark:"
echo "  cd $REPO_ROOT"
echo "  LD_LIBRARY_PATH=$LIB_DIR \\"
echo "    mvn test -Dtest=RustVsJavaBenchmarkTest -DfailIfNoTests=false"
