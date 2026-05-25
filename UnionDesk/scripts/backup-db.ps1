# 联调库全库备份（US-S0-07）
# 密码：环境变量 UNIONDESK_DB_PASSWORD，或从 application.yml 读取（勿写入仓库）

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not $env:UNIONDESK_DB_PASSWORD) {
    $appYml = Join-Path $root "src/main/resources/application.yml"
    if (Test-Path $appYml) {
        $match = Select-String -Path $appYml -Pattern '^\s*password:\s*(\S+)\s*$' | Select-Object -First 1
        if ($match) {
            $env:UNIONDESK_DB_PASSWORD = $match.Matches[0].Groups[1].Value
        }
    }
}
if (-not $env:UNIONDESK_DB_PASSWORD) {
    Write-Error "请设置环境变量 UNIONDESK_DB_PASSWORD"
}

New-Item -ItemType Directory -Force -Path "backups" | Out-Null
$ts = Get-Date -Format "yyyyMMdd_HHmmss"
$outFile = "backups/uniondesk_$ts.sql"

$mysqldump = Get-Command mysqldump -ErrorAction SilentlyContinue
if ($mysqldump) {
    & $mysqldump.Source -h 127.0.0.1 -P 30306 -u uniondesk_app `
        -p$env:UNIONDESK_DB_PASSWORD `
        --single-transaction --routines --triggers `
        uniondesk | Set-Content -Encoding utf8 $outFile
    Write-Host "备份完成 (mysqldump): $((Resolve-Path $outFile).Path)"
    exit 0
}

Write-Host "未找到 mysqldump，使用 JDBC 备份 (DbBackup.java)..."
.\mvnw.cmd -q dependency:build-classpath "-Dmdep.outputFile=target/cp.txt" "-DincludeScope=runtime"
$cp = Get-Content "target/cp.txt" -Raw
javac -encoding UTF-8 -cp $cp -d "target/backup-classes" "scripts/DbBackup.java"
java -cp "target/backup-classes;$cp" DbBackup $outFile
Write-Host "备份完成 (JDBC): $((Resolve-Path $outFile).Path)"
