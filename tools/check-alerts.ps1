<#
.SYNOPSIS
  Checks on a real phone (USB debugging) that Shorashim's alerts are set up: chat message notifications and
  birthday / memorial reminders.

.DESCRIPTION
  Read-only: nothing is installed, uninstalled or changed on the phone, so the user's data and login stay intact.

  Before running:
    1. On the phone: Settings > About phone > tap "Build number" 7 times, then Settings > Developer options >
       enable "USB debugging".
    2. Connect the phone with a USB cable and accept the "Allow USB debugging?" prompt on the phone.
    3. Make sure Shorashim 1.5.2 or newer is installed, you are logged in, and you opened the app once.

  Run from the repository root:
    powershell -ExecutionPolicy Bypass -File tools\check-alerts.ps1
#>

$ErrorActionPreference = 'Stop'
$Package = 'com.example.remember_me'

function Find-Adb {
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $candidates = @(
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "$env:ANDROID_HOME\platform-tools\adb.exe",
        "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe"
    )
    foreach ($c in $candidates) { if ($c -and (Test-Path $c)) { return $c } }
    throw 'adb not found. Install Android Studio (SDK platform-tools) or add adb to PATH.'
}

function Write-Check($ok, $label, $detail) {
    if ($ok) { Write-Host "  [OK]   $label" -ForegroundColor Green }
    else { Write-Host "  [FAIL] $label" -ForegroundColor Red }
    if ($detail) { Write-Host "         $detail" -ForegroundColor DarkGray }
}

$adb = Find-Adb
$devices = (& $adb devices) | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' }
if (-not $devices) {
    Write-Host 'No phone found. Connect it with USB, enable USB debugging and accept the prompt on the phone.' -ForegroundColor Yellow
    & $adb devices
    exit 1
}
if (@($devices).Count -gt 1) {
    Write-Host 'More than one device is connected; disconnect the others (or set ANDROID_SERIAL).' -ForegroundColor Yellow
    exit 1
}

$model = (& $adb shell getprop ro.product.model).Trim()
$sdk = [int]((& $adb shell getprop ro.build.version.sdk).Trim())
Write-Host "`nPhone: $model (Android API $sdk)`n" -ForegroundColor Cyan

$pkgDump = (& $adb shell dumpsys package $Package) -join "`n"
if ($pkgDump -notmatch 'versionName=') {
    Write-Check $false 'Shorashim is installed' "Package $Package not found on this phone."
    exit 1
}
$version = [regex]::Match($pkgDump, 'versionName=(\S+)').Groups[1].Value
$parts = $version.Split('.') | ForEach-Object { [int]($_ -replace '\D', '') }
$isNewEnough = ($parts[0] -gt 1) -or ($parts[0] -eq 1 -and ($parts[1] -gt 5 -or ($parts[1] -eq 5 -and $parts[2] -ge 2)))

Write-Host 'App' -ForegroundColor Cyan
Write-Check $isNewEnough "Version $version" $(if (-not $isNewEnough) { 'Update to 1.5.2 or newer first (the app offers the update on launch).' })
$stopped = $pkgDump -match 'stopped=true'
Write-Check (-not $stopped) 'App is not in "force stopped" state' $(if ($stopped) { 'Open the app once; a force-stopped app gets no alarms or background work.' })

Write-Host "`nPermissions" -ForegroundColor Cyan
if ($sdk -ge 33) {
    $notif = $pkgDump -match 'android.permission.POST_NOTIFICATIONS: granted=true'
    Write-Check $notif 'Notifications allowed' $(if (-not $notif) { 'Allow notifications: Settings > Apps > Shorashim > Notifications (the app asks after login).' })
} else {
    Write-Check $true 'Notifications allowed (no runtime permission before Android 13)' $null
}
$exact = ($pkgDump -match 'USE_EXACT_ALARM: granted=true') -or ($pkgDump -match 'SCHEDULE_EXACT_ALARM: granted=true') -or ($sdk -lt 31)
Write-Check $exact 'Exact alarms allowed (reminders at exactly 09:00)' $null
$boot = $pkgDump -match 'RECEIVE_BOOT_COMPLETED: granted=true'
Write-Check $boot 'Reschedules reminders after a phone restart' $null

$battery = (& $adb shell dumpsys deviceidle whitelist) -join "`n"
$unrestricted = $battery -match $Package
$bgOp = ((& $adb shell cmd appops get $Package RUN_ANY_IN_BACKGROUND) -join ' ')
$bgBlocked = $bgOp -match 'ignore|deny'
Write-Check (-not $bgBlocked) 'Background activity allowed' $(if ($bgBlocked) { 'Settings > Apps > Shorashim > Battery > set to "Unrestricted" (otherwise chat checks while closed are blocked).' } elseif (-not $unrestricted) { 'Tip: setting Battery to "Unrestricted" makes chat notifications arrive faster when the app is closed (Samsung/Xiaomi are aggressive).' })

Write-Host "`nNotification channels" -ForegroundColor Cyan
$channels = (& $adb shell dumpsys notification) -join "`n"
foreach ($ch in @(
        @{ Id = 'shorashim_chat_messages'; Name = 'Chat messages' },
        @{ Id = 'shorashim_events_channel'; Name = 'Birthday / memorial reminders' })) {
    $m = [regex]::Match($channels, "(?s)$Package.{0,4000}?mId='$($ch.Id)'.{0,400}?mImportance=(\d+)")
    if (-not $m.Success) {
        Write-Check $false "$($ch.Name) channel exists" 'Not created yet - open the app while logged in (chat channel appears with the first message).'
    } else {
        $imp = [int]$m.Groups[1].Value
        Write-Check ($imp -ge 3) "$($ch.Name) channel enabled" $(if ($imp -lt 3) { "Importance $imp - the user turned this category off or to silent in the app's notification settings." })
    }
}

Write-Host "`nBackground check job (chat messages while app is closed)" -ForegroundColor Cyan
$jobs = (& $adb shell dumpsys jobscheduler) -join "`n"
$hasJob = $jobs -match "JOB #\S+: \S+ $([regex]::Escape($Package))/androidx.work"
Write-Check $hasJob 'Periodic background job registered' $(if (-not $hasJob) { 'Open the app once; the job is registered on app start.' })

Write-Host "`nScheduled birthday / memorial reminders" -ForegroundColor Cyan
$alarms = & $adb shell dumpsys alarm
$found = @()
for ($i = 0; $i -lt $alarms.Count - 1; $i++) {
    # Header: "RTC_WAKEUP #38: Alarm{8ae52b type 0 origWhen 1793084400000 ... com.example.remember_me}"
    # Next line: "tag=*walarm*:com.example.remember_me/com.shorashim.core.notifications.EventReminderReceiver"
    # (Tag test first: the header -match must run last so $Matches still holds its capture group.)
    if ($alarms[$i + 1] -match 'EventReminderReceiver' -and
        $alarms[$i] -match "Alarm\{\S+ type \d+ origWhen (\d{12,}) .*$([regex]::Escape($Package))\}") {
        $found += [DateTimeOffset]::FromUnixTimeMilliseconds([long]$Matches[1]).ToLocalTime().DateTime
    }
}
$found = $found | Sort-Object -Unique
if ($found.Count -gt 0) {
    Write-Check $true "$($found.Count) reminder alarm(s) scheduled" $null
    $found | Select-Object -First 15 | ForEach-Object { Write-Host ('         ' + $_.ToString('yyyy-MM-dd HH:mm')) }
} else {
    Write-Check $false 'Reminder alarms scheduled' 'None found. Normal if no one in your trees has a full birth date or a memorial in the coming year; otherwise open the app while logged in and run again.'
}

Write-Host "`nNotifications currently shown by the app" -ForegroundColor Cyan
$shown = [regex]::Matches($channels, "NotificationRecord\(\S+: pkg=$([regex]::Escape($Package))[^\n]*").Count
Write-Host "  $shown notification(s) in the shade right now."

Write-Host "`nLive test: ask your friend to send you a chat message now (keep the app closed)." -ForegroundColor Cyan
Write-Host 'Expected: a notification within ~20 s if the app was used recently, otherwise within ~15 min.'
Write-Host "Watching the app's log for 60 seconds (Ctrl+C to stop)..." -ForegroundColor DarkGray
& $adb logcat -c
$deadline = (Get-Date).AddSeconds(60)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 5
    $log = & $adb logcat -d | Where-Object { $_ -match 'ChatNotifier|EventScheduler|ChatNotificationWorker|SupabaseChatRepo' }
    $now = (& $adb shell dumpsys notification) -join "`n"
    $count = [regex]::Matches($now, "NotificationRecord\(\S+: pkg=$([regex]::Escape($Package))").Count
    if ($count -gt $shown) {
        Write-Host '  [OK]   A new Shorashim notification appeared on the phone.' -ForegroundColor Green
        break
    }
}
@($log) | Select-Object -Last 10 | ForEach-Object { Write-Host "  log: $_" -ForegroundColor DarkGray }
Write-Host "`nDone." -ForegroundColor Cyan
