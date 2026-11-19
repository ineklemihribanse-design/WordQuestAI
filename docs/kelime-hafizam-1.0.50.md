# WordQuest AI — Kelime Hafızam 1.0.50

## Tasarım kilidi
Kullanıcının 19.11.2026 tarihli iki ekranlı referans görseli nihai tasarımdır. Bu dosya yeni bir tasarım önerisi değildir. Mevcut ana sayfa ve Oyun Odası değiştirilmeyecek.

### Ekran A: Kelime Hafızam
- Açık buz mavisi arka plan, beyaz/mavi hafif gölgeli yuvarlatılmış paneller.
- Üstte WordQuest AI başlığı ve sevimli mavi-beyaz 3B görünümlü robot (robotu düz ikonla değiştirme).
- Üç istatistik: Kelimem / Tekrar / Öğrenilen.
- Bugünkü Tekrar: bekleyen kelimeler, ilerleme halkası, 'Tekrara Başla'.
- Çalışma Modları: Kartlarla Çalış, Yazarak Öğren, Tür Eşleştir, Karma Antrenman.
- Akıllı Koleksiyonlar: Favorilerim, Öğrendiklerim, Zorlandıklarım, Tümünü Gör.
- Altta mevcut uygulamanın mobil gezinme çubuğu; ana sayfanın tasarımına dokunma.

### Ekran B: Kartlarla Çalış
- Üstte geri düğmesi, başlık, ayarlar ve deste ilerleme çubuğu.
- Türkçe ↔ İngilizce yönü ve tümü filtresi.
- Önde kelime, tür etiketi, karşılık, anlamlı illüstrasyon, ses düğmesi.
- Gerçek ön/arka kart dönüş animasyonu; sağ/sol kart gezinmesi.
- Dört derecelendirme: Bilmiyorum, Zor, Orta, Biliyorum.
- Altta büyük mavi 'Kartı Çevir' düğmesi.

## Davranış ve veri
- Kullanıcı verilerini silmeden mevcut depolama yapısına uyumlu migrasyon.
- Günlük tekrarlar ve öğrenme dereceleri kalıcı saklanır.
- Oyun tarihiyle ilişkilendirilmiş klasörler; yeniden adlandırma.
- Yeni Kelimeler klasöründe yanlış ve bilinmeyen kelime filtreleri.
- 20/30/40/50 kelimelik karışık çalışma desteleri.
- Çok kelimeli ifadeleri tek sözcük gibi parçalama; yanlış boşlukları doğru sayma.
- Robotun baş, göz, kol, gövde animasyonları orijinal tasarımı bozmayacak ve düşük güçlü Android cihazlarda performanslı olacak.

## Uygulama ve sürüm doğrulama
1. main'deki .wq-source arşivini aç ve gerçek Android kaynak ağacını incele.
2. Ekranları mevcut UI çatısına uygula; yeni, paralel demo ekranı bırakma.
3. Unit/UI test: kart yönü, flip, dört öğrenme durumu, çok kelimeli doğrulama, veri migrasyonu.
4. Telefon/tablet boyutlarında referans görselle karşılaştırmalı ekran görüntüsü testi.
5. main'in son sürümünü ve yayınlanmış APK versionCode değerini incele; versionName=1.0.50, versionCode > yayınlanan son değer.
6. Paket kimliği ve imza sertifikası önceki kurulumla aynı olmalı; offline debug variant yayınlama.
7. GitHub Actions build/test başarılı olmadan Release/update.json güncelleme.
