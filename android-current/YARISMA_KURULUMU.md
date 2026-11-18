# WordQuest AI 0.6.0
## İki kişilik yarışma kurulumu

Mevcut Android uygulamasının paket kimliği `com.kelimebulmacasi.a1.practice`. APK, dağıtılan 0.4.4 ile aynı sertifikayı kullanır. **Güncelle** seçeneğiyle yükle. Ana sayfada WordQuest AI, Android uygulama listesinde Kelime Bulmacası 0.4 adını görürsün.

Yarışma için bir Supabase projesi, iki kişiye ait gerçek oturum ve AI özellikleri için bir Cloudflare Worker gerekir. Uygulama sunucu adreslerini **AI Yarışma Merkezi → Sunucu Ayarları** ekranından alır. AI tercihi başlangıçta kapalıdır. Yerel bulmacaları, kişisel paketleri ve yerel yedeklemeyi misafir olarak kullanabilirsin. Mevcut Firebase hesap ayarları ayrı kalır.

### 1. Supabase Free

1. Supabase'te ücretsiz bir proje aç. **Authentication → Users → Add user** ile iki ayrı e-posta ve şifre tanımla. Hesapları doğrulanmış olarak oluştur veya e-posta doğrulamasını tamamla. İki UUID'yi **Users** ekranından kopyala. Proje bu uygulamaya ayrılmışsa dışarıdan kayıt açmayı **Authentication** ayarlarında kapat.
2. **SQL Editor** üzerinden `arena/db/01_arena.sql` ve `arena/db/02_ai.sql` dosyalarını sırayla çalıştır. Ardından `arena/db/03_seed.sql` dosyasını yeni bir sorguda çalıştır. Soru dosyası tekrar çalıştırılabilir. Açılmış odalar kendi soru kopyalarını kullanmaya devam eder.
3. Aşağıdaki iki satırdaki UUID ve adları kendi iki hesabınla değiştir. Gerekirse ad içinde geçen tek tırnağı iki tek tırnakla yaz. A ve B, Cloudflare'deki anahtarların iki hesaba bağlanmasını sağlar.

   ```sql
   insert into arena_private.players(user_id, name, secret_slot) values
     ('BIRINCI_KULLANICI_UUID', 'Birinci oyuncunun adı', 'A'),
     ('IKINCI_KULLANICI_UUID', 'İkinci oyuncunun adı', 'B');
   ```

4. **Project URL**, **Publishable key** ve Worker'a konacak **Secret key** değerlerini projenin **Connect** veya **Settings → API Keys** ekranından al. Eski projelerde `anon` ve `service_role` türleri de desteklenir. `arena_events` tablosu `supabase_realtime` yayınına şema dosyasıyla eklenir. Üyelik kuralı, olay tablosunu yalnızca odanın iki üyesine açar.

[Supabase kullanıcıları](https://supabase.com/docs/guides/auth/users) · [API anahtarları](https://supabase.com/docs/guides/getting-started/api-keys) · [Realtime](https://supabase.com/docs/guides/realtime/protocol)

### 2. Cloudflare Workers Free

**Bilgisayardan** Node.js 22 veya üzeriyle proje ZIP'indeki `arena` klasöründe şu komutları çalıştır. Oturum sırasında kendi Cloudflare hesabını seç.

```sh
npx wrangler@4 login
npx wrangler@4 deploy
npx wrangler@4 secret put SUPABASE_URL
npx wrangler@4 secret put SUPABASE_PUBLIC_KEY
npx wrangler@4 secret put SUPABASE_SERVICE_ROLE
```

`SUPABASE_SERVICE_ROLE` adı altında sunucuya özel Supabase `sb_secret_…` veya eski `service_role` değerini gir. `SUPABASE_PUBLIC_KEY` için uygulamaya da girilecek yayımlanabilir anahtarı kullan. Komutlar değeri ayrı bir giriş alanında ister.

**Tarayıcıdan** Cloudflare **Workers & Pages** bölümünde bir Worker oluşturup `arena/src/worker.mjs` içeriğini düzenleyiciye yapıştır ve dağıt. **Settings → Variables and Secrets** bölümünden aynı üç değişkeni ekle. Workers AI seçeneği için **Bindings → Add binding → Workers AI** altında bağlama adını `AI` yap. Wrangler ile dağıtım bu bağlamayı `wrangler.toml` dosyasından alır. `workers.dev` adresine `/health` eklediğinde `{"app":"wordquest-arena","protocol":1}` görürsün.

Harici model kullanacaksan her oyuncu kendi sağlayıcı hesabının anahtarıyla çalışabilir. Ücretsiz planda etkin olan bir hesabın anahtarını şu isimlerle **Secrets** bölümüne ekle.

| Oyuncu | Gemini | Groq | OpenRouter |
| --- | --- | --- | --- |
| A | `A_GEMINI_KEY` | `A_GROQ_KEY` | `A_OPENROUTER_KEY` |
| B | `B_GEMINI_KEY` | `B_GROQ_KEY` | `B_OPENROUTER_KEY` |

Workers AI seçilirse iki oyuncu aynı Cloudflare Free kotasını kullanır. Uygulama çağrıları iki oyuncu için ayrı sayar.

[Cloudflare Worker kurulumu](https://developers.cloudflare.com/workers/get-started/dashboard/) · [Secrets](https://developers.cloudflare.com/workers/configuration/secrets/) · [Workers AI bağlaması](https://developers.cloudflare.com/workers-ai/get-started/workers-wrangler/)

Yarışma sunucusunu daha önce 0.6.0 için kurduysan mevcut kurulumda yalnızca `arena/db/02_ai.sql` dosyasını yeniden çalıştır ve güncel `arena/src/worker.mjs` ile Worker'ı tekrar dağıt. Giriş bilgileri, odalar ve sohbet kayıtları aynı veritabanında kalır. Bu sunucu güncellemesi mevcut 0.6.0 APK'sıyla çalışır.

### 3. İki cihazda ilk yarışma

1. APK'yı iki cihaza kur. Her ikisinde **AI Yarışma Merkezi → Sunucu Ayarları** bölümüne aynı Supabase proje adresi, yayımlanabilir anahtar ve Worker adresini gir. Supabase ve Cloudflare'in verdiği standart HTTPS adreslerini kullan.
2. Her cihazdaki oyuncu kendi yarışma hesabıyla giriş yapsın. Birinci oyuncu **Yarışma Odası Oluştur** ile mod, seviye, konu, soru sayısı, süre, eğitim modu ve hakemi seçsin.
3. İkinci oyuncu davet kodunu **Odaya Katıl** bölümüne yazsın. Davet metniyle paylaşılan `wordquestai://` bağlantısını, destekleyen Android uygulamalarından açabilir veya aynı alana yapıştırabilir.
4. İki cihazda **Hazırım**'a dokun. Dört saniyelik geri sayımdan sonra soru gelir. Kazanılan puanları ve sürenin iki cihazda eşleştiğini kontrol et. Uygulamayı kapatıp döndüğünde **Yarışma Geçmişi** üzerinden aynı odayı açabilirsin.
5. Özel öğretmen veya hakem için **AI Sağlayıcıları** ekranından sunucuda etkin bir model seç. Kesin cevaplı modlar için hakem seçimi isteğe bağlıdır. Sözlü sınavda Android'in sesini metne çevirme hizmeti veya klavye kullanılır. Hakem metni anlam, kelime seçimi ve dilbilgisi açısından değerlendirir. Sözlü sınavdan önce her oyuncu paylaşımı onaylar.

### Oyun ve AI verileri

- Yarışma durumu, cevaplar, sonuçlar ve ortak sohbet Supabase'te kalır. Android oturum bilgisi cihazın Keystore anahtarıyla şifrelenir. Okuma ve yazma işlemleri sunucu tarafından kimliği doğrulanmış hesaba bağlıdır.
- Kesin cevaplar PostgreSQL'de aynı kuralla puanlanır. Doğru 10, yanlış 0, ilk harfi açılan doğru cevap 5 puandır. Süreyi sunucu ölçer. Düelloda aynı puanla tahtayı tamamlayanlar arasında en az bir saniye fark varsa daha kısa süre kazandırır. Ortak bulmacada bir kelimeyi ilk doğru gönderen takımın puanına ekler ve doğrulanan harfler iki ekrana gelir.
- AI hakemi isteğe bağlı maç sonu yorumu ve maç sonu sohbet sorularına yanıt verir. Performans özetinde oyuncular koltuk numarasıyla anılır. Sözlü modda puan, doğrulanan ölçütlerin toplamıdır. Canlı AI kotası dolarsa kesin cevaplı yarışma sonuçlanır. Sözlü sınavı daha sonra değerlendirebilir veya puansız kapatabilirsin.
- Özel öğretmenin serbest sohbeti, son beş özel sohbet turunu ve tamamlanan yarışmalardaki en fazla sekiz tekrar kelimesini kullanır. Bitmiş bir odadan öğretmeni açıp AI veri paylaşımına izin verirsen model kendi yanlış cevaplarını, doğru çözümleri ve varsa sözlü değerlendirme ölçütlerini alır. Ortak hakem toplu yarışma istatistiklerini kullanır. Sözlü değerlendirme için iki oyuncunun hazır olurken verdiği onayla cevap metinlerini işler.
- Açık maçta öğretmene yalnızca seçtiğin genel çalışma başlığı gönderilir. Eğitim ipucunu sunucu verir ve kullanımını kaydeder.
- Sözlü sınav soruları seviyeye göre hazırlanmış havuzdan gelir. Telefonun konuşma tanıma hizmeti sesi metne çevirir. Cihaz ayarları ve yüklü dillere göre ses tanıma hizmeti kendi internet bağlantısını kullanabilir. Uygulamanın yarışma sunucusu ses dosyası almaz. Bulut üzerinden bir ses dakikası paketi gerekmez.
- **Kişisel Yarışma İstatistikleri**, yanlış cevapları hesaba özel tekrar listesine ekler. Yerel bulmaca ilerleme puanları ile yarışma puanları ayrı tutulur. Yerel JSON/XML yedeği cihazın bulmaca verisini taşır. Yarışma geçmişine ve özel öğretmen yazışmalarına diğer cihazdan aynı yarışma hesabıyla erişilir.

### Ücretsiz sınırlar

| Hizmet | 14 Kasım 2026 itibarıyla kontrol edilen sınır |
| --- | --- |
| Supabase Free | Proje başına 500 MB veritabanı. Realtime için ayda 2 milyon mesaj ve 200 eşzamanlı bağlantı sınırı. |
| Cloudflare Workers Free | Günde 100.000 istek ve çağrı başına 10 ms CPU. Modelin yanıtını bekleme süresi CPU süresinden ayrıdır. |
| Cloudflare Workers AI | Hesap için günde 10.000 nöron. Free planda sınır aşıldığında yeni çağrı hata verir. |
| Gemini 3.1 Flash-Lite | Free Tier metin çağrıları ücretsiz. Çağrı ve token limitleri projeye göre AI Studio'da görünür. |
| Groq GPT-OSS | Standart Free Plan Tablosunda GPT-OSS 20B ve 120B için 30 istek/dakika ve 1.000 istek/gün. Hesap ve token limitleri ayrıca uygulanır. |
| OpenRouter Free | Yalnızca ücretsiz modelleri yönlendiren `openrouter/free` kullanılır. Kredi satın alınmamış hesap için 20 istek/dakika ve 50 istek/gün sınırı açıklanmıştır. |

Yarışma sunucusu her oyuncu için UTC gün başına toplam 30 AI çağrısı ve sağlayıcı başına 15 çağrı ayırır. Otomatik geçişi açtığında uygulama bir istekte en fazla bir alternatif sağlayıcı dener. Gerçek ücretlendirme sağlayıcıdaki planına bağlıdır. Gemini ve Groq anahtarlarını ücretsiz projelerden kullan ve Cloudflare'ı Free'de tut. Ayrıca Google'ın fiyatlandırma tablosuna göre Gemini Free Tier'da gönderilen içerik ürün geliştirmede kullanılabilir.

[Supabase fiyatları](https://supabase.com/pricing) · [Realtime sınırları](https://supabase.com/docs/guides/realtime/pricing) · [Workers](https://developers.cloudflare.com/workers/platform/pricing/) · [Workers AI](https://developers.cloudflare.com/workers-ai/platform/pricing/) · [Gemini fiyatları](https://ai.google.dev/gemini-api/docs/pricing) · [Gemini kotaları](https://ai.google.dev/gemini-api/docs/rate-limits) · [Groq kotaları](https://console.groq.com/docs/rate-limits) · [OpenRouter Free](https://openrouter.ai/collections/free-models/) · [Android ses tanıma](https://developer.android.com/reference/android/speech/RecognizerIntent#EXTRA_PREFER_OFFLINE)

### Kaynaklar ve doğrulama

| Dosya | Görev |
| --- | --- |
| `MainActivity.kt`, `HomeScreen.kt`, `AndroidManifest.xml` | Ana menü, davet bağlantısı ve Android yaşam döngüsü. |
| `ArenaApi.kt`, `ArenaSession.kt` | Auth oturumu, WebSocket ve yedek durum sorgusu, AI istekleri. |
| `ArenaScreen.kt`, `ArenaRoomScreen.kt` | Oda ve mod seçimi, yarışma tahtası, puan, geçmiş, öğretmen ve ortak sohbet. |
| `arena/db/*.sql`, `arena/seed.py`, `arena/content.json` | Oda yetkisi, soru havuzları, cevap kontrolü, puan ve geçmiş. `02_ai.sql` özel öğretmene biten maçta yalnızca kendi hesabının yanlış cevaplarını verir. |
| `arena/src/worker.mjs`, `arena/wrangler.toml` | Ayrı AI rolleri, sağlayıcı seçimi, kota ve maç sonu kişisel değerlendirme. |
| `arena/test/arena.test.mjs` | İki kullanıcı, yeniden katılım, tekil puan, AI rolleri ve kota testleri. |
| `VocabularyBank.kt`, `vocabulary-extra.tsv`, `Catalog.kt`, `AppSession.kt`, `NewGameScreen.kt` | A1–C2 ve YDS yeni oyun havuzu, konu ve tür seçimi. |
| `CustomLayout.kt`, `PuzzleScreen.kt`, `PlayerStore.kt`, `PlayerBackup.kt`, `Accounts.kt` | Büyük tahtalar, otomatik kayıt ve mevcut cihaz kayıtlarının uyumluluğu. |
| `app/build.gradle.kts` | 0.6.0 ve WebSocket bağımlılığı. |

Mevcut iki hazır bulmaca verisi korunur. Sürüm 657 hazır tahta, eski deneme, 175 fiil, OCR ile bulmaca oluşturma, isteğe bağlı Google hesabı, Gemini konuşma asistanı ve yerel ilerleme/yedekleme üzerinde kuruludur. Klasik kelime laboratuvarı ve ayrı bir çevrimdışı aralıklı tekrar algoritması bu sürümde yoktur. Yarışmanın tekrar listesi ve özel öğretmen geçmişi ayrı işlevlerdir.

0.6.0 APK'sının imzası ve paket adı doğrulandı. Android 15 emülatöründe aynı imzalı APK yeniden kurulduktan sonra test kaydı çevrimdışı olarak **Devam Et** ile açıldı. **Yeni Oyun**, **AI Yarışma Merkezi** ve davet bağlantısının sunucu ayarlarına yönlendirmesi emülatörde görüldü. Kaynak derleme, Android Lint ve Worker dağıtım öncesi kontrolü geçti. Kotlin'de 657 hazır tahta, 36 yeni üretim örneği, doğru cevabın kilitlenmesi, bulmaca karıştırma ve eski XML ile JSON kayıtlarını açma doğrulandı. 0.4.4 üzerine kurulum önceki paketin doğrulama notunda kayıtlıdır. Bu incelemede eski APK'ya yeniden erişilemediği için o geçiş tekrarlanamadı.

Yerel testler PostgreSQL motorunda iki farklı kullanıcı kimliğiyle çalışır. Hazır olma, yeniden katılım, tekil puanlama, ortak tahta harfleri, günlük soru eşleşmesi, özel sohbet ayrımı, maç sonu yanlışların kişisel öğretmene aktarılması, sözlü değerlendirme sonucu kaydı ve kota dolduğunda kesin cevaplı oyunun sürmesi kontrol edildi. Mevcut veritabanına AI güncellemesi uygulanınca oda ve sohbet kayıtları korundu. Android'in Realtime istemcisi yerel WebSocket sunucusuyla kontrol edildi. Dört AI sağlayıcısında kontrollü yanıtlar kullanıldı. Canlı Supabase Realtime, gerçek AI sağlayıcıları ve iki fiziksel Android cihazla uçtan uca deneme için yukarıdaki kurulum gerekir.

Testi yeniden çalıştırmak için `arena` dizininde `npm install` ve `ARENA_SEED="$PWD/db/03_seed.sql" npm test` komutlarını kullan. Soru paketlerini yeniden oluşturmak için proje kökünde Python 3.10+ ve g++ ile `python arena/seed.py arena/db/03_seed.sql` çalıştır.
