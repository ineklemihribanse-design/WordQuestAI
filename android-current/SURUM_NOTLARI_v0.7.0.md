# WordQuest AI v0.7.0 — Kelime Hafızam

Mevcut Android projesine eklendi. Android SDK 35 ve JDK 17 ile derlendi.

## Kullanım

- **Kelime Hafızam → Kelime defteri.** İngilizce kelimeyi veya Türkçe anlamını ara. Kelimeyi yıldızla kaydet. **Geçmişi gör** ile doğru çözüm, gönderilen yanlış cevap, harf ipucu ve tekrar kayıtlarına bak.
- **Kelime Hafızam → Tekrar.** Yanlış cevap, ipucu veya “Tekrar çalış” sonrasında ilk tekrar yaklaşık 10 dakika sonra açılır. “Hatırladım” ile aralıklar 1, 3, 7, 14, 30, 60 ve 120 güne çıkar. Tekrar planı cihazda çalışır.
- **Oyuncu 1 ve Oyuncu 2.** Her oyuncunun kendi bulmaca ilerlemesi, defteri ve tekrar sırası var. Önceki cihaz içi misafir kayıtları Oyuncu 1 altında açılır. Profil seçimi uygulama yeniden açıldığında hatırlanır. Google ile giriş kullanılıyorsa hesabın öğrenme kayıtları kendi veritabanında saklanır.
- **Bulmacalarım → Yedekle ve aktar.** Etkin oyuncunun bulmaca ilerlemesi, kelime defteri ve öğrenme olaylarını bir JSON dosyasına kaydet. Eski JSON ve XML yedekleri okunur. Aynı öğrenme yedeğini yeniden içe aktarmak olayları ikinci kez saymaz.

## Samsung tabletine kurma

`WordQuest_AI_v0.7.0_Test.apk` Samsung **Dosyalarım → İndirilenler** bölümünden açılır. **WordQuest AI Test** olarak v0.6.0’ın yanında kullanılır. Test uygulamasına eski bulmaca ilerlemesini taşımak için v0.6.0’da etkin profilin JSON yedeğini al ve test uygulamasında aynı menüden içe aktar. Önceden oynanmış bulmacaların ilerlemesi JSON ile taşınır. Ayrıntılı kelime olayları v0.7.0’da yapılan hamlelerle kayda başlar.

Yarışma oturum bilgileri, sunucudaki sohbetler ve diğer uygulama ayarları bu JSON yedeğine dahil değildir. Daha önce kurulmuş bir yarışma sunucusuna erişim için o sunucunun ayarlarını ve oyuncu hesabını kullan.

## Doğrudan yükseltme

Mevcut APK paketi `com.kelimebulmacasi.a1.practice`, sürüm kodu 10. v0.7.0 dağıtım derlemesi aynı paket kimliğini ve sürüm kodu 11’i taşıyor. Eski özel imzalama anahtarı bulunamadı. Android 15 emülatörü farklı imzayla aynı paketi yükleme denemesini reddetti. v0.6.0’ı cihazda tut. Kaynak ZIP’indeki `APK_OLUSTURMA.md`, özgün anahtar bulunduğunda dağıtım APK’sını nasıl imzalayacağını anlatır.

## Doğrulama

- Test APK’sı ve imzalanabilir dağıtım APK’sı derlendi. Test imzası ve paket kimliği Android Build Tools ile kontrol edildi. Android 15 emülatöründe v0.6.0 ve v0.7.0 Test aynı cihaza kuruldu. Ana ekran, Kelime Hafızam, kelime defteri ve arama alanı açıldı. İkinci oyuncu profiline geçiş denendi. Fiziksel cihaz testi yapılmadı.
- Kotlin kontrolleri tekrar aralıklarını, Türkçe aramayı, öğrenme olayı kimliklerini ve doğru cevabın kilitlenmesini sınadı.
- Üç Android çalışma zamanı testi profil ayrımını, tekrar zamanını, eski JSON/XML okumasını, çift kayıt önlemeyi ve SQLite geçişinde geçmişin korunmasını doğruladı.
- Android Lint sıfır hata ve mevcut Material 3 bağımlılığı için bir sürüm uyarısı bildirdi.

FSRS, otomatik cihaz eşitlemesi ve yarışma hatalarının öğrenme defterine otomatik aktarılması ileriki sürümlerin konusu.
