#!/usr/bin/env bash
# MOVRIT 로컬 통합 환경(백엔드 18080, DB movrit_integration) 실행 스크립트입니다.
#
# 사용법: scripts/integration.sh [start|stop|restart|status|build|logs]
#   start   : 실행 중이 아니면 서버를 시작합니다(jar가 없으면 먼저 빌드합니다).
#   restart : 실행 중인 서버를 멈추고 최신 jar로 다시 시작합니다.
#   build   : 테스트 없이 실행용 jar만 다시 만듭니다.
#
# 비밀값(Google·Kakao 키, JWT 서명 키)은 저장소 밖 환경 파일에서 읽습니다.
#   기본 위치: 저장소 상위 폴더의 .movrit-integration.env (MOVRIT_ENV_FILE로 변경 가능)
#   양식: scripts/integration.env.example
# 값이 비어 있으면 JWT는 실행마다 임의 생성되고(재시작 시 로그아웃), 소셜 로그인은 비활성됩니다.
# AI 초안 키(ANTHROPIC_API_KEY)는 환경 파일 또는 ~/.movrit/anthropic-key(권한 600)에서 읽습니다. 없으면 견본 초안으로 동작합니다.
set -euo pipefail

REPO="$(cd "$(dirname "$0")/.." && pwd)"
ENV_FILE="${MOVRIT_ENV_FILE:-$(dirname "$REPO")/.movrit-integration.env}"
RUN_DIR="$REPO/build/run"
PORT="${INTEGRATION_SERVER_PORT:-18080}"
LOG="$RUN_DIR/backend.log"

# JDK 17이 있으면 그대로 쓰고, 없으면 설치된 17 이상 JDK 중 하나로 빌드·실행합니다(macOS java_home 기준).
pick_java() {
  if [ -n "${JAVA_HOME:-}" ]; then return; fi
  if [ -x /usr/libexec/java_home ]; then
    JAVA_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || /usr/libexec/java_home -v 17+ 2>/dev/null || true)"
    export JAVA_HOME
  fi
}
java_major() { "${JAVA_HOME:+$JAVA_HOME/bin/}java" -version 2>&1 | awk -F'[".]' '/version/ {print ($2=="1")?$3:$2; exit}'; }

server_pid() { lsof -nP -iTCP:"$PORT" -sTCP:LISTEN -t 2>/dev/null | head -1 || true; }

build() {
  pick_java
  local v; v="$(java_major)"
  echo "Java $v 로 실행용 jar를 만듭니다."
  (cd "$REPO" && ./gradlew -q bootJar -x test -PmovritJavaVersion="$v")
}

start() {
  if [ -n "$(server_pid)" ]; then echo "이미 실행 중입니다(포트 $PORT)."; return; fi
  [ -f "$REPO/build/libs/movrit-0.0.1-SNAPSHOT.jar" ] || build
  pick_java
  mkdir -p "$RUN_DIR"
  # 실행 중 재빌드해도 서버 클래스가 깨지지 않도록 복사본으로 실행합니다.
  cp "$REPO/build/libs/movrit-0.0.1-SNAPSHOT.jar" "$RUN_DIR/movrit-run.jar"
  if [ -f "$ENV_FILE" ]; then set -a; . "$ENV_FILE"; set +a; else echo "환경 파일이 없습니다: $ENV_FILE (소셜 로그인 비활성)"; fi
  : "${GOOGLE_CLIENT_ID:=local-unconfigured}" "${GOOGLE_CLIENT_SECRET:=local-unconfigured}" "${KAKAO_CLIENT_ID:=local-unconfigured}"
  [ -n "${JWT_SECRET:-}" ] || JWT_SECRET="$(openssl rand -hex 32)"
  if [ -z "${ANTHROPIC_API_KEY:-}" ] && [ -f "$HOME/.movrit/anthropic-key" ]; then ANTHROPIC_API_KEY="$(cat "$HOME/.movrit/anthropic-key")"; fi
  export GOOGLE_CLIENT_ID GOOGLE_CLIENT_SECRET KAKAO_CLIENT_ID KAKAO_CLIENT_SECRET="${KAKAO_CLIENT_SECRET:-}" JWT_SECRET DB_PASSWORD="${DB_PASSWORD:-}"
  export ANTHROPIC_API_KEY="${ANTHROPIC_API_KEY:-}" # secret-check: allow (환경 변수·키 파일 값을 그대로 넘깁니다)
  (cd "$REPO" && nohup "${JAVA_HOME:+$JAVA_HOME/bin/}java" -jar "$RUN_DIR/movrit-run.jar" --spring.profiles.active=integration >"$LOG" 2>&1 &)
  for _ in $(seq 1 40); do
    sleep 1
    if grep -q "Started MovritApplication" "$LOG"; then
      echo "서버가 시작되었습니다: http://localhost:$PORT (로그: $LOG)"
      [ "$GOOGLE_CLIENT_ID" = local-unconfigured ] && echo "  Google 로그인: 미설정" || echo "  Google 로그인: 설정됨"
      [ "$KAKAO_CLIENT_ID" = local-unconfigured ] && echo "  카카오 로그인: 미설정" || echo "  카카오 로그인: 설정됨"
      [ -n "$ANTHROPIC_API_KEY" ] && echo "  AI 초안: Claude 사용" || echo "  AI 초안: 견본(키 없음)"
      return
    fi
    if grep -q "APPLICATION FAILED" "$LOG"; then echo "서버 시작에 실패했습니다. 로그를 확인해 주세요: $LOG"; exit 1; fi
  done
  echo "40초 안에 시작을 확인하지 못했습니다. 로그를 확인해 주세요: $LOG"; exit 1
}

stop() {
  local pid; pid="$(server_pid)"
  if [ -z "$pid" ]; then echo "실행 중인 서버가 없습니다."; return; fi
  kill "$pid"; for _ in $(seq 1 15); do [ -z "$(server_pid)" ] && break; sleep 1; done
  echo "서버를 멈췄습니다."
}

case "${1:-start}" in
  start) start ;;
  stop) stop ;;
  restart) stop; start ;;
  status) [ -n "$(server_pid)" ] && echo "실행 중(포트 $PORT, PID $(server_pid))" || echo "멈춤" ;;
  build) build ;;
  logs) tail -n 100 -f "$LOG" ;;
  *) echo "사용법: $0 [start|stop|restart|status|build|logs]"; exit 1 ;;
esac
