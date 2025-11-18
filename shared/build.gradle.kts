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
            baseName = "cloud"
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
            implementation(libs.ktor.client.android)
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
        val xcframeworkPath = "$buildDir/XCFrameworks/release/cloud.xcframework"
        
        // Remove old XCFramework if exists
        delete(xcframeworkPath)
        
        exec {
            commandLine(
                "xcodebuild",
                "-create-xcframework",
                "-framework", "$buildDir/bin/iosArm64/releaseFramework/cloud.framework",
                "-framework", "$buildDir/bin/iosSimulatorArm64/releaseFramework/cloud.framework",
                "-framework", "$buildDir/bin/iosX64/releaseFramework/cloud.framework",
                "-output", xcframeworkPath
            )
        }
        
        println("✅ XCFramework created at: $xcframeworkPath")
    }
}

