# WordQuest AI — Firebase Realtime Database (Spark ücretsiz)

Bu sürümün Firebase Android paket kimliği `com.kelimebulmacasi.a1.practice.offline101`.
Verilen `google-services.json` içindeki proje bilgileri uygulamaya yerel olarak bağlanmıştır. **Google ile giriş henüz yapılandırılmış değildir**: dosyada OAuth web istemcisi yok. Yarışma için `Authentication → Sign-in method → Anonymous` etkin olmalıdır.

Veritabanı URL'si ekran görüntüsünden doğrulanarak **uygulamaya önceden eklendi**: `https://wordquest-ai-default-rtdb.firebaseio.com`. Uygulamada URL'yi tekrar yazmana gerek yok.

`Realtime Database → Rules` bölümüne bu paketle gelen `database.rules.json` içindeki **rules nesnesini** bütünüyle yapıştırıp Publish'e bas. Locked mode'dan güvenli üye bazlı erişime geçiş için bu işlem gereklidir; kullanıcıya açık Test mode kullanma.

İki ayrı cihazda uygulamayı aç. İlk cihaz **Firebase Canlı Yarışma → Yeni oda oluştur** seçerek kelime, gramer veya karma oyun belirler; 9 karakterli oda kodunu ikinci cihaza iletir. İkinci cihaz **Odaya katıl** ile aynı odaya girer. Oda sahibi başlatır. Her soru için kullanıcı başına tek cevap kabul edilir ve skor iki cihazda ortak verilerden hesaplanır. Rövanş yeni oda koduyla açılır.

**Sınırlar:** Bu deneme yarışması canlı veri paylaşımını test eder; güvenilir sunucu tarafı puanlama, hile önleme, AI hakemi, otomatik eşleştirme veya kalıcı hesaplar henüz eklenmedi. Firebase bağlantısının gerçek iki cihazda çalışması, veritabanı güvenlik kurallarını yayımladıktan sonra doğrulanmalıdır. Kurallar üye olmayanların oda detaylarını okuyamamasını, iki oyuncudan fazlasının katılamamasını ve bir cevap kaydının ikinci kez yazılamamasını amaçlar; üretim öncesinde Firebase Emulator Suite ile kuralların da sınanması gerekir.

Bu projede **özel servis hesabı anahtarı** veya ücretli Firebase hizmeti yoktur. Google'ın Firebase istemci API anahtarı, Android uygulamasına derlenmesi gereken istemci yapılandırmasıdır; gerçek erişim kontrolü Authentication ve Database Rules tarafından sağlanır.
