# WordQuest AI 1.0.75 — Onaylanan Görsel Varlık Politikası

Bu dosya, uygulama kodunun hangi görsel kaynaklara öncelik verdiğini sabitler. Ana uygulama sürüm numarası, imza ve kullanıcı verileri bu görsel işleme kapsamında değiştirilmez.

## Kaynakların mevcut durumu

- **GitHub'da mevcut:** `tools/v175/premium_webp/` altında 10 oda için 10 adet ikon ve 10 adet hero WebP; `tools/v175/generate_premium_icons_175.py` tarafından üretilen 46 iç ekran ikonu; önceki tasarımların Android/Compose bağlama betikleri.
- **GitHub'a yüklenen kullanıcı paketi:** `tools/v175/WordQuestAI_175_Gorsel_Entegrasyon_Hazirlik.zip` (9.060.166 bayt, SHA Git blob `bdfc2340cc9725c47d64343871df3fd753ae334b`). İçinde 108 bağımsız WebP ikon (102 ana + 6 özel) ve ilgili premium arka plan/banner/kart/sahne kaynakları var. `extra/` kesilmiş ikonları derlemeye alınmaz. Ayrı kaynak ZIP'leri tek tek depoya yüklemek gerekmiyor; bunlardan oluşturulan entegrasyon paketi kullanılıyor.
- Sonraki mesajda düşük kaliteli bulunduğu belirtilen ayrı ekran görselleri kullanıma alınmayacak. Mevcut onaylı özgün görseller korunacak.

## Derlemede kullanılan öncelik

1. GitHub'da bulunan, onaylı modül görselleri (Arena, YDS Akademi, Kelime Hafızam, Eşleştirmelerim, Günlük Planım, Dilbilgisi, Bulmacalarım, Dinle ve Yaz).
2. 46 adet kendine özgü mod/araç ikonu (özellikle Arena düello, takım ve 5 şıklı mod ikonları).
3. Gerçek varlık olmadığında kontrollü fallback. Kolajı tüm ekran görüntüsü olarak kullanma.

İlgili Kotlin noktaları:
- `WordQuest175Icons.kt`: `Wq175GlossIcon`, `Wq175IconNames.approvedResource`
- `WordQuest175ThemeArt.kt`: `Wq175ThemeArt.fromTitle`, `secondaryIcon`, `Wq175ThemeBanner`
- `Wq175PremiumIconRoutingTest.kt`: oda eşleştirme ve Arena özel ikon testleri
- `verify_theme_art_175.py`: APK hazırlığında görsel bağlantıların kontrolü

## Korunan kullanıcı kararları

- **Ana Sayfa:** Daha önce onaylanmış iki sütunlu yerleşim; oyunlar ve diğer modüller kaybolmaz.
- **Kelime Hafızam:** Onaylanmış oyun içi görünümü korunur; modül giriş ikonunda premium resim kullanılabilir.
- **Arena:** Mor-altın sahne; oyuncunun seçtiği maskot canlı ayrı katmanda kalır. Özel çapraz kılıç/düello tasarımı kupa ile değiştirilmez. Oyun bekleme mekaniklerine dokunulmaz.
- **YDS Akademi:** Ayrı diploma/kitap/akademi görsel kimliği.
- **Eşleştirmelerim:** Bulmacalarım içine gizlenmeyen bağımsız modül.
- **Lena:** Konuşma, ses, animasyon, ekler ve sohbet kodları görsel menü değişikliğinden etkilenmez.
- **Karakter kataloğu:** PDF'deki resimler henüz rigli animasyon değildir. Gereksiz mesleki/dini işaret eklenmez, Zeynep hariç tutulur.

## Yayınlama kuralı

Bu dal üstündeki commit, APK yayınlandığı anlamına gelmez. `v1.0.75` için Android CI testlerinin geçmesi, sürümün `versionCode=101` olması, imza sertifikasının mevcut yükseltme imzasıyla eşleşmesi ve gerçek cihaz QA kanıtı gerekir. Bunlar doğrulanmadan üretim güncellemesi olarak sunulmamalıdır.

## Yüksek çözünürlüklü paket için tek dosyalık geçiş

`WordQuestAI_175_Gorsel_Entegrasyon_Hazirlik.zip` dosyası aynı geliştirme dalında tam olarak `tools/v175/WordQuestAI_175_Gorsel_Entegrasyon_Hazirlik.zip` konumuna eklendiğinde `apply_theme_art_175.py`, `apply_uploaded_art_pack_175.py` yükleyicisini otomatik çağırır.

Yükleyici paketteki en az 102 bağımsız ikon dosyasını ve kullanılabilir premium arka planları `source/app/src/main/assets/wq175_user_art` altına kopyalar. `extra/` altındaki 108 düşük kaliteli kesilmiş ikon özellikle **dışarıda bırakılır**. Kullanıcının bağımsız ikonları varsa varsayılan yerleşik ikonlardan önceliklidir; eksik dosyada çalışan önceki görsel korunur. Paketi oluşturan dosya yüklenmiş ve doğru isimle depoda bulunmaktadır; derleme başarıyla tamamlanırsa yeni görseller APK'ya alınır.

Yüklenen ZIP yerel olarak açılabildi; dosya yapısı ve ikon isimleri incelendi. Çalıştırılabilir APK için GitHub Actions testleri ve gerçek cihaz doğrulaması ayrıca gereklidir.
