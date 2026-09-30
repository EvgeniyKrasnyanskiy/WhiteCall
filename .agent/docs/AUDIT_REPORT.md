# WhiteCall Audit Report

## BATTERY
- OK: CallScreeningService - event-driven, no background polling
- OK: Widget updatePeriodMillis=1800000 (30min) - standard
- WARN: CallStateMonitor uses CoroutineScope(Dispatchers.IO) - orphan scope, not tied to lifecycle. Minor leak risk on rapid blocked calls.
- WARN: WhiteCallWidgetProvider.updateAllWidgets also uses orphan CoroutineScope(Dispatchers.IO). Same issue.

## BUGS
- CRITICAL: WhiteCallScreeningService.serviceScope (SupervisorJob) is never cancelled in onDestroy. Coroutine leak when service is destroyed.
- WARN: ShouldBlockCallUseCase line 66 - double ContactHelper.getContactNameByNumber lookup when allowAllContacts=true and number not found. Redundant DB query.
- WARN: WhiteListRepository.isNumberInWhiteList loads ALL entries (whiteListDao.getAllNumbers()) as fallback - O(N) scan on every blocked call.
- WARN: build.gradle.kts has signing passwords in plaintext.
- OK: Deduplication guard in CallBlockingRepository (2.5s window) - good.
- OK: Database trimOldRecords(500) prevents infinite growth.
- OK: fallbackToDestructiveMigration - acceptable for v2.

## SECURITY
- WARN: Signing key password in build.gradle.kts (line 28-29) in plaintext. Move to local.properties or env vars.
- OK: No INTERNET permission - no data leaks.
- OK: CallScreeningService requires BIND_SCREENING_SERVICE permission.

## SUMMARY
Critical: 1 (service scope leak)
Warnings: 4 (orphan coroutines, redundant contact lookup, O(N) whitelist scan, plaintext passwords)
Overall: App is solid. Battery drain risk is minimal.
