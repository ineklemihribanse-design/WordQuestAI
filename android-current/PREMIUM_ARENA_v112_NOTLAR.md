# WordQuest AI v1.0.12 — Premium Arena çevrim içi geçişi

- Uygulamanın ana yarışma menüsü Premium Arena'dır. Eski, beyaz Firebase yarışma ekranı yeni oyunculara ana menüden açılmaz; eski sürümden gelen davetlerde uyumluluk için tutulur.
- Premium Arena'da çevrim içi bulmaca düellosu, ortak bulmaca ve 12 soruluk 5 şıklı düello bulunur. A1, A2 ve A1–A2; konu paketi; bulmacada 20,30,40,50 kelime; 0,20,30,45 saniye seçimleri korunur.
- Oda sahibi aynı bulmacayı belirleyen değişmez `boardId` veya 5 şıklı sorular için ortak `quizSeed` değerini Firebase `premiumRooms` altına kaydeder. İki cihaz aynı paket sürümünü kullanmalıdır.
- Kesişimli bulmacanın çözülen kelime indeksleri Firebase üzerinde gerçek zamanlı ortaklaşa açılır. Düelloda doğru bilen devam eder, pas/yanlışta sıra geçer; ortak bulmacada her oyuncu çözebilir. Skorlar canlı izlenir.
- Davet ve arkadaşlık sistemi `roomInvites` ve `friends` üzerinden çalışır; yeni davetlerde `arenaType=premium` alanı ile yeni ekrana yönlendirme yapılır.
- Aynı oda kotası (`roomQuota`) korunur. Eski Firebase Rooms ve kayıtlar silinmez.
- **Zorunlu:** `database.rules.json` içindeki güncel kurallar Firebase Realtime Database → Rules bölümüne bir defa yayımlanmalıdır. Önceki sürümün kuralları `premiumRooms` yazımına izin vermez.
- **Güvenlik sınırı:** Puanlama içerik ve cevap kontrolünü istemci yapar. Bunun rekabetçi, hile korumalı sıralama için sunucu doğrulaması/Cloud Functions ile güçlendirilmesi gerekir.
- Yeni tasarımın gerçek iki cihaz testini ve canlı Firebase kurallarının doğrulamasını kullanıcı yapmalıdır; statik analiz, test ve APK derlemesi bunların yerine geçmez.
