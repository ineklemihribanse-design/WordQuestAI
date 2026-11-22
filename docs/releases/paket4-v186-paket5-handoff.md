# WordQuest AI — Paket 4 / Paket 5 devri (1.0.86 çalışma dalı)

## Durum ve yayım kararı
- Paket 4 kaynak kodu, ana sürümden izole **feature/v186-package4-integration-qa** dalında saklanır. Bu dal doğrudan uygulama güncellemesi yayımlamaz.
- 1.0.85 tam uygulama kaynakları üzerine Paket 4 güvenli üç yönlü birleştirme adımı GitHub Actions'ta başarıyla geçti.
- Birleşik uygulamanın **:app:compileOfflineKotlin** görevi geçti (Actions 45054479814).
- Aynı çalışmada **233 testten 220'si geçti, 13'ü** Robolectric MavenArtifactFetcher/HTTP indirme sorunu yüzünden çalışamadı. Bu **tam bir test geçişi değildir**; yeniden koşulmalı.
- `:app:assembleOffline` aynı Gradle çağrısında test hatası nedeniyle bitirilemedi. İmzalı APK ya da kullanıcıya yayımlanmış güncelleme **yoktur**.
- Paket 5'e geliştirme olarak geçilebilir; Paket 4'ün nihai Android APK/emülatör/fiziksel cihaz doğrulaması toplu sürümün yayın kapısında zorunludur.

## Paket 4'te hazırlanan işlevler
1. Yeni oyun için iki sütunlu buton-kart seçimi; birden çok seviye/konu/kelime türü; boş konu/tür seçimi tümünü kapsar.
2. Tek tahtada **8–70 kelime**; 10, 20, 30, 50, 70 hazır sayılar ve özel aralık; **oyunu aşamalara bölme yok**. Seçilen filtrelerde yeterli benzersiz kelime olmadığında başlatma engellenir. Ekran/performans testi gereklidir.
3. Klasik, sayaçlı ve süreli oyun; **15, 20, 25, 30, 40 dk** hazır süre veya **1–180 dk** özel süre. Her oyun ve oyuncu profili için ayrı kayıt.
4. Günlük çalışma hedefi kaydedildikten sonra küçük özet kart; kalemle düzenleme; hedef tamamlandığında büyük tik. Önceki günlük ilerleme ve seri takibi korunur.
5. Paragraf dinle-yaz ve TR→EN çeviri; British / American ses seçenekleri (yeni paragraf çalışmasında başlangıç British); PDF/görsel/kamera/elle/opsiyonel kullanıcı AI anahtarı ile **önce taslak** ve kullanıcı onayı sonrası profile özel yerel kayıt. Cihazda ilgili TTS sesi gerekli.
6. Günlük YDS ve kelime soru havuzu dönüşü, aynı soruyu sık tekrarlamama yaklaşımı.

## Toplu sürüm öncesi kapanması gereken doğrulamalar
- GitHub Actions koşusunda Robolectric gerekli Android SDK/Maven test bağımlılıklarının erişimini düzelt; **13 başarısız testi yeniden çalıştır**, tüm regresyonu geç.
- `:app:assembleOffline` ile tam APK oluştur; paket kimliği, imza ve yerinde güncelleme yolu doğrula.
- Telefon ve 13 inç tablet üzerinde 70 kelimelik tek pano; taşma, kaydırma, performans, yanlışlıkla çoklu bölünme kontrolleri.
- Zaman aşımı sonrası giriş kilidi ve kaydetme; klasik/sayaçlı davranış; eski 3/5/10 dk bulmacalarının kayıtlı süreleri.
- British ve American ses seçimi; ilgili ses paketi olmayan cihazda anlaşılır hata.
- Günlük hedef düzenleme/başarı tiki; profil ayrımı; paragraf içe alma onayı ve veri koruma.
- Firebase kurallarına, gerçek kullanıcı kayıtlarına ve yayın akışına **bu geliştirme dalından dokunma**.

## Referanslar
- Başarılı bağımsız Paket 4 kaynak ve Kotlin derleme kontrolü: GitHub Actions 45052424668
- Tam 1.0.85 + Paket 4 entegrasyonunun geçtiği, Android Kotlin derlemesinin başarılı olduğu ve Robolectric indirme hatalarının görüldüğü çalışma: GitHub Actions 45054479814
- Uygulama kaynağına birleştirme betiği: `tools/v186/apply_package4_to_185.py`
- Özgün Paket 4 oyun/paragraf kodu: `tools/v186/staged/`, `tools/v186/Wq186ParagraphWorkspace.kt`, `tools/v186/Wq186ParagraphRepository.kt`
