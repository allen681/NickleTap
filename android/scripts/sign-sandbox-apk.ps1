param(
    [string]$InputApk,
    [string]$OutputApk
)

$ErrorActionPreference = "Stop"
$androidDir = Split-Path -Parent $PSScriptRoot
$repoDir = Split-Path -Parent $androidDir

if (-not $InputApk) {
    $InputApk = Join-Path $androidDir "app\build\outputs\apk\sandbox\app-sandbox-unsigned.apk"
}
if (-not $OutputApk) {
    $OutputApk = Join-Path $androidDir "app\build\outputs\apk\sandbox\NickelTap-sandbox-release-v2.apk"
}

$signingDir = Join-Path $androidDir ".sandbox-signing"
$keystore = Join-Path $signingDir "nickeltap-sandbox.jks"
$passwordFile = Join-Path $signingDir "password.txt"
$apkSigner = Join-Path $repoDir ".android-sdk\build-tools\35.0.0\apksigner.bat"
$keytool = (Get-Command keytool.exe -ErrorAction Stop).Source

if (-not (Test-Path -LiteralPath $InputApk)) {
    throw "Unsigned sandbox APK not found. Run assembleSandbox first: $InputApk"
}
if (-not (Test-Path -LiteralPath $apkSigner)) {
    throw "Android apksigner not found: $apkSigner"
}

New-Item -ItemType Directory -Path $signingDir -Force | Out-Null

$keystoreExists = Test-Path -LiteralPath $keystore
$passwordExists = Test-Path -LiteralPath $passwordFile
if ($keystoreExists -ne $passwordExists) {
    throw "Sandbox signing files are incomplete. Restore both files from backup before signing."
}

if (-not $keystoreExists) {
    $password = [Guid]::NewGuid().ToString("N") + [Guid]::NewGuid().ToString("N")
    [System.IO.File]::WriteAllText(
        $passwordFile,
        $password,
        [System.Text.UTF8Encoding]::new($false)
    )
    & $keytool -genkeypair `
        -keystore $keystore `
        -storepass $password `
        -keypass $password `
        -alias nickeltap-sandbox `
        -keyalg RSA `
        -keysize 2048 `
        -sigalg SHA256withRSA `
        -validity 10000 `
        -dname "CN=NickelTap Sandbox, OU=Development, O=NickelTap, L=Sandbox, ST=Sandbox, C=US"
    if ($LASTEXITCODE -ne 0) {
        throw "Could not create the dedicated sandbox signing key."
    }
} else {
    $password = [System.IO.File]::ReadAllText($passwordFile).Trim()
}

if (Test-Path -LiteralPath $OutputApk) {
    Remove-Item -LiteralPath $OutputApk -Force
}

& $apkSigner sign `
    --ks $keystore `
    --ks-key-alias nickeltap-sandbox `
    --ks-pass "pass:$password" `
    --key-pass "pass:$password" `
    --v1-signing-enabled true `
    --v2-signing-enabled false `
    --v3-signing-enabled false `
    --v4-signing-enabled false `
    --v1-signer-name CERT `
    --out $OutputApk `
    $InputApk
if ($LASTEXITCODE -ne 0) {
    throw "Could not sign the Clover sandbox APK."
}

& $apkSigner verify --verbose --print-certs $OutputApk
if ($LASTEXITCODE -ne 0) {
    throw "The signed Clover sandbox APK did not verify."
}

Write-Output $OutputApk
