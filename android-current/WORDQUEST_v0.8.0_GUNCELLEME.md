# WordQuest AI 0.8.0 — v0.7.5 üzerine veri kaybetmeden güncelleme

## Önce kimlik ve imzayı doğrula

Gönderilen **WordQuest_AI_v0.7.5_Test.apk** dosyasından doğrudan okunan değerler:

| Özellik | Değer |
| --- | --- |
| Paket kimliği | `com.kelimebulmacasi.a1.practice.test` |
| `versionCode` | `12` |
| `versionName` | `0.7.5` |
| İmzalama | APK Signature Scheme v2, Android Debug sertifikası |
| Sertifika SHA-256 | `88E25B3AD209D33F7052C29A939588C6C8E898D6398468C2A2E0BB78878BD19C` |
| Hedef APK varyantı | `preview`, yani `...practice.test` |
| Hedef kod / ad | `13` / `0.8.0` |

Android yeni APK'yı ancak **aynı paket kimliği**, **uyumlu imza sertifikası** ve **daha büyük sürüm koduyla** mevcut uygulamanın üzerine kurar. Eski `.test` paketinin üzerine normal `debug` veya `release` APK'sı kurulamaz; paket kimlikleri ayrıdır.

**İmza özel anahtarı APK'dan üretilemez.** Debug keystore dosyasının **APK'yı imzalayan aynı dosya** olması gerekir. Farklı bilgisayarda otomatik oluşturulan `debug.keystore` çoğunlukla farklı sertifikadır; kullanılması `INSTALL_FAILED_UPDATE_INCOMPATIBLE` ile sonuçlanır. Eski uygulamayı kesinlikle kaldırmayın.

## Derleme ortamı

- Android Studio ile Android SDK API 35 ve Build Tools 35.0.0
- JDK 17, Gradle wrapper 8.9 (kaynakta mevcut)
- İnternet bağlantısı: ilk kurulumda Gradle ve Android kütüphaneleri için
- Eski APK'nın *orijinal* imzalama keystore'u, alias'ı ve parolaları
- Gerçek test cihazı / Android Emulator, US ve UK TTS sesleri

### Linux/macOS terminali

```bash
export WQ_UPGRADE_KEYSTORE='/path/to/original-debug.keystore'
export WQ_UPGRADE_STORE_PASSWORD='...'
export WQ_UPGRADE_KEY_ALIAS='...'
export WQ_UPGRADE_KEY_PASSWORD='...'
# Bu ortam değişkenleri yalnızca sizin bilgisayarınızda ayarlanmalıdır.

bash tools/build_075_upgrade.sh /path/to/WordQuest_AI_v0.7.5_Test.apk
```

Bu komut önce `testPreviewUnitTest` ve `assemblePreview` yapar. Daha sonra `tools/verify_apk_upgrade.py` yeni APK'nın kimliğini, sürümünü ve **SDK apksigner ile gerçek imzasını** kontrol eder. Başarısızsa yayın için durur. Parolaları dosyaya veya sohbetlere koymayın; terminal geçmişi konusunda da dikkatli olun.

### Windows (PowerShell)

PowerShell'de eşdeğer dört `WQ_UPGRADE_*` ortam değişkenini ayarlayın, ardından proje kökünde:

```powershell
.\gradlew.bat :app:testPreviewUnitTest :app:assemblePreview
python .\tools\verify_apk_upgrade.py C:\path\WordQuest_AI_v0.7.5_Test.apk .\app\build\outputs\apk\preview\app-preview.apk
```

Windows üzerinde `apksigner`'ın SDK Build Tools dizininden PATH'e erişilebilir olması veya `ANDROID_HOME` ortam değişkeninin doğru ayarlanması gerekir. PowerShell ortam değişkenleri tanımlanmamışsa `preview` Android debug imzasına döner; **imza doğrulaması geçmedikçe güncelleme sanılmamalıdır**.

### Cihaza güncelleme

APK doğrulaması geçerse ve telefonda eski uygulama/veri yedeği varsa:

```bash
adb install -r app/build/outputs/apk/preview/app-preview.apk
```

`-r` kaldırmadan mevcut paketi değiştirir. **Kesinlikle `adb uninstall` kullanmayın.** Gerçek güncelleme testinde her iki yerel profilin, bulmacaların, öğrenme defterinin, tercihlerin ve tüm puanların eski sürümle tutarlı kaldığını gözlemleyin. `INSTALL_FAILED_UPDATE_INCOMPATIBLE` olursa cihazdan eski sürümü silip yeniden yüklemek doğru çözüm değildir; doğru eski imza anahtarını bulun.

## Telaffuz kabul testi

1. Ayarlar → Amerikan / Britanya karşılaştırması. `Water`, `Tomato`, `Better`, `Can't`, `Schedule` için iki sesi ayrı ayrı dinleyin.
2. `en-US` ve `en-GB` için hem `Voice.name` hem de `Voice.locale` doğru gösterilmeli. `setVoice()` başarılı olmalı, sonra `getVoice()` ile doğrulanmalı. İki sesin duyulabilir olarak ayrılıp ayrılmadığını ayrıca kontrol edin.
3. Android metin okuma paketlerinden Britanya sesini kaldırınca UK düğmesi **US'ye sessizce geçmemeli**; uyarı göstermeli.
4. Çevrimdışı/çevrimiçi ses, değişen konuşma hızı ve ses seviyesi, otomatik seslendirme anahtarı, bulmaca tamamlanınca tam kelimeyi bir kez okuma, solved kelimeye tekrar dokunma ve kelime defterinden tekrar dinleme test edilmeli.
5. İki oyuncu birbirini görmemeli: ayrı sohbet, not, öğrenme hedefi, API anahtarı ve kayıtlarıyla deneme yapılmalı. Uygulama tamamen kapatılıp açıldığında geçmiş araması da çalışmalı.

**Cihaz testi yapılmadığı için US/UK seslerinin gerçekten farklı duyulduğu veya Android'in yerinde güncellemeyi kabul ettiği iddia edilmez.** `getDefaultEngine()` yalnızca *varsayılan* TTS motorunu belirtir, etkin motorun kimliğini kesin olarak söylemez; ayrıca motor iç ses seçimi uygulamadan bütünüyle ölçülemez.

## Yeniliklerin kapsamı ve sınırlar

- Öğretmen mevcut Gemini API anahtarını kullanır; internet ve geçerli anahtar gereklidir. Harici hizmete mesaj, son sekiz tur, öğrenme hedef notları ve mevcut oyuncunun kelime hatası özeti gönderilir (sohbet ekranında kullanıcı bilgilendirilir).
- Eski sohbetler v0.7.5'te RAM'de tutulduğu için önceki kapatılmış oturumlar kurtarılamaz. v0.8.0'dan itibaren **yeni sohbetler** profil bazında SQLite dosyasına yazılır, aranabilir ve devam ettirilebilir. Bu yeni öğretmen geçmişi **cihazlar arası senkronize değildir**.
- Eski kullanıcı ve kelime veritabanları *değiştirilmez* ve otomatik temizlenmez; yeni öğretmen veritabanları ayrı tutulur.
- AI yanıtlarının eğitim kalitesi, API hizmeti ve model davranışına bağlıdır; otomatik ölçülmüş gramer notu ve gerçek mikrofondan telaffuz puanı olduğunu iddia etmez.
