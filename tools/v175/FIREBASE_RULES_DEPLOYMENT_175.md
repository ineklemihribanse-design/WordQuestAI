# WordQuest AI 1.0.75 — Çevrimiçi 5 Şıklı Düello / Firebase geçişi

Bu dosya bir canlı dağıtım kaydı **değildir**.

Android kodunda 5, 10, 15, 20 soruluk yeni oda seçenekleri bulunmaktadır. Eski 12 soruluk odalar da desteklenir. Ancak hâlihazırdaki Firebase Realtime Database güvenlik kuralları yalnızca 12 soruluk odaları ve 0–11 numaralı cevapları kabul ediyor olabilir.

CI üzerindeki `tools/v175/apply_arena_firebase_rules_175.py` kodu `source/database.rules.json` dosyasını **geçici derleme dizininde** günceller. Bu işlem canlı Firebase veritabanına erişmez ve herhangi bir güvenlik kuralını yayımlamaz.

Yayın görevlisi, 1.0.75 güncellemesinden **önce** ilgili Firebase projesinin Realtime Database > Rules bölümünde bu dosyayı kontrollü şekilde yayımlamalı ve Firebase Emulator Suite ya da iki ayrı test hesabıyla 5 / 10 / 12 / 15 / 20 soru sınırlarını ve yetkisiz erişimin engellendiğini test etmelidir.

**Üretim kuralı:** Gerçek iki hesaplı Arena testinden ve dağıtılmış sunucu kuralı kanıtından önce 1.0.75 otomatik güncellemesi yayımlanmaz. Eski kullanıcı profil kayıtlarına ve Firebase verisine doğrudan müdahale edilmez.
