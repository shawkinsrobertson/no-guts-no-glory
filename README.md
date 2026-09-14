# No Guts No Glory

No Guts No Glory is an Android app built for my partner to gauge her daily stress/load for gastritis.

It turns a short daily check-in (what happened today — poor sleep, stress, alcohol, a recovery day, etc.) into a transparent, deterministic "load" score, tracked as a 72-hour rolling average against a personal target. The whole point is to make logging almost effortless — the primary way to log a day is straight from an interactive notification, no need to open the app.

This is a personal tracking tool, not a medical device. It doesn't diagnose anything or claim to prevent flares — it just makes patterns visible.

## How it works

- **Daily check-in**: pick the things that applied today (a quick set of common factors, with more available), add a note if you want, save. Zero-effort path for "nothing notable happened today."
- **Load score**: each factor has a configurable weight; selecting it (optionally at an intensity) contributes points. Recovery factors offset load, capped at 30% of that day's gross load so a single good day can't erase a bad one.
- **72-hour rolling load**: the score shown day to day is a 3-day rolling mean, normalized against the maximum possible load from your currently active factors. A missing day is never treated as zero — only an explicitly logged day (including a deliberate "nothing notable" zero) counts.
- **Target**: a personal, self-chosen threshold — not a medical cutoff — shown alongside the absolute green/yellow/red gauge bands.
- **Notification logging**: the daily reminder shows your most personally relevant factors (ranked by how often/recently you've picked them), lets you toggle them right there, add a note via the keyboard, and save — all without opening the app.
- **Logbook & Stats**: a plain history of what was actually recorded each day, a 7-day trend graph, and a "what's showing up most" card — with guardrails against implying causation from a handful of data points.
- **Your data**: fully local, no account, no server. Export everything as CSV whenever you want it.

## Project structure

```
scoring/   Pure Kotlin module - the load-scoring math. No Android, no database, no UI.
app/       The Android app: Compose UI, Room persistence, DataStore preferences,
           AlarmManager-driven reminders, and the interactive notification.
```

`scoring` is kept dependency-free on purpose: it's the one part of the app where "is this calculation correct" needs to be answerable by reading a pure function and its unit tests, not by tracing through the UI or database.

## Building

Requires Android Studio (or the command line with an Android SDK installed) targeting compileSdk 34 / minSdk 26. From a checkout:

```
./gradlew :scoring:test        # fast, pure-JVM unit tests for the scoring engine
./gradlew testDebugUnitTest     # Robolectric-backed repository/notification tests
./gradlew assembleDebug         # the app itself
```

The `scoring` module has zero Android dependency, so its tests run anywhere Kotlin/JVM does. The `app` module needs a real Android SDK to compile.

## Status

V1 is feature-complete against the implementation plan: onboarding, daily logging (dashboard + notification), the scoring engine, the logbook, stats, and settings/export. See the PR history for how it was built, milestone by milestone.
