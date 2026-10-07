# [Выполнено] План: Отображение статуса контактов в журнале заблокированных звонков

## Причина проблемы
1. При перехвате звонка не из белого списка в базу сохраняется запись с причиной `reason = "NOT_IN_WHITELIST"`.
2. После добавления абонента в контакты устройства `BlockedLogViewModel` подтягивает имя через `ContactHelper.getContactNameByNumber()`, поэтому имя отображается.
3. Однако статус проверки звонка в `BlockedLogScreen` проверяет исключительно `item.isWhitelisted` (наличие номера в локальной таблице белого списка WhiteCall).
4. Проверка того, что номер теперь находится в контактах и включена настройка «Разрешить все контакты из книги» (`allowAllContacts`), отсутствовала. Из-за этого карточка всегда выводила красный статус `• Не в белом списке` и красную иконку сброса.

---

## 1. Изменения в модели данных и ViewModel (`BlockedLogViewModel.kt`)
- В `GroupedBlockedCall` добавить поля:
  - `isContact: Boolean` (номер найден в телефонной книге устройства);
  - `isAllowedByContacts: Boolean` (`isContact && allowAllContacts`);
  - `val isAllowed: Boolean get() = isWhitelisted || isAllowedByContacts`.
- В `BlockedLogViewModel`:
  - В `combine` объединить `blockingRepository.getAllBlockedCallsFlow()`, `whiteListRepository.getAllEntriesFlow()`, `preferences.allowAllContactsFlow` и `refreshTrigger`.
  - Добавить метод `refresh()`, чтобы экран мог актуализировать данные контактов при `Lifecycle.Event.ON_RESUME` (когда пользователь вернулся из системной книги контактов).

---

## 2. Отображение в UI (`BlockedLogScreen.kt`)
- **Статус вызова**:
  - Если `item.isWhitelisted` -> зеленый `• В белом списке` (`status_in_whitelist`, цвет `StatusActive`).
  - Иначе если `item.isAllowedByContacts` -> зеленый `• В контактах` (`status_in_contacts`, цвет `StatusActive`).
  - Иначе -> красный `• Не в белом списке` (`blocked_reason_not_in_whitelist`, цвет `error`).
- **Иконка-бейдж слева**:
  - Если `item.isWhitelisted` -> зеленая плашка с `ic_check`.
  - Если `item.isAllowedByContacts` -> зеленая плашка с `ic_contact`.
  - Иначе -> красная плашка с `ic_call_missed`.
- **Диалог действий с номером**:
  - Если номер уже в контактах (`target.isContact == true`), показывать пункт «Открыть в контактах» (`action_view_contacts`) вместо повторного «В контакты телефона».
- **Жизненный цикл**:
  - Добавить `DisposableEffect(lifecycleOwner)` с вызовом `viewModel.refresh()` на `ON_RESUME` для мгновенного обновления без перезапуска приложения.

---

## 3. Локализация (`strings.xml` / `values-ru/strings.xml`)
- Добавить строки:
  - `status_in_contacts`: RU `В контактах`, EN `In Contacts`
  - `action_view_contacts`: RU `Открыть в контактах`, EN `View in Contacts`

---

## 4. Верификация
- Проверка компиляции и сборки через `./gradlew test`.
- Проверка корректности отображения статусов.
