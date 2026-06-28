// swift-tools-version:6.0
import PackageDescription

// __FRAMEWORK__ is the XCFramework's Swift module name. At rest it is "Src"
// (derived from the :src module); `mise run init` rewrites it to the display
// name. This local form points at the debug XCFramework Gradle builds; the first
// real release (mise run publish:maven, or the GitHub release workflow) flips it
// to a remote `.binaryTarget(url:checksum:)` against the GitHub Release asset.
//
//   mise run spm:dev      — rebuild the debug XCFramework + point this file at it
//   mise run spm:restore  — restore the committed (released) form
let packageName = "__FRAMEWORK__"

let package = Package(
    name: packageName,
    platforms: [
        .iOS(.v18),
        .macOS(.v15),
    ],
    products: [
        .library(
            name: packageName,
            targets: [packageName]
        ),
    ],
    targets: [
        .binaryTarget(
            name: packageName,
            path: "./src/build/XCFrameworks/debug/__FRAMEWORK__.xcframework"
        ),
    ]
)
