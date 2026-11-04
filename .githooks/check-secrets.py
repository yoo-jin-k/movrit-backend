#!/usr/bin/env python3
"""커밋 직전에 올릴 내용(staged)에서 비밀값처럼 보이는 것을 찾아 커밋을 막습니다.

- 비밀값(키·토큰·비밀번호·DB 접속 정보)은 저장소에 두지 않고 환경 변수로만 받습니다.
- 찾은 값은 화면에 출력하지 않고 파일·줄·종류만 알려 줍니다.
- 환경 파일(.env 등)과 인증서·키 파일은 이름만으로도 막습니다(값이 없는 예시 파일은 허용).
- 테스트 코드의 'test'로 시작하는 가짜 값은 허용합니다.
정말 비밀값이 아니라면 해당 줄 끝에 `secret-check: allow` 주석을 붙여 예외로 둘 수 있습니다.

CI에서는 `--diff 이전커밋..지금커밋`으로 올라온 변경 전체를 같은 규칙으로 검사합니다.
"""
import re
import subprocess
import sys

STRONG = [
    ("Google API 키", re.compile(r"AIza[0-9A-Za-z_\-]{35}")),
    ("Google OAuth 비밀값", re.compile(r"GOCSPX-[0-9A-Za-z_\-]{20,}")),
    ("AWS 접근 키", re.compile(r"AKIA[0-9A-Z]{16}")),
    ("GitHub 토큰", re.compile(r"gh[pousr]_[0-9A-Za-z]{30,}")),
    ("Slack 토큰", re.compile(r"xox[baprs]-[0-9A-Za-z\-]{10,}")),
    ("개인 키", re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----")),
    ("JWT 토큰", re.compile(r"eyJ[0-9A-Za-z_\-]{10,}\.eyJ[0-9A-Za-z_\-]{10,}\.[0-9A-Za-z_\-]{10,}")),
    ("비밀번호가 든 DB 주소", re.compile(r"(?:postgres(?:ql)?|mysql|mongodb(?:\+srv)?)://[^:\s/]+:[^@\s$]{3,}@")),
]
# 이름이 비밀값을 뜻하는데 값이 직접 적힌 경우(환경 변수 참조·빈 값·변수 이름은 제외)
QUOTED = re.compile(
    r"(?i)\b([A-Z0-9_.\-]*(?:secret|password|passwd|api[_\-]?key|access[_\-]?token|refresh[_\-]?token|client[_\-]?secret|private[_\-]?key))"
    r"\s*[:=]\s*(['\"])([^'\"\s]{6,})\2"
)
PLAIN = re.compile(
    r"(?i)^\s*(?:export\s+)?([A-Z0-9_.\-]*(?:secret|password|api[_\-]?key|token))\s*=\s*([^\s$'\"#{][^\s#]{5,})\s*$"
)
SAFE_VALUE = re.compile(r"(?i)^(test|dummy|example|sample|changeme|placeholder)|^\*+$|^<.*>$")
BLOCKED_FILE = re.compile(r"(^|/)(\.env(\.[^/]*)?|[^/]*\.(pem|key|p12|jks|keystore))$")
ALLOWED_FILE = re.compile(r"(^|/)\.env\.(example|integration)$|\.example$")
TEST_PATH = re.compile(r"(^|/)(src/test/|scripts/[^/]*\.test\.mjs$)")
SKIP_FILE = re.compile(r"(package-lock\.json|\.min\.js|\.(svg|png|jpe?g|gif|webp|ico|mp4|woff2?|zip|jar))$")


def git(*args):
    return subprocess.run(["git", *args], capture_output=True, text=True, errors="replace").stdout


# 기본은 커밋 직전(staged) 검사, CI는 --diff RANGE 로 커밋 범위를 검사합니다.
if len(sys.argv) == 3 and sys.argv[1] == "--diff":
    TARGET = [sys.argv[2]]
else:
    TARGET = ["--cached"]

problems = []
for path in git("diff", *TARGET, "--name-only", "--diff-filter=ACMR").splitlines():
    if BLOCKED_FILE.search(path) and not ALLOWED_FILE.search(path):
        problems.append(f"{path}: 환경 파일·키 파일은 올릴 수 없습니다")

file = ""
line_no = 0
for line in git("diff", *TARGET, "--no-color", "--unified=0", "--diff-filter=ACMR").splitlines():
    if line.startswith("+++ "):
        file = line[6:] if line.startswith("+++ b/") else ""
        continue
    if line.startswith("@@"):
        m = re.search(r"\+(\d+)", line)
        line_no = int(m.group(1)) if m else 0
        continue
    if not line.startswith("+") or not file or SKIP_FILE.search(file):
        continue
    text = line[1:]
    where = f"{file}:{line_no}"
    line_no += 1
    if "secret-check: allow" in text:
        continue
    for name, rx in STRONG:
        if rx.search(text):
            problems.append(f"{where}: {name} 형태의 값")
    found = [(m.group(1), m.group(3)) for m in QUOTED.finditer(text)]
    plain = PLAIN.match(text)
    if plain:
        found.append((plain.group(1), plain.group(2)))
    for key, value in found:
        if SAFE_VALUE.search(value):
            continue
        if TEST_PATH.search(file) and value.lower().startswith(("test", "old", "new", "prev")):
            continue
        problems.append(f"{where}: '{key}'에 값이 직접 적혀 있습니다(길이 {len(value)})")

if problems:
    print("커밋을 멈췄습니다. 비밀값은 저장소에 두지 말고 환경 변수로 받아 주세요.", file=sys.stderr)
    for p in problems:
        print(f"  - {p}", file=sys.stderr)
    print("비밀값이 아니라면 해당 줄 끝에 'secret-check: allow' 주석을 붙여 주세요.", file=sys.stderr)
    sys.exit(1)
