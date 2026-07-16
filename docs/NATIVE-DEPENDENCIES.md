# Native Dependencies

This document describes the native (iOS/Android) build requirements and peer
dependencies for `react-native-pos-thermal-printer`. It is generated as part
of the IP Ownership & Escrow audit (July 2026) to make the native build
surface explicit for anyone building or auditing this library.

## Peer dependencies (from `package.json`)

| Package        | Version range | Notes                                               |
| -------------- | ------------- | --------------------------------------------------- |
| `react`        | `*`           | Must match the consuming app's React version        |
| `react-native` | `*`           | Must match the consuming app's React Native version |

The library declares no runtime `dependencies` in `package.json` — only
`devDependencies` (used to build/test this package itself) and the two
`peerDependencies` above, which are supplied by the host application.

## iOS

- **Podspec**: `react-native-pos-thermal-printer.podspec` (name, version, and
  license are read from `package.json` at pod-install time).
- **Minimum iOS deployment target**: `13.4` (from
  `node_modules/react-native/scripts/cocoapods/helpers.rb` →
  `Helpers::Constants.min_ios_version_supported`, consumed via
  `min_ios_version_supported` in the podspec).
- **Minimum Xcode version**: `14.3` (same constants file).
- **CocoaPods dependencies** (old architecture): `React-Core`.
- **CocoaPods dependencies** (new architecture, `RCT_NEW_ARCH_ENABLED=1`):
  `React-Codegen`, `RCT-Folly`, `RCTRequired`, `RCTTypeSafety`,
  `ReactCommon/turbomodule/core`, plus `boost` header search path and Folly
  compiler flags (`-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1`).
  These are all React Native framework-internal pods resolved transitively
  from the host app's `node_modules/react-native`, not separately vendored
  by this library.
- **Example app's resolved React Native version**: `react-native-pos-thermal-printer (0.9.0-beta.16)` is
  the only project-owned pod in `example/ios/Podfile.lock`; all other pods
  (`React-*`, `RCT-Folly`, `Yoga`, `glog`, `fmt`, `DoubleConversion`,
  `hermes-engine`, etc.) come from the pinned `react-native@0.75.4` in
  `package.json`/`example` and are licensed under the standard React Native
  license set (MIT/BSD-3-Clause), unrelated to this library's own license.
- **Vendored native SDK source** — `ios/Tools/POSSDK.h`,
  `ios/Tools/POSBLEManager.{h,m}`, `ios/Tools/POSWIFIManager*.{h,m}`,
  `ios/Tools/TscCommand.{h,m}`, `ios/Tools/PosCommand.{h,m}`,
  `ios/Tools/ImageTranster.{h,m}`: this is a third-party thermal-printer
  vendor SDK (Chinese-language header comments, copyright "Admin", dated
  2016, no license file or attribution present in-tree) that has been
  copied directly into the repository rather than referenced as an
  external pod dependency. **No license or provenance information is
  present for this code** — flagged for legal review below.
- System frameworks required by the vendor SDK (per `ios/Tools/POSSDK.h`
  comments): `SystemConfiguration.framework`, `CFNetwork.framework`,
  `CoreBluetooth.framework`.

## Android

- **`compileSdkVersion` / `targetSdkVersion`**: `31` (from
  `android/gradle.properties` → `PosThermalPrinter_compileSdkVersion` /
  `PosThermalPrinter_targetSdkVersion`, consumed via `getExtOrIntegerDefault`
  in `android/build.gradle`).
- **`minSdkVersion`**: `21` (`PosThermalPrinter_minSdkVersion`).
- **Kotlin version**: `1.7.0` (`PosThermalPrinter_kotlinVersion`).
- **NDK version**: `21.4.7075529` (`PosThermalPrinter_ndkversion`).
- **Android Gradle Plugin**: `7.2.1` (`android/build.gradle` buildscript
  classpath).
- **Gradle dependencies** (`android/build.gradle`):
  - `com.facebook.react:react-native:+` — resolved to the host app's pinned
    React Native version via the React Native Gradle plugin (>=0.71).
  - `org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version` — Apache-2.0.
  - Test-only: `junit:junit:4.13.2` (EPL-2.0/CPL), `org.mockito:*:4.8.1`
    (MIT), `org.robolectric:robolectric:4.11.1` (MIT), `androidx.test:core:1.5.0`
    (Apache-2.0), `org.mockito.kotlin:mockito-kotlin:4.1.0` (MIT),
    `org.powermock:*:2.0.9` (Apache-2.0). Test-only dependencies are not
    shipped in the published package.
- **Bundled vendor SDK jars** (`android/libs/`, referenced via
  `implementation files(...)` in `android/build.gradle`):

  - `PosPrinterSDK.jar`
  - `iminPrinterSDK.jar`
  - `IminLibs1.0.15.jar` (iMin brand internal/USB POS printer SDK — see
    the "iMin D4" row in `README.md`'s supported-printers table).

  These are pre-built, closed-source third-party binaries checked directly
  into the repository (not resolved from Maven Central or any public
  registry). **No license file, NOTICE, or vendor attribution accompanies
  these `.jar` files in the repository** — flagged for legal review below.

## Flagged for legal review

The following native-layer items could not be resolved to a known, verifiable
open-source license and should be reviewed by legal/compliance before this
package's licensing posture is treated as fully clear:

1. **`android/libs/PosPrinterSDK.jar`, `android/libs/iminPrinterSDK.jar`,
   `android/libs/IminLibs1.0.15.jar`** — closed-source vendor SDK binaries
   (printer manufacturer SDKs) bundled directly in the repo with no
   accompanying license/EULA text. Redistribution terms from the SDK vendor
   (iMin / the POS printer manufacturer) are undeterminable from the
   repository alone.
2. **`ios/Tools/POSSDK.h` and the associated `POSBLEManager`, `POSWIFIManager`,
   `POSWIFIManagerAsync`, `TscCommand`, `PosCommand`, `ImageTranster` source
   files** — a third-party printer-vendor SDK whose source has been copied
   into `ios/Tools/` with a 2016 "Copyright © 2016 Admin" header and no
   license file. Original licensing terms and permission to redistribute
   are undeterminable from the repository alone.

No other non-permissive or undeterminable native licenses were found: all
React Native framework-internal CocoaPods and Gradle dependencies use
standard MIT/BSD-3-Clause/Apache-2.0 licensing consistent with the React
Native project itself.
