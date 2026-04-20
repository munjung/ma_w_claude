# MA Hub — MSA Spring Boot 자동 생성 시스템 계획서

> 작성일: 2026-04-20  
> 상태: DRAFT

---

## 1. 요구사항 요약

| 항목 | 내용 |
|------|------|
| 입력 | 자체 설계 Markdown 포맷 인터페이스 정의서 (.md) |
| 출력 | Spring Boot 서비스 코드, Docker/k8s 매니페스트, API Gateway 설정 |
| 코드 생성 엔진 | Claude API (LLM 주체) |
| 서비스 형태 | Web UI 포함 Spring Boot 애플리케이션 |
| 통신 방식 | 인터페이스 정의서에서 지정 (REST / Kafka / 혼합 가능) |
| 포맷 우선순위 | 사람이 읽고 쓰기 쉬운 가독성 |

---

## 2. 인터페이스 정의서 포맷 설계

### 2.1 설계 원칙
- **가독성 우선**: 개발자가 별도 교육 없이 작성 가능한 plain Markdown
- **LLM 파싱 친화적**: 섹션 헤더(##)와 키-값 패턴으로 구조화
- **확장 가능**: 통신 방식(REST/Kafka) 유연 지정
- **이중 목적**: 코드 생성 입력 + API 문서로 동시 활용

### 2.2 포맷 스펙 (초안)

```markdown
# Service: {service-name}

## Description
{서비스 한 줄 설명}

## Domain Models

### {EntityName}
- {field}: {type} [{constraint}]
  예) id: Long [PK, auto]
      username: String [required, unique, max=50]
      email: String [required, unique]
      createdAt: LocalDateTime [auto]

## APIs

### {HTTP_METHOD} {path}
- Summary: {한 줄 설명}
- Auth: none | jwt | api-key
- Request: {RequestDto} { field: type, ... }
- Response: {ResponseDto} { field: type, ... }
- Errors: 400 | 404 | 409

## Dependencies

### {target-service}
- {HTTP_METHOD} {path}: {용도 설명}

## Events

### Published
- {topic-name}: {payload schema 또는 설명}

### Consumed
- {topic-name}: {처리 내용}

## Communication
- Outbound: rest | kafka | rest+kafka
- DB: postgresql | mysql | h2 | mongodb

## Config
- port: {port}
- context-path: /{service-name}
```

### 2.3 실제 예시 (user-service.md)

```markdown
# Service: user-service

## Description
사용자 계정 생성, 인증, 프로필 관리 서비스

## Domain Models

### User
- id: Long [PK, auto]
- username: String [required, unique, max=50]
- email: String [required, unique]
- passwordHash: String [required]
- createdAt: LocalDateTime [auto]

## APIs

### POST /users
- Summary: 신규 사용자 등록
- Auth: none
- Request: CreateUserRequest { username: String, email: String, password: String }
- Response: UserResponse { id: Long, username: String, email: String }
- Errors: 409

### GET /users/{id}
- Summary: 사용자 단건 조회
- Auth: jwt
- Response: UserResponse { id: Long, username: String, email: String }
- Errors: 404

## Dependencies

### order-service
- GET /orders?userId={id}: 해당 사용자의 주문 목록 조회

## Events

### Published
- user.created: { userId: Long, email: String, createdAt: String }

### Consumed
- (none)

## Communication
- Outbound: rest
- DB: postgresql

## Config
- port: 8081
- context-path: /user-service
```

---

## 3. 시스템 아키텍처

### 3.1 MA Hub 전체 구조

```
┌─────────────────────────────────────────────────────┐
│                    MA Hub (Web App)                 │
│                                                     │
│  ┌──────────┐    ┌─────────────────────────────┐   │
│  │ Web UI   │───▶│      MA Hub Backend         │   │
│  │(React /  │    │    (Spring Boot 3.x)         │   │
│  │ Thymeleaf│    │                             │   │
│  └──────────┘    │  ┌─────────┐ ┌───────────┐ │   │
│                  │  │ MD      │ │ IR        │ │   │
│                  │  │ Parser  │▶│ Builder   │ │   │
│                  │  └─────────┘ └─────┬─────┘ │   │
│                  │                    │       │   │
│                  │  ┌─────────────────▼─────┐ │   │
│                  │  │  Claude API Client    │ │   │
│                  │  │  (Prompt Builder +    │ │   │
│                  │  │   Response Parser)    │ │   │
│                  │  └─────────────────┬─────┘ │   │
│                  │                    │       │   │
│                  │  ┌─────────────────▼─────┐ │   │
│                  │  │  Project Assembler    │ │   │
│                  │  │  (파일 구조 + ZIP)     │ │   │
│                  │  └───────────────────────┘ │   │
│                  └─────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
              │
              ▼
    Claude API (claude-sonnet-4-6)
```

### 3.2 생성 파이프라인 흐름

```
.md 파일 업로드
    │
    ▼
MD Parser → Internal Representation (IR)
    │         - ServiceSpec[]
    │         - DomainModel[]
    │         - ApiSpec[]
    │         - DependencyGraph
    ▼
Validation & Dependency Resolution
    │         - 서비스 간 참조 검증
    │         - 포트 충돌 체크
    ▼
Generation Job 생성 (비동기)
    │
    ├──▶ [Service별 병렬 실행]
    │         │
    │         ▼
    │    Prompt Builder
    │         │  - 도메인 모델 컨텍스트
    │         │  - API 스펙 컨텍스트
    │         │  - 의존 서비스 Feign 인터페이스
    │         ▼
    │    Claude API 호출
    │         │  - Controller 생성
    │         │  - Service 생성
    │         │  - Repository 생성
    │         │  - DTO 생성
    │         ▼
    │    Response Parser
    │         │  - 코드블록 추출
    │         │  - 파일 경로 매핑
    │
    ├──▶ [공통 생성]
    │         - pom.xml (per service)
    │         - application.yml (per service)
    │         - Dockerfile (per service)
    │
    ├──▶ [인프라 생성 (템플릿 기반)]
    │         - docker-compose.yml
    │         - k8s manifests (Deployment, Service, Ingress)
    │         - API Gateway 라우팅 설정
    │         - Feign Client 인터페이스
    │
    ▼
Project Assembler
    │  - 파일 트리 구성
    │  - ZIP 패키징
    ▼
다운로드 제공 / 파일 브라우저 UI
```

---

## 4. 출력 디렉터리 구조

```
generated-project/
├── docker-compose.yml
├── api-gateway/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/resources/
│       └── application.yml          ← 라우팅 규칙 포함
├── k8s/
│   ├── namespace.yaml
│   ├── {service-name}-deployment.yaml
│   ├── {service-name}-service.yaml
│   └── ingress.yaml
└── {service-name}/                  ← 서비스별 반복
    ├── pom.xml
    ├── Dockerfile
    └── src/
        ├── main/
        │   ├── java/com/example/{service}/
        │   │   ├── {ServiceName}Application.java
        │   │   ├── controller/
        │   │   │   └── {Entity}Controller.java
        │   │   ├── service/
        │   │   │   └── {Entity}Service.java
        │   │   ├── repository/
        │   │   │   └── {Entity}Repository.java
        │   │   ├── domain/
        │   │   │   └── {Entity}.java
        │   │   ├── dto/
        │   │   │   ├── {Entity}Request.java
        │   │   │   └── {Entity}Response.java
        │   │   ├── client/            ← 의존 서비스 Feign Client
        │   │   │   └── {Target}Client.java
        │   │   └── config/
        │   │       └── SecurityConfig.java
        │   └── resources/
        │       └── application.yml
        └── test/...
```

---

## 5. MA Hub 기술 스택

| 레이어 | 기술 | 비고 |
|--------|------|------|
| Backend | Spring Boot 3.3 (Java 21) | MA Hub 서버 |
| Web UI | React (Vite) + TypeScript | 또는 Thymeleaf (단순화 시) |
| MD 파싱 | Flexmark-java | CommonMark 확장 지원 |
| LLM 연동 | Anthropic Java SDK | claude-sonnet-4-6 기본 모델 |
| 비동기 처리 | Spring WebFlux + Virtual Thread | 생성 잡 병렬 실행 |
| 코드 렌더링 | FreeMarker | pom.xml, application.yml 등 구조적 파일 |
| 파일 패키징 | Java ZipOutputStream | 생성 결과 다운로드 |
| DB | PostgreSQL | 잡 이력, 정의서 저장 |
| 컨테이너 | Docker Compose | MA Hub 자체 배포 |

---

## 6. Claude API 프롬프트 전략

### 6.1 아티팩트별 프롬프트 분리
단일 거대 프롬프트 대신 아티팩트별로 분리 호출:

```
1회 호출 → Domain Entity + DTO 생성
2회 호출 → Repository 인터페이스 생성
3회 호출 → Service 클래스 생성
4회 호출 → Controller 클래스 생성
5회 호출 → Feign Client 인터페이스 생성 (의존 서비스 있을 때)
```

### 6.2 프롬프트 컨텍스트 구성

```
System Prompt:
  - Spring Boot 3.x, Java 21 코드 생성 전문가 역할
  - 출력 형식: 파일경로와 코드블록만 출력
  - 스타일 규칙 (Lombok 사용, JPA 어노테이션, etc.)

User Prompt:
  - 서비스명, 도메인 모델 전체
  - 이전 단계 생성 코드 (컨텍스트 누적)
  - 생성할 아티팩트 지시
```

### 6.3 프롬프트 캐싱 활용
- System Prompt + 공통 스타일 가이드 → **캐시 앵커**로 고정
- 서비스별 반복 호출 시 토큰 비용 절감

---

## 7. 구현 단계 (Phases)

### Phase 1: 인터페이스 정의서 포맷 확정 + 파서 구현
**목표**: .md 파일 → IR(Internal Representation) 변환
- [ ] 포맷 스펙 문서 확정 (섹션 정의, 필드 타입 목록)
- [ ] Flexmark 기반 MD 파서 구현
- [ ] IR 데이터 모델 정의 (`ServiceSpec`, `DomainModel`, `ApiEndpoint`, `DependencyGraph`)
- [ ] 파서 단위 테스트 (샘플 .md 5종)
- [ ] 포맷 유효성 검증기 구현 (필수 섹션 누락 오류 등)

**인수 조건**:
- 샘플 .md 5종 파싱 성공률 100%
- 잘못된 포맷 입력 시 사용자 친화적 오류 메시지 반환

### Phase 2: MA Hub Backend 기반 구축
**목표**: Spring Boot 프로젝트 + DB + Job 관리
- [ ] Spring Boot 3.3 프로젝트 생성 (Web, JPA, Security, WebFlux)
- [ ] PostgreSQL 스키마 설계 (`generation_job`, `interface_definition`, `generated_artifact`)
- [ ] 파일 업로드 API (`POST /api/definitions`)
- [ ] 생성 잡 API (`POST /api/generate`, `GET /api/jobs/{id}`)
- [ ] 결과 다운로드 API (`GET /api/jobs/{id}/download`)

**인수 조건**:
- .md 파일 업로드 후 파싱 결과 JSON 반환 < 500ms
- 생성 잡 상태 조회 실시간 반영 (SSE 또는 polling)

### Phase 3: Claude API 코드 생성 엔진
**목표**: IR → Spring Boot 코드 생성
- [ ] Anthropic Java SDK 연동 설정
- [ ] 프롬프트 빌더 구현 (아티팩트별 5종)
- [ ] 응답 파서 구현 (코드블록 → 파일 경로 + 내용 매핑)
- [ ] 생성 실패 시 재시도 로직 (최대 3회)
- [ ] 프롬프트 캐싱 설정 (System Prompt 캐시 앵커)
- [ ] 서비스 간 병렬 생성 (Virtual Thread 기반)

**인수 조건**:
- 서비스 1개(엔드포인트 5개) 기준 생성 완료 < 60초
- 서비스 5개 병렬 생성 시 순차 대비 3배 이상 속도 향상
- 생성된 코드 컴파일 오류율 < 10%

### Phase 4: 인프라 파일 생성
**목표**: 템플릿 기반 인프라 코드 생성
- [ ] FreeMarker 템플릿 작성:
  - `pom.xml` (서비스별 의존성 자동 결정)
  - `application.yml` (서비스명, 포트, DB 설정)
  - `Dockerfile` (멀티스테이지 빌드)
  - `docker-compose.yml` (전체 서비스 오케스트레이션)
  - `k8s/` manifests (Deployment, Service, Ingress)
  - API Gateway 라우팅 설정
- [ ] Feign Client 인터페이스 자동 생성 (의존 서비스 기반)

**인수 조건**:
- `docker-compose up` 실행 시 모든 서비스 기동 성공
- k8s manifest `kubectl apply` 오류 없음

### Phase 5: Web UI 구현
**목표**: 사용자 인터페이스 완성
- [ ] .md 파일 에디터 (Monaco Editor) + 업로드 UI
- [ ] 생성 잡 진행 상황 표시 (서비스별 진행률)
- [ ] 생성 결과 파일 트리 브라우저
- [ ] 코드 프리뷰 (신택스 하이라이팅)
- [ ] ZIP 다운로드 버튼

**인수 조건**:
- 브라우저에서 .md 작성 → 생성 → 다운로드 전체 플로우 동작
- 생성 중 에러 발생 시 UI에 명확한 오류 표시

---

## 8. 리스크 및 대응

| 리스크 | 가능성 | 대응 방안 |
|--------|--------|----------|
| LLM 생성 코드 컴파일 오류 | 높음 | 후처리 검증 + 재시도, 오류 라인 강조 표시 |
| 복잡한 비즈니스 로직 미생성 | 중간 | 스캐폴딩 위주 + TODO 주석으로 생성 범위 명시 |
| Claude API 응답 지연 | 중간 | 타임아웃 설정, 비동기 잡 처리, 진행률 표시 |
| .md 포맷 파싱 실패 | 중간 | 단계적 포맷 검증 + 사용자 친화적 오류 가이드 |
| 서비스 간 의존성 순환 참조 | 낮음 | 의존성 그래프 사이클 검출 후 오류 반환 |
| 포트 충돌 | 낮음 | 포트 자동 할당 + 충돌 감지 |

---

## 9. 검증 단계

| 항목 | 검증 방법 |
|------|----------|
| 파서 정확도 | 샘플 .md 파일 10종 파싱 후 IR 일치율 100% 확인 |
| 코드 컴파일 | 생성된 서비스 `mvn compile` 통과 여부 |
| 컨테이너 기동 | `docker-compose up` 후 각 서비스 `/actuator/health` 200 응답 |
| API 호출 | 생성된 엔드포인트 curl 호출 정상 응답 확인 |
| UI E2E | .md 업로드 → 생성 → 다운로드 → 빌드 성공 전체 플로우 |

---

## 10. 향후 확장 포인트

- **Git 연동**: 생성 결과를 GitHub 레포지토리로 직접 push
- **Kafka 이벤트 코드 생성**: `Events` 섹션 기반 Kafka Producer/Consumer 자동 생성
- **테스트 코드 생성**: JUnit5 + Mockito 기반 단위/통합 테스트 자동 생성
- **OpenAPI 변환**: 생성된 Controller에서 Swagger 문서 역추출
- **CLI 모드**: `ma-hub generate --input ./specs/` 명령어 지원
