# Flyway history 与 db/migration/current 一致性检查（US-S0-07）
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
    Write-Error "Set UNIONDESK_DB_PASSWORD or configure application.yml"
}

.\mvnw.cmd -q dependency:build-classpath "-Dmdep.outputFile=target/cp.txt" "-DincludeScope=runtime"
$cp = Get-Content "target/cp.txt" -Raw
javac -encoding UTF-8 -cp $cp -d "target/backup-classes" "scripts/DbFlywayCheck.java"
java -cp "target/backup-classes;$cp" DbFlywayCheck
