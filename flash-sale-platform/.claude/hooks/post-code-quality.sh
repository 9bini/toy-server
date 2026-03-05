#!/bin/bash
set -euo pipefail

INPUT=$(cat)

# Extract file_path with fallback chain: jq -> python3 -> grep
if command -v jq &> /dev/null; then
  FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')
elif command -v python3 &> /dev/null; then
  FILE_PATH=$(echo "$INPUT" | python3 -c "import json,sys; d=json.load(sys.stdin); print(d.get('tool_input',{}).get('file_path',''))" 2>/dev/null || echo "")
else
  FILE_PATH=$(echo "$INPUT" | grep -oP '"file_path"\s*:\s*"\K[^"]+' 2>/dev/null || echo "")
fi

# Target .kt files only
if [[ -z "$FILE_PATH" || "$FILE_PATH" != *.kt ]]; then
  exit 0
fi

if [[ ! -f "$FILE_PATH" ]]; then
  exit 0
fi

WARNINGS=""

# 1. Check for functions exceeding 50 lines
LONG_FUNCTIONS=$(awk '
  /^\s*(override\s+)?(suspend\s+)?fun\s/ { start=NR; name=$0 }
  start && /^\s*\}/ {
    len=NR-start;
    if(len>50) printf "  [Warning] %d-line function (lines %d-%d)\n", len, start, NR
    start=0
  }
' "$FILE_PATH" 2>/dev/null || true)

if [ -n "$LONG_FUNCTIONS" ]; then
  WARNINGS="${WARNINGS}[Function Length] Exceeds 50 lines:\n${LONG_FUNCTIONS}\n"
fi

# 2. GlobalScope usage check
if grep -qn "GlobalScope" "$FILE_PATH" 2>/dev/null; then
  WARNINGS="${WARNINGS}[Coroutine] GlobalScope usage detected - Structured Concurrency violation\n"
fi

# 3. runBlocking check (excluding test code)
if [[ "$FILE_PATH" != *Test* && "$FILE_PATH" != *test* ]]; then
  if grep -qn "runBlocking" "$FILE_PATH" 2>/dev/null; then
    WARNINGS="${WARNINGS}[Coroutine] runBlocking usage detected - recommend using suspend fun\n"
  fi
fi

# 4. Check for external calls without withTimeout
if grep -qn "redisTemplate\|reactiveRedisTemplate\|kafkaTemplate\|webClient\|r2dbcEntityTemplate" "$FILE_PATH" 2>/dev/null; then
  if ! grep -qn "withTimeout" "$FILE_PATH" 2>/dev/null; then
    WARNINGS="${WARNINGS}[Timeout] External calls (Redis/Kafka/WebClient/DB) detected but withTimeout is missing\n"
  fi
fi

# 5. Nesting depth check (cumulative open braces per line, 4 levels or more)
DEEP_NESTING=$(awk '
  {
    for(i=1; i<=length($0); i++) {
      c = substr($0,i,1)
      if(c == "{") depth++
      if(c == "}") depth--
    }
    if(depth >= 4) printf "  [Warning] Line %d: nesting depth %d levels\n", NR, depth
  }
' "$FILE_PATH" 2>/dev/null || true)

if [ -n "$DEEP_NESTING" ]; then
  WARNINGS="${WARNINGS}[Nesting] Deep nesting detected:\n${DEEP_NESTING}\n"
fi

# 6. Magic Number check (hardcoded numbers in timeout/delay/retry)
MAGIC_NUMBERS=$(grep -nE '(withTimeout|delay|retry|Duration\.of)\s*\(\s*[0-9]+' "$FILE_PATH" 2>/dev/null | \
  grep -v "companion\|const\|val \|private val\|object " || true)

if [ -n "$MAGIC_NUMBERS" ]; then
  WARNINGS="${WARNINGS}[Magic Number] Hardcoded numbers in timeout/delay - should be defined as constants:\n${MAGIC_NUMBERS}\n"
fi

if [ -n "$WARNINGS" ]; then
  echo "=== Code Quality Warnings ==="
  echo -e "$WARNINGS"
fi

exit 0
