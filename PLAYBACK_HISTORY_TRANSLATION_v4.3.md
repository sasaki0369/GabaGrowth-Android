# GabaGrowth Android v4.3
- Each English phrase playback request increments a persistent counter, keyed by the same CSV-specific phrase key as the mastered setting.
- Japanese translation voice modes: before (default), after, both, none.
- Separate Japanese TTS engine (ja-JP) and English TTS engine (en-US). If Japanese TTS language is unavailable, skip the translation without blocking.
- Speech timeout and recognition hand-off retained. In conversation mode the English speech deadline starts only after any preceding Japanese speech.
- Counters persist across sessions on the device; they do not currently sync with the web app.
- Settings are saved to gaba_walk_v1 SharedPreferences. Current phrase counter appears on Walk & Talk screen.
- A playback count represents an English TTS request, not independently verified audible completion.
- Build and real-device tests have not been run.
