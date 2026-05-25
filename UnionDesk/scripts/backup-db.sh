#!/usr/bin/env bash
# 联调库全库备份（US-S0-07）-> UnionDesk/backups/
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ -z "${UNIONDESK_DB_PASSWORD:-}" ]] && [[ -f src/main/resources/application.yml ]]; then
  export UNIONDESK_DB_PASSWORD
  UNIONDESK_DB_PASSWORD="$(grep -E '^[[:space:]]*password:[[:space:]]*' src/main/resources/application.yml | head -1 | sed -E 's/^[[:space:]]*password:[[:space:]]*//')"
fi
if [[ -z "${UNIONDESK_DB_PASSWORD:-}" ]]; then
  echo "请设置环境变量 UNIONDESK_DB_PASSWORD" >&2
  exit 1
fi

mkdir -p backups
TS="$(date +%Y%m%d_%H%M%S)"
OUT="backups/uniondesk_${TS}.sql"

if command -v mysqldump >/dev/null 2>&1; then
  mysqldump -h 127.0.0.1 -P 30306 -u uniondesk_app -p"${UNIONDESK_DB_PASSWORD}" \
    --single-transaction --routines --triggers uniondesk > "$OUT"
  echo "备份完成 (mysqldump): $(pwd)/$OUT"
  exit 0
fi

echo "未找到 mysqldump，使用 JDBC 备份 (DbBackup.java)..."
./mvnw -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt -DincludeScope=runtime
CP="$(cat target/cp.txt)"
mkdir -p target/backup-classes
javac -encoding UTF-8 -cp "$CP" -d target/backup-classes scripts/DbBackup.java
java -cp "target/backup-classes:$CP" DbBackup "$OUT"
echo "备份完成 (JDBC): $(pwd)/$OUT"
