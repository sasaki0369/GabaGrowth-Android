# Gaba Growth Unified v3.0

Android: single icon and single app. On launch, the shared Web learning dashboard is shown inside an Android WebView. The native top-bar Walk & Talk button opens the existing native walk/conversation screen, retaining screen-off playback and native Firebase walk sync. The Browser button opens the public Web version in the default browser for Google sign-in and PC/iPhone access.

IMPORTANT: Android WebView Google sign-in is NOT guaranteed or supported in this release. Sign in using the native Walk & Talk screen for walk sync; sign in to the Web learning dashboard using the Browser button or standalone Safari/Chrome. No login tokens are transferred between WebView and native code. WebView's unauthenticated dashboard may show different cloud stats; use Browser to view signed-in stats. This is a transitional unified UI, not fully unified authentication.

1. Upload the CONTENTS of Android/ to the existing GabaGrowth-Android repository, preserving existing app/google-services.json.
2. Keep GitHub Actions secrets GABA_KEYSTORE_BASE64, GABA_KEYSTORE_PASSWORD, GABA_KEY_ALIAS, GABA_KEY_PASSWORD unchanged.
3. Build APK in GitHub Actions; install the signed release artifact.
4. Upload Web/index.html, icon-192.png, icon-512.png and manifest.webmanifest to GabaGrowth Web repository.
5. Check native walk, background audio, Google login, Firebase sync and browser Web login on device.

The APK itself is not included. Build with your existing GitHub Actions workflow.
