# Start Spring Boot with JRebel Agent (no IDEA)
# Requires JREBEL_HOME and config/jrebel.local.properties
param(
    [string]$JrebelHome = $env:JREBEL_HOME
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$PropsFile = Join-Path $ProjectRoot "config\jrebel.local.properties"

if (-not $JrebelHome -or -not (Test-Path $JrebelHome)) {
    throw "Set JREBEL_HOME to JRebel install dir (e.g. C:\jrebel). Download: https://www.jrebel.com/products/jrebel/download"
}

$AgentDll = Join-Path $JrebelHome "lib\jrebel64.dll"
if (-not (Test-Path $AgentDll)) {
    throw "JRebel Agent not found: $AgentDll"
}

if (-not (Test-Path $PropsFile)) {
    throw "Missing $PropsFile - copy config\jrebel.properties.example to jrebel.local.properties"
}

Push-Location $ProjectRoot
try {
    Write-Host "Generating rebel.xml ..."
    & .\mvnw.cmd -q process-resources

    $RebelXml = Join-Path $ProjectRoot "target\classes\rebel.xml"
    if (-not (Test-Path $RebelXml)) {
        throw "rebel.xml not generated; check jrebel-maven-plugin in pom.xml"
    }

    $JvmArgs = "-agentpath:$AgentDll -Drebel.properties=$PropsFile"
    Write-Host "Starting Spring Boot with JRebel ..."
    & .\mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=$JvmArgs"
}
finally {
    Pop-Location
}
