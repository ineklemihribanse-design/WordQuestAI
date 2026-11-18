# 1.0.27.1 — Avatar görünümü ve hareket kabul kriterleri

## Kullanıcı testinden doğrulanan hata

16 Kasım 2026 tarihinde gönderilen 1.0.27.1 beta ekran görüntüsünde **Sophia, mor saçlı/kedi kulaklı VRoid anime modeli** olarak görünüyordu. Kullanıcının referansı anime değil, Sims/ZEPETO tarzına yakın **yetişkin, doğal, hoş görünümlü, gerçek 3D insan öğretmen**. Eski `tools/v127/install_avatar_candidates.py` uygulama derlemesinde çağrıldığı için görsel referanslardan farklı dosyalar paketlendi. Bu hata artık gelecekteki taslak build tanımında engellenmelidir.

## Yeni gerçek insan modelleri — yalnızca deneme

| Uygulamadaki isim | 3D kaynak | Temel görünüm |
| --- | --- | --- |
| Emma | Avatar Studio / Rocketbox `olivia_smart.glb` | Kahverengi uzun saç, beyaz üst, mavi pantolon |
| Sophia | Avatar Studio / Rocketbox `sophia_formal.glb` | Koyu iş kıyafeti, doğal yüz |
| Ethan | Avatar Studio / Rocketbox `ethan_casual.glb` | Yetişkin erkek, gündelik kıyafet |

Kaynak commit: `2fd9ec82fbe1613d40ae38aadcca3f410725f740`. Tam Microsoft Rocketbox ve Avatar Studio MIT lisans bildirimleri korunacak.

**Not:** Güncel modeller fotoğraf gerçekçiliği yüksek. Kullanıcının yarı stilize Sims/ZEPETO referansına uygunluk henüz onaylanmadı. Bu taslak modellerin kurulumu/yayımlanması otomatik olarak onaylanmış sayılmamalı.

## Görsel ve hareket koşulları

- VRoid, anime saç/aksesuar, kedi kulakları, düşük kaliteli procedural manken ve T-pose yok.
- Her karakterde açıkça tanınan doğal insan yüzü, saç, ayakkabı, giyinik gövde, doğal cilt tonu; bozuk kıyafet birleşimi yok.
- Gerçek kamera testinde tam boy, bel, yakın yüz seçenekleri, pinch-zoom, orbit ve sıfırlama çalışmalı.
- Eski bütün vücut animasyonları korunmalı: `Idle`, `Talk`, `Walk`, `Wave`, `Think` dahil 13 klip.
- Bağımsız yüzde en az 52 ARKit morph hedefi. Yeni `Teacher_Viseme_A/E/I/O/U/MBP/NEUTRAL` ve `Teacher_Blink` klipleri yalnızca yüz morph'larını etkilemeli, iskeleti bozmamalı.
- Göz kırpma, konuşma, bekleme, dinleme, düşünme durumları ekranda gerçek hareketle eşleşmeli. Özellikle bir animasyonun başlaması öncekini bozmamalı.
- TTS `onStart`, `onRangeStart`, `onDone` ile durum ve ağız hareketi bağlantısı sağlanmalı. **Metin aralığı ile yönlendirilen ağız klibi fonem senkronizasyonu değildir**; gerçek fonem veya ses seviyesine göre doğrulanana kadar bu ifade kullanılmamalı.
- Cihazda 30 FPS hedefi, bellek/ekran-dışı kaynak serbest bırakma, mikrofon iptali, uygulama durdurma, karakter değişiminde eski sesin kesilmesi test edilmeli.

## Dağıtım kararları

- QA branch: `feature/1271-no-anime-human-teachers-qa`. Draft PR; ana dala birleştirilmeyecek.
- Yalnızca kaynak inceleme, Blender görsel QA, veri doğrulama ve **Kotlin derlemesi/JUnit testleri** yapılır.
- Bu dalda APK oluşturma, imzalama, yeni beta yayımlama, uygulama içi güncelleme gönderme **YOK**.
- Önceden yayımlanan isteğe bağlı anime beta, uygun kabul edilmiş yeni bir sürüm değildir; kullanıcıya yeni APK gönderilmez.
- Paket kimliği `com.kelimebulmacasi.a1.practice.offline101`, kullanıcı verisi ve mevcut US/UK ses ayarları korunmalıdır.

## Kalan açık işler

1. Yarı stilize yetişkin yüz/saç kalitesinin Sims/ZEPETO referansına gerçekten yaklaşması (mevcut Rocketbox foto-gerçekçi).
2. Android cihazında üç karakterin animasyon, ağız klibi/göz kırpma ve katman çakışmalarının görüntülü kabul testi.
3. Fonem doğruluğu veya en azından gerçek ses verisine dayalı ağız hareketlerinin bağımsız testi.
4. 3D GLB model yükleme bellek ve FPS testleri.
5. Gerekli estetik ve teknik onaylar alınmadan yeni APK yayımlamama.
