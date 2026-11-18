#!/usr/bin/env bash
# Build only the .test package and FAIL unless it is compatible with the supplied old APK.
set -euo pipefail
if [[ $# -ne 1 ]]; then
  echo "Kullanım: tools/build_075_upgrade.sh /dosya/WordQuest_AI_v0.7.5_Test.apk" >&2
  exit 2
fi
: "${WQ_UPGRADE_KEYSTORE:?v0.7.5 imzalama anahtarının bulunduğu keystore yolu gerekli}"
: "${WQ_UPGRADE_STORE_PASSWORD:?Keystore parolası gerekli}"
: "${WQ_UPGRADE_KEY_ALIAS:?Keystore alias gerekli}"
: "${WQ_UPGRADE_KEY_PASSWORD:?Özel anahtar parolası gerekli}"
cd "$(dirname "$0")/.."
./gradlew :app:testPreviewUnitTest :app:assemblePreview
python3 tools/verify_apk_upgrade.py "$1" app/build/outputs/apk/preview/app-preview.apk
printf 'APK metadata, imza ve birim testleri uygun. Gerçek cihazda güncelleme testi yine gereklidir.\n'
