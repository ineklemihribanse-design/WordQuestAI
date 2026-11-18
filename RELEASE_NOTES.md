## WordQuest AI 1.0.48 — Kelime Hafızam Yeni Tasarım ve Yapay Zekâ Görselleri

**19 Kasım 2026.** Onaylanan tasarım temel alınarak Kelime Hafızam sade, modern lacivert–turkuaz–lila bir çalışma merkezine dönüştürüldü.

### Yenilikler
- **Ana ekran:** Öğrenme istatistikleri, bugünkü tekrar, dört çalışma modu ve akıllı koleksiyonlar tek ekranda.
- **Profil robotu:** İnsan öğretmen yerine profil bazında saklanan dört robot görünümü arasından seçim. Gerçek konuşma öğretmenini veya diğer avatar projelerini değiştirmez.
- **Klasör kalabalığı kalktı:** Kelimeler akıllı koleksiyonlarda görüntülenir. Tarihli yarışma klasörleri ve bütün geçmiş kayıtlar **Geçmiş Oyunlarım** bölümündedir.
- **Tam ekran kelime kartı:** Görsel ağırlıklı, krem renkli kart. Ön yüzde cevap kesinlikle gösterilmez; dokunarak çevrilir, arkada telaffuz ve değerlendirme bulunur.
- **İki yönlü çalışma:** TR → EN, EN → TR ve karışık; üstteki kontrol üzerinden değiştirilebilir.
- **Yazma, tür eşleştirme, karma alıştırma:** Eski cevap değerlendirmeleri ve öğrenme geçmişi korunur.
- **AI görseller:** Kart üzerinde isteğe bağlı **AI Görsel Oluştur** düğmesi. Kullanıcının zaten ayarlarda sakladığı Gemini API anahtarı şifreli kayıttan, yalnızca görsel oluşturma sırasında kullanılır. Kelime ve Türkçe anlamı dışında profil veya öğrenme geçmişi paylaşılmaz. Gemini görsel modelinin desteklenmesi ve sağlayıcı kotası gerekir. İşlem ücretli olabilir.
- **Akıllı görsel içeriği:** Kelimenin türüne göre gerçekçi insanlar, nesneler, hayvanlar, sahneler ve eylemler oluşturulur; her karta robot resmi konmaz.
- **Önbellek:** Oluşturulan JPEG görseller cihazın önbelleğinde profil/kelimeye göre tutulur, en fazla 60 görsel saklanır ve gerektiğinde cihaz tarafından temizlenebilir.

### Korunanlar
- Kelime Hafızam SQLite veritabanı ve bütün kayıtlı kelimeler, klasörler, yanlışlar, tekrar geçmişleri silinmez.
- Aynı Android paket adı, cihazdaki eski imzayla uyumlu yayın sertifikası ve uygulama içi güncelleme sistemi.
- 2.5D konuşma asistanı prototipi, API anahtarları, kişisel oyun kayıtları ve US/UK telaffuz sistemi.
- API anahtarı yeni dosyalara veya sunuculara kaydedilmez, GitHub'a gönderilmez.
- Kullanıcı görsel üretimine basmazsa ağ veya görsel API maliyeti oluşmaz.

**Sürüm:** 1.0.48-neon-word-memory · Android versionCode 73.

**Cihaz testleri:** Kelime Hafızam → Kartlarla Çalış; çevirme, TR/EN yönü, yazı kontrolü, AI görseli, profil robot seçimi, Geçmiş Oyunlarım, tekrar kaydı. Gemini izin/kota sorunları cihazda ayrı doğrulanmalıdır.
