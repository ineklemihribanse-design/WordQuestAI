# WordQuest AI v0.8.2 — Yumuşak ses ve okunabilirlik

Bu proje Android Studio ile açılır: `settings.gradle.kts`, Kotlin + Jetpack Compose, Android SDK 35, Gradle Wrapper.

## Bu sürümdeki değişiklikler

- Ayarlar > Yazı boyutu: 75%–140% arası kaydırıcı (5 puanlık adımlarla), ayrıca eski Standart/Büyük/Çok büyük kısayolları.
- Doğru kelime yazıldığında 0,34 saniyelik, düşük seviyeli özgün iki notalı WAV onay sesi. İpucu/kaydedilmiş ilerleme geri yükleme bunu tetiklemez.
- Ayarlar > Telaffuz sesini aç/kapat ve Doğru kelimede nazik onay sesi: bağımsız anahtarlar. Efekt için ayrıca ses seviyesi ve dinleme düğmesi.
- İki yerel profilin ayarları birbirinden ayrılmıştır. Eski yazı boyutu ayarları ilk açılışta korunur.
- Bulmaca ve yazılı fiil alıştırmalarında doğru giriş için onay sesi.
- Amerikan ve Britanya cihaz sesi seçimi, eski 4.406 OGG ses dosyası, IPA gösterimleri, AI öğretmeni ve eski bulmacalar kod olarak değiştirilmemiştir.

## İmza ve veri konusunda kritik ayrım

v0.8.1 UK ve önceki v0.8.1 APK'ları her derlemede farklı debug sertifikasıyla imzalanmış. Bu sertifikalara ait özel anahtarlar mevcut değil. Bu nedenle bu derlemenin eski sürümün üstüne doğrudan kurulabileceği iddia edilemez. APK'nın `offline082` varyantı ayrı uygulama kimliği kullanır; eski uygulama ve verisi silinmemelidir. Mevcut yedekleme/aktarma ekranı verilerin kontrollü taşınması için kullanılabilir. Yeni sürümden itibaren aynı kalıcı özel imza anahtarı korunursa ileriki APK'lar bunun üstüne güncelleme olarak kurulabilir.

## Doğrulama

- `python tools/verify_audio_ipa.py` → 4.406 OGG, 2.184 IPA çifti, 19 belirsiz eşleşme.
- `python tools/make_soft_sound.py` → özgün, hafif onay WAV; yeniden üretilebilir.
- Android derleme/test ve cihaz testleri ayrıca yapılmalı, bunlar yapılmadan tamamlandı denmemeli.

## Güvenlik

Özel imzalama anahtarlarını, şifrelerini ve AI API anahtarlarını kaynak koduna, GitHub deposuna veya APK varlıklarına koymayın. Kalıcı anahtarın güvenli yedeği gerektiğinde ayrıca saklanmalıdır.
