#!/bin/bash
# ============================================================
# Build release APK in Codespace using GitHub Actions secrets
# ============================================================

set -e  # stop on first error

# 1. Ensure we're in the project root
cd /workspaces/mobile-apk || exit

# 2. Set Android SDK environment variables
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH
export PATH=$ANDROID_SDK_ROOT/platform-tools:$PATH

# 3. Install Android command-line tools (if not already present)
if [ ! -d "$ANDROID_SDK_ROOT" ]; then
  echo "📦 Installing Android SDK..."
  mkdir -p "$ANDROID_SDK_ROOT"
  cd ~
  wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q -o commandlinetools-linux-11076708_latest.zip
  mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
  mv cmdline-tools "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  rm commandlinetools-linux-11076708_latest.zip
  cd /workspaces/mobile-apk
else
  echo "✅ SDK already present – skipping installation."
fi

# 4. Accept licenses and install required SDK components
yes | sdkmanager --licenses > /dev/null 2>&1
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" > /dev/null 2>&1

# 5. Create mandatory files for Gradle
echo "sdk.dir=$ANDROID_HOME" > local.properties
touch .env .env.example

# 6. Check that all required secrets are set
if [ -z "$RELEASE_KEYSTORE_BASE64" ]; then
  echo "❌ RELEASE_KEYSTORE_BASE64 is not set. Export it first."
  exit 1
fi
if [ -z "$RELEASE_STORE_PASSWORD" ]; then
  echo "❌ RELEASE_STORE_PASSWORD is not set. Export it first."
  exit 1
fi
if [ -z "$RELEASE_KEY_PASSWORD" ]; then
  echo "❌ RELEASE_KEY_PASSWORD is not set. Export it first."
  exit 1
fi

# 7. Decode the release keystore from Base64
echo "$RELEASE_KEYSTORE_BASE64" | base64 -d > release-upload-key.jks
if [ ! -f release-upload-key.jks ]; then
  echo "❌ Failed to decode keystore."
  exit 1
fi
echo "✅ Keystore decoded successfully."

# 8. Set the environment variables expected by build.gradle.kts
export KEYSTORE_PATH="$PWD/release-upload-key.jks"
export STORE_PASSWORD="$RELEASE_STORE_PASSWORD"
export KEY_PASSWORD="$RELEASE_KEY_PASSWORD"

# (The key alias is hardcoded as "upload" in the build file, so we don't need to set it.)

# 9. Clean and build the release APK
rm -rf build/ .gradle/ app/build
echo "🔨 Building release APK – this will take a few minutes..."
gradle clean :app:assembleRelease --no-daemon

# 10. Verify the output
if [ -f app/build/outputs/apk/release/app-release.apk ]; then
  echo "✅ Build successful! APK at: app/build/outputs/apk/release/app-release.apk"
  ls -l app/build/outputs/apk/release/app-release.apk
else
  echo "❌ Build failed – check the error messages above."
  find app/build/outputs -name "*.apk" 2>/dev/null || echo "No APK found."
fi

# 11. Clean up sensitive keystore file (optional)
rm -f release-upload-key.jks
