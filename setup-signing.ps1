# setup-signing.ps1 - create a NEW private app-signing key and register it in android-app\local.properties.
# The key lives in the git-ignored "keys" folder. BACK IT UP (keys folder + the passwords below):
# if you lose it you can never ship an update that installs over the existing app.
$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot
$keysDir = Join-Path $PSScriptRoot 'keys'
$jks = Join-Path $keysDir 'autoclicker-release.jks'
$lp = Join-Path $PSScriptRoot 'android-app\local.properties'

if (Test-Path $jks) { Write-Host "A key already exists: $jks"; Write-Host "Nothing changed. (Delete it yourself only if you really want a different key.)"; exit 0 }

# find keytool
$keytool = $null
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\keytool.exe'))) { $keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe' }
if (-not $keytool) { $c = Get-Command keytool -ErrorAction SilentlyContinue; if ($c) { $keytool = $c.Source } }
if (-not $keytool) {
    $roots = @("$env:ProgramFiles\Android\Android Studio", "$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Microsoft", "$env:ProgramFiles\Zulu", "$env:ProgramFiles\BellSoft", "$env:USERPROFILE\.jdks", "$env:LOCALAPPDATA\Programs", "${env:ProgramFiles(x86)}\Java")
    foreach ($r in $roots) {
        if (Test-Path $r) {
            $hit = Get-ChildItem -Path $r -Filter keytool.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
            if ($hit) { $keytool = $hit.FullName; break }
        }
    }
}
if (-not $keytool) { Write-Host "keytool not found. Install a JDK (or Android Studio) and try again."; Write-Host "(JAVA_HOME=$env:JAVA_HOME)"; exit 1 }
Write-Host "Using: $keytool"

New-Item -ItemType Directory -Path $keysDir -Force | Out-Null
function New-Pw {
    $bytes = New-Object byte[] 18
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    return [Convert]::ToBase64String($bytes).Replace('+','x').Replace('/','y').Replace('=','z')
}
$pw = New-Pw
& $keytool -genkeypair -keystore $jks -alias autoclicker -keyalg RSA -keysize 2048 -validity 10000 `
    -storepass $pw -keypass $pw -dname "CN=AutoClicker Pro, O=songkong72, C=KR"
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $jks)) { Write-Host "keytool failed."; exit 1 }

if (-not (Test-Path $lp)) { New-Item -ItemType File -Path $lp -Force | Out-Null }
$lines = @(Get-Content -Path $lp -Encoding utf8 | Where-Object { $_ -notmatch '^\s*signing\.' })
$jksFwd = $jks.Replace('\','/')
$lines += "signing.store.file=$jksFwd"
$lines += "signing.store.password=$pw"
$lines += "signing.key.alias=autoclicker"
$lines += "signing.key.password=$pw"
[System.IO.File]::WriteAllLines($lp, [string[]]$lines, (New-Object System.Text.UTF8Encoding($false))) # no BOM: a BOM breaks sdk.dir

Write-Host ""
Write-Host "New key created: $jks"
Write-Host "Password (store and key): $pw"
Write-Host "Registered in $lp"
Write-Host ""
Write-Host "BACK UP NOW: copy the 'keys' folder and this password to a safe place (NOT GitHub)."
Write-Host "Next: build-apk.bat. The phone must UNINSTALL the old app once before installing the new APK."
