# WordQuest AI v1.0.2 — Firebase yarışma merkezi ve Google OAuth

- **Ana ekran:** AI Yarışma Merkezi artık Firebase'i varsayılan bağlantı olarak açar.
- **Altı tür:** hızlı kelime, bulmaca düellosu (kelime ipuçları), ortak bulmaca (paylaşılan kelime ipuçları), YDS düellosu, sözlü ses tanıma alıştırması, günlük meydan okuma.
- **Alternatif:** "Sunucuya bağlan" ekranındaki eski Supabase/Cloudflare akışı korunur, Firebase'e dönüş düğmesi bulunur.
- **Oyuncu 2:** Yerel profil geçiş düğmesi gizlenmiştir. Profil-2 verisi silinmez. İkinci kişi yarışmaya kendi cihazından kodla katılır.
- **Kota önlemi:** Her cihazda UTC başına 10 yeni oda; her oyuncu en çok 12 soruya bir kez yanıt verebilir. Her oda sadece açıkken dinlenir. Bu **güvenli sunucu tarafı kota zorlaması değildir**. Spark veritabanı kota limitleri Google tarafından uygulanır.
- **Oda erişimi:** Firebase kuralları sadece odayı açan veya katılan kullanıcı için okuma izni verir. Yeni 6 tür, mevcut yayımlanmış kurallardaki `mode` değerleriyle uyum için alt türü ayrı `gameType` alanında tutar.
- **Puanlama:** Cevaplar istemci tarafında hesaplanır. Hileye karşı güvenilir sunucu tarafı hakem mevcut değildir. Canlı AI hakemi için ayrıca sağlayıcı gerekir.
- **Google OAuth:** Kaynakta Credential Manager ve Firebase Auth entegrasyonu bulunur. Bu Firebase Android yapılandırmasında `cloud_web_client_id` alanı **boş**. Gerçek Google OAuth'un çalışması için Firebase Authentication'da Google sağlayıcısını açıp uygulamanın SHA sertifikalarını tanımlayın; ardından Firebase Web OAuth client ID'sini uygulama içindeki *Google OAuth ayarları* alanına girin. Bu web istemci kimliği gizli anahtar değildir. OAuth tanımlanmamış olsa da anonim oturumla yarışmalar çalışabilir.
- **Android uygulama kimliği:** `com.kelimebulmacasi.a1.practice.offline101` (önceki bağımsız Firebase APK ile aynı). Sürüm kodu yükseltilmiştir. Önceki APK'nın imza sertifikasıyla aynı özel anahtarla imzalanırsa uyumlu güncelleme yapılabilir.
- **Veri koruma:** Önceki yerel kayıtlar ve mevcut bulmacalar kaynakta korunur; ikinci oyuncu verisi silinmedi. Android cihazda dağıtım/yeniden giriş testleri ayrıca gereklidir.

**Yapılmayanlar:** Bulut veritabanında globale güvenli liderlik tablosu, iki gerçek telefonda test, gerçek AI hakemliği, orijinal kare bulmaca algoritmasının ortak Firebase gerçek zamanlı koordinasyonu, güvenilir kota/anti-hile sunucu işleyicisi. Bunların hepsinin çalıştığını iddia etmeyin.
