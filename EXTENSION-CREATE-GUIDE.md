# <img src="https://raw.githubusercontent.com/cosmix-extensions/Anime-Dekho/main/assets/icons/docs.svg" width="28" height="28"/> Comprehensive Guide to Creating a Cosmix Extension

Welcome to the ultimate guide for creating extensions for **Cosmix**! This guide will walk you through the entire lifecycle of an extension—from cloning this template, to writing your web-scraping code, testing locally, and finally publishing your `.csx` file to the world.

> [!CAUTION]
> **Security Notice:** Do NOT use the extension framework to create malware, spyware, or perform malicious actions on user devices. All extensions should strictly be used for fetching media content safely. Extensions found violating this rule will be strictly banned and blacklisted.

---

## <img src="https://raw.githubusercontent.com/cosmix-extensions/Anime-Dekho/main/assets/icons/structure.svg" width="24" height="24"/> 1. The Core Architecture

A Cosmix extension is composed of three interconnected systems:
1. **The Plugin Class**: The master entry point where all your individual providers are registered.
2. **The Provider (`CsxApi`)**: The heart of the extension. This defines the source name, website URL, and the scraping logic for searching, loading show details, and fetching video links.
3. **The Extractors**: Built-in helper classes used to bypass popular third-party video hosts (like StreamTape, Voe, FileMoon, etc.) to extract direct `.mp4` or `.m3u8` streams.

---

## <img src="https://raw.githubusercontent.com/cosmix-extensions/Anime-Dekho/main/assets/icons/folder.svg" width="24" height="24"/> 2. Setting Up the Foundation

### Step 1: Set up the Plugin Registry
Every extension needs a central registry. Navigate to `src/main/kotlin/com/example/ExamplePlugin.kt` and rename it to match your extension (e.g., `MyExtensionPlugin.kt`).

```kotlin
package com.myextension

import com.cosmix.app.plugins.CsxPlugin
import com.cosmix.app.plugins.CsxPluginAnnotation

@CsxPluginAnnotation
class MyExtensionPlugin : CsxPlugin() {
    override fun load() {
        // Register all your providers here!
        registerCsxApi(MyProvider())
        
        // You can register multiple providers in a single extension
        // registerCsxApi(MySecondProvider()) 
    }
}
```

> [!WARNING]
> <img src="https://raw.githubusercontent.com/cosmix-extensions/Anime-Dekho/main/assets/icons/warning.svg" width="16" height="16"/> **Crucial:** The `@CsxPluginAnnotation` is strictly required above your class declaration. Without it, the Gradle build system will completely ignore your plugin and the compilation will fail.

---

## <img src="https://raw.githubusercontent.com/cosmix-extensions/Anime-Dekho/main/assets/icons/features.svg" width="24" height="24"/> 3. Deep Dive: Building the Provider

Your provider inherits from `CsxApi`. This is where you perform all the heavy HTTP requests and HTML parsing. Cosmix provides an `app.get()` HTTP client and `Jsoup` for HTML parsing automatically.

### <img src="https://raw.githubusercontent.com/cosmix-extensions/Anime-Dekho/main/assets/icons/star.svg" width="20" height="20"/> A. Configuring the Basics
Set up your provider's identity and capabilities:

```kotlin
class MyProvider : CsxApi() {
    override var mainUrl = "https://example.com"
    override var name = "My Extension"
    
    // Define what your extension serves
    override val supportedTypes = setOf(TvType.Anime, TvType.Movie, TvType.TvSeries)
    
    // Choose your default language
    override var lang = "en"
    
    // Enable or disable the homepage feature
    override var hasMainPage = true
```

### <img src="https://raw.githubusercontent.com/cosmix-extensions/Anime-Dekho/main/assets/icons/star.svg" width="20" height="20"/> B. Generating the Homepage (`getMainPage`)
When users open the app, this function populates the home screen rows (e.g., "Latest Anime", "Popular Movies").

```kotlin
override suspend fun getMainPage(page: Int, requestPath: String?): HomePageResponse {
    // 1. Fetch the HTML
    val document = app.get("$mainUrl/home").document
    
    // 2. Scrape elements using CSS Selectors
    val items = document.select("div.movie-card").mapNotNull { element ->
        val title = element.selectFirst("h3.title")?.text() ?: return@mapNotNull null
        val url = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
        val poster = element.selectFirst("img")?.attr("src")
        
        // 3. Return as a Cosmix SearchResponse object
        newAnimeSearchResponse(title, url) {
            this.posterUrl = poster
        }
    }
    
    // 4. Group them into a row
    return newHomePageResponse(name, items)
}
```

### <img src="https://raw.githubusercontent.com/cosmix-extensions/Anime-Dekho/main/assets/icons/star.svg" width="20" height="20"/> C. Search Functionality (`search`)
When users type in the global search bar, this function executes.

```kotlin
override suspend fun search(query: String): List<SearchResponse> {
    // Format the search URL
    val searchUrl = "$mainUrl/search?keyword=$query"
    val document = app.get(searchUrl).document
    
    // Parse the search results exactly like the homepage
    return document.select("div.result-item").mapNotNull {
        val title = it.selectFirst("h2")?.text() ?: return@mapNotNull null
        val url = it.selectFirst("a")?.attr("href") ?: return@mapNotNull null
        val posterUrl = it.selectFirst("img")?.attr("src")
        
        newMovieSearchResponse(title, url) {
            this.posterUrl = posterUrl
        }
    }
}
```

### <img src="https://raw.githubusercontent.com/cosmix-extensions/Anime-Dekho/main/assets/icons/star.svg" width="20" height="20"/> D. Loading Show Details (`load`)
When a user taps on a movie or anime, they need to see the description, release year, and the list of episodes.

```kotlin
override suspend fun load(url: String): LoadResponse {
    val document = app.get(url).document
    
    val title = document.selectFirst("h1")?.text() ?: "Unknown Title"
    val description = document.selectFirst("p.synopsis")?.text()
    val poster = document.selectFirst("img.poster")?.attr("src")
    
    // Scrape the episodes list
    val episodes = document.select("ul.episodes li").map { ep ->
        val epUrl = ep.selectFirst("a")?.attr("href") ?: ""
        val epName = ep.text()
        
        Episode(
            data = epUrl, // This URL gets passed to loadLinks()
            name = epName
        )
    }

    // Return the bundled LoadResponse
    return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
        this.posterUrl = poster
        this.plot = description
    }
}
```

### <img src="https://raw.githubusercontent.com/cosmix-extensions/Anime-Dekho/main/assets/icons/star.svg" width="20" height="20"/> E. Extracting Video Links (`loadLinks`)
When the user presses "Play", the episode's `data` URL is passed here. Your job is to find the raw `.mp4` or `.m3u8` video player link.

```kotlin
override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {
    val document = app.get(data).document
    
    // Find the embedded iframe
    val iframeUrl = document.selectFirst("iframe.video-player")?.attr("src") ?: return false
    
    // 1. Direct Links: If it's a direct .mp4 or .m3u8, return it immediately
    if (iframeUrl.contains(".mp4") || iframeUrl.contains(".m3u8")) {
        callback(
            ExtractorLink(
                name,
                name,
                iframeUrl,
                referer = mainUrl,
                quality = Qualities.Unknown.value,
                isM3u8 = iframeUrl.contains(".m3u8")
            )
        )
        return true
    }
    
    // 2. Extractors: If the video is hosted on a 3rd-party server, use a Cosmix Extractor
    if (iframeUrl.contains("streamtape.com")) {
        StreamTape().getUrl(iframeUrl, data, isCasting, subtitleCallback, callback)
    } else if (iframeUrl.contains("filemoon.sx")) {
        FileMoon().getUrl(iframeUrl, data, isCasting, subtitleCallback, callback)
    }

    return true
}
```

---

## <img src="https://raw.githubusercontent.com/cosmix-extensions/Anime-Dekho/main/assets/icons/wrench.svg" width="24" height="24"/> 4. Building and Testing

Once you have finished writing your scraping logic, you need to compile it into a `.csx` file.

### Local Compilation
Open your terminal inside the project root and run:
```bash
./gradlew ExampleProvider:makeCsx
```
*(Make sure to replace `ExampleProvider` with the name of your Gradle module if you renamed it).*

If successful, your extension will be built at:
`ExampleProvider/build/ExampleProvider.csx`

### Sideloading into Cosmix
1. Connect your Android device via USB or email the file to yourself.
2. Open the **Cosmix App**.
3. Navigate to **Settings > Extensions**.
4. Click **Install from Local File** and select your `.csx` file!

---

## <img src="https://raw.githubusercontent.com/cosmix-extensions/Anime-Dekho/main/assets/icons/package.svg" width="24" height="24"/> 5. Publishing via GitHub Actions

This template comes fully pre-configured with a CI/CD pipeline! You do not need to manually build your extension for users.

1. Update the `repo.json` file in the root directory. Change the `pluginLists` URL to match your GitHub Username and Repository name.
2. Commit and push your code to the `main` or `master` branch.
3. GitHub Actions will automatically trigger, build the `.csx` file, and generate a `plugins.json` index.
4. It will deploy these artifacts to a silent branch called `builds`.

### <img src="https://raw.githubusercontent.com/cosmix-extensions/Anime-Dekho/main/assets/icons/check.svg" width="24" height="24"/> Adding the Repository to the App
Users can install your entire repository over the air by adding your `plugins.json` URL directly into the Cosmix App:
`https://raw.githubusercontent.com/YOUR_USERNAME/YOUR_REPO/builds/repo.json`

---

## <img src="https://raw.githubusercontent.com/cosmix-extensions/Anime-Dekho/main/assets/icons/warning.svg" width="24" height="24"/> 6. Troubleshooting & FAQ

**Q: My build fails with `"No plugin class annotated with @CsxPluginAnnotation was found"`**
- **Fix:** You forgot to add the `@CsxPluginAnnotation` directly above your Plugin class (e.g., `MyExtensionPlugin`), or you placed it in the wrong file. The compiler *must* see this annotation to register your extension!

**Q: GitHub Actions says `"Minimum supported Gradle version is 9.4.1"`**
- **Fix:** Ensure that your `gradle/wrapper/gradle-wrapper.properties` file has `distributionUrl` pointing to `gradle-9.4.1-bin.zip` or higher. The Android Gradle Plugin requires newer Gradle versions.

**Q: The `.csx` builds fine, but my videos don't play inside the app?**
- **Fix:** This usually means your `loadLinks` function is failing to parse the embedded iframe correctly. Double-check the website's HTML (they often change class names!) and verify you are passing the correct URL to the Extractor.

**Q: How do I test changes without publishing to GitHub every time?**
- **Fix:** Run `./gradlew makeCsx` locally and move the generated `.csx` file to your phone to install via **Settings > Extensions > Install from Local File**. This is much faster for debugging!

**Q: I get "Unresolved reference" errors when compiling.**
- **Fix:** Ensure you are using the correct Cosmix imports (`com.cosmix.app.*`) in your Kotlin files and not outdated or incorrect package names.

**Q: My extension shows up as "Update Available" infinitely.**
- **Fix:** Make sure the version in your `build.gradle.kts` matches the version defined in your generated `plugins.json`. If they mismatch, the app will constantly think there is a new update available.

**Q: How do I support multiple languages?**
- **Fix:** Create a separate Provider class for each language and override the `lang` variable (e.g., `lang = "es"`), then register all of them sequentially in your Plugin class's `load()` method!

**Q: Do I need to manually specify an `iconUrl` in `build.gradle.kts`?**
- **Answer:** No! The Cosmix build system now features an **Automatic Favicon Fetcher**. If you completely remove the `iconUrl` line from your `cosmix` block, the plugin will scan your code for your `mainUrl` and automatically fetch the best available 128px favicon directly from the website during compilation!

---
<div align="center">
  <img src="https://raw.githubusercontent.com/cosmix-extensions/Anime-Dekho/main/assets/icons/heart.svg" width="32" height="32"/>
  <h3>Happy Coding!</h3>
  <p>If you build something awesome, be sure to share it with the community!</p>
</div>
