# How the KMP Shared Framework is Linked to iOS App

This document explains how the Kotlin Multiplatform shared library is integrated into the iOS Xcode project.

## Overview

The iOS app uses a **build-time integration** approach where:
1. Xcode **automatically builds** the shared framework before compiling Swift code
2. The framework is **embedded** and **code-signed** automatically
3. Framework paths are configured via **build settings**

## Architecture

```
iOS Build Process:
┌─────────────────────────────────────────────────┐
│ 1. Xcode Build Starts                           │
│    (Cmd+R or ⌘R)                                │
└──────────────┬──────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────┐
│ 2. Build Script Runs (Shell Script Phase)      │
│    ./gradlew :shared:embedAndSignApple...       │
│    → Builds Kotlin/Native framework             │
│    → Creates shared.framework                   │
│    → Places in: shared/build/xcode-frameworks/ │
└──────────────┬──────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────┐
│ 3. Framework Location:                          │
│    shared/build/xcode-frameworks/               │
│      └── Debug/iphonesimulator18.5/             │
│          └── shared.framework                   │
└──────────────┬──────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────┐
│ 4. Xcode Compiles Swift                         │
│    - Finds framework via FRAMEWORK_SEARCH_PATHS │
│    - Links via OTHER_LDFLAGS                    │
│    - Embeds framework in app bundle             │
└──────────────┬──────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────┐
│ 5. iOS App Bundle Created                       │
│    iosApp.app/                                  │
│      └── Frameworks/                            │
│          └── shared.framework                   │
└─────────────────────────────────────────────────┘
```

## Step-by-Step Breakdown

### 1. Build Script Phase (Automatic)

**Location:** Xcode project → Build Phases → Shell Script

**Script:**
```bash
cd "$SRCROOT/.."
./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

**What it does:**
- Runs **before** Swift compilation
- Builds the Kotlin/Native framework for the current target architecture
- Detects Xcode's build environment variables:
  - `SDK_NAME` (e.g., `iphonesimulator`, `iphoneos`)
  - `CONFIGURATION` (e.g., `Debug`, `Release`)
  - `ARCHS` (e.g., `arm64`, `x86_64`)
- Creates `shared.framework` for the specific architecture
- Places it in: `shared/build/xcode-frameworks/{CONFIGURATION}/{SDK_NAME}/shared.framework`

**Key Point:** This happens **automatically** every time you build in Xcode, ensuring the framework is always up-to-date!

### 2. Framework Search Paths

**Location:** Xcode → Build Settings → Framework Search Paths

**Setting:**
```
FRAMEWORK_SEARCH_PATHS = $(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)
```

**What it does:**
- Tells Xcode **where to find** the `shared.framework`
- Uses **Xcode build variables**:
  - `$(SRCROOT)` = `iosApp/` directory
  - `$(CONFIGURATION)` = `Debug` or `Release`
  - `$(SDK_NAME)` = `iphonesimulator` or `iphoneos`
- Resolves to something like: `iosApp/../shared/build/xcode-frameworks/Debug/iphonesimulator`
- Xcode automatically finds `shared.framework` in that directory

### 3. Linking Framework

**Location:** Xcode → Build Settings → Other Linker Flags

**Setting:**
```
OTHER_LDFLAGS = (
    "$(inherited)",
    "-framework",
    shared,
)
```

**What it does:**
- **Links** the `shared.framework` into the app binary
- `-framework shared` tells the linker to link `shared.framework`
- The linker looks in `FRAMEWORK_SEARCH_PATHS` to find it

### 4. Framework Embedding (Automatic)

**What happens:**
- Xcode **automatically embeds** frameworks linked with `-framework`
- Framework is copied into app bundle at: `iosApp.app/Frameworks/shared.framework`
- Framework is **code-signed** automatically (same signature as app)

## How Swift Code Accesses Kotlin Code

### 1. Import Statement

In your Swift files:
```swift
import shared
```

This imports all public declarations from the Kotlin shared module.

### 2. Accessing Kotlin Classes

Kotlin classes become Swift classes with naming conventions:

```swift
// Kotlin
class CloudflareImageUploader { ... }

// Swift access
let uploader = CloudflareImageUploader.Companion().create(...)

// Kotlin
object NetworkClient {
    companion object {
        fun create(...): NetworkClient
    }
}

// Swift access (Companion is required for object)
let client = NetworkClient.Companion().create(...)
```

### 3. Calling Suspend Functions

Kotlin `suspend` functions become async in Swift:

```kotlin
// Kotlin
suspend fun uploadImage(...): NetworkResult<T>
```

```swift
// Swift
let result = try await uploader.uploadImage(...)
```

**Important:** Must be called from `@MainActor` context due to KMP limitations.

## Build Process Flow

### When You Press ⌘R in Xcode:

1. **Pre-build Script** runs
   ```
   ./gradlew :shared:embedAndSignAppleFrameworkForXcode
   ```
   - Gradle builds Kotlin/Native code
   - Creates `shared.framework` for current target
   - Places in build output directory

2. **Xcode Compilation**
   - Compiles Swift code
   - Finds `shared.framework` via `FRAMEWORK_SEARCH_PATHS`
   - Links framework via `OTHER_LDFLAGS`

3. **Code Signing**
   - Xcode signs the app
   - Also signs the embedded framework

4. **App Bundle Created**
   - Framework copied to `Frameworks/` directory
   - Ready to run!

## Framework Locations

### During Build:
```
kmp-test/
├── shared/
│   └── build/
│       └── xcode-frameworks/
│           ├── Debug/
│           │   ├── iphonesimulator/
│           │   │   └── shared.framework  ← Found by Xcode
│           │   └── iphoneos/
│           │       └── shared.framework
│           └── Release/
│               └── ...
└── iosApp/
```

### In App Bundle (Runtime):
```
iosApp.app/
├── iosApp (executable)
└── Frameworks/
    └── shared.framework  ← Embedded here
        ├── shared (binary)
        ├── Info.plist
        └── Modules/
            └── shared.swiftmodule/
```

## Key Files

### 1. Xcode Project Configuration
**File:** `iosApp/iosApp.xcodeproj/project.pbxproj`

Contains:
- Build script phase (calls Gradle)
- Framework search paths
- Linker flags

### 2. Gradle Build Configuration
**File:** `shared/build.gradle.kts`

```kotlin
iosX64(), iosArm64(), iosSimulatorArm64()
    .forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }
```

This configures:
- Which iOS targets to build for
- Framework name (`shared`)
- Static vs dynamic linking (`isStatic = true` means static)

### 3. Kotlin Code
**Location:** `shared/src/`

- `commonMain/` - Shared code
- `iosMain/` - iOS-specific implementations
- `iosMain/kotlin/com/kmpnetworking/Platform.ios.kt` - iOS platform code

## Static vs Dynamic Linking

Your project uses **static linking** (`isStatic = true`):

**Advantages:**
- ✅ Framework code is compiled directly into app binary
- ✅ No separate framework file needed at runtime
- ✅ Faster app launch (no dynamic loading)
- ✅ Simpler distribution

**Disadvantages:**
- ❌ App binary is larger
- ❌ Can't share framework between apps

For this use case (single app), static linking is perfect!

## Troubleshooting

### "No such module 'shared'"

**Problem:** Framework not built or wrong path

**Solution:**
```bash
# Manually build framework
cd /Users/timkreger/Dev/kmp-test
./gradlew :shared:embedAndSignAppleFrameworkForXcode

# Or build for simulator specifically
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

### "Framework not found"

**Problem:** Framework search path is wrong

**Check in Xcode:**
1. Project settings → Build Settings
2. Search for "Framework Search Paths"
3. Should be: `$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`

### Build Script Fails

**Problem:** Gradle not in PATH or wrong directory

**Solution:**
- Script runs: `cd "$SRCROOT/.."` then `./gradlew`
- `$SRCROOT` = `iosApp/` directory
- So it runs from `kmp-test/` directory
- Make sure `gradlew` is executable: `chmod +x gradlew`

### Wrong Architecture

**Problem:** Framework built for wrong CPU

**Solution:**
- Xcode automatically builds for correct architecture
- Build script uses Xcode's `ARCHS` environment variable
- Framework built matches simulator/device architecture

## Manual Framework Build

If you want to build manually (outside Xcode):

```bash
# For iOS Simulator (Apple Silicon Mac)
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# For iOS Simulator (Intel Mac)
./gradlew :shared:linkDebugFrameworkIosX64

# For iOS Device
./gradlew :shared:linkDebugFrameworkIosArm64

# For all architectures
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 \
           :shared:linkDebugFrameworkIosX64 \
           :shared:linkDebugFrameworkIosArm64
```

Framework will be at:
```
shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework
```

## Summary

The linking happens through **3 mechanisms**:

1. **Build Script** - Builds framework before Swift compilation
2. **Framework Search Paths** - Tells Xcode where to find the framework
3. **Linker Flags** - Links the framework into the app

All of this is **automatic** - you just press ⌘R in Xcode and it works!

The framework is:
- ✅ Built automatically on every build
- ✅ Found automatically via search paths
- ✅ Linked automatically via linker flags
- ✅ Embedded automatically in app bundle
- ✅ Signed automatically with app

This is the **recommended approach** for KMP iOS integration! 🚀


