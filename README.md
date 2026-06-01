# Cosmix Extension Template

⚠️ This is currently under development,
don't use it yet if you're not comfortable
with constantly merging new changes

Official template for building Cosmix extensions (.csx format).

⚠️ Make sure you check "Include all branches"
when using this template

## Getting Started

This template includes 1 example extension.

1. Open root build.gradle.kts and replace all placeholders
2. Familiarize yourself with the project structure
3. Build your first extension using:
   - Windows: .\gradlew.bat ExampleProvider:makeCsx
   - Linux & Mac: ./gradlew ExampleProvider:makeCsx

## Local Testing with ADB

For local extension testing on Android 11+,
grant All Files Access:

Using ADB:
adb shell appops set --uid PACKAGE_NAME
MANAGE_EXTERNAL_STORAGE allow

Replace PACKAGE_NAME with:
- debug: com.cosmix.app.debug
- release: com.cosmix.app

Manually:
1. Open Settings
2. Apps → Special app access
3. All files access
4. Find Cosmix and enable it
5. Restart the app

## Extension Structure

```text
ExampleProvider/
├── build.gradle.kts
└── src/main/kotlin/com/example/
    ├── ExampleProvider.kt
    └── ExamplePlugin.kt
```

## Publishing

After push, GitHub Actions will:
- Build your .csx extension
- Deploy to builds branch
- Update plugins.json automatically

Add this URL to Cosmix app:
https://raw.githubusercontent.com/YOUR_USERNAME/
YOUR_REPO/builds/plugins.json
