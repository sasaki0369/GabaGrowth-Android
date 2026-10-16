# GG v3.9 — Conversation hand-off recovery

- Display version updated to 3.9 (was hard-coded to 3.6).
- Adds a short independent TTS-to-recognition hand-off deadline for conversation mode.
- Removes the second “Your turn” TTS playback that could stall independently.
- Cancels TTS and invalidates its callbacks before starting recognition to avoid the app recognizing its own speech.
- Leaves repeat-only playback and CSV library intact.
- The APK has not been built or tested on a physical device in this environment.
