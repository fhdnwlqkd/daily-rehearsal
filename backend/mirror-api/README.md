# Mirror API

StyleCaster 스마트 거울의 백엔드 API입니다.

## 로컬 인프라 실행

로컬 개발 환경은 Docker Compose로 MySQL과 Redis를 실행합니다.

```bash
cd docker
docker compose up -d
```

실행되는 컨테이너는 다음과 같습니다.

| 서비스 | 이미지 | 포트 | 용도 |
| --- | --- | --- | --- |
| MySQL | `mysql:8.0` | `3306` | 로컬 RDB, Flyway migration 대상 |
| Redis | `redis:7.0-alpine` | `6379` | 세션 상태, 작업 ID, polling 상태 저장 |

MySQL 초기화 스크립트는 `docker/init.sql`에 있습니다. 로컬 데이터는 `docker/mysql_data`에 저장되며 git에는 포함하지 않습니다.

## Spring Profile

설정 파일은 profile별로 분리되어 있습니다.

| 파일 | 용도 |
| --- | --- |
| `src/main/resources/application.yml` | 공통 설정, 서버 포트, 기본 active profile |
| `src/main/resources/application-local.yml` | 로컬 MySQL/Redis/Flyway 설정 |
| `src/main/resources/application-test.yml` | 테스트용 H2 설정 |

기본 active profile은 `local`입니다.

## Database Migration

로컬 환경에서는 Flyway로 DB 형상을 관리합니다.

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
```

마이그레이션 파일은 아래 경로에 추가합니다.

```text
src/main/resources/db/migration
```

파일명 예시는 다음과 같습니다.

```text
V1__create_session_tables.sql
V2__create_risk_result_tables.sql
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
