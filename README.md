# WhiteCall (White List Call Blocker)

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20Material%203-blue.svg)](https://developer.android.com/jetpack/compose)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20Offline%20(No%20Internet%20Permission)-success.svg)](#privacy--security)
[![GitHub release](https://img.shields.io/github/v/release/EvgeniyKrasnyanskiy/WhiteCall?label=Latest%20Release)](https://github.com/EvgeniyKrasnyanskiy/WhiteCall/releases)

[English](#english) | [Русский](#russian)

---

<a name="english"></a>
## English

**WhiteCall** is a reliable, privacy-first Android call blocking application built with Kotlin and Jetpack Compose. It operates on a **strict whitelist principle**: all unwanted incoming calls from numbers not on your whitelist or address book are silently rejected at the system telecom level via `CallScreeningService` before the screen wakes up or the ringtone sounds.

### Key Features

1. **System-Level Silent Blocking (`CallScreeningService`)**:
   - Rejects unallowed incoming calls instantly before ringing or screen wakeup.
   - Filters anonymous, private, and hidden caller IDs.
   - Optional toggle to automatically allow all contacts from your device address book.
   - Zero false alarms, zero battery drain, and complete silence.

2. **100% Offline & Privacy-First**:
   - **No Internet permission** (`android.permission.INTERNET` is not requested).
   - Your contacts, whitelist entries, and call logs never leave your device.
   - No tracking, analytics, or third-party SDKs.

3. **Modern 3-Tab Interface (Jetpack Compose & Material 3)**:
   - **White List**: Organize allowed numbers into customizable folders/groups, search instantly, add from phone contacts or enter manually with full normalization and validation.
   - **Blocked Calls Log**: Grouped by date (*Today*, *Yesterday*, older dates) and by caller number with repeated call counters (e.g. `(3)`), expandable timestamp details, and one-tap whitelist addition.
   - **Settings & Help**: System screening role manager, protection toggle, weekly/hourly schedule planner, language switcher (System / English / Русский), theme switcher (Dark / Light / System), JSON backup import/export, and OEM setup guide (MIUI/HyperOS, Samsung OneUI, EMUI/HarmonyOS).

4. **Interactive Home Screen Widgets**:
   - Adaptive sizes (Compact 2x1, Expanded 3x1..5x1, and Tall 2x2..5x2).
   - Shows active protection status (*Active* / *Scheduled* / *Inactive*).
   - Shows blocked call counter (today, week, month) and incoming/last blocked call alerts.
   - Quick one-tap protection toggle button directly on the home screen.
   - Tapping the widget body opens the Blocked Calls Log directly.

5. **Backup & Restore**:
   - Export your whitelist folders and entries to a clean JSON file.
   - Easily restore or migrate your whitelist to a new device.

### Tech Stack & Architecture
- **Language**: Kotlin 1.9+
- **Architecture**: Clean Architecture + MVVM
- **UI Framework**: Jetpack Compose with Material Design 3
- **Local Database**: Room Database (SQLite)
- **Settings Storage**: Jetpack DataStore / SharedPreferences
- **Call Screening**: Android Telecom `CallScreeningService`
- **Widgets**: Android AppWidgetProvider with RemoteViews (support for Android 12+ SizeF templates)

### Installation & Download
Download the latest pre-built APK from the [GitHub Releases](https://github.com/EvgeniyKrasnyanskiy/WhiteCall/releases) page.

---

<a name="russian"></a>
## Русский

**WhiteCall** — надежное и ориентированное на приватность Android-приложение на Kotlin для блокировки входящих вызовов по принципу **«Белого списка»**. Нежелательные звонки сбрасываются бесшумно на системном уровне до пробуждения экрана и звонка с помощью `CallScreeningService`.

### Основные возможности

1. **Блокировка на уровне системы (`CallScreeningService`)**:
   - Мгновенный сброс звонков от абонентов, не входящих в белый список.
   - Фильтрация скрытых и анонимных номеров.
   - Опция автоматического пропуска всех номеров из телефонной книги.
   - Телефон не загорается, не вибрирует и не звенит при нежелательном звонке.

2. **100% Офлайн и полная конфиденциальность**:
   - **Не требует доступа в интернет** (разрешение `android.permission.INTERNET` отсутствует).
   - Телефонная книга, белый список и журнал звонков никогда не покидают ваше устройство.
   - Никаких трекеров, аналитики или сторонних рекламных сервисов.

3. **Современный 3-вкладочный интерфейс (Jetpack Compose & Material 3)**:
   - **Белый список**: группировка номеров по папкам (категориям), быстрый поиск, импорт из контактов устройства или ручной ввод с валидацией.
   - **Журнал блокировок**: группировка по датам (*Сегодня*, *Вчера*, архивные даты) и объединение повторных звонков с одного номера со счетчиком вызовов, раскрытием времени каждого звонка и добавлением в белый список в один клик.
   - **Настройки и справка**: управление системной ролью фильтрации, рубильник защиты, расписание по часам и дням недели, выбор языка (Системный / English / Русский), выбор темы (Темная / Светлая / Системная), резервное копирование в JSON и рекомендации по настройке OEM-прошивок (Xiaomi MIUI/HyperOS, Samsung, Huawei).

4. **Интерактивные виджеты для рабочего стола**:
   - Адаптивные размеры (Компактный 2x1, Расширенный 3x1..5x1 и Высокий 2x2..5x2).
   - Отображение текущего статуса (*Активно* / *По расписанию* / *Отключено*).
   - Счетчики заблокированных звонков (за сегодня, неделю, месяц) и информация о последнем заблокированном вызове.
   - Кнопка быстрого включения/выключения защиты прямо с виджета.
   - При тапе по виджету сразу открывается экран журнала звонков.

5. **Резервное копирование**:
   - Экспорт папок и номеров белого списка в структурированный JSON-файл.
   - Быстрое восстановление или перенос списка на новый смартфон.

### Архитектура и технологии
- **Kotlin 1.9+**, **Jetpack Compose + Material 3**
- **Clean Architecture + MVVM**
- **Room Database** (SQLite)
- **SharedPreferences / DataStore**
- **Android Telecom `CallScreeningService`**
- **AppWidgetProvider**

### Загрузка и установка
Скачать готовый APK последней версии можно на странице [GitHub Releases](https://github.com/EvgeniyKrasnyanskiy/WhiteCall/releases).
