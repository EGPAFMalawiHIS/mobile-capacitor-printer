// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "HtmlToPdfSaver",
    platforms: [.iOS(.v13)],
    products: [
        .library(
            name: "HtmlToPdfSaver",
            targets: ["HtmlToPdfSaverPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", branch: "main")
    ],
    targets: [
        .target(
            name: "HtmlToPdfSaverPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/HtmlToPdfSaverPlugin"),
        .testTarget(
            name: "HtmlToPdfSaverPluginTests",
            dependencies: ["HtmlToPdfSaverPlugin"],
            path: "ios/Tests/HtmlToPdfSaverPluginTests")
    ]
)