# build-apk.ps1 - build a debug APK of origin/feature/relative-rally in a SEPARATE folder (_build).
# Your current working folder, branch and uncommitted changes are NOT touched.
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

# keep _build out of "git status" of the main folder
$exclude = Join-Path $PSScriptRoot '.git\info\exclude'
if (Test-Path $exclude) {
    if (-not (Select-String -Path $exclude -Pattern '^_build/?$' -Quiet)) { Add-Content -Path $exclude -Value "`n_build/" }
}

if ((Step "git fetch" { git fetch origin }) -ne 0) { Log "RESULT: FAIL (git fetch)"; exit 1 }
Step "git worktree prune" { git worktree prune } | Out-Null

if (-not (Test-Path (Join-Path $wt '.git'))) {
    if ((Step "create build folder" { git worktree add --detach _build origin/feature/relative-rally }) -ne 0) { Log "RESULT: FAIL (worktree add)"; exit 1 }
} else {
    if ((Step "update build folder" { git -C _build checkout --force --detach origin/feature/relative-rally }) -ne 0) { Log "RESULT: FAIL (checkout)"; exit 1 }
}
Step "commit being built" { git -C _build log -1 --oneline } | Out-Null

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
    Log "RESULT: SUCCESS"
} else {
    Log ""
    Log "RESULT: FAIL (see errors above)"
}
