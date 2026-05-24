$base = "http://localhost:8080/api/v1/auth"
$headers = @{
    "X-UD-Client-Code" = "ud-admin-web"
    "Content-Type"     = "application/json"
}

Write-Host "=== login-config ==="
$config = Invoke-RestMethod -Uri "$base/login-config" -Method Get
$config | ConvertTo-Json -Compress
if (-not $config.captchaEnabled) { Write-Host "captcha disabled" }

Write-Host "=== captcha ==="
$challenge = Invoke-RestMethod -Uri "$base/captcha/challenge" -Method Post -Headers $headers
$verifyBody = @{
    challengeId = $challenge.challengeId
    track       = @(
        @{ x = 0; t = 0 },
        @{ x = 320; t = 900 }
    )
} | ConvertTo-Json -Depth 5
$token = Invoke-RestMethod -Uri "$base/captcha/verify" -Method Post -Headers $headers -Body $verifyBody
Write-Host "captchaToken=$($token.captchaToken)"

Write-Host "=== login ==="
$loginBody = @{
    username      = "admin"
    password      = "admin123"
    portal_type   = "staff"
    captcha_token = $token.captchaToken
} | ConvertTo-Json
try {
    $login = Invoke-RestMethod -Uri "$base/login" -Method Post -Headers $headers -Body $loginBody
    Write-Host "login OK accessToken length=$($login.accessToken.Length)"
    $login | ConvertTo-Json -Compress
    exit 0
}
catch {
    Write-Host "login FAILED: $($_.Exception.Message)"
    if ($_.ErrorDetails.Message) { Write-Host $_.ErrorDetails.Message }
    exit 1
}
