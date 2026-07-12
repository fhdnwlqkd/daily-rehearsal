# Backend 배포 파이프라인

`release/backend` 브랜치에 머지(push)되면 GitHub Actions가 자동으로 Docker 이미지를 빌드해서 ECR에 올리고, EC2에 SSH로 접속해 새 이미지로 컨테이너를 재기동합니다. 서버에서 직접 `git pull`이나 빌드를 하지 않습니다.

```
release/backend에 merge
  → test (./gradlew test)
  → Docker 이미지 빌드 & ECR push (태그: git commit SHA, latest)
  → EC2에 SSH 접속 → 새 이미지 pull → app 컨테이너만 재기동
```

워크플로우 정의: [`.github/workflows/deploy-backend.yml`](../../../.github/workflows/deploy-backend.yml)

## 최초 1회 AWS 준비 (수동)

아래는 코드로 자동화되지 않는, 사람이 한 번만 해두면 되는 작업입니다.

### 1. ECR 리포지토리 생성

```bash
aws ecr create-repository --repository-name daily-rehearsal-backend
```

생성 후 리포지토리 URI(`<계정ID>.dkr.ecr.<region>.amazonaws.com/daily-rehearsal-backend`)를 기록해둔다. 이 중 호스트 부분(`<계정ID>.dkr.ecr.<region>.amazonaws.com`)이 `ECR_REGISTRY`, 리포지토리 이름이 `ECR_REPOSITORY`.

### 2. EC2 인스턴스 생성

- AMI: Ubuntu 22.04 LTS
- 인스턴스 타입: 처음 테스트는 **t3.small(2GB RAM) 권장**. MySQL + Redis + Spring Boot(JVM)가 한 인스턴스에 같이 뜨기 때문에 1GB RAM(t2/t3.micro)은 빠듯할 수 있다. 굳이 micro로 시작하고 싶다면 swap 파일을 잡아두거나(`fallocate -l 1G /swapfile ...`) MySQL `innodb_buffer_pool_size`를 낮춰서 테스트한다.
- 보안 그룹: `22`(SSH, 내 IP만), `8080`(앱, 필요 범위만) 오픈. `3306`(MySQL), `6379`(Redis)는 **외부에 열지 않는다** — 같은 인스턴스 안에서 컨테이너끼리만 통신하면 되기 때문.
- 인스턴스는 나중에 언제든 stop → 타입 변경 → start로 스펙을 올릴 수 있다. 이 파이프라인은 `EC2_HOST`(IP/도메인) 하나만 알면 되므로, 스펙을 바꿔도 워크플로우나 이 문서의 나머지 절차는 그대로다.

### 3. EC2에 Docker 설치

SSH로 접속해서:

```bash
sudo apt-get update
sudo apt-get install -y docker.io docker-compose-plugin awscli
sudo usermod -aG docker $USER
# 재로그인 후 docker 명령이 sudo 없이 되는지 확인
```

### 4. 배포 디렉토리 준비

```bash
sudo mkdir -p /opt/daily-rehearsal
sudo chown $USER:$USER /opt/daily-rehearsal
```

레포의 `backend/rehearsal-modules/docker/docker-compose.prod.yml`을 `/opt/daily-rehearsal/docker-compose.prod.yml`로 복사한다 (scp 또는 직접 붙여넣기).

같은 디렉토리에 `.env` 파일을 만든다 (git에는 없음, EC2에만 존재). 레포의 [`docker/.env.example`](../docker/.env.example)을 `/opt/daily-rehearsal/.env`로 복사한 뒤 값을 채운다.

```bash
cp docker/.env.example /opt/daily-rehearsal/.env
# /opt/daily-rehearsal/.env 를 열어서 DB_PASSWORD, GEMINI_API_KEY, ECR_IMAGE 등 값을 채운다
```

### 5. 최초 수동 기동으로 확인

```bash
cd /opt/daily-rehearsal
aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <ECR_REGISTRY>
docker compose -f docker-compose.prod.yml up -d
curl http://localhost:8080/actuator/health   # {"status":"UP"} 확인
```

### 6. IAM 사용자 + GitHub Secrets 등록

ECR push 권한(`AmazonEC2ContainerRegistryPowerUser`)을 가진 IAM 사용자를 만들고 access key를 발급한다. GitHub repo → Settings → Secrets and variables → Actions에 아래를 등록:

| Secret | 설명 |
| --- | --- |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | 위에서 만든 IAM 사용자 키 |
| `AWS_REGION` | 예: `ap-northeast-2` |
| `ECR_REPOSITORY` | `daily-rehearsal-backend` |
| `ECR_REGISTRY` | `<계정ID>.dkr.ecr.<region>.amazonaws.com` |
| `EC2_HOST` | EC2 퍼블릭 IP 또는 도메인 |
| `EC2_SSH_USER` | 보통 `ubuntu` |
| `EC2_SSH_KEY` | EC2 접속용 private key(PEM) 전체 내용 |

> 보안 강화(나중에): access key 대신 GitHub OIDC + IAM Role로 바꾸면 장기 자격증명을 GitHub에 저장하지 않아도 된다. 지금은 access key로 시작하고 익숙해진 뒤 전환을 권장.

이후부터는 `release/backend`에 머지하면 자동으로 배포된다.

## 인프라를 나중에 바꾸는 경우

- **인스턴스 스펙만 변경**: EC2 stop → 타입 변경 → start. `EC2_HOST` secret만 최신 IP로 갱신하면 끝.
- **EC2 → ECS/Fargate 같은 다른 배포 대상으로 전환**: `test`, `build-and-push` job은 그대로 재사용하고, `deploy` job만 SSH 방식에서 `aws ecs update-service` 등으로 교체하면 된다.
- **MySQL/Redis를 RDS/ElastiCache로 분리**: 앱은 `DB_HOST`/`REDIS_HOST`를 포함한 모든 연결 정보를 환경변수로만 읽는다(`application-prod.yml`). `.env`의 호스트값을 관리형 서비스 엔드포인트로 바꾸고 `docker-compose.prod.yml`에서 `mysql`/`redis` 서비스를 빼면 된다. 이미지 재빌드나 코드 변경은 필요 없다.

## 로컬 검증

```bash
# 이미지 빌드
docker build -f backend/rehearsal-modules/rehearsal-api/Dockerfile -t rehearsal-api backend/rehearsal-modules

# compose로 mysql+redis+app 기동 (.env는 로컬 테스트용 더미 값)
cd backend/rehearsal-modules/docker
ECR_IMAGE=rehearsal-api IMAGE_TAG=latest docker compose -f docker-compose.prod.yml up -d

curl http://localhost:8080/actuator/health
```
