---
name: document
description: 기술 문서를 생성합니다. API 문서, 아키텍처 문서, 운영 가이드, 트레이드오프 분석, 면접 대비 자료를 작성합니다.
argument-hint: [doc-type api|architecture|operation|tradeoff|runbook] [target]
---

$ARGUMENTS 문서를 작성하세요.

## 문서 유형별 가이드

### api - API 문서
- 엔드포인트 목록 (메서드, 경로, 설명)
- 요청/응답 스키마 (JSON 예시 포함)
- 에러 코드 및 에러 응답 형식
- 인증/인가 요구사항
- 경로: `docs/api/{service-name}.md`

### architecture - 아키텍처 문서
- 시스템 전체 구조 (Mermaid 다이어그램)
- 서비스 간 통신 흐름
- 데이터 흐름도
- 기술 선택 근거
- 경로: `docs/architecture/`

### operation - 운영 가이드
- 배포 절차
- 모니터링 지표 및 알림 설정
- 로그 확인 방법
- 스케일링 가이드
- 경로: `docs/operation/`

### tradeoff - 트레이드오프 분석 (면접 대비)
- 문제 정의 (어떤 상황에서 이 결정이 필요했는가)
- 고려한 대안들
- 각 대안의 장단점 비교표
- 최종 선택과 근거
- 수치 데이터 (성능 비교, 벤치마크)
- 면접 예상 Q&A
- 경로: `docs/decisions/`

### runbook - 장애 대응 매뉴얼
- 장애 시나리오 (Redis 다운, Kafka 지연, 결제 타임아웃 등)
- 증상 및 탐지 방법
- 즉시 대응 절차
- 근본 원인 분석 가이드
- 사후 조치
- 경로: `docs/runbook/`

## 작성 원칙
- 명확하고 간결하게 (장황한 설명 금지)
- 다이어그램 포함 (Mermaid 형식)
- "왜 이렇게 했는가" 트레이드오프 명시
- 코드 예시 반드시 포함
- 한국어로 작성
