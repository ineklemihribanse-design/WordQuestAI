# WordQuest AI 1.0.75 — Onaylanan Görsel Varlık Politikası

Bu dosya, uygulama kodunun hangi görsel kaynaklara öncelik verdiğini sabitler. Ana uygulama sürüm numarası, imza ve kullanıcı verileri bu görsel işleme kapsamında değiştirilmez.

## Kaynakların mevcut durumu

- **GitHub'da mevcut:** `tools/v175/premium_webp/` altında 10 oda için 10 adet ikon ve 10 adet hero WebP; `tools/v175/generate_premium_icons_175.py` tarafından üretilen 46 iç ekran ikonu; önceki tasarımların Android/Compose bağlama betikleri.
- **Kullanıcının ayrıca sağladığı fakat GitHub'a henüz ikili olarak aktarılmamış paketler:** `WordQuestAI_Tum_Ikonlar_Bagimsiz_Yuksek_Cozunurluk_175.zip` (102 bağımsız ikon), `WordQuestAI_Tek_Tek_Yuksek_Kalite_Gorseller.zip`, `WordQuestAI_Premium_Gorsel_Paketi_175.zip`. Dosya adları burada görsel varlıkların kendisinin GitHub'a yüklendiği anlamına **gelmez**.
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
