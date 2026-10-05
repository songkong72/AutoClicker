# build-apk.ps1 [branch] - build a debug APK of origin/<branch> (default: main) in a SEPARATE folder (_build).
# Your current working folder, branch and uncommitted changes are NOT touched.
param([string]$Branch = 'main')
$ErrorActionPreference = 'Continue'
Set-Location -Path $PSScriptRoot
$log = Join-Path $PSScriptRoot 'build-log.txt'
"=== build started $(Get-Date -Format s) ===" | Set-Content -Path $log -Encoding utf8

function Log($text) { $text | Add-Content -Path $log -Encoding utf8; Write-Host $text }

function Step($label, [scriptblock]$sb) {
    Log ""
    Log ">>> $label"
    $out = & $sb 2>&1 | ForEach-Object { "$_" }
    $code = $LASTEXITCODE
    foreach ($line in $out) { Log $line }
    Log "(exit code: $code)"
    return $code
}

$wt = Join-Path $PSScriptRoot '_build'

# keep build output and private keys out of "git status" of the main folder (never commit them by accident)
$exclude = Join-Path $PSScriptRoot '.git\info\exclude'
if (Test-Path $exclude) {
    foreach ($pat in @('_build/', 'keys/', '*.jks', 'setup-signing-log.txt')) {
        if (-not (Select-String -Path $exclude -SimpleMatch -Pattern $pat -Quiet)) { Add-Content -Path $exclude -Value "`n$pat" }
    }
}

if ((Step "git fetch" { git fetch origin }) -ne 0) { Log "RESULT: FAIL (git fetch)"; exit 1 }
Step "git worktree prune" { git worktree prune } | Out-Null

if (-not (Test-Path (Join-Path $wt '.git'))) {
    if ((Step "create build folder" { git worktree add -q --detach _build origin/$Branch }) -ne 0) { Log "RESULT: FAIL (worktree add)"; exit 1 }
} else {
    if ((Step "update build folder" { git -C _build checkout -q --force --detach origin/$Branch }) -ne 0) { Log "RESULT: FAIL (checkout)"; exit 1 }
}
Log "branch: origin/$Branch"
Step "commit being built" { git -C _build log -1 --format="%h  %cd" --date=format:%Y-%m-%d_%H:%M } | Out-Null

# local.properties (SDK path) is not in git: copy it from the main folder if it exists
$lp = Join-Path $PSScriptRoot 'android-app\local.properties'
if (Test-Path $lp) { Copy-Item -Path $lp -Destination (Join-Path $wt 'android-app\local.properties') -Force; Log "copied local.properties" }

Set-Location -Path (Join-Path $wt 'android-app')
$code = Step "gradlew assembleDebug" { .\gradlew.bat assembleDebug --stacktrace }

$apk = Join-Path $wt 'android-app\app\build\outputs\apk\debug\app-debug.apk'
if ($code -eq 0 -and (Test-Path $apk)) {
    $dest = Join-Path $PSScriptRoot 'AutoClicker-debug.apk'
    Copy-Item -Path $apk -Destination $dest -Force
    Log ""
    Log "APK copied to: $dest"
    # also copy it to the shared Google Drive folder (skipped if the folder is not there on this PC)
    $share = 'G:\내 드라이브\공유'
    if (Test-Path -LiteralPath $share) {
        try {
            Copy-Item -LiteralPath $apk -Destination (Join-Path $share 'AutoClicker-debug.apk') -Force -ErrorAction Stop
            Log "APK also copied to: $share"
        } catch { Log "WARN: could not copy to ${share}: $($_.Exception.Message)" }
    } else { Log "NOTE: shared folder not found, skipped: $share" }
    Log "RESULT: SUCCESS"
} else {
    Log ""
    Log "RESULT: FAIL (see errors above)"
}
