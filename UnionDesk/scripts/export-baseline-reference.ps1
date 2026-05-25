# 联调库参考快照（US-S0-07）-> docs/architecture/reference-schema/
# 密码：UNIONDESK_DB_PASSWORD 或 application.yml（勿提交仓库）

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

$date = Get-Date -Format "yyyyMMdd"
$outFile = "../docs/architecture/reference-schema/uniondesk_baseline_$date.sql"
New-Item -ItemType Directory -Force -Path (Split-Path $outFile -Parent) | Out-Null

Write-Host "导出参考快照 (JDBC)..."
.\mvnw.cmd -q dependency:build-classpath "-Dmdep.outputFile=target/cp.txt" "-DincludeScope=runtime"
$cp = Get-Content "target/cp.txt" -Raw
javac -encoding UTF-8 -cp $cp -d "target/backup-classes" "scripts/GenerateBaselineReference.java"
java -cp "target/backup-classes;$cp" GenerateBaselineReference $outFile
Write-Host "完成: $((Resolve-Path $outFile).Path)"
