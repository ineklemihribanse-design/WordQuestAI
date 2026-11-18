# WordQuest AI · Kelime Bulmacası 0.7.5

Test APK’sını Samsung tabletinde **Dosyalarım → İndirilenler** üzerinden aç. Kurulu **WordQuest AI Test v0.7.0** için Android’in **Güncelle** seçeneğini kullan. Yalnızca v0.6.0 kuruluysa v0.7.5 Test onun yanına kurulur. v0.6.0’daki ilerlemeyi taşımak için **Bulmacalarım → Yedekle ve aktar → Yedek dosyası oluştur** adımlarını izle. v0.7.5 Test’te aynı menüde **Kayıt dosyasını içe aktar** ile yedeği seç. [APK imzalama kılavuzu](APK_OLUSTURMA.md)

Ana ekrandaki **Kelime Hafızam** bölümünden **Tekrar** veya **Kelime defteri** açılır. Oyuncu 1 ve Oyuncu 2’nin yerel kayıtları ayrıdır. Kelimenin tarihli doğru, yanlış ve ipucu kayıtlarını görmek için **Geçmişi gör**’e dokun. Yedek ve geri yükleme ekranda görünen etkin profile uygulanır.

**Tekrar** ekranında cevabı açıp **Tekrar çalış**, **Zor**, **Hatırladım** veya **Kolay** seç. Uygulama sonraki aralığı düğmenin altında gösterir ve tekrar geçmişine göre hesaplar.

Ana sayfadaki dişli simgesi **Ayarlar**’ı açar. Yazı boyutunu ve telaffuz ayarlarını etkin oyuncu için seç. Doğru tamamlanan kelime İngilizce okunur. **Tekrar dinle** ile yeniden oynatabilirsin. Ses için cihazda İngilizce metin okuma verisi gerekir. [v0.7.5 sürüm notları](SURUM_NOTLARI_v0.7.5.md)

**Yeni Oyun** ekranında A1–C2 veya YDS seviyesini seç. Kullanılabilir konu ve kelime türü filtreleriyle havuzu daralt. 10, 20, 30 ve 50 kelime arasından seçim yapabilir ya da 8–50 arasında bir sayı girebilirsin. Filtre sonucu kaç farklı cevap kaldığı ekranda görünür. Yeni tahta otomatik kaydedilir. **Devam Et** son yarım oyunu açar. **İlerlemem** daha önce kaydedilmiş tahtaları listeler.

**Hazır Paketler** bölümünde 657 hazır bulmaca var. İlk sürümdeki 8 cevaplı deneme ana sayfadan açılır. Yeni C1 ve C2 kelimelerinden tahta oluşturmak için **Yeni Oyun** seç. Kelime seviyeleri Türkçe ipucundaki anlama göre uygulama için yaklaşık olarak atanmıştır.

**AI Yarışma Merkezi** çevrimiçi çalışır. İki cihazla oynamak için [sunucu kurulumunu](YARISMA_KURULUMU.md) tamamla ve iki ayrı yarışma hesabıyla giriş yap. Uygulama içindeki klasikleri misafir olarak da çözebilirsin.

- Her konu sayfasında **Alt bölümler**, **Karışık** ve **Eklediklerim** bulunur. **Karıştır**, o konuya ait karışık bulmacalardan veya kendi eklediğin bulmacalardan birini seçer. Daha önce bitirdiğin bir bulmaca çıkarsa **Tekrar oyna** ile boş tahtada çözebilirsin. İlk kazandığın puanlar ve tamamlanma kaydın saklanır.
- Oyun ekranındaki **Karıştır**, açık bulmacanın aynı kelimeleriyle yeni bir tahta kurar ve ipuçlarını yeniden sıralar. Çözülmüş cevaplar, doğru harfler ve puanlar taşınır. Yanlış harfler temizlenir. **Yeniden başla** ile tahtayı sıfırlayıp tekrar çözebilirsin.
- Bir ipucuna veya kutuya dokununca Android klavyesi açılır. Doğru cevaplar sabitlenir. **Sonraki ipucu** veya klavyedeki **İleri** ile ilerleyebilirsin.
- **Bulmacalarım** ve konuların **Eklediklerim** sekmesinde **Düzenle** düğmesi var. Buradan adı, İngilizce cevapları ve Türkçe ipuçlarını değiştirebilir, satır ekleyip kaldırabilirsin. Yalnızca ad veya ipucu değişirse mevcut tahta ve çözüm korunur. Cevaplar değişirse yeni tahta boş açılır ve eski puanlar toplamda kalır.
- İlk doğru çözüm 10 puan, açılan bir harften yararlanan cevap 5 puan kazandırır. **Devam Et** son yarım oyunu açar. Misafir kayıtları cihazda saklanır.

## Fiillerin 1., 2. ve 3. hâli

Ana sayfada **Fiillerin 1., 2. ve 3. Hâli** bölümünü aç. 175 fiil arasından **Tümü**, **Düzenli** veya **Düzensiz** havuzunu seçebilirsin. Eşleştirmede V1’i V2 ve V3’le eşleştirirsin. Boşluk doldurmada İngilizce cümledeki eksik hâli, yazmada ise Türkçe anlamı verilen fiilin üç hâlini klavyeyle girersin. Kabul edilen yazım çeşitleri, örneğin `learned` ve `learnt`, cevabı kontrol ettiğinde gösterilir.

## Fotoğraf veya PDF ile bulmaca oluşturma

1. Bir konuyu açıp **Bulmaca ekle**'ye dokun. Ana sayfadaki **Bulmacalarım** üzerinden de ekleyebilirsin.
2. **Kamerayla çek**, **Galeriden seç** veya **PDF seç** düğmesini kullan. PDF’de sayfa numarası veya aralık girebilirsin. Fotoğrafta önizlemedeki **Döndür** düğmesiyle yönü düzeltebilir ve tablonun çevresini seçebilirsin.
3. Okunan satırları gözden geçir. Özellikle eğik, bulanık veya kısmen karanlık tablolarda hatalı okunan kelimeyi ve anlamını elle düzenleyebilirsin. Kelime listesi yapıştırırken her satıra `predict: öngörmek` biçiminde bir çift yazabilirsin.
4. Bir ad gir ve **Bir bulmacadaki cevap sayısı** alanından 8–20 arasında hedef seç. En az 8 geçerli çift gerekir. Uzun listelerde **Otomatik** önerinin kaç bulmaca çıkaracağını gösterir. **Elle böl** ile her tahtanın sayısını belirle. Örneğin 30 kelime için `10 + 12 + 8` yazabilirsin. Her tahta 8–20 cevap içermeli ve sayıların toplamı kelime sayısına eşit olmalı.
5. **Bulmacayı oluştur**’a dokun. Oluşan oyunlar seçtiğin konunun **Eklediklerim** bölümünde ve ana sayfadaki **Bulmacalarım** listesinde görünür.

Tarama cihazda çalışır. İçe aktarılan görsel veya PDF tarama tamamlandığında saklanmaz. Hesap eşitlemesi açılmışsa oluşturduğun kelime–ipucu listesi ve bulmaca ilerlemen eşitlenebilir.

## Yapay zekâyla kelime üretme

**Bulmaca ekle → Yapay zekâyla kelime üret** seçeneğini aç. Konuyu, seviyeyi ve 8–20 arasında üretilecek kelime sayısını seç. İlk istekte [Google AI Studio](https://aistudio.google.com/apikey) üzerinden aldığın **Gemini API anahtarını** uygulamadaki alana gir. Bu kişisel deneme sürümü Gemini 3.1 Flash-Lite kullanır ve internet bağlantısı gerekir. Anahtar uygulama açıkken bellekte tutulur. İstediğin zaman **Anahtarı unut** ile temizleyebilirsin.

Sürüm 0.4.3, `AQ.` ile başlayan yeni Gemini anahtarlarını destekler. Anahtarın tamamını Google AI Studio’daki **Kopyala** düğmesiyle alıp uygulamaya yapıştır. Google bir istek hatası döndürürse açıklaması ve HTTP kodu üretim ekranında görünür.

**Kelime üret** ile önerileri gör. **Kelimeleri bulmacaya ekle** ile taslağa alıp Türkçe ipuçlarını gözden geçir. Google’a konu, seviye ve sayı gönderilir. Taslaktaki tekrarlar cihazda ayıklanır.

## Konuşma Asistanı

1. Ana sayfada **Konuşma Asistanı**’nı aç. **İngilizce** modunda A1–C2 arasında seviye seç. **Türkçe** modunda Türkçe yanıt alırsın.
2. Mesajı yaz veya **Mikrofon**’a dokunup konuş. Android’in ses tanıma aracı konuşmanı metne dönüştürür. Mesajı gözden geçirip **Gönder**’e dokun.
3. **Yanıtları sesli oku** açıkken Android metin okuma motoru yanıtı seslendirir. Bir yanıtın altındaki **Dinle** ile tekrar çalabilirsin. **Yeni sohbet** oturumu temizler.

Kelime üretmede kullandığın Gemini anahtarı asistan için de geçerli. Uygulama kapanınca anahtarı yeniden girmen gerekebilir. Ses tanıma ve okuma, cihazındaki Android hizmetlerine bağlıdır. Android’in ses tanıma hizmeti, ayarlarına göre sesi kendi sunucusunda işleyebilir. Uygulama Gemini’ye yazılı mesajı ve son sekiz konuşma turunu iletir. Sohbeti açık oturumda bellekte tutar.

## Eski kayıtları aktarma

**Bulmacalarım → Yedekle ve aktar** bölümünden ilerlemenin JSON yedeğini alabilirsin. Önceki uygulamadan alınan XML dosyasını da açabilirsin.

### Bilgisayarla

1. Tabletinde **Ayarlar → Tablet hakkında → Yazılım bilgileri → Yapım numarası** satırına yedi kez dokun. **Ayarlar → Geliştirici seçenekleri → USB hata ayıklama** seçeneğini aç. Bilgisayarda [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools) paketini çıkar. Tableti bağla ve cihazdaki izin ekranını onayla. `adb devices` çıktısında tabletin karşısında `device` yazdığını kontrol et.
2. Windows’ta Platform Tools klasöründe Komut İstemi (`cmd.exe`) aç. Aşağıdaki komut misafir kayıtlarını o klasöre kopyalar. macOS veya Linux’ta komutun başında `./adb` yaz.

   ```text
   adb exec-out run-as com.kelimebulmacasi.a1 cat shared_prefs/player_v2_guest.xml > Eski_Kelime_Bulmacasi.xml
   ```

3. `Eski_Kelime_Bulmacasi.xml` dosyasını tabletin **Download** klasörüne kopyala. **Kelime Bulmacası 0.4 → Bulmacalarım → Yedekle ve aktar → Kayıt dosyasını içe aktar** yoluyla seç. **Bulmacalarım** ve **İlerlemem** ekranlarından kayıtlarını kontrol et.

### Yalnızca tabletle

1. Tableti Wi-Fi’ye bağla. [Termux](https://f-droid.org/packages/com.termux/) sayfasından APK’yı kur. Termux’ta `pkg install android-tools` komutunu çalıştır. **Ayarlar → Geliştirici seçenekleri → Kablosuz hata ayıklama** seçeneğini aç.
2. Ayarlar ile Termux’u bölünmüş ekranda açık tut. Ayarlarda **Eşleştirme koduyla cihazı eşleştir** seçeneğine dokun. Termux’ta `adb pair 127.0.0.1:PORT` yaz ve `PORT` yerine bu penceredeki portu kullan. Altı haneli eşleştirme kodunu Termux’a gir.
3. Kablosuz hata ayıklamanın ana ekranındaki **IP adresi ve bağlantı noktası** satırının sonundaki portu kullanarak Termux’ta `adb connect 127.0.0.1:PORT` çalıştır. Sonra aşağıdaki komut eski uygulamanın misafir kayıtlarını İndirilenler’e kopyalar.

   ```text
   adb shell 'run-as com.kelimebulmacasi.a1 cat shared_prefs/player_v2_guest.xml > /sdcard/Download/Eski_Kelime_Bulmacasi.xml'
   ```

4. Yeni uygulamadaki **Kayıt dosyasını içe aktar** düğmesiyle dosyayı seç. Kayıtları doğruladıktan sonra kablosuz hata ayıklamayı kapatabilirsin.

Eski uygulamada Google hesabıyla oynadıysan `adb shell run-as com.kelimebulmacasi.a1 ls shared_prefs` komutuyla `player_v2_account_… .xml` dosyasının adını görüp kopyalama komutunda `player_v2_guest.xml` yerine onu kullan. Birinci sürümden sadece ilk deneme kaldıysa dosya adı `crossword_progress.xml` olabilir.

## Projeyi açma

ZIP'i çıkar ve `A1KelimeBulmacasi` klasörünü aç. Android Studio'da SDK 35 ve JDK 17 veya üstüyle **Run** ya da `:app:assembleDebug` görevi kullanılabilir. APK `app/build/outputs/apk/debug/` klasörüne yazılır.

CodeAssist'te klasörü içe aktarabilirsin. Proje AndroidX Credentials, Firebase ve Google ML Kit kitaplıklarını içerir. CodeAssist'in kullanılan bağımlılıkları işleyemediği durumda Android Studio'da derle veya verilen APK'yı doğrudan yükle.

## Google ile giriş ve eşitlemeyi açma

Bu APK, kişisel bir Firebase projesine bağlı olmadan çalışır. Gerçek Google girişi ve cihazlar arası eşitleme için aşağıdaki yapılandırma gerekir.

1. Firebase Console'da bir proje oluştur. Android uygulamasını `com.kelimebulmacasi.a1.practice` paket adıyla kaydet. Kullanacağın APK'nın imza sertifikasının SHA-1 ve SHA-256 parmak izlerini uygulama kaydına ekle.
2. Authentication bölümünde Google oturum açmayı etkinleştir. Uygulamaya verilen güncel `google-services.json` dosyasını indir. Firestore veritabanını oluştur ve bu projedeki `firestore.rules` içeriğini Firestore Rules sekmesinde yayımla.
3. `app/src/main/res/values/cloud_config.xml` dosyasına proje kimliğini, Android uygulama kimliğini, Android API anahtarını ve Google oturumu için **Web** OAuth istemci kimliğini yerleştir. İndirilen Firebase yapılandırmasında bunlar `project_info.project_id`, Android istemcisindeki `client_info.mobilesdk_app_id`, `api_key.current_key` ve `oauth_client` içinde `client_type: 3` değerli `client_id` alanlarıdır.
4. Uygulamayı yeniden derle. **Hesap → Google ile giriş** üzerinden giriş yap. Bu cihazdaki misafir kayıtları, bu hesaba ilk girişte aktarılır. Aynı Google hesabıyla ikinci bir cihazda giriş yaparak eşitlemeyi kontrol et.

[Firebase Android kurulumu](https://firebase.google.com/docs/android/setup)  
[Android'de Google ile giriş](https://firebase.google.com/docs/auth/android/google-signin)

## Kelime içerikleri

Önceki bulmacalar `app/src/main/assets/puzzles.json` dosyasında, ek bulmacalar `expanded-puzzles.json` dosyasında yer alır. Yeni kelime havuzları, alt konu başlıkları ve özgün ipuçları `content/` klasöründedir. Seviyeler konu paketlerine göre öneri niteliğindedir. **The CEFR-J Wordlist Version 1.6** (Yukio Tono, Tokyo University of Foreign Studies) ve **New Academic Word List** genel seviye ve akademik kelime seçimi için referans alındı. YDS zorlukları uygulamanın sınıflandırmasıdır.

Hazır tahtaları yeniden üretmek için `content/crossword-layout.cpp` dosyasını C++17 ile derle. Sonra `python3 content/generate.py /derlenen/arac/yolu` komutunu çalıştır. Önceki oyun kayıtları sabit bulmaca kimliklerine ve tahta düzenlerine bağlıdır.

[CEFR-J Wordlist 1.6](https://cefr-j.org/download_eng)  
[New Academic Word List](https://www.newgeneralservicelist.com/new-academic-word-list)

Fiil çekimleri için [Cambridge Dictionary](https://dictionary.cambridge.org/grammar/british-grammar/table-of) ve [British Council](https://learnenglish.britishcouncil.org/free-resources/grammar/english-grammar-reference/irregular-verbs) referans alındı. Örnek cümleler bu uygulama için yazıldı.
