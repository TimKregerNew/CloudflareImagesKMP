plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    `maven-publish`
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "cloudflareImagesKMP"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            
            // Kotlinx
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.test)
        }
        
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.robolectric)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp) // Use OkHttp engine for PATCH support
        }
        
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.kmpnetworking"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

// Version for publishing
version = project.findProperty("library.version")?.toString() ?: "1.0.0"
group = "com.kmpnetworking"

// Apply publishing configuration
apply(from = "publish.gradle.kts")

// Task to create XCFramework for Swift Package Manager
tasks.register("createXCFramework") {
    group = "build"
    description = "Creates XCFramework for distribution via Swift Package Manager"
    
    dependsOn(
        "linkReleaseFrameworkIosArm64",
        "linkReleaseFrameworkIosX64",
        "linkReleaseFrameworkIosSimulatorArm64"
    )
    
    doLast {
        val buildDir = project.layout.buildDirectory.asFile.get()
        val xcframeworkPath = "$buildDir/XCFrameworks/release/cloudflareImagesKMP.xcframework"
        
        // Remove old XCFramework if exists
        delete(xcframeworkPath)
        
        // Build command with frameworks
        val frameworks = mutableListOf<String>()
        frameworks.add("-framework")
        frameworks.add("$buildDir/bin/iosArm64/releaseFramework/cloudflareImagesKMP.framework")
        
        // Add simulator frameworks - only include what's available
        val simulatorArm64 = file("$buildDir/bin/iosSimulatorArm64/releaseFramework/cloudflareImagesKMP.framework")
        val simulatorX64 = file("$buildDir/bin/iosX64/releaseFramework/cloudflareImagesKMP.framework")
        
        // Prefer arm64 simulator (Apple Silicon), but include x64 if arm64 doesn't exist
        if (simulatorArm64.exists()) {
            frameworks.add("-framework")
            frameworks.add(simulatorArm64.absolutePath)
        } else if (simulatorX64.exists()) {
            frameworks.add("-framework")
            frameworks.add(simulatorX64.absolutePath)
        }
        
        frameworks.add("-output")
        frameworks.add(xcframeworkPath)
        
        exec {
            commandLine("xcodebuild", "-create-xcframework", *frameworks.toTypedArray())
        }
        
        println("✅ XCFramework created at: $xcframeworkPath")
    }
}

