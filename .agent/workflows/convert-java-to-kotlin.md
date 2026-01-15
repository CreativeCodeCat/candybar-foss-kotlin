# Java to Kotlin Conversion Workflow

This workflow guides you through converting all Java source files in the library module to Kotlin using Android Studio's built-in converter.

## Prerequisites

- Android Studio installed and project opened
- All changes committed to git (recommended for easy rollback if needed)

## Conversion Steps

### Option 1: Bulk Conversion (Fastest)

1. **Open Project in Android Studio**
    - Open the `candybar-foss` project in Android Studio
    - Wait for Gradle sync to complete

2. **Select All Java Files**
    - In the Project view, navigate to `library/src/main/java`
    - Right-click on the `java` folder
    - Select **Convert Java File to Kotlin File**
    - Or use keyboard shortcut:
        - Windows/Linux: `Ctrl + Alt + Shift + K`
        - Mac: `⌥ + ⇧ + ⌘ + K`

3. **Confirm Conversion**
    - Android Studio will show a dialog asking to convert all files
    - Click **OK** to proceed
    - The conversion will process all 111 Java files

4. **Review Conversion Warnings**
    - Android Studio may show warnings about code that needs manual review
    - Note these warnings for the cleanup phase

5. **Build and Test**
   ```bash
   ./gradlew clean build
   ```

### Option 2: Gradual Conversion (Safer)

Convert files package by package for better control:

#### Phase 1: Utilities and Helpers (Low Risk)

```
library/src/main/java/candybar/lib/helpers/
library/src/main/java/candybar/lib/utils/
```

#### Phase 2: Models and Databases

```
library/src/main/java/candybar/lib/databases/
library/src/main/java/candybar/lib/models/
library/src/main/java/candybar/lib/items/
```

#### Phase 3: Adapters

```
library/src/main/java/candybar/lib/adapters/
```

#### Phase 4: Fragments

```
library/src/main/java/candybar/lib/fragments/
```

#### Phase 5: Activities (Highest Risk)

```
library/src/main/java/candybar/lib/activities/
library/src/main/java/candybar/lib/applications/
```

For each phase:

1. Right-click on the package folder
2. Select **Convert Java File to Kotlin File**
3. Build and test: `./gradlew :library:assembleDebug`
4. Fix any compilation errors before moving to the next phase
5. Commit changes: `git add . && git commit -m "Convert [package] to Kotlin"`

## Post-Conversion Cleanup

After conversion, apply Kotlin idioms and best practices:

### 1. Use Data Classes

Convert simple POJOs to data classes:

```kotlin
// Before (converted from Java)
class Icon {
    var name: String? = null
    var drawable: Int = 0
}

// After (Kotlin idiom)
data class Icon(
    val name: String?,
    val drawable: Int
)
```

### 2. Apply Null Safety

Review and fix null safety:

```kotlin
// Replace !! with safe calls where possible
val name = icon!!.name  // Risky
val name = icon?.name   // Safe

// Use Elvis operator for defaults
val name = icon?.name ?: "Unknown"
```

### 3. Use Kotlin Collections

```kotlin
// Before
val list = ArrayList<String>()
list.add("item")

// After
val list = mutableListOf<String>()
list += "item"

// Or immutable
val list = listOf("item1", "item2")
```

### 4. Apply Scope Functions

```kotlin
// Before
val intent = Intent(this, MainActivity::class.java)
intent.putExtra("key", value)
intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
startActivity(intent)

// After
Intent(this, MainActivity::class.java).apply {
    putExtra("key", value)
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
}.also { startActivity(it) }
```

### 5. Use When Instead of Switch

```kotlin
// Converted switch becomes when
when (type) {
    TYPE_HEADER -> HeaderViewHolder(view)
    TYPE_ITEM -> ItemViewHolder(view)
    else -> throw IllegalArgumentException("Unknown type")
}
```

### 6. Extension Functions

Create extension functions for common operations:

```kotlin
fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

// Usage
context.toast("Hello")
```

## Verification

After conversion and cleanup:

1. **Build Successfully**
   ```bash
   ./gradlew clean build
   ```

2. **Run Tests**
   ```bash
   ./gradlew test
   ```

3. **Test App Functionality**
    - Install and run the app
    - Test all major features
    - Verify no runtime crashes

4. **Check for Warnings**
   ```bash
   ./gradlew :library:lintDebug
   ```

## Troubleshooting

### Common Issues

**Issue**: `Unresolved reference` errors

- **Fix**: Add missing imports or check if the class was renamed during conversion

**Issue**: `Type mismatch` errors

- **Fix**: Add explicit type annotations or fix null safety

**Issue**: `Platform declaration clash` errors

- **Fix**: Rename methods that conflict with Kotlin stdlib or add `@JvmName` annotation

**Issue**: Compilation is very slow

- **Fix**: Increase Gradle memory in `gradle.properties`:
  ```properties
  org.gradle.jvmargs=-Xmx4096m
  ```

## Rollback

If conversion causes issues:

```bash
# Discard all changes and restore Java files
git checkout HEAD -- library/src/main/java

# Or restore from specific commit
git checkout <commit-hash> -- library/src/main/java
```

## Benefits After Conversion

✅ **Null Safety**: Compile-time null checking prevents NullPointerExceptions  
✅ **Concise Code**: Kotlin reduces boilerplate by ~40%  
✅ **Modern Features**: Coroutines, extension functions, data classes  
✅ **Better IDE Support**: Enhanced autocomplete and refactoring  
✅ **Interoperability**: Seamless Java/Kotlin interop during migration
