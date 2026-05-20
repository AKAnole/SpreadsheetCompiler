#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
#  compile_and_run.sh
#  Builds and runs the Spreadsheet Formula Language Compiler Front-End.
#
#  Usage:
#    ./compile_and_run.sh            # compile + test suite + REPL
#    ./compile_and_run.sh --demo     # compile + curated demo
#    ./compile_and_run.sh --test     # compile + test suite only
#    ./compile_and_run.sh "=SUM(A1)" # compile + parse one formula
# ─────────────────────────────────────────────────────────────────────────────
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC="$SCRIPT_DIR/src"
OUT="$SCRIPT_DIR/out"

echo "─── Compiling ───────────────────────────────"
mkdir -p "$OUT"
javac -cp "$SRC" -d "$OUT" $(find "$SRC" -name "*.java")
echo "Compilation successful."
echo ""

echo "─── Running ─────────────────────────────────"
java -cp "$OUT" Main "$@"
