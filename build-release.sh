#!/bin/bash
set -e

cd /workspaces/mobile-apk || exit

# ------------------- 1. JAVA 17 via APT -------------------
echo "📦 Installing Java 17 (OpenJDK) via apt..."
sudo apt update -qq
sudo apt install -y openjdk-17-jdk

export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
echo "✅ Java version: $(java -version 2>&1 | head -n1)"

# ------------------- 2. ANDROID SDK (unchanged) -------------------
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$PATH
export PATH=$ANDROID_SDK_ROOT/platform-tools:$PATH

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
  echo "✅ SDK already present."
fi

yes | sdkmanager --licenses > /dev/null 2>&1
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" > /dev/null 2>&1
echo "sdk.dir=$ANDROID_HOME" > local.properties
touch .env .env.example

# ------------------- 3. LOAD SECRETS -------------------
if [ -f .env.release ]; then
  set -a
  source .env.release
  set +a
else
  echo "❌ .env.release not found – please create it with your secrets."
  exit 1
fi

if [ -z "$RELEASE_KEYSTORE_BASE64" ] || [ -z "$RELEASE_STORE_PASSWORD" ] || [ -z "$RELEASE_KEY_PASSWORD" ]; then
  echo "❌ One or more secrets are missing in .env.release."
  exit 1
fi

# ------------------- 4. DECODE KEYSTORE -------------------
echo "$RELEASE_KEYSTORE_BASE64" | base64 -d > release-upload-key.jks
export KEYSTORE_PATH="$PWD/release-upload-key.jks"
export STORE_PASSWORD="$RELEASE_STORE_PASSWORD"
export KEY_PASSWORD="$RELEASE_KEY_PASSWORD"

# ------------------- 5. GRADLE WRAPPER (if missing) -------------------
if [ ! -f "./gradlew" ]; then
  echo "📦 Generating Gradle wrapper..."
  gradle wrapper --gradle-version 9.3.1
fi

# ------------------- 6. SET MEMORY OPTIONS -------------------
export GRADLE_OPTS="-Xmx2g -XX:MaxMetaspaceSize=512m"
# Also write to gradle.properties for extra safety
echo "org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m" >> gradle.properties

# ------------------- 7. BUILD RELEASE APK (with memory-friendly flags) -------------------
rm -rf build/ .gradle/ app/build
echo "🔨 Building release APK with memory limits..."
./gradlew clean :app:assembleRelease --stacktrace --no-daemon --max-workers=1

# ------------------- 8. VERIFY OUTPUT -------------------
if [ -f app/build/outputs/apk/release/app-release.apk ]; then
  echo "✅ Build successful! APK at: app/build/outputs/apk/release/app-release.apk"
  ls -l app/build/outputs/apk/release/app-release.apk
else
  echo "❌ Build failed – check the stacktrace above."
  exit 1
fi

rm -f release-upload-key.jks
echo "✅ Keystore removed from disk."
