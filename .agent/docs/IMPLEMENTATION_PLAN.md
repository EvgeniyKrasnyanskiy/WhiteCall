# План устранения замечаний аудита и подготовки релиза

## Цель
Устранить обнаруженные в ходе аудита баги утечки памяти и неоптимальных запросов, проверить сборку и тесты, собрать релизный APK без изменения версии (1.0.0), выполнить коммит и пуш в GitHub.

---

## 1. Устранение критической утечки CoroutineScope в службе фильтрации
- **Файл**: `app/src/main/java/com/whitecall/app/service/WhiteCallScreeningService.kt`
- **Проблема**: `serviceScope` (`SupervisorJob() + Dispatchers.IO`) создается в сервисе и никогда не отменяется при уничтожении сервиса (`onDestroy`). При перезапуске службы системой корутины и контекст могут утекать.
- **Решение**: Переопределить `onDestroy()`, вызвать `super.onDestroy()` и `serviceScope.cancel()`.

---

## 2. Устранение двойного обращения к ContactsContract
- **Файл**: `app/src/main/java/com/whitecall/app/domain/usecase/ShouldBlockCallUseCase.kt`
- **Проблема**: При включенном `allowAllContacts` метод `ContactHelper.getContactNameByNumber` вызывался сначала на строке 55, а если контакт не найден — повторно вызывался на строке 66 для записи в журнал.
- **Решение**: Вызывать `ContactHelper.getContactNameByNumber` один раз, кэшировать имя в локальную переменную и повторно использовать его при возврате `CallFilterResult`.

---

## 3. Устранение O(N) загрузки всей базы номеров в память при блокировке вызова
- **Файл**: `app/src/main/java/com/whitecall/app/data/repository/WhiteListRepository.kt`
- **Проблема**: В `isNumberInWhiteList` в качестве запасного варианта выполнялся `whiteListDao.getAllNumbers()`, загружавший в память и десериализовавший все записи белого списка на каждый заблокированный звонок.
- **Решение**: Исключить вызов `getAllNumbers()`. Метод `whiteListDao.findMatchingNumber(sigDigits)` уже выполняет поиск по SQL LIKE по последним значащим цифрам. При необходимости дополнить SQL-запрос в `WhiteListDao` проверкой исходного поля `phone_number`.

---

## 4. Оптимизация жизненного цикла CoroutineScope в мониторе звонков
- **Файл**: `app/src/main/java/com/whitecall/app/service/CallStateMonitor.kt`
- **Проблема**: На каждый заблокированный звонок создавался новый экземпляр `CoroutineScope(Dispatchers.IO).launch`.
- **Решение**: Использовать единый постоянный `monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)` внутри синглтона `CallStateMonitor`.

---

## 5. Проверка, релизная сборка, коммит и пуш
1. Запуск unit-тестов: `.\gradlew.bat test`.
2. Сборка релизного APK: `.\gradlew.bat assembleRelease` (версия остается 1.0.0, файл `WhiteCall-v1.0.0.apk`).
3. Проверка `git diff --stat`.
4. Создание коммита по Conventional Commits (`fix: resolve service scope leak, duplicate contact queries, and whitelist scan`).
5. Пуш изменений в GitHub: `git push origin main`.
