# Fixing Android Studio Gradle Sync Error

## Error
```
Task 'wrapper' not found in project ':androidApp'.
```

## Root Cause
Android Studio is trying to run the `wrapper` task on the `:androidApp` subproject, but the `wrapper` task only exists at the root project level. This is a common Android Studio sync issue.

## Solutions (Try in order)

### Solution 1: Invalidate Caches and Restart

1. In Android Studio, go to: **File → Invalidate Caches...**
2. Select: **Invalidate and Restart**
3. Wait for Android Studio to restart and sync

### Solution 2: Check Project Structure

Make sure you opened the **root project** (`kmp-test`), not the `androidApp` subproject:

1. **File → Open...**
2. Navigate to: `/Users/timkreger/Dev/kmp-test`
3. Select the **root directory** (`kmp-test`), not `androidApp`
4. Click **OK**

The root directory should contain:
- `settings.gradle.kts`
- `build.gradle.kts`
- `gradlew`
- `androidApp/` folder
- `shared/` folder

### Solution 3: Sync Gradle Files

1. In Android Studio, go to: **File → Sync Project with Gradle Files**
2. Wait for sync to complete
3. If it fails, try: **File → Reload Gradle Project**

### Solution 4: Clean and Rebuild

From terminal:
```bash
cd /Users/timkreger/Dev/kmp-test
./gradlew clean
./gradlew :androidApp:build
```

Then in Android Studio: **File → Sync Project with Gradle Files**

### Solution 5: Delete .idea Folder

If the above don't work:

1. Close Android Studio completely
2. Delete the `.idea` folder (if it exists):
   ```bash
   cd /Users/timkreger/Dev/kmp-test
   rm -rf .idea
   ```
3. Reopen Android Studio
4. Let it recreate the `.idea` folder
5. Sync Gradle files

### Solution 6: Check Gradle Settings

In Android Studio:
1. **File → Settings** (or **Android Studio → Preferences** on Mac)
2. Go to: **Build, Execution, Deployment → Build Tools → Gradle**
3. Make sure:
   - **Use Gradle from:** `'wrapper' task in Gradle build script` (recommended)
   - Or select: `Specified location` and point to: `/Users/timkreger/Dev/kmp-test/gradle/wrapper/gradle-wrapper.properties`
4. Click **OK**
5. **File → Sync Project with Gradle Files**

### Solution 7: Manual Gradle Wrapper Update

If all else fails, regenerate the wrapper:

```bash
cd /Users/timkreger/Dev/kmp-test
./gradlew wrapper --gradle-version 8.4
```

Then in Android Studio: **File → Sync Project with Gradle Files**

## Verification

After fixing, verify the project structure is correct:

1. In Android Studio, open **View → Tool Windows → Gradle**
2. You should see:
   - `:androidApp`
   - `:shared`
   - Root project tasks (including `wrapper`)
3. Try running a build: **Build → Make Project** (⌘F9)

## Root Cause Found: .idea Folder in Subproject

If you have a `.idea` folder in the `androidApp/` directory, Android Studio is treating it as a standalone project instead of a subproject. This causes it to look for the wrapper task in the wrong place.

**Fix:**
```bash
cd /Users/timkreger/Dev/kmp-test
rm -rf androidApp/.idea
```

Then:
1. **Close Android Studio completely**
2. **File → Open...** → Select the **root directory** (`kmp-test`)
3. Let Android Studio sync
4. The `.idea` folder should be created in the root directory only

## Most Common Fix

In most cases, **removing the `.idea` folder from the subproject** and opening the root project fixes the issue.

## Still Having Issues?

If none of the above work:

1. Check Android Studio logs: **Help → Show Log in Explorer**
2. Look for Gradle sync errors
3. Try updating Android Studio to the latest version
4. Make sure you're using a compatible Gradle version (8.4)

