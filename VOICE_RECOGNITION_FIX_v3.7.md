# GG Android v3.7 — conversation recovery patch

Based on user-provided v3.6 source.

- Adds TTS completion watchdog (11–45 s based on phrase length and playback speed), allowing conversation to progress if onDone is never delivered.
- Guards TTS completion against duplicate callbacks and stale sessions.
- Shows transition status from speaking to preparing recognition to microphone startup and listening.
- Preserves Google login, CSV library, icons, signing setup and package ID.
- Raises Android versionCode 36 -> 37 and versionName 3.6 -> 3.7.

**Important:** The watchdog is a fallback, not proof of a specific device cause. Speech recognition still depends on the Android recognition service and device policies.

Upload the ZIP contents to the existing GitHub repository, preserving the configured GitHub Actions secrets.
