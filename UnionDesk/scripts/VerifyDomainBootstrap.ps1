$base = "http://localhost:8080/api/v1"
$headers = @{ "X-UD-Client-Code" = "ud-admin-web"; "Content-Type" = "application/json" }

$challenge = Invoke-RestMethod -Uri "$base/auth/captcha/challenge" -Method Post -Headers $headers
$verifyBody = @{ challengeId = $challenge.challengeId; track = @(@{ x = 0; t = 0 }, @{ x = 320; t = 900 }) } | ConvertTo-Json -Depth 5
$token = Invoke-RestMethod -Uri "$base/auth/captcha/verify" -Method Post -Headers $headers -Body $verifyBody

$login = Invoke-RestMethod -Uri "$base/auth/login" -Method Post -Headers $headers -Body (@{
    username = "admin"; password = "admin123"; portal_type = "staff"; captcha_token = $token.captchaToken
} | ConvertTo-Json)
$auth = @{ "X-UD-Client-Code" = "ud-admin-web"; "Authorization" = "Bearer $($login.accessToken)"; "Content-Type" = "application/json" }

$code = "verify-" + [guid]::NewGuid().ToString("N").Substring(0, 10)
$create = Invoke-RestMethod -Uri "$base/admin/domains" -Method Post -Headers $auth -Body (@{
    name = "Verify Bootstrap"; code = $code; logo = "/default-domain-logo.svg"
    visibility_policy_codes = @("public"); registration_policy = "open"
} | ConvertTo-Json)
Write-Host "created domain id=$($create.id) code=$($create.code)"

$members = Invoke-RestMethod -Uri "$base/admin/domains/$($create.id)/members?page=1&page_size=10" -Method Get -Headers $auth
Write-Host "members total=$($members.total)"
if ($members.list.Count -gt 0) {
    $members.list | ConvertTo-Json -Depth 5
}
