# WordQuest AI v0.8.0 — Çevrimdışı Sesli Sürüm / Bağımsız APK

## Neler değişti?

- Bulmaca, sözlük ve fiil çalışmalarında eSpeak'in en-us ve en-gb profilleriyle **önceden üretilmiş iki ayrı Ogg Vorbis ses kaydı** kullanılır. Cihaz TTS motoruna, Android metin okuma dil paketine veya internete ihtiyaç yoktur.
- 2.203 benzersiz kelime/ifade için toplam 4.406 yerel ses dosyası gömülüdür. 5 karşılaştırma kelimesi hazırdır.
- Kaynakta AI öğretmeni (pratik/dilbilgisi/kişisel ders/düzeltme), profil bazlı SQLite sohbet geçmişi, arama, öğrenci notları ve öğrenme geçmişiyle bağlam bütünlüğü bulunur. Gemini API anahtarı ve internet kullanılır; bunlar yerel ses sistemiyle karıştırılmamalıdır.
- Kişisel kullanıcı girdileriyle eklenen veya çevrimdışı pakette bulunmayan kelimelerde diğer aksana geçiş yapılmaz; açıklayıcı uyarı verilir.
- Serbest biçimli uzun AI yanıtları paketin kelimeleriyle tamamen karşılanmıyorsa, yalnız doğrulanmış aksan modeline sahip cihaz TTS motoruyla okunabilir. Uygun ses yoksa yanıt yazılı kalır. **Bütün AI yanıtları için cihazdan bağımsız ses desteği hazır değildir.**
- Kayıtlar eSpeak ile sentezlenmiş robotik seslerdir, profesyonel insan ses kaydı değildir.

## Android Studio ile APK üretme

JDK 17, Android SDK 35, Build Tools 35.0.0 ve internet bağlantısı gereklidir.

1. Bu ZIP içindeki proje kökünü Android Studio'da aç.
2. Gradle eşitlemesinin bitmesini bekle; eksik SDK paketlerini kur.
3. Terminalde Linux/Mac: `./gradlew :app:testOfflineUnitTest :app:assembleOffline`; Windows: `gradlew.bat :app:testOfflineUnitTest :app:assembleOffline`.
4. Çıktı `app/build/outputs/apk/offline/app-offline.apk` dosyasıdır.
5. Eski uygulamaları kaldırmana gerek yoktur: yeni paket kimliği **`com.kelimebulmacasi.a1.practice.offline`**. Bu APK eski kurulumların kullanıcı verilerini otomatik olarak taşımaz.

## GitHub Actions ile Android Studio olmadan APK

Projeyi GitHub deposunun kök dizinine yükle (dosyaları tek başına ZIP olarak değil, klasör yapısıyla).
Actions > `WordQuest v0.8.0 offline APK` > `Run workflow` komutunu çalıştır.
Build ve birim testleri başarılıysa `WordQuestAI-v0.8.0-Offline-APK` adlı çıktıdan APK indir.

GitHub Actions testleri yalnızca bulutta derleme ve Robolectric testlerini sağlar; gerçek Android cihazı ve ses işitsel kalite testi ayrıca gereklidir.

## Doğrulama

- `python tools/check_offline_audio.py`: 4.406 kaydın sayısını, varlığını ve 5 örneğin farklı kayıtlar olduğunu denetler.
- `python tools/static_upgrade_audit.py /path/to/original-source /path/to/current-project`: Eski bulmaca/kelime verilerinin değişmediğini denetler.
- Ayarlar > Amerikan/Britanya karşılaştırması: Water, Tomato, Better, Can't, Schedule.

## Lisans / kayıt kaynağı

Sesler bu kaynak arşivi hazırlanırken Debian eSpeak 1.48.15 `en-us` / `en-gb` profilleriyle üretilmiştir; kod ve ses üretim aracının lisansını dağıtıma dahil etmeden önce gözden geçir. eSpeak programı GPL-3.0 veya üstü lisans kapsamındadır. Üretilen sesler insan ses kaydı veya ticari, stüdyo sesleri değildir.

## Yapılmayan testler

Bu ortamda Android Gradle dağıtımı indirilemediği için derleme tamamlanmadı. APK üretilemedi. Telefon kurulum testi, çevrimdışı cihazda US/UK ses dinleme testi ve AI Gemini API canlı testi yapılmadı. Başarılı kabul edilmemelidir.
