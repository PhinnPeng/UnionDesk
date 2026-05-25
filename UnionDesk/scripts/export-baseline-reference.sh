#!/usr/bin/env bash
# 联调库参考快照（US-S0-07）-> docs/architecture/reference-schema/
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

DATE="$(date +%Y%m%d)"
OUT="../docs/architecture/reference-schema/uniondesk_baseline_${DATE}.sql"
mkdir -p "$(dirname "$OUT")"

echo "导出参考快照 (JDBC)..."
./mvnw -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt -DincludeScope=runtime
CP="$(cat target/cp.txt)"
mkdir -p target/backup-classes
javac -encoding UTF-8 -cp "$CP" -d target/backup-classes scripts/GenerateBaselineReference.java
java -cp "target/backup-classes:$CP" GenerateBaselineReference "$OUT"
echo "完成: $(cd "$(dirname "$OUT")" && pwd)/$(basename "$OUT")"
