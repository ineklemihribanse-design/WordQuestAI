# v1.0.19 — Arena ve Türkiye Saatine Göre Günlük Oda Kotası

- Oda oluşturma hakkı Türkiye saatiyle gece 00.00 itibarıyla yenilenir.
- Açık kalan Arena ekranında ertesi günün Firebase kota kaydı otomatik yüklenir.
- Hem yeni Arena hem eski yarışma ekranları aynı Türkiye saati/Firebase oda kotasını paylaşır.
- Doğru/yanlış sesleri telefonda oyun/medya ses kanalından çalınır; asenkron yüklenme sırasında ilk sesin kaçması önlenir.
- Normal bulmacada yanlış cevap kontrolü için ayrı oyun sesi eklendi. Mevcut oyun sesi ayarları korunur.
- Kullanıcıya gösterilen Premium Arena adı yalnızca Arena oldu; iç uygulama verileri değiştirilmedi.
- Arkadaşlar arası davetlerde oda kodu gösterilmiyor, Kabul Et/Katıl otomatik olarak odaya götürüyor.
- Dışarıdan kodla katılma ve paylaşılan davet bağlantısı korunur.

**Firebase Realtime Database kurallarının yeni JSON dosyasıyla değiştirilmesi zorunludur.**
Kurallar Türkiye saatine göre kota günü hesabı kullanır. Eski kurallar yayınlı kalırsa
uygulamanın oda açma isteği Firebase tarafından reddedilir.

İmza ve applicationId aynı; versionCode=42.
