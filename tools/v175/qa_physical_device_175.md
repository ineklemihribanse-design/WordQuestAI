# WordQuest AI 1.0.75 — fiziksel cihaz ve yayın onay protokolü

Bu dosya **tamamlandı raporu değildir**. CI #358 imzalı QA APK'sı hazırdır ancak fiziksel cihaz ve iki hesaplı Arena kanıtları henüz toplanmadı.

## Test sırası (veri kaybını önle)

1. Gerçek telefondaki 1.0.74 profil/kelime/ilerleme sayılarını not al ve mevcut uygulama içi yedekleme özelliğiyle dışarı aktar. APK'yı kaldırma veya verileri temizleme.
2. Yalnızca #358 imzalı QA APK'sını `adb install -r` veya Android paket yükleyicisiyle **mevcut kurulum üzerine** kur. Android paket adı `com.kelimebulmacasi.a1.practice.fresh`, sürüm kodu 101 olmalı.
3. Aynı profil, kayıtlı kelime sayısı, çalışma geçmişi, günlük hedef, kişisel bulmacalar ve Arena skorlarını 1.0.74 öncesi kayıtla karşılaştır. Herhangi bir azalma veya kayıp = başarısız.
4. Kamera ile gerçek sayfa fotoğrafı çek; OCR sonucu önizle, İngilizce/Türkçe düzelt, kaydet ve uygulamayı yeniden açarak kayıtları doğrula. Kamera izni gerekip gerekmediğini cihaz üzerinde gözle. Fotoğraf, belge ve ses verilerini herkese açık hata kaydına yükleme.
5. Gerçek çok sayfalı PDF seç, sayfa aralığı belirt, sekizli önizleme sayfaları arasında ilerle; seçim/düzenleme korunsun, onaylanmayan kelimeler kaydedilmesin.
6. İki ayrı test hesabı ve iki cihazla çevrimiçi Arena eşleşmesi, cevap, pas, süre, skor ve bağlantı kopup yeniden bağlanma akışını kontrol et.
7. Telefon, %140 yazı ve yatay tablet üzerinde **her iç oda** ve sonuç ekranının ekran görüntülerini insan gözüyle incele. Lena animasyonu ve mesaj ekranı ayrıca denetlenmeli.
8. A1–C2 kelime bankasını seviye, tekrar, çeviri doğruluğu, kaynak lisansı ve hedef adet açısından incele. Eksik seviye hedefleri tamamlanmadan içerik onayı verme.
9. YDS soru havuzunda 2.000 **benzersiz** ve özgün soru, tek doğru cevap, Türkçe açıklama ve kategori dağılımını doğrula. Banka 150 soruda iken başarı onayı verme; doğrulanan benzersiz soru sayısını release_evidence_175.json içindeki yds_2000_verified.unique_question_count alanına kaydet.

## Yayın kararı

Her bölüm için test tarihi, sorumlu kişi ve erişilebilir bir kanıt bağlantısı gereklidir. Bunları `tools/v175/release_evidence_175.json` şemasına göre hazırlayıp `python3 tools/v175/release_gate_175.py <dosya>` komutuyla doğrula. Gerçek kullanıcı yedeklerini ve kişisel bilgileri depoya commit etme. **Kanıt yoksa otomatik güncelleme yayınlanmaz.**

QA APK: https://github.com/ineklemihribanse-design/WordQuestAI/actions/runs/44760917145/artifacts/14642226417
