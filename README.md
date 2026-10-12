# Gaba Growth Android 2.0 — Walk & Talk prototype

This is a separate native Android proof of concept, **not** a replacement for the existing Gaba Growth PWA. It opens the existing site in the phone browser so the existing browser localStorage data is retained.

## Smartphone-only GitHub Actions build

1. On GitHub, create a new repository, e.g. `GabaGrowth-Android` (recommended; keep `GabaGrowth` PWA unchanged).
2. Extract this ZIP and upload its **contents**, including the hidden `.github` folder, to the root of the new repository. On Android, GitHub web UI may not support bulk folder upload easily; use a GitHub-compatible file manager or Git client. Alternatively use a computer once for the initial upload.
3. Open the repository > **Actions** > **Build Android APK** > **Run workflow** (or commit to main).
4. Wait for the green build. Open the workflow run > **Artifacts** > `GabaGrowth-Android-v2.0-debug` and download ZIP.
5. Extract `app-debug.apk`, install on Android (allow install from this source when prompted). Debug builds are for personal testing only.

## Prototype limitations
- Screen-off: foreground-service **audio playback** is implemented; it reads practice prompts and leaves pauses for spoken repetition. It does not capture speech in the background.
- Speech recognition is a **separate screen-on test** using Android SpeechRecognizer and en-US.
- Translation, AI feedback, full voice dialogue, adaptive review, notifications controls beyond Stop, and synchronization with PWA are not implemented.
- Native app has no account credentials and does not send audio to our own server. Device speech recognition may use an external recognition service, depending on device settings.
- The debug APK is unsigned for release purposes (signed with a debug key); for durable app updates, use a stable private signing key stored as GitHub Secrets, never commit it.
- Avoid watching the screen while walking. Background audio may be affected by battery management or TTS engine availability.
