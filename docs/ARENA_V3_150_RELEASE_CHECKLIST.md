# WordQuest AI Arena V3 — Yayın Sırası / 1.0.50

## Geliştirme ve deney sürümü

- Android sürüm: `1.0.50-arena-v3-preview`, versionCode `75`.
- Android kimliği: `com.kelimebulmacasi.a1.practice.fresh` — 1.0.49 ile aynı.
- İmza: mevcut 1.0.49 sertifikasının SHA-256 özeti derlemede doğrulanır.
- Önizleme etiketi: `arena-v1.0.50-preview`. Bu yayın **normal güncelleme kanalına** girmez.
- Öğretmen, seslendirme, Kelime Hafızam ve önceki Android veri kayıtları değişmez.
- İndirilebilir APK yalnızca GitHub Actions başarıyla imzalayıp yayımladığında geçerlidir.

## Yayın engeli: canlı Firebase RTDB kuralları

Android istemcisinin `ready/{uid}` alanı yeni odalarda **Boolean yerine konfigürasyon sürümü (Number)** yazar. Canlı Firebase veritabanındaki eski kurallar Boolean dışında veriyi reddediyor. Yalnızca APK yayımlamak "Hazırım" düğmesini onarmaz.

GitHub Action `Verify Arena V2 Firebase Rules (Emulator)` güvenlik testini tamamlar; **kuralları gerçek sunucuya dağıtmaz**.

Yayın için yetkili Firebase proje yöneticisi şunları yapmalı:

1. Doğru Firebase RTDB projesinin yedeklemesini ve mevcut kurallarının kopyasını almalı; mevcut uygulamanın bağlı olduğu veritabanını doğrulamalı.
2. GitHub Android CI'nin **Apply Arena v2 Firebase rules and integration audits** adımıyla oluşturduğu `source/database.rules.json` içindeki güncel `rules` ağacını incelemeli.
3. Firebase Console → Realtime Database → Rules bölümünde **yeni kuralları yayımlamalı**; açık test izni (`.read: true`, `.write: true`) vermemeli.
4. İki farklı oturumla iki gerçek cihazda oda oluşturma, davet, hazır olma, ayar değiştirme, yeniden hazır olma, doğru/yanlış/pas, ikinci şans, skor ve zafer animasyonunu doğrulamalı.
5. Yeni odada, ayar değiştikten sonra yalnızca bir kişi hazır olduğunda oyunun başlamadığını doğrulamalı.
6. Kullanıcı verilerinin ve eski odaların erişim durumunu kontrol etmeli. Başarısız olursa daha önce yedeklenmiş güvenli RTDB kurallarına dönmeli.

**Bu işlem henüz üretim sistemine uygulanmadı.** Servis hesabı gizli anahtarlarını veya Firebase kimlik bilgilerini issue, sohbet veya APK içine eklemeyin.

## Normal güncelleme kanalına geçiş

1. GitHub PR #4 ve 1.0.50 preview APK için Android + Firebase emülatör testlerinin **başarılı** olduğundan emin ol.
2. Önce Firebase üretim kurallarını yayımla ve iki cihazdan doğrula.
3. Onaylanan PR'ı korumalı ana dala birleştir.
4. `Publish Arena 1.0.50 to in-app updater` iş akışını **manuel** başlat. Hem Firebase kural onayını hem iki cihaz kabul testini teyit et.
5. İş akışı zaten imzalı preview APK'sını yeniden doğrular, SHA-256 ve paket/imza sertifikasını karşılaştırır, `update.json` üretir ve yalnızca o zaman `v1.0.50` normal sürümünü yayımlar.
6. Telefonda Ayarlar → Güncellemeleri Kontrol Et ekranında sürümün `1.0.50` görünmesini doğrula.

`v1.0.49` normal sürümünün üzerine **aynı imza + aynı Android kimliği + daha büyük versionCode** gerekir. Üretim doğrulanmadan eski sürümü silmeyin veya normal yayın etiketini değiştirmeyin.

## Kabul listesi

- [ ] Küçük Android ekranda Arena girişindeki konu/seviye çipleri yatay kaydırılabiliyor
- [ ] Profil avatarı gizli seçim penceresinden değişiyor, lobiye yansıyor
- [ ] Tek Başına Oyna eski koyu formu göstermiyor
- [ ] Arkadaş adı ve durumu iki farklı oturumda güncelleniyor
- [ ] İki oyuncu hazır değilse başlatma engelleniyor
- [ ] Oda sahibi ayarı değiştirince hazır durumu sıfırlanıyor
- [ ] İki oyuncu yeniden hazırsa yarışma açılıyor
- [ ] Kesişen harfler kilitleniyor; klavye açıkken bulmaca görünür
- [ ] Tek yanlış tahmin → sıra değişimi; pas → sıra değişimi
- [ ] İki oyuncunun bıraktığı kelimeler son şans turuna kalıyor
- [ ] İpucu sınırları, puan kesintileri, kazanan animasyonu doğru
- [ ] Diğer uygulama bölümleri ve kullanıcı kayıtları korunuyor
