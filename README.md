# dev.monorithm:ui

The Compose Multiplatform library of the Monorithm Experience Language. It will give the components of the language as composables for Android, iOS, and the desktop, with the tokens of the language.

At this time the library is a skeleton. It has the tokens of language 0.4.0 and no components. The 20 components of version 1 follow in the order of the list in `monorithm/design` (`contracts/v1-components.md`).

## Use the library

The library is not on Maven Central at this time. An app can use it through a Gradle composite build:

1. Clone the repository with the language source: `git clone --recurse-submodules https://github.com/monorithm/compose.git`.
2. In `settings.gradle.kts` of the app, include the build: `includeBuild("../compose")`.
3. In the source set `commonMain` of the app, add the dependency `implementation("dev.monorithm:ui:0.0.1")`. Gradle replaces it with the module `ui` of the included build.
4. Use a token:

```kotlin
import dev.monorithm.ui.tokens.MonoColors
import dev.monorithm.ui.tokens.MonoDensity
import dev.monorithm.ui.tokens.MonoDimensions
import dev.monorithm.ui.tokens.MonoTheme

val accent = MonoColors.roleAccent.of(MonoTheme.Dark)
val height = MonoDimensions.sizeControlMd.of(MonoDensity.Touch)
```

The axes of the language are `MonoTheme` (`Dark`, `Light`) and `MonoDensity` (`Pointer`, `Touch`). A token that changes with an axis is a `ByTheme` value or a `ByDensity` value. Its function `of` gives the value of one context.

## How the library gets the language

The language source is the git submodule `language/`. It is `github.com/monorithm/design` at the commit of one tag, for example `v0.4.0`. The emitter reads `language/ir/<version>/`. The version is the version in `language/package.json`.

| Step | Command | Result |
| --- | --- | --- |
| Get the language | `git submodule update --init` | Checks out the pinned commit of the language source |
| Change the version | `git -C language fetch --tags`, then `git -C language checkout v<version>`, then `git add language` | Pins the commit of that tag |
| Emit | `./gradlew emitTokens` | Writes `Tokens.kt` in `ui/src/commonMain/kotlin/dev/monorithm/ui/tokens/` from `ir/<version>/` |
| Drift check | `./gradlew checkLanguage` | Stops with an error if a file of the IR does not match its checksum in the manifest. Also stops if the generated file differs from the output of the emitter. The task `check` also runs it |

Run `emitTokens` in a build of its own: the compile tasks read the file that it writes.

Do not edit the generated file. Change the language source in `monorithm/design`, tag a version, then change the version here and emit again.

The emitter is a stub at this time. It writes the axes and the tokens of four types: the colors, the dimensions (in dp), the durations, and the numbers. The other types of the IR come with the components that use them: the cubic Bézier curves, the font weights, the shadows, and the transitions. The emitter and the check are in `build-logic/`, in the Gradle plugin `dev.monorithm.language`.

## The layout of the source

| Path | Contents |
| --- | --- |
| `ui/` | The library module |
| `ui/src/commonMain/kotlin/dev/monorithm/ui/tokens/` | The generated tokens. Do not edit them |
| `ui/src/jvmTest/` | The test of the tokens |
| `build-logic/` | The Gradle plugin of the language: the emitter and the check of the IR |
| `language/` | The submodule of the language source |
| `gradle/libs.versions.toml` | The versions of the plugins and the libraries |

## The targets

| Platform | Kotlin target |
| --- | --- |
| Android (minimum SDK 24, compile SDK 37) | `androidLibrary` |
| iOS | `iosArm64`, `iosSimulatorArm64` |
| The desktop | `jvm` |

The skeleton has no web target.

## The tests

`./gradlew jvmTest` runs the test of the tokens. The test reads `canonical.json` of the pinned IR and compares its values with the generated tokens.

On the JVM, a test name cannot have a slash. Thus each test class has the name of a component, and each test name starts with the clause of the contract. For example, the test of the clause `switch/keyboard` is in the class `SwitchTest`, and its name starts with `keyboard`. The test of the tokens is `ir` in the class `TokensTest`.

## The checks of CI

CI runs on macOS, because Xcode is necessary for the iOS targets. It does these steps on each pull request and on each push to `main`:

1. The validation of the Gradle wrapper.
2. The drift check: `./gradlew checkLanguage`.
3. The analysis: `./gradlew lint`. The Kotlin compiler also stops on each warning.
4. The tests: `./gradlew allTests`.
5. The build of the library for each target: `./gradlew assemble`.

## The tools

| Tool | Version |
| --- | --- |
| JDK | 17 or a newer version |
| Gradle | The wrapper gets Gradle 9.6.1 |
| Android SDK | Platform 37. Give its folder in `ANDROID_HOME` or in `local.properties` |
| Xcode | For the iOS targets only |

## The text

All text is in ASD-STE100 Simplified Technical English.

## License

Copyright 2026 Harnicode. The Apache License, Version 2.0 applies to this repository (`LICENSE`). Harnicode is the owner of the name Monorithm, the Monorithm icon, and the related branding (`NOTICE`).
