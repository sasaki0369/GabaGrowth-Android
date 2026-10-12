Gaba Growth Android 2.7: Google Sign-In (sasaki0369@gmail.com only).

Important: Firebase Android app must have SHA-1 of the exact APK signing certificate registered, and google-services.json must be downloaded again after SHA registration. Google sign-in may fail with error 10 until that is done.

GitHub Actions workflow included. To use stable release signing, configure GABA_KEYSTORE_BASE64, GABA_KEYSTORE_PASSWORD, GABA_KEY_ALIAS, GABA_KEY_PASSWORD as repository secrets. Debug fallback signing varies per runner and is not stable.

Upload the full ZIP with folder paths preserved. Firebase Realtime Database rules are in firebase-rules.json and must be deployed manually.
