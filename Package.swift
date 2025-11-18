// swift-tools-version:5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "CloudflareImagesKMP",
    platforms: [
        .iOS(.v14)
    ],
    products: [
        .library(
            name: "CloudflareImagesKMP",
            targets: ["cloudflareImagesKMP"]
        ),
    ],
    targets: [
        .binaryTarget(
            name: "cloudflareImagesKMP",
            url: "https://github.com/TimKregerNew/CloudflareImagesKMP/releases/download/1.0.0/cloudflareImagesKMP.xcframework.zip",
            checksum: "c8c63acb3084815b1beb717013b59ba05a75521f3df74f41139b589f2cfa6084"
        )
    ]
)

