# Gaba Growth Android 2.1

This source ZIP is intended for GitHub Actions builds. Upload the **folder paths**, not flattened files. Replace the corresponding files in your existing `GabaGrowth-Android` repository.

Changes: Japanese-first labels; en-US recognition with ready/listening status and readable error messages; embedded WebView for existing Gaba Growth PWA; background TTS walk service retained; SDK workflow fix retained.

**Important limitations:** The embedded WebView has separate localStorage from Chrome; prior learning history is NOT automatically migrated. The native microphone test only works with the screen on. WebView microphone permission integration has not been implemented. The Android app build and device behavior have not been verified in this environment. Walk & Talk remains audio-only, without automatic speech recognition or AI feedback during screen-off.

To build: Actions > Build Android APK > Run workflow, or commit files to main. Download artifact and install debug APK. If GitHub reports a build error, inspect the `Build debug APK` step.
