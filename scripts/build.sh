#!/usr/bin/env bash
# Build with a JDK and official Android SDK build tools; no Gradle or dependencies.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
BT="${ANDROID_BUILD_TOOLS:-${SDK_ROOT}/build-tools/35.0.0}"
PLATFORM="${ANDROID_PLATFORM_JAR:-${SDK_ROOT}/platforms/android-35/android.jar}"
if [[ ! -f "$PLATFORM" || ! -x "$BT/aapt2" ]]; then
  echo 'Install Android SDK Platform 35 and Build-Tools 35.0.0, then set ANDROID_SDK_ROOT.' >&2
  exit 1
fi
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
OUT="$ROOT/build"
mkdir -p "$OUT/classes" "$OUT/dex" "$OUT/signing"
# Remove only prior build products, keeping the local signing identity.
find "$OUT/classes" -type f -delete
find "$OUT/dex" -type f -delete
javac -source 8 -target 8 -cp "$PLATFORM" -d "$OUT/classes" "$ROOT"/app/src/main/java/org/stevenchu/screencover/*.java
jar cf "$OUT/classes.jar" -C "$OUT/classes" .
"$BT/d8" --lib "$PLATFORM" --min-api 26 --output "$OUT/dex" "$OUT/classes.jar"
"$BT/aapt2" compile --dir "$ROOT/app/src/main/res" -o "$OUT/resources.zip"
"$BT/aapt2" link -I "$PLATFORM" --manifest "$ROOT/app/src/main/AndroidManifest.xml" -o "$OUT/base.apk" "$OUT/resources.zip"
cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && zip -q "$OUT/unsigned.apk" classes.dex)
"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
KEYSTORE="${SCREEN_COVER_KEYSTORE:-$OUT/signing/development.jks}"
export SCREEN_COVER_STORE_PASS="${SCREEN_COVER_STORE_PASS:-android}"
ALIAS="${SCREEN_COVER_KEY_ALIAS:-screen-cover}"
if [[ ! -f "$KEYSTORE" ]]; then
  if [[ -n "${SCREEN_COVER_KEYSTORE:-}" ]]; then echo 'Release keystore does not exist.' >&2; exit 1; fi
  keytool -genkeypair -keystore "$KEYSTORE" -storepass:env SCREEN_COVER_STORE_PASS -keypass:env SCREEN_COVER_STORE_PASS -alias "$ALIAS" -dname 'CN=Screen Cover Development' -keyalg RSA -validity 3650
fi
"$BT/apksigner" sign --ks "$KEYSTORE" --ks-key-alias "$ALIAS" --ks-pass env:SCREEN_COVER_STORE_PASS --out "$OUT/ScreenCover.apk" "$OUT/aligned.apk"
"$BT/apksigner" verify --verbose "$OUT/ScreenCover.apk"
python3 "$ROOT/scripts/checksum.py" "$OUT/ScreenCover.apk" > "$OUT/SHA256SUMS"
echo "Built $OUT/ScreenCover.apk"
