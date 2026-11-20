# WordQuest AI 1.0.73 — Kişisel 3D Avatar Testi

Bu dal, kullanıcının gönderdiği gerçek 10 GLB modeliyle **özel, cihaz içi test** için hazırlanmıştır. Genel kullanıma açılmış kararlı sürüm değildir.

## 1. Kurulum

1. [WordQuest AI 1.0.73 Personal QA3 (test build 100)](https://github.com/ineklemihribanse-design/WordQuestAI/releases/tag/v1.0.73-personal-qa3) sayfasındaki `WordQuestAI-Personal-QA.apk` dosyasını kur.
2. Bu sürüm, mevcut 1.0.72'den **ayrı bir uygulama kimliğine** sahiptir. Ana uygulama ve kullanıcı verileri değişmez.
3. Bu konuşmadan sağlanan `WordQuestAI_1.0.73_10_Avatar_Ozel_Paket_Gorselli.zip` dosyasını indir ve telefonda sakla. ZIP'i **açma**.
4. Test uygulamasında **Arena** veya **Kelime Hafızam** → Avatar Seç → **3D Avatar Paketini Telefonumdan Ekle** düğmesine bas.
5. İndirilen ZIP'i seç. SHA-256 doğrulamasından sonra “10/10 model” görüntülenmelidir.
6. Bir avatar seç. Bu seçim Arena ve Kelime Hafızam'da ortak olarak kaydedilir.
7. Bulmaca düellosu, ortak bulmaca, 5 şıklı düello, seviye/konu seçimi, geri sayım, Pas Geç, Cevabı Gönder, başarı ve kupa animasyonunu dene.

## Güvenlik ve sınırlar

- Satın alınmış model dosyaları uygulamanın **özel yerel dosya alanına** alınır; normal GitHub release veya CDN'ye yüklenmez.
- Tek paket: 10 rigli QA GLB + 10 gerçek modelden hazırlanmış önizleme atlası. Kaynaklar kullanıcı tarafından sağlanmıştır.
- Tek paket ZIP SHA-256: `7098827636309a9f71dd67d69e7e23adb12fd9258702e1cdd9c1053cb8ba2f13` **(görselli özel paket)**.
- Cihazınızda çalışmayan Filament/GLB yüklemesinde karakterin statik gerçek kaynak önizlemesine geçiş sağlanır. Bazı native sürücü hataları uygulama içinden yakalanamayabilir.
- Rigleme, model animasyonları ve doğal hareket estetiği **gerçek telefonda ayrıca test edilmelidir**; Kotlin/CI derlemesi bu testi yerine getirmez.
- 1.0.72 kararlı sürümü, güncelleme manifesti, Lena dosyaları ve kullanıcı geçmişi değiştirilmemiştir.

## 1.0.73 kararlı yayın koşulu

Kararlı APK orijinal `.fresh` uygulama kimliği, aynı imza ve `versionCode=98` ile üretilecek; ancak önce gerçek cihazda 10 avatar için açılış, animasyon, bellek/çökme, çevrimiçi ve çevrimdışı oyun kontrolleri onaylanacak. Bu adım olmadan üretim kanalına yeni `update.json` gönderilmeyecek.
