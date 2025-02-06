# Cavity

<p>
  <img src="/assets/readme.png" alt="Cavity screenshots">
</p>

Cavity is a wine cellar manager for Android.
It follows Material Design 2 principles to have a clear and simple user interface.

Since Google is doing everything they can to prevent independent developers from maintaining their
apps there, the app will no longer receive updates on the Play Store and might disappear soon.
The app is now maintained on [F-Droid](https://f-droid.org/en/) and the APK is published in the
GitHub [release](https://github.com/ninjinskii/Cavity/releases) section.
After installing the F-Droid client, you can download the app here:
[<img src="https://f-droid.org/badge/get-it-on.png"
alt="Get it on F-Droid"
height="80">](https://f-droid.org/packages/com.louis.app.cavity)

IzzyOnDroid:
[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png"
alt="IzzyOnDroid"
height="80">](https://apt.izzysoft.de/packages/com.louis.app.cavity)

## Features

- Stock management
- Highlighting of peaking bottles
- Stock history
- Handle friends (Give a bottle to someone, share with a friend...)
- Buy & consumptions stats
- Search
- Tasting sessions: decanting notification, chill a bottle

## Design

Cavity uses gold as its primary color with no secondary color to emphasize a luxurious and classy wine
atmosphere.

Primary gold is used sparingly as it breaks the gold effect when using it in large surfaces.

The app is designed to be used in dark mode, but the light version is also supported for light mode
lovers.

## Run the project locally

Clone the project, and you're ready to build and run it via Android Studio.

## Architecture

Cavity uses Kotlin, Coroutines, Room, LiveData, ViewPager2 with a standard MVVM pattern.

## Release process

This project uses CI/CD.
Workflows are triggered when pushing a tag using this pattern : x.x.x, and create a GitHub release
with the APK file linked.
F-Droid store automatically picks up the APK file from there.

Things to do to prepare a release:

- Merge target code into `master` branch
- Update `/metadata/<languages>/changelogs.txt`
- Update `/app/build.gradle`: increment version code & update __version name__
- Update `/app/src/AndroidManifest.xml`: increment version code & update __version name__
- Check for database changes & migration
- Commit previous changes, using name: `[version name]`
- Create and push a tag named __version name__

## Run the monkey

Pin your app (Enable pinned app in android settings, then pin Cavity)

__WARNING: the monkey might play your music very loud at random moments when using it__

Run this command from your terminal:

```bash
./adb shell monkey -p com.louis.app.cavity -v 1000 --pct-nav 0
```

where `-v 1000` is the number of inputs sent to the phone.
