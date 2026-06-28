# __DISPLAY_NAME__

![iOS 18+](https://img.shields.io/badge/iOS-18%2B-blue.svg?style=for-the-badge&logo=apple)
![macOS 15+](https://img.shields.io/badge/macOS-15%2B-blue.svg?style=for-the-badge&logo=apple)
![Android 11+](https://img.shields.io/badge/Android-11%2B-3DDC84.svg?style=for-the-badge&logo=android&logoColor=white)
![JVM 21+](https://img.shields.io/badge/JVM-21%2B-orange.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Kotlin 2.3](https://img.shields.io/badge/Kotlin-2.3-7F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg?style=for-the-badge)

> **TODO:** one-paragraph description of what __DISPLAY_NAME__ does, behind one
> Kotlin Multiplatform API for iOS, macOS, Android, and the JVM.

UI is out of scope — __DISPLAY_NAME__ is the headless `:src` KMP module
(see [`CLAUDE.md`](CLAUDE.md) §1). Each platform app consumes it natively; see
[`apps/`](apps/) for samples on every platform.

## Modules

| Module | Coordinate | What it is |
|--------|-----------|-----------|
| `:src` | `com.happycodelucky.__PROJECT_NAME__:__PROJECT_NAME__` | The library. |
| `:src-testing` | `com.happycodelucky.__PROJECT_NAME__:__PROJECT_NAME__-testing` | Public test fakes + helpers for consumers. |

## Quick example

```kotlin
import com.happycodelucky.src.Greeter

println(Greeter().greet())   // "Hello from <platform>"
```

Replace the placeholder `Greeter` with your real API.

## Install

### Gradle (KMP / Android / JVM)

```kotlin
// gradle/libs.versions.toml
[libraries]
__PROJECT_NAME__ = { module = "com.happycodelucky.__PROJECT_NAME__:__PROJECT_NAME__", version = "<latest>" }

// build.gradle.kts (commonMain)
implementation(libs.__PROJECT_NAME__)
```

### Swift (SPM)

Add `https://github.com/happycodelucky/__PROJECT_NAME__.git` as a package
dependency, pinned to a release tag. The XCFramework ships as a GitHub Release
asset (see [`.github/PUBLISHING.md`](.github/PUBLISHING.md)).

## Development

Everything runs through [mise](https://mise.jdx.dev):

```bash
brew install mise
mise trust && mise install

mise run check         # ktlint + detekt + every test target — the done gate
mise run test:jvm      # fast inner loop
mise run build:src     # assemble the library
mise run open:macos    # build XCFramework + xcodegen + open Xcode (or open:ios / open:android)
mise tasks             # full task list
```

See [`CLAUDE.md`](CLAUDE.md) for conventions and [`CONTRIBUTING.md`](CONTRIBUTING.md)
to get started.

## License

[Apache 2.0](LICENSE)
