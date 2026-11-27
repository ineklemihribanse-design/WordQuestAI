# WordQuest AI 1.0.158 — HD arkadaş maskotu kurulumu

APK yayınlandı. HD alanı isteğe bağlıdır; eski avatarPreview alanı ve paylaşım izni korunur.

## Canlı kuralı değiştirmeden önce

1. Firebase Console > wordquest-ai > Realtime Database > Rules sayfasındaki güncel JSON'u dışarı aktar ve yedekle.
2. Depodaki eski tam rules dosyasını canlı kuralların üzerine yapıştırma.
3. Mevcut canlı dosyadan yalnızca yeni friendMascotHd düğümünü üreten araç:
   python3 tools/v132_engine/prepare_friend_hd_rules.py --live live-rules.json --output merged-rules.json
4. Araç mevcut karşılıklı arkadaş okuma/sahip yazma sözleşmesi farklıysa durur. Çıktı ile canlı yedeğin tek farkının rules.friendMascotHd olması gerekir.
5. Firebase emülatöründe yetkilendirme ve sınır testleri başarılı olmadan canlıya yayınlama.
6. İki ayrı hesapla izin açık/kapalı, arkadaşlıktan çıkarma, maskot değişimi ve eski sürüm ile uyumluluğu kontrol et.

HD önizleme en fazla 1024 piksel kenar ve 120.000 ham bayt; yalnızca profil açılırken okunur. Orijinal ZIP paylaşılmaz. Düşük çözünürlüklü kaynak büyütülmez. Yeni kural yoksa uygulama küçük önizlemeye döner.

Bu doküman canlı Firebase kurallarının değiştirildiği anlamına gelmez.
