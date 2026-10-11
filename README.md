# Goals

An Android goal, habit, and task tracker. The root Gradle project builds the app in `app/`; its home screen organizes entries by date and item type.

## What it does

- Create goals with a title, category, priority, notes, start date, optional deadline, and optional target metric.
- Create habits with daily, interval-based, weekly/monthly quota, or selected-date schedules; add a metric and optionally link a habit to a goal.
- Add dated task entries, view goals/habits/tasks in separate lists, and navigate the home timeline by date.
- Open an item’s detail view, update progress where a target is configured, mark it done or undo completion, and review/search completed items.

## Technology

- Kotlin and Android view/XML layouts
- AndroidX, Material Components, and Room 2.6.1
- Gradle 9.2.1; application ID `com.example.goals`; minimum Android API 23

## Build

Open the repository root in Android Studio, or build a debug APK from the root directory with JDK 17 or newer and the Android SDK required by the project (compile SDK 36, minor API level 1):

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. On Windows, use `gradlew.bat assembleDebug`.

The root Gradle settings include the `:app` module. There is also a separate nested Android Studio project under `MyGoals/`, with its own wrapper and settings; the command above builds the root app.

## Data storage

The app stores goals, habits, and tasks in a local Room database named `goal_database`. The manifest contains no network permission or sign-in flow, so this codebase does not provide cloud sync or user accounts. The Room builder uses `fallbackToDestructiveMigration()`, which can erase the local database if a schema migration is not supplied.