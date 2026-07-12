# Rehearsal Modules

Daily Rehearsal 프로젝트의 백엔드 API입니다.

## 프로젝트 구조 및 모듈 전략

본 프로젝트는 레이어드 아키텍처와 클린 아키텍처의 개념을 차용한 3개 모듈 구조로 구성되어 있습니다. 모든 패키지는 `com.rehearsal`로 시작합니다.

```text
rehearsal-modules
├── domain
├── datasource
└── rehearsal-api
```

### 1. `domain` (Core Business Logic)
핵심 도메인 모델과 도메인 경계를 정의하는 영역입니다. 세부 도메인은 하위 Gradle 모듈로 나누지 않고 `src` 내부 패키지로 구분합니다.
- **역할**: 핵심 비즈니스 로직, 도메인 모델(Entity/VO), 비즈니스 규칙, 공통 예외 정의
- **패키지 예시**:
    - `com.rehearsal.domain.core`: 공통 예외, 에러 코드
    - `com.rehearsal.domain.slot`: 상황 및 slot 관련 Enum 스키마 정의 (정적 설정)
- **포트(Interface)**:
    - `rehearsal-api`가 호출할 **UseCase** 인터페이스
    - `datasource`가 구현할 **Port**(Repository 인터페이스, 외부 API 인터페이스 등)
- **의존성**: 현재 공통 `ErrorCode`에서 HTTP 상태를 함께 관리하기 위해 `spring-web`의 `HttpStatus`를 사용합니다.

### 2. `datasource` (Infrastructure & Adapters)
`domain`에서 정의한 인터페이스를 실제로 구현하는 인프라스트럭처 영역입니다.
- **역할**: 
    - **Persistence**: JPA Repository와 MySQL 데이터 저장 및 조회
    - **External API**: `WebClient` 기반 외부 서비스(STT, 날씨, LLM, VTON 등) 호출 클라이언트 구현
    - **Infrastructure Adapter**: DB 관련 adapter 구현
- **패키지 예시**:
    - `com.rehearsal.datasource.dbintegrated`: JPA 기반 DB adapter
- **의존성**: `:domain`

### 3. `rehearsal-api` (Application & Presentation)
사용자의 요청을 받고 전체 흐름을 제어하는 진입점입니다.
- **역할**: 
    - **Presentation**: REST Controllers, 요청/응답 DTO
    - **Application**: UseCase 구현체를 통한 비즈니스 흐름 제어(Orchestration)
    - **Configuration**: Spring Boot 메인 애플리케이션, JPA 설정, Flyway migration, Swagger, 공통 예외/응답 처리
- **의존성**: `:domain`, `:datasource`
    - `:domain`은 컴파일 의존성으로 사용합니다.
    - `:datasource`는 adapter bean 등록을 위해 런타임 의존성으로 사용합니다.

## 로컬 인프라 실행

로컬 개발 환경은 Docker Compose로 MySQL을 실행합니다.

```bash
cd docker
docker compose up -d
```

실행되는 컨테이너는 다음과 같습니다.

| 서비스 | 이미지 | 포트 | 용도 |
| --- | --- | --- | --- |
| MySQL | `mysql:8.0` | `3306` | 로컬 RDB (rehearsal), Flyway migration 대상 |

MySQL 초기화 스크립트는 `docker/init.sql`에 있습니다. 로컬 데이터는 `docker/mysql_data`에 저장되며 git에는 포함하지 않습니다.

## Spring Profile

설정 파일은 `rehearsal-api` 모듈의 profile별로 분리되어 있습니다.

| 파일 | 용도 |
| --- | --- |
| `application.yml` | 공통 설정, 서버 포트, 기본 active profile |
| `application-local.yml` | 로컬 MySQL/Flyway 설정 |
| `application-test.yml` | 테스트용 H2 설정 |

기본 active profile은 `local`입니다.

## AI Provider

AI 외부 연동 설정은 `rehearsal-api/src/main/resources/application.yml`의 `rehearsal.ai` 아래에서 관리합니다.

AI provider는 Gemini 하나로 고정합니다. OpenAI/Gemini 혼용이나 task별 provider routing은 두지 않습니다. `rehearsal.ai.defaults.provider`의 기본값은 `gemini`이며, `OPENAI_API_KEY` 같은 다른 provider의 설정은 필요하지 않습니다.

현재 실제 코드에 연결된 AI 작업은 `slot-extraction`입니다. 목표 플로우에서는 같은 Gemini provider로 다음 AI 작업을 처리합니다.

- `context-normalize`: 사용자 transcript를 slot 값으로 정규화
- `simulation-evaluation`: 현재 turn 성공/실패와 피드백 JSON 생성
- `simulation-next-line`: 다음 상대 발화 생성, 폴링(polling) 조회 대상
- `ticket-generation`: 최종 티켓 문구 생성

`rehearsal.ai.defaults.provider`가 가질 수 있는 값은 다음 둘뿐입니다.

| provider | 용도 | API key |
| --- | --- | --- |
| `gemini` | 실제 AI 작업 (기본값) | `GEMINI_API_KEY` |
| `fake` | 외부 호출 없이 deterministic 값을 반환, `test` profile 기본값 | 불필요 |

`gemini` provider를 선택한 상태에서 `GEMINI_API_KEY`가 비어 있으면 애플리케이션 기동이 실패합니다. `NONE`/unconfigured 같은 placeholder provider는 두지 않습니다.

설정 예시는 다음과 같습니다.

```yaml
rehearsal:
  ai:
    defaults:
      provider: ${REHEARSAL_AI_DEFAULT_PROVIDER:gemini}
    gemini:
      api-key: ${GEMINI_API_KEY:}
      model: ${GEMINI_MODEL:gemini-2.5-flash-lite}
      temperature: ${GEMINI_TEMPERATURE:0.0}
      thinking-budget: ${GEMINI_THINKING_BUDGET:0}
```

`test` profile(`application-test.yml`)은 `rehearsal.ai.defaults.provider: fake`를 고정값으로 설정해 API key 없이 테스트가 동작합니다.

로컬 실행 예시는 다음과 같습니다.

```bash
GEMINI_API_KEY=... \
./gradlew :rehearsal-api:bootRun
```

## Secret Management

API key와 password 같은 secret은 git에 커밋하지 않습니다. 애플리케이션은 secret 저장소를 직접 알지 않고, `application.yml`에 선언된 환경변수만 읽습니다.

로컬 개발에서는 backend 모듈의 `.env.example`을 참고해 개인별 `.env.local`을 만듭니다. `.env.local`과 `.env*` 파일은 gitignore 대상이고, 실제 값은 팀 password manager나 AWS Secrets Manager에서 공유합니다.

```bash
cp .env.example .env.local
```

터미널에서 실행할 때는 필요한 값을 export하거나 dotenv를 로드한 뒤 실행합니다.

```bash
export GEMINI_API_KEY=...
./gradlew :rehearsal-api:bootRun
```

AWS 배포에서는 secret과 일반 설정을 나눕니다.

| 구분 | 저장 위치 | 예시 |
| --- | --- | --- |
| Secret | AWS Secrets Manager | `GEMINI_API_KEY`, DB password |
| 일반 설정 | SSM Parameter Store 또는 배포 환경변수 | `GEMINI_MODEL`, `GEMINI_TEMPERATURE` |

권장 secret 이름은 환경을 포함한 계층형 이름을 사용합니다.

```text
/daily-rehearsal/local/gemini/api-key
/daily-rehearsal/prod/gemini/api-key
```

ECS/Fargate로 배포할 경우 task definition에서 Secrets Manager 값을 환경변수로 주입합니다. Spring Boot 앱은 AWS SDK로 secret을 직접 조회하지 않고, 주입된 `GEMINI_API_KEY`만 사용합니다.

## Database Migration

로컬 환경에서는 Flyway로 DB 형상을 관리합니다.

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
```

마이그레이션 파일은 Spring Boot 애플리케이션이 실행하는 `rehearsal-api` 모듈의 아래 경로에 추가합니다.

```text
rehearsal-api/src/main/resources/db/migration
```

파일명 예시는 다음과 같습니다.

```text
V1__create_context_slot_tables.sql
```

`local` profile에서는 Hibernate가 직접 테이블을 생성하지 않도록 `ddl-auto: validate`를 사용합니다. 실제 테이블 변경은 Flyway migration으로 처리합니다.

## Test

테스트는 H2 in-memory DB에서 실행합니다.

```bash
./gradlew test
```

`application-test.yml`에서는 Flyway를 비활성화하고 Hibernate `create-drop`을 사용합니다.

## 커밋 전 해야 할 일

커밋 전 Java 코드 포맷을 적용합니다.

```bash
./gradlew spotlessApply
```

`spotlessApply`는 모든 Java 코드에 Google Java Format을 적용하고, 사용하지 않는 import와 줄 끝 공백을 정리합니다.
