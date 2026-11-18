# WordQuest AI v0.7.5 APK derleme ve imzalama

Android SDK 35, Build Tools 35.0.0 ve JDK 17 ile bu klasörü aç. Linux veya macOS’ta Gradle görevlerini `./gradlew` ile, Windows’ta `gradlew.bat` ile çalıştır.

| Amaç | Gradle görevi | Çıktı |
| --- | --- | --- |
| WordQuest AI Test APK’sı | `:app:assemblePreview` | `app/build/outputs/apk/preview/app-preview.apk` |
| Mevcut paket adıyla dağıtım derlemesi | `:app:assembleRelease` | `app/build/outputs/apk/release/app-release-unsigned.apk` |
| Öğrenme ve yedekleme testleri | `:app:testDebugUnitTest` | `app/build/reports/tests/testDebugUnitTest/` |
| Android Lint | `:app:lintDebug` | `app/build/reports/lint-results-debug.html` |

## WordQuest AI Test güncellemesi

Teslim edilen Test APK’sı v0.7.0 Test ile aynı imzaya ve `com.kelimebulmacasi.a1.practice.test` paket kimliğine sahiptir. v0.7.5 sürüm kodu 12’dir. Android’in **Güncelle** seçeneğiyle v0.7.0 Test’in üzerine kurulur.

`assemblePreview` derleme ortamının Android debug anahtarını kullanır. Başka bir ortamdan derlerken önce imzayı `apksigner verify --print-certs` ile karşılaştır. v0.7.0 ve v0.7.5 Test sertifikasının SHA-256 parmak izi `88e25b3ad209d33f7052c29a939588c6c8e898d6398468c2a2e0bb78878bd19c`. İmzalama anahtarını özel ve güvenli biçimde sakla.

## v0.6.0’ın üzerine doğrudan kurulum

Dağıtım paket kimliği **`com.kelimebulmacasi.a1.practice`**, v0.7.5 sürüm kodu **12**, eski v0.6.0 sürüm kodu **10**. Doğrudan güncelleme için özgün v0.6.0 anahtarını kullan.

v0.6.0 sertifikasının doğrulanmış SHA-256 parmak izi

```text
d8b269bf8b060ae6fd905c981914b8076f28f54cfcfc2aa87c77fcbc6621a6b0
```

1. Aday anahtar dosyasını `keytool -list -v -keystore /guvenli/yol/anahtar.jks` ile kontrol et. Anahtar ve parola sende kalsın.
2. Android Studio’da **Build → Generate Signed Bundle / APK → APK** seç. Eşleşen mevcut anahtarı kullanarak **release** derlemesini imzala.
3. İmzayı `apksigner verify --print-certs WordQuest_AI_v0.7.5.apk` ile kontrol et. Eski APK ile aynı sertifika parmak izini görmelisin.
4. Ayrı bir test cihazında eski APK’yı kur ve bir bulmaca kaydet. `adb install -r WordQuest_AI_v0.7.5.apk` ile güncelle. **Devam Et** ve eski puanları kontrol et.

Özgün özel anahtar mevcut arşivlerde bulunamadı. Android güncelleme için imzayı denetler. Yeni bir anahtar kullanacaksan önce bir yedeğin geri yüklenmesini doğrula. Sonraki dağıtımları aynı yeni anahtarla imzala. Özel anahtarları kaynak ZIP’ine veya Git’e ekleme.

[APK güncelleme koşulları](https://developer.android.com/google/play/app-updates) · [APK imzalama](https://developer.android.com/studio/publish/app-signing) · [İmza doğrulama](https://developer.android.com/tools/apksigner)
