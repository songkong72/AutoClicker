# setup-signing.ps1 - create a NEW private app-signing key and register it in android-app\local.properties.
# The key lives in the git-ignored "keys" folder. BACK IT UP (keys folder + the password shown at the end):
# if you lose it you can never ship an update that installs over the existing app.
# A diagnostic log (never containing the password) is written to setup-signing-log.txt.
$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot
$logFile = Join-Path $PSScriptRoot 'setup-signing-log.txt'
"=== setup-signing $(Get-Date -Format s) ===" | Set-Content -Path $logFile -Encoding utf8
function Log($t) { Write-Host $t; Add-Content -Path $logFile -Value $t -Encoding utf8 }

try {
    $keysDir = Join-Path $PSScriptRoot 'keys'
    $jks = Join-Path $keysDir 'autoclicker-release.jks'
    $lp = Join-Path $PSScriptRoot 'android-app\local.properties'
    Log "PSVersion: $($PSVersionTable.PSVersion)  JAVA_HOME: $env:JAVA_HOME"

    if (Test-Path $jks) { Log "A key already exists: $jks"; Log "Nothing changed."; return }

    # find keytool
    $keytool = $null
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\keytool.exe'))) { $keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe' }
    if (-not $keytool) { $c = Get-Command keytool -ErrorAction SilentlyContinue; if ($c) { $keytool = $c.Source } }
    if (-not $keytool) {
        $roots = @("$env:ProgramFiles\Android\Android Studio", "$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Microsoft", "$env:ProgramFiles\Zulu", "$env:ProgramFiles\BellSoft", "$env:USERPROFILE\.jdks", "$env:LOCALAPPDATA\Programs", "${env:ProgramFiles(x86)}\Java", "$env:USERPROFILE\.gradle\jdks", "$env:USERPROFILE\.antigravity", "$env:USERPROFILE\.vscode", "$env:USERPROFILE\.sts4", "$env:USERPROFILE\.p2", "$env:USERPROFILE\.eclipse")
        foreach ($r in $roots) {
            if (Test-Path $r) {
                $hit = Get-ChildItem -Path $r -Filter keytool.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
                if ($hit) { $keytool = $hit.FullName; break }
            }
        }
    }
    if (-not $keytool) { Log "RESULT: keytool NOT FOUND. Install a JDK (or Android Studio) and run again."; return }
    Log "Using keytool: $keytool"

    New-Item -ItemType Directory -Path $keysDir -Force | Out-Null
    $bytes = New-Object byte[] 18
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $pw = [Convert]::ToBase64String($bytes).Replace('+','x').Replace('/','y').Replace('=','z')

    $ErrorActionPreference = 'Continue'
    $out = & $keytool -genkeypair -keystore $jks -alias autoclicker -keyalg RSA -keysize 2048 -validity 10000 -storepass $pw -keypass $pw -dname "CN=AutoClicker Pro, O=songkong72, C=KR" 2>&1 | ForEach-Object { "$_" }
    $code = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    foreach ($l in $out) { Log "keytool: $l" }
    Log "keytool exit code: $code"
    if ($code -ne 0 -or -not (Test-Path $jks)) { Log "RESULT: keytool FAILED"; return }

    if (-not (Test-Path $lp)) { New-Item -ItemType File -Path $lp -Force | Out-Null }
    $lines = @(Get-Content -Path $lp -Encoding utf8 | Where-Object { $_ -notmatch '^\s*signing\.' })
    $lines += "signing.store.file=$($jks.Replace('\','/'))"
    $lines += "signing.store.password=$pw"
    $lines += "signing.key.alias=autoclicker"
    $lines += "signing.key.password=$pw"
    [System.IO.File]::WriteAllLines($lp, [string[]]$lines, (New-Object System.Text.UTF8Encoding($false))) # no BOM: a BOM breaks sdk.dir

    Log "RESULT: SUCCESS. New key created: $jks (registered in local.properties)"
    Write-Host ""
    Write-Host "Password (store and key): $pw"
    Write-Host "BACK UP NOW: copy the 'keys' folder and this password to a safe place (NOT GitHub)."
    Write-Host "Next: build-apk.bat. The phone must UNINSTALL the old app once before installing the new APK."
} catch {
    Log "RESULT: SCRIPT ERROR: $($_.Exception.Message)"
    Log "$($_.ScriptStackTrace)"
}
