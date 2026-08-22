# Key Launcher

An Android home-screen widget that puts a compact QWERTY keyboard on your
launcher. Tap a letter to see every installed app whose name starts with it,
then tap an app to launch it. No app drawer, no swiping — just type the
first letter of what you want.

## What it does

- **Widget**: a black QWERTY keyboard bar (`KeyLauncherWidget`) sits on the
  home screen. Tapping a letter key opens a transparent overlay activity
  anchored to the widget.
- **Overlay** (`AppListActivity`): shows a scrollable list of installed apps
  starting with the tapped letter, above a copy of the same keyboard so you
  can keep typing/switching letters without closing the overlay. Tapping the
  transparent backdrop, or an app in the list, dismisses it.
- **Refresh key** (↺): force-rebuilds the installed-app list on demand (e.g.
  after installing/uninstalling apps).

## Architecture

| File | Role |
|---|---|
| `KeyLauncherWidget.kt` | `AppWidgetProvider`. Builds the `RemoteViews` for the home-screen widget, wires each letter key to a `PendingIntent` that launches `AppListActivity` with the tapped letter and the widget's current height, and wires the refresh key to a self-broadcast (`ACTION_REFRESH_CACHE`) that forces `AppCache` to rebuild. |
| `AppListActivity.kt` | The overlay activity. Shows the cached app list filtered by the current letter in a `RecyclerView`, and re-filters in place as the user taps other letters on the embedded keyboard (`widget_layout.xml` reused via `<include>`). Sizes the keyboard to match the real widget's height so the overlay lines up visually. |
| `AppCache.kt` | Singleton in-memory + on-disk (`SharedPreferences`) cache of the installed, launchable app list (name + package), sorted by name. `warm()` refreshes in the background if stale (10 min TTL); `getOrLoad()` blocks and returns memory → disk → live `PackageManager` query, in that order of speed; `forceRefresh()` is used by the refresh key. All rebuilds run on a single background executor thread. |
| `AppAdapter.kt` | `RecyclerView.Adapter` binding `AppInfo` rows to `item_app.xml`, launching the tapped app's launch intent. |
| `AppInfo.kt` | Plain `data class(name, packageName)`. |
| `res/layout/widget_layout.xml` | The keyboard UI, shared verbatim between the home-screen widget and the overlay activity. |
| `res/layout/activity_app_list.xml` | Overlay layout: transparent backdrop + bottom-anchored results panel + embedded keyboard. |
| `res/xml/widget_info.xml` | `AppWidgetProviderInfo` — resizable, `home_screen` category, no periodic auto-update (`updatePeriodMillis="0"`; the widget doesn't need it since it has no live content). |

**Data flow**: widget key tap → `PendingIntent` starts `AppListActivity` with
the letter and widget height → activity reads `AppCache` (instant if warm,
otherwise disk, otherwise a live `PackageManager` scan on a background
thread) → filters by starts-with → renders in the `RecyclerView`. Tapping an
app resolves its launch `Intent` via `PackageManager` and finishes the
overlay.

## Build & run

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

or in one step:

```bash
./gradlew installDebug
```

Then long-press the home screen, add the "Key Launcher" widget, and resize
as desired.

- **Package / applicationId**: `com.keylauncher`
- **minSdk**: 26, **targetSdk / compileSdk**: 34
- Kotlin, no Compose — plain Views + `RemoteViews` (required for widgets).

## Notable conventions / gotchas

- `QUERY_ALL_PACKAGES` is required in the manifest so `AppCache` can see all
  launchable apps, not just ones with a declared intent-query match.
- The overlay activity and widget **share the exact same
  `widget_layout.xml`** via `<include>` — any UI change to the keyboard
  (key size, colors, refresh button) should be made once in that file and
  will apply to both.
- The overlay's keyboard height is passed explicitly from the widget
  (`OPTION_APPWIDGET_MAX_HEIGHT`) via `EXTRA_WIDGET_HEIGHT_DP`, so the
  overlay's keyboard lines up with the real widget on the home screen behind
  it. If you resize the widget, this is what keeps the illusion of "the
  overlay grew out of the widget" intact.
- `AppCache` is a process-wide singleton, not tied to any one component —
  both the widget's `onEnabled`/`onUpdate` and the activity's `onCreate`
  call `warm()`/`getOrLoad()`, so whichever runs first pays the cost of the
  `PackageManager` scan and everything else gets it for free (subject to the
  10-minute TTL).
- The disk cache format is a deliberately simple `name\tpackageName` per
  line in `SharedPreferences` — not JSON — to avoid pulling in a
  serialization dependency for two fields.
- `updatePeriodMillis="0"` in `widget_info.xml` is intentional: the widget's
  content (keyboard) never changes, so there's nothing to auto-update.
