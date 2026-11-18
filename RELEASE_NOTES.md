## WordQuest AI 1.0.47 — Kelime Hafızam ve 2.5D Uyumlu Güncelleme

**18 Kasım 2026.** Otomatik güncellemede görülen "Uygulamanız güncel" sorununun nedeni giderildi.

### Güncelleme neden görünmüyordu?
- Daha önce yüklenen **1.0.46-2p5d-vertex-rig-prototype-qa** test APK'sı Android'de **versionCode 71** kullanıyordu.
- 1.0.46 Kelime Hafızam APK'sı da **versionCode 71** ile yayımlanmıştı. Aynı kod yeni güncelleme sayılamadığı için uygulama, yeni özellikleri göstermiyordu.
- Yeni birleşik paket **versionCode 72** ile yayımlanır; eski test APK'sının üzerine veri kaybetmeden kurulabilir.

### Birleştirilen özellikler
- **Kelime Hafızam:** Ayrı çalışma hazırlık ve soru ekranları.
- **Çift yön:** Türkçe → İngilizce, İngilizce → Türkçe veya karışık.
- **Yazılı cevaplar:** Çevrimdışı denetim, küçük yazım hatalarına "yakın" geri bildirimi, kabul edilen açık alternatifler.
- **Tür eşleştirme:** Güvenilir kelimelerde isim/fiil/sıfat/zarf, türü belirsiz kelimelerde yanlış bilgi verme yok.
- **Karma çalışma ve tekrar:** Mevcut klasörler, kelimeler, öğrenme geçmişi ve aralıklı tekrar sistemi korunur.
- **2.5D test öğretmen:** Telefonda zaten yüklü olan `drawBitmapMesh` tabanlı sürekli hareket motoru bu pakette de bulunur; eski fotoğraf/kare döngüsüne geri dönülmez.

### Önemli sınırlar ve korumalar
- 2.5D hareket motoru **hâlâ deneysel**. İnsan gibi doğal ağız/kapak/kol hareketleri gerçek cihazda henüz görsel olarak onaylanmış değildir.
- Aynı Android paket kimliği `com.kelimebulmacasi.a1.practice.offline101` ve aynı yayın sertifikasıyla yerinde güncelleme hedeflenir.
- Sohbet kayıtları, kayıtlı AI anahtarları, US/UK sesler, bulmacalar, Arena, YDS ve kişisel veriler silinmez.
- Deneysel 2.5D sürümün kodunu yeniden kullanmamak için yeni sürüm **1.0.47-vocabulary-2p5d-compat / versionCode 72** olarak ayrıldı.
- Gereksiz 100 animasyon karelik ZIP, APK'nın yanına tekrar yüklenmez; yalnızca APK ve update.json yayımlanır.

**Test önerisi:** Uygulamayı kaldırmadan Ayarlar → Güncellemeleri Kontrol Et.
Yeni sürüm görünürse indir ve kur; sonra Kelime Hafızam → Kartlar → Yazarak cevapla modunu ve Konuşma Asistanı'ndaki 2.5D hareketi ayrı ayrı kontrol et.
