# WordQuest AI v0.7.5

## Yenilikler

- **Akıllı tekrar.** Dört yanıt seçeneği ile FSRS-6 varsayılan ağırlıklarını kullanır. Sonraki aralık cevap düğmesinde görünür. Bulmacadaki yanlış veya harf ipucu kısa tekrar adayı oluşturur. Önceki kelime geçmişi, planlanan tekrar tarihi ve yıldızlar korunur.
- **İngilizce telaffuz.** Klavyeyle tamamlanan doğru kelime otomatik olarak sıraya alınır. Bulmaca ekranında **Tekrar dinle**, kelime geçmişinde ve tekrar kartında **Telaffuzu dinle** bulunur. İngilizce çevrimdışı ses kuruluysa ona öncelik verilir. Varsayılan Amerikan İngilizcesi seçilebilir ve Britanya İngilizcesi de sunulur. Ses motoruna göre telaffuz ve aksan değişebilir.
- **Yazı boyutu.** Ana sayfadaki dişliden **Ayarlar → Yazı boyutu** açılır. Standart, Büyük ve Çok büyük seçenekleri Android yazı boyutuyla birlikte uygulanır. Metin ölçeği menüleri, ipuçlarını, alıştırmaları ve sohbeti etkiler. Tahta harfleri hücreye sığacak ölçüde büyür. Her oyuncunun yazı ve telaffuz ayarı kendi profilinde saklanır.

## Kurulum

**v0.7.0 WordQuest AI Test kuruluysa** v0.7.5 Test APK’sını açıp **Güncelle**’ye dokun. Paket adı ve imza aynıdır. Sürüm kodu 11’den 12’ye çıkar.

**Yalnızca v0.6.0 kuruluysa** v0.7.5 Test yanına kurulur. İlerlemeni taşımak için v0.6.0’da **Bulmacalarım → Yedekle ve aktar → Yedek dosyası oluştur** yolunu kullan. Test sürümünde aynı menüde **Kayıt dosyasını içe aktar**’ı seç. v0.6.0’ın doğrudan güncellemesi için onun özgün imzalama anahtarı gerekir.

## Doğrulama

- Android 15 emülatöründe v0.7.5, v0.7.0 Test’in üzerine kuruldu. Örnek SQLite kelime geçmişi ve tekrar tarihi korundu.
- Emülatörde Ayarlar ekranı açıldı. Yazı boyutu değişikliği ve iki yerel oyuncunun farklı boyutları koruduğu doğrulandı.
- Altı Android çalışma zamanı testi ve Kotlin öğrenme kuralları testi geçti. Çevrimdışı ses seçimi ve kelime kuyruğu test ses motoruyla doğrulandı.
- Android derlemesi ve Lint kontrolü geçti. Fiziksel tablette sesin duyulması ve canlı iki cihazlı yarışma ayrıca denenmedi.

[FSRS-6 modeli ve ağırlıkları](https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm#fsrs-6) · [Android metin okuma](https://developer.android.com/reference/android/speech/tts/TextToSpeech)
