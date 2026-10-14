# Gaba Growth Android Ver.2.9 design update

Design refresh based on the agreed navy/teal reference. Preserves WalkService and FirebaseSync logic from supplied Ver.2.7 source.

IMPORTANT: This source is based on the locally available Ver.2.7 project, not the current Ver.2.8 GitHub repository. Before applying, compare changes introduced in Ver.2.8 (particularly Android deep-link handling) and preserve the current `app/google-services.json` and fixed-signing `.github/workflows/android-apk.yml` in GitHub. Never commit keystore or base64 secrets.

Updated: MainActivity.java, app/build.gradle (versionCode 29), icon mipmaps and adaptive icon resources. This is source code, not a tested APK. Build and verify using GitHub Actions.

Ver.2.9 navigation fix: Removed Android-native duplicate back toolbar from embedded WebView; retained the styled Web back button and gabagrowth://walk interception. Android system Back still works.
