#!/bin/bash
set -euo pipefail

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"

# Gradle wrapper 실행 권한 설정
if [ -f "$PROJECT_DIR/gradlew" ]; then
  chmod +x "$PROJECT_DIR/gradlew"
fi

# Docker Compose 인프라 상태 확인
if command -v docker &> /dev/null; then
  if [ -f "$PROJECT_DIR/docker-compose.yml" ]; then
    echo "=== Docker 인프라 상태 ==="
    docker compose -f "$PROJECT_DIR/docker-compose.yml" ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "Docker compose 서비스 없음 (docker compose up -d 로 시작하세요)"
  fi
fi

# Java 버전 확인
if command -v java &> /dev/null; then
  JAVA_VERSION=$(java -version 2>&1 | head -n 1)
  echo "Java: $JAVA_VERSION"
fi
