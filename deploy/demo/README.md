# 공개 시연 서버 배포

클라우드 서버 1대에 DB·백엔드·화면·HTTPS를 Docker로 함께 띄우는 방법입니다. 포트폴리오 시연용이며, 실제 서비스 운영 설정은 아닙니다.

## 구성

```
인터넷 ─443/80─> Caddy(HTTPS) ─> frontend(nginx: 화면, /api·/uploads 중계) ─> backend(Spring Boot) ─> db(PostgreSQL 18)
```

- 밖으로 열리는 포트는 Caddy의 80·443뿐입니다. DB와 백엔드는 Docker 내부 네트워크에서만 접속합니다.
- 백엔드는 `integration,prod` 프로필로 뜹니다. 인증번호는 메일·문자 대신 화면에 보이고, SQL 로그·오류 상세·CORS는 운영 기준을 따릅니다.
- 업로드 파일은 `upload-data` 볼륨의 `/data/uploads`(공개), `/data/uploads-private`(구매 인증·채팅 첨부 등), `/data/uploads-campaign-private`(캠페인 원본 이미지)에 저장됩니다.

## 1. 서버 준비

- 권장 사양: 2 vCPU, 메모리 4GB, 디스크 30GB 이상, Ubuntu 24.04. 메모리 2GB 서버는 이미지를 빌드하다 멈출 수 있어 스왑(2GB 이상)을 먼저 켭니다.
- 방화벽(보안 그룹)에서 22(SSH), 80, 443만 엽니다.
- Docker 설치: Docker 공식 문서의 Ubuntu 설치 절차(Docker Engine + Compose 플러그인)를 따릅니다.

## 2. 코드 받기

두 저장소를 같은 폴더에 나란히 받습니다.

```bash
git clone -b feature/brand https://github.com/yoo-jin-k/movrit-backend.git movrit
git clone -b developer https://github.com/yoo-jin-k/movrit-frontend.git movrit-frontend
cd movrit/deploy/demo
cp demo.env.example .env && chmod 600 .env   # 값을 직접 채웁니다
```

## 3. 시연 데이터 옮기기 (로컬 → 서버)

로컬에서 묶습니다.

```bash
cd deploy/demo
./export-demo-data.sh            # data/movrit-demo.dump, data/movrit-uploads.tgz
scp data/movrit-demo.dump data/movrit-uploads.tgz <서버>:~/movrit/deploy/demo/data/
```

서버에서 DB만 먼저 띄우고 복원한 뒤, 업로드 파일을 볼륨에 풉니다.

```bash
docker compose up -d db
docker compose exec -T db pg_restore -U movrit_user -d movrit --no-owner --role=movrit_user < data/movrit-demo.dump
docker compose build backend
docker compose run --rm --no-deps -v "$PWD/data:/import:ro" --entrypoint sh backend \
  -c 'tar -xzf /import/movrit-uploads.tgz -C /data'
```

- 덤프에는 Flyway 이력도 들어 있어, 백엔드가 뜰 때 이미 적용된 버전은 건너뛰고 새 버전만 적용합니다.
- 데이터 없이 빈 DB로 시작하려면 이 단계를 건너뜁니다. 첫 실행 때 Flyway가 V1~최신 구조를 만듭니다.

## 4. 실행

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f backend   # "Started MovritApplication"이 보이면 준비 완료
```

브라우저에서 `https://<SITE_ADDRESS>`로 접속합니다. 첫 접속 때 Caddy가 인증서를 받느라 몇 초 걸릴 수 있습니다.

## 5. 업데이트

```bash
(cd ~/movrit && git pull) && (cd ~/movrit-frontend && git pull)
cd ~/movrit/deploy/demo && docker compose up -d --build
```

## 확인할 것

- 실시간 채팅: 두 브라우저(일반 창·시크릿 창)에서 업체·참여자로 로그인해 메시지를 주고받습니다.
- 지도: NCP 콘솔 Maps 앱의 Web 서비스 URL에 `https://<SITE_ADDRESS>`를 등록해야 보입니다.
- 소셜 로그인: 각 개발자 앱에 `https://<SITE_ADDRESS>/api/auth/{google|kakao}/callback`을 등록해야 합니다.

## 주의

- 시연 계정 비밀번호를 공개하면 누구나 그 계정으로 데이터를 바꿀 수 있습니다. 공개할 계정과 비밀번호를 정하고, 필요하면 주기적으로 3단계 복원을 다시 해 초기 상태로 되돌립니다.
- `ANTHROPIC_API_KEY`를 넣으면 방문자가 AI 기능을 부를 때마다 비용이 생깁니다.
- 백업: `docker compose exec -T db pg_dump -U movrit_user -d movrit -Fc > backup.dump`
- 끄기: `docker compose down` · 데이터까지 지우기: `docker compose down -v`(되돌릴 수 없음)
