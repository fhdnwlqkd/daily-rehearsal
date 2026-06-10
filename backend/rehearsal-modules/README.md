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
    - `com.rehearsal.domain.slot`: slot 도메인 모델, repository port, usecase interface
- **포트(Interface)**:
    - `rehearsal-api`가 호출할 **UseCase** 인터페이스
    - `datasource`가 구현할 **Port**(Repository 인터페이스, 외부 API 인터페이스 등)
- **의존성**: 현재 공통 `ErrorCode`에서 HTTP 상태를 함께 관리하기 위해 `spring-web`의 `HttpStatus`를 사용합니다.

### 2. `datasource` (Infrastructure & Adapters)
`domain`에서 정의한 인터페이스를 실제로 구현하는 인프라스트럭처 영역입니다.
- **역할**: 
    - **Persistence**: JPA Repository 구현, Redis 데이터 저장 및 조회
    - **External API**: `WebClient` 기반 외부 서비스(STT, 날씨, LLM, VTON 등) 호출 클라이언트 구현
    - **Infrastructure Adapter**: DB/Redis 관련 adapter 구현
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

로컬 개발 환경은 Docker Compose로 MySQL과 Redis를 실행합니다.

```bash
cd docker
docker compose up -d
```

실행되는 컨테이너는 다음과 같습니다.

| 서비스 | 이미지 | 포트 | 용도 |
| --- | --- | --- | --- |
| MySQL | `mysql:8.0` | `3306` | 로컬 RDB (rehearsal), Flyway migration 대상 |
| Redis | `redis:7.0-alpine` | `6379` | 세션 상태, 작업 ID, polling 상태 저장 |

MySQL 초기화 스크립트는 `docker/init.sql`에 있습니다. 로컬 데이터는 `docker/mysql_data`에 저장되며 git에는 포함하지 않습니다.

## Spring Profile

설정 파일은 `rehearsal-api` 모듈의 profile별로 분리되어 있습니다.

| 파일 | 용도 |
| --- | --- |
| `application.yml` | 공통 설정, 서버 포트, 기본 active profile |
| `application-local.yml` | 로컬 MySQL/Redis/Flyway 설정 |
| `application-test.yml` | 테스트용 H2 설정 |

기본 active profile은 `local`입니다.

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
