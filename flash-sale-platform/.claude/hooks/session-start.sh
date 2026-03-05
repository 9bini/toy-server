#!/bin/bash
set -euo pipefail

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"

# Set Gradle wrapper execute permission
if [ -f "$PROJECT_DIR/gradlew" ]; then
  chmod +x "$PROJECT_DIR/gradlew"
fi

# Check Docker Compose infrastructure status
if command -v docker &> /dev/null; then
  if [ -f "$PROJECT_DIR/docker-compose.yml" ]; then
    echo "=== Docker Infrastructure Status ==="
    docker compose -f "$PROJECT_DIR/docker-compose.yml" ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "No Docker compose services (start with docker compose up -d)"
  fi
fi

# Check Java version
if command -v java &> /dev/null; then
  JAVA_VERSION=$(java -version 2>&1 | head -n 1)
  echo "Java: $JAVA_VERSION"
fi

# Memory system status notification (environment-independent discovery)
MEMORY_FOUND=false
if [ -d "$HOME/.claude/projects" ]; then
  for CANDIDATE_DIR in "$HOME/.claude/projects"/*/memory; do
    if [ -d "$CANDIDATE_DIR" ] && [ -f "$CANDIDATE_DIR/MEMORY.md" ]; then
      echo "=== Project Memory Loaded ==="
      MEMORY_FILES=$(ls "$CANDIDATE_DIR"/*.md 2>/dev/null | xargs -I {} basename {} | tr '\n' ', ' | sed 's/,$//')
      echo "Files: $MEMORY_FILES"
      MEMORY_FOUND=true
      break
    fi
  done
fi
if [ "$MEMORY_FOUND" = false ]; then
  echo "=== Project Memory Not Configured ==="
fi

# Recent git changes summary
echo "=== Recent Commits (5) ==="
git -C "$PROJECT_DIR" log --oneline -5 2>/dev/null || echo "No git log"
