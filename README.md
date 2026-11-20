# Https Browser

**A fast, lightweight Android browser built for exploration, learning, and everyday browsing.**

Developed by **MD.Nirob** · **RootDeck-Org**

Https Browser is a native Android browser built with **Kotlin**, **Jetpack Compose**, and **Android WebView**. It combines a Chromium-based browsing engine with memory-conscious tab management, built-in developer tools, and offline reading features.

The project aims to provide a smooth browsing experience on devices with limited RAM while giving curious users practical tools to understand how the web works.

Https Browser is designed to be approachable for younger learners (ages 5–16) with adult guidance, while also providing inspection tools for students, educators, and developers.

> **Important:** Https Browser is a browser, not a parental-control or certified child-safe browsing product. Younger users should browse with a parent, guardian, or teacher.

---

## Features

### Browsing

- Chromium-based page rendering through Android WebView.
- Hardware-accelerated rendering.
- Multi-tab browsing with a dedicated tab-switcher grid.
- Live tab-preview thumbnails.
- Incognito mode with no persistent browsing history, cache, or cookies.
- Renderer-process recovery using `onRenderProcessGone`, allowing affected tabs to recover instead of terminating the app.
- Chrome-inspired interface featuring a pill-shaped address bar, SSL indicator, linear loading bar, and overflow menu.

### Low-memory architecture

- Background tabs can be hibernated into compressed `.tab` pagefiles.
- Pagefiles preserve the URL, title, scroll position, form state, and navigation history.
- Saved tab state is restored when a tab is reopened.
- LRU-based disk eviction.
- A 2 MB per-tab pagefile cap.
- Memory-pressure handling through `ComponentCallbacks2.onTrimMemory`.

These mechanisms are intended to reduce unnecessary memory use while keeping browsing sessions recoverable.

### Reading and saving

- **Reader Mode** — distraction-free article reading with adjustable typography and light, sepia, and dark themes.
- **Reading List** — save pages for offline reading.
- **Full-page capture** — capture the complete scrollable page as PNG or paginated PDF.
- **Bookmarks** — save frequently visited websites.
- **Browsing history** — locally stored browsing records.
- **Session Manager** — save and restore named groups of tabs.

Bookmarks and history use a local Room database. An account is not required.

### Built-in developer tools

Developer tools are available from the overflow menu.

| Tool | Capabilities |
|---|---|
| **Elements** | Live DOM inspection and inspect-element touch mode |
| **Console** | JavaScript log, warning, error, and info streams; interactive JavaScript REPL |
| **Network** | Request waterfall, headers, status, timing, and cookies |
| **Application** | Cookie inspection, `localStorage`, `sessionStorage`, and clear-site-data |
| **Security** | TLS version, certificate status, and mixed-content warnings |

Additional browser diagnostics and experiments:

- `chrome://flags` — access supported experimental Chromium switches.
- `chrome://net-internals` — inspect available browser diagnostics, memory information, pagefiles, and blocked trackers.
- `chrome://dino` — access the offline T-Rex game, which is also embedded in the offline error page.

Experimental functionality depends on the Android WebView implementation and device environment.

### Security and privacy

- **DNS Blackhole** — local blocklist-based interception of known tracker and advertising domains.
- **Incognito mode** — separate browsing experience using `FLAG_SECURE`, with no persistent browsing history, cache, or cookies.
- **Clear browsing data** — clear history, cache, cookies, and pagefiles.
- **No telemetry** — the application does not phone home.

HTTPS security still depends on the website being visited and the underlying Android WebView implementation. The DNS blocklist is not a guarantee that all trackers or advertisements will be blocked.

### Additional features

- **PDF viewer** — in-app PDF rendering using Android `PdfRenderer`, with system-viewer fallback.
- **Picture-in-Picture** — supported video playback in a floating window (Android 8.0+).
- **File opener** — accepts `.link`, `.html`, `.xhtml`, `.mhtml`, and `.pdf` files shared from other applications.
- **Google Password Manager** — autofill integration on supported Android versions.
- **Offline error page** — Chrome-inspired offline page featuring the Dino game.

Feature availability depends on Android version, website compatibility, WebView version, and device hardware.

---

## Technology stack

| Layer | Technology |
|---|---|
| Programming language | Kotlin |
| UI framework | Jetpack Compose |
| Rendering engine | Android WebView (Chromium) |
| Local persistence | Room |
| Code generation | KSP |
| Asynchronous operations | Kotlin Coroutines and Flow |
| Build system | Gradle |
| Android build plugin | Android Gradle Plugin |
| Release optimization | R8 |

### Application information

| Property | Value |
|---|---|
| Application name | Https Browser |
| Application ID | `com.rootdeck.httpsbrowser` |
| Minimum SDK | 24 (Android 7.0) |
| Compile SDK | 36 |
| Target SDK | 36 |
| Primary language | Kotlin |

---

## Requirements

To build the project locally, you will need:

- JDK 17 or later.
- Android SDK Platform 36.
- Android SDK Build-Tools 36.0.0.
- Android Studio compatible with the project's Android Gradle Plugin version.
- Internet access for the initial Gradle dependency download.

---

## Build

Clone the repository or open the project in Android Studio. Run the following commands from the project root.

### Debug build

```bash
./gradlew assembleDebug
```

### Windows

```bat
gradlew.bat assembleDebug
```

### Unit tests

```bash
./gradlew testDebugUnitTest
```

### Release build

```bash
./gradlew assembleRelease
```

Generated APKs are written to:

```text
app/build/outputs/apk/
```

### Release signing

Before distributing a release build, configure your own signing credentials.

Create a release keystore and add the following properties to your local `local.properties` file. Do not commit this file or your keystore.

```properties
sdk.dir=/path/to/Android/Sdk
KEYSTORE_PATH=/path/to/release-key.jks
STORE_PASSWORD=your_store_password
KEY_ALIAS=your_key_alias
KEY_PASSWORD=your_key_password
```

Then run:

```bash
./gradlew clean assembleRelease
```

A successful release build confirms that the project compiles and packages in that environment. It does not guarantee that every feature works on every device. Perform physical-device testing and complete the relevant publishing checks before distribution.

---

## For parents and educators

Https Browser is intended to support exploration and learning, but it is **not** a certified child-safe browsing service.

The project does not claim to provide:

- Parental controls.
- Guaranteed content filtering.
- Safe-search enforcement.
- Child-safety certification.

Parents, guardians, and teachers should help younger users choose appropriate websites and develop responsible browsing habits.

Suggested learning contexts:

| Age group | Suggested use |
|---|---|
| **5–8** | Explore parent- or teacher-selected websites together. |
| **9–12** | Collect research links, organize reading, and develop information skills. |
| **13–16** | Explore web technologies, inspect browser features, and learn how websites work. |

These are suggested learning contexts, not age restrictions or separate application modes.

---

## Project status

The Release variant has completed all checks.

---

## Contributing

Contributions and improvements are welcome.

Before submitting an issue or pull request:

1. Check existing issues to avoid duplicates.
2. Provide clear reproduction steps.
3. Include your Android version and device model.
4. Explain the expected and actual behavior.
5. Test changes on an emulator or physical device whenever possible.

Please do not include passwords, authentication credentials, private browsing data, or screenshots containing sensitive information in public issues.

By submitting a contribution, you confirm that you have the right to submit it under the project's licensing terms. The project maintainer should obtain explicit agreement from contributors before applying the project's additional attribution terms to their contributions.

---

## License and attribution

Https Browser is distributed under the **GNU General Public License, version 3 only (GPL-3.0-only)**, together with the additional author-attribution term stated at the end of [`LICENSE`](LICENSE).

When you distribute Https Browser, or a modified version containing covered original Https Browser material, you must comply with the GPLv3 and the additional attribution term. In particular:

- Preserve applicable copyright and license notices.
- Provide the corresponding source code as required by GPLv3 when conveying covered object-code versions.
- Credit **MD.Nirob · RootDeck-Org** in both the project's README (or equivalent primary documentation) and the Android application's About, Credits, or Legal Notices screen.
- Clearly identify modified versions and the date of modification.
- Do not imply that the original project or its original author endorses your modified version.

**Required attribution:**

> Based on Https Browser, originally developed by MD.Nirob · RootDeck-Org.  
> Original project: https://github.com/RootDeck-Org

The attribution term applies to original Https Browser material for which the relevant copyright holder has authority to impose it. Third-party libraries, assets, and contributions from other copyright holders may have separate license terms. Review and comply with those terms as well.

See [`LICENSE`](LICENSE) for the complete GPLv3 text and the additional attribution term.

---

## Contact

- **Developer:** MD.Nirob
- **Organization:** RootDeck-Org
- **Website:** [RootDeck-Org](https://github.com/Official-RootDeck/)
- **GitHub organization:** [RootDeck-Org](https://github.com/RootDeck-Org)

---

<p align="center">
  <strong>Https Browser</strong><br/>
  Explore with curiosity. Learn with confidence. Browse thoughtfully.<br/><br/>
  <sub>Built with Kotlin and Jetpack Compose · A RootDeck Project</sub>
</p>
