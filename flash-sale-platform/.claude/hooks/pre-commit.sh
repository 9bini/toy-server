#!/bin/bash
set -euo pipefail

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
WARNINGS=""
ERRORS=""

# 1. ktlint auto format
echo "=== ktlint Auto Format ==="
cd "$PROJECT_DIR"
if ./gradlew ktlintFormat --quiet 2>/dev/null; then
  echo "ktlint format complete"
else
  ERRORS="${ERRORS}[ktlint] Format failed - manual review required\n"
fi

# 2. Secret hardcoding check (source code only, excluding tests/config)
SECRETS=$(grep -rn "password\s*=\s*\"[^\"]\+\"\|secret\s*=\s*\"[^\"]\+\"\|apiKey\s*=\s*\"[^\"]\+\"" \
  --include="*.kt" \
  "$PROJECT_DIR/services/" "$PROJECT_DIR/common/" 2>/dev/null | \
  grep -iv "test\|Test\|mock\|Mock\|TODO\|FIXME\|interface\|abstract\|companion\|application.*yml" || true)

if [ -n "$SECRETS" ]; then
  WARNINGS="${WARNINGS}[Security] Suspected hardcoded secrets:\n${SECRETS}\n"
fi

# 3. TODO/FIXME remaining notification
TODOS=$(grep -rn "TODO\|FIXME\|HACK\|XXX" --include="*.kt" \
  "$PROJECT_DIR/services/" "$PROJECT_DIR/common/" 2>/dev/null | head -10 || true)

if [ -n "$TODOS" ]; then
  WARNINGS="${WARNINGS}[Notice] Unresolved TODO/FIXME:\n${TODOS}\n"
fi

# Output results
if [ -n "$ERRORS" ]; then
  echo "=== Pre-commit Errors ==="
  echo -e "$ERRORS"
fi

if [ -n "$WARNINGS" ]; then
  echo "=== Pre-commit Warnings ==="
  echo -e "$WARNINGS"
fi

# Fail if there are errors, pass if only warnings
if [ -n "$ERRORS" ]; then
  exit 1
fi

exit 0
