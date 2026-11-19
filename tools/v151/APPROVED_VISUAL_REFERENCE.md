# Arena — ONAYLANMIŞ TASARIM REFERANSI (V151)

> Bu belge, kullanıcının beğendiği iki 941×1672 px tasarım görseline dayalıdır. Gerçek Android ekranı bir tasarım görüntüsü olarak değil, erişilebilir ve çalışan Compose bileşenleriyle oluşturulmalıdır. Yakın benzerlik yalnızca Gradle derlemesiyle kanıtlanamaz; cihaz ekran görüntüsü kabulü gerekir.

## 1 — Arena giriş ekranı (referans: wordquest_ai_arena_maç_ekranı.png)

- Kırık beyaz, açık krem/lila arka plan; zümrüt yeşili-koyu mor yazı.
- Üstte WordQuest AI + Arena marka başlığı; sağ üstte küçük profil/avatar.
- Profilin altında Bip/Pufi/Mimo maskot görünümü. Kullanıcı seçimi profil diyaloğunda saklanır; Arena içinde üstten seçim kartı yapılmaz.
- Kupanın eşlik ettiği maskotlu geniş **İNGİLİZCE ARENASI / Her maç yeni bir keşif** kahraman kartı. Sadece metin ve düz kare bırakılmaz.
- Oyun Türü: yan yana Bulmaca ve 5 Şıklı; yeşil seçili durum, mor vektör simgeler, kısa açıklama. Bulmaca içinde Düello ve Ortak seçimi korunur.
- İngilizce Seviyesi: yedi seviyeyi tek yatay şeritte göster, dar ekranda yana kaydır.
- Konu / Paket: 4 konu/ikon şeridi, seçili yeşil, seçilmemiş açık lila, yatay kaydır.
- Oyun Biçimi: Arkadaşınla Oyna ve Tek Başına Oyna aynı iki kart.
- Maç Ayarları: Süre, Bulmaca Boyutu, İpucu aynı yatay panelde, gerçek seçim menüleri.
- En altta tam genişlik ve koyu yeşil geçişli **Odayı Oluştur** düğmesi. Tekli oyun seçildiğinde aynı alan **Oyuna Başla** olur.
- Ayarlar, davet, avatar ve oyun motoruyla bağlantı korunur; ana sayfaya, öğretmene, seslendirmeye, Kelime Hafızam'a dokunulmaz.

## 2 — Oda lobisi (referans: wordquest_ai_arena_oda_lobisi.png)

- Ortalanmış mor **WordQuest / AI Arena** marka bloğu, altında büyük **Oda Lobisi** başlığı ve kısa açıklama.
- Gölgeli lila oda kodu şeridi, büyük kod, mor **Kopyala** düğmesi.
- Yarışma Ayarları başlığı; dört açık şeftali/lila küçük bilgi kartı (mod, seviye, süre, kelime sayısı). Oda sahibi arkadaş katıldıktan sonra da Düzenle'ye basabilmeli.
- Karşılıklı eşit ve büyük iki oyuncu kartı. Solda lavanta, sağda somon/krem; her kartta büyük maskot, **gerçek kullanıcı adı**, rol ve tam genişlikte açık yeşil Hazır düğmesi.
- İki taraf hazırsa yeşil onay bandı, altında büyük mor degrade **Yarışmayı Başlat**.
- En altta beyaz/lila çerçeveli **Arkadaşlarını Davet Et** arama paneli.
- Ayar değişince eski hazır onayları geçersiz; iki kullanıcı tekrar hazır olmadan maç başlamaz.
- Firebase kuralları gerçek ortamda uygulanmadıysa hazır veya başlat yazma hatası kullanıcıya gösterilmelidir.

## 3 — Görsel kabul

- 360, 400 ve 450 dp genişlikte yatay taşma olmamalı; yatay seviyeler/konular kontrollü kaymalı.
- Bütün düğmeler gerçek, oda ayarları Firebase bağlantılı, oyuncu adları profil verisinden geliyor olmalı.
- Başka ekranın tema değerleri / uygulama kimliği / imzası değişmemeli.
- V151 kaynakları: ArenaApprovedHome.kt, ArenaApprovedLobby.kt, ArenaApprovedTheme.kt.
- Kurulu 1.0.50 üzerine yalnızca 1.0.51 **önizleme** APK'sı kurulabilir (versionCode 76). Kullanıcı görsel kabulü ve iki cihaz testi olmadan normal kanala yayımlanmaz.
