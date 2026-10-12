# Gaba Growth Android Ver.2.2

## Changes
- Native launcher icon generated from the existing Gaba Growth icon.
- WebView top navigation bar with a dedicated return button.
- Status bar / navigation bar safe-area handling for Android 15+ edge-to-edge.
- Existing audio playback and speech recognition retained.

## GitHub upload
Upload the contents of `app/` to the **same paths** in the GitHub repository. You can leave `.github/workflows/android-apk.yml` unchanged to avoid PAT workflow permission errors. Build using the existing Actions workflow.

## Important
GitHub Actions debug APK signatures may change across runs; if Android rejects an update, uninstall the previous prototype first. This deletes native app data. Production updates require a stable signing key.

WebView and Chrome use separate storage. Existing Chrome learning records are not automatically imported. Screen-off speech recognition is not implemented yet.
