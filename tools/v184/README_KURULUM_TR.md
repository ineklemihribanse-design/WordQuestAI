# WORDQUEST AI 1.0.84 — Maskot, Kupa Pozu ve Arena Görsel Yaması

Bu ZIP gerçek proje için **kaynak kodu entegrasyon paketidir**; APK değildir.

## İçerik

- `assets/user_uploads/WordQuestAI_Arena_13inc_Tablet_ve_Telefon.zip`: Kullanıcının onayladığı tablet/telefon Arena arka planı, SHA-256 kontrolü birebir.
- `mascot_packs/WordQuestAI_Kiz_Idle_Kupa_v184.zip`: Kız maskotu, `idle` ve `win` görselleri.
- `mascot_packs/WordQuestAI_Erkek_Idle_Kupa_v184.zip`: Erkek maskotu, `idle` ve `win` görselleri; erkek biraz daha uzun.
- `tools/v184/apply_visuals_184.py`: 1.0.83 kaynak ağacına 1.0.84 kod değişikliklerini uygular.
- `tools/v184/Wq184BitmapTrim.kt`: Yerel maskotun alfa boşluklarını **bellekte** kırpar; orijinal görsele/yüze dokunmaz.
- `tools/v184/verify_assets_184.py`: Orijinal Arena SHA'sını, mobil/tablet çözünürlükleri ile her ZIP'in bütünlüğünü ve gerçek şeffaflığını denetler.

## Kurulum

1. GitHub'da `ineklemihribanse-design/WordQuestAI` kaynak ağacını `feature/v183-quality-stability` tabanından kullanın.
2. Kaynağı 1.0.83 için mevcut `tools/v182/apply_ui_182.py`, `tools/v183/apply_quality_183.py` ve `tools/v183/apply_home_183.py` adımlarından geçirerek `source/` klasörünü oluşturun.
3. **Bu arşivin içini**, aynı depo köküne açın. Mevcut kullanıcı verilerini, Firebase kurallarını veya eski dosyalarını silmeyin. Örnek maske ZIP'lerini uygulama kaynak `assets/` içine doğrudan kopyalamayın: onlar kullanıcı tarafından Kişisel Maskotlarım ekranından ayrı ayrı yüklenecek.
4. Kaynak kodunu ve Arena görsellerini uygulamak için depo kökünde `python3 tools/v184/apply_visuals_184.py` çalıştırın.
5. Doğrulama için `python3 tools/v184/verify_assets_184.py` çalıştırın (`Pillow` gerektirir). `python3 -m py_compile tools/v184/apply_visuals_184.py` ile betiğin sözdizimini doğrulayın.
6. GitHub Actions CI içerisinde Gradle JVM testlerini ve `assembleOffline` adımını **1.0.84/versionCode 110** sürümüyle çalıştırın. Önce test APK'sını mevcut uygulamanın üzerine veri kaybetmeden kurarak ekranları kontrol edin. İmza ve paket kimliği aynı kalmalıdır.
7. **Kız ve erkek maskotları ayrı ZIP'ler** olarak telefona indirilip uygulamada Hesabım > Kişisel Maskotlarım > ZIP dosyası seç ile ayrı ayrı yüklenmeli ve eski paketlerin yerine yeni oluşturulanlar seçilmelidir. Önceki ZIP'ler kendiliğinden değiştirilmez.

## Gerçek kodda neden sorun oluştu?

- `WordQuest2DPicker.kt` kazanma pozu alanını 125 dp yüksekliğinde çiziyordu. Kişisel 1024x1536 tam-boy resim için bu çok küçüktü. Yeni yükseklik 265 dp.
- `Wq183MascotStore.picture()` resimleri her seferinde iki kat küçülterek çözümlüyordu. Artık orijinal detay ve bellekte alfa kenar kırpması korunur.
- Kelime Hafızam afişi 163/220 dp **kare** portreyi kullanıyordu, böylece uzun figürün yatay genişliği çok küçülüyordu. Artık dikey dikdörtgen görüntü alanı var.
- Arena afişinde maskot kutusu 163 × 208 dp idi, özel maskot için 176 × 327 / 265 × 393 dp olur.
- `Wq183ArenaBackdrop` dosyaları gerçek ZIP'de verilmemişse eski yedeğe düşüyordu. Yama, görsel eksikse derleme öncesinde açık hata vererek bu sessiz düşüşü önler.
- Özel maskotun `win.png` yüklenip yüklenmediği profilde görünmüyordu. Artık profil yönetiminde zafer önizlemesi ve dosya durumu gösterilir.
- Maç sonucunda kazanan maskotunun `victory=true` olarak çizilmesi için çevrimiçi oyuncu şeridine bitti bilgisi iletilir.

## Güvenlik ve sınırlar

- **Firebase, SQLite, SharedPreferences ve kullanıcı kaydedilmiş maskotları silinmez.**
- Yalnızca orijinal Arena ZIP'in önceden bilinen SHA-256 değeri kabul edilir.
- ZIP dosyaları `manifest.json`, `images/idle.png`, `images/win.png` içerir; kodun izin verdiği yolların dışına çıkmaz.
- Kişisel görseller tam çözünürlükte sadece yerel depolamada kalır; Arena rakibine paylaşım özel küçük önizleme ve kullanıcının iznine bağlıdır.

## Doğrulama durumunun sınırı

PNG/ZIP/arşiv kontratları yerel olarak test edilmiştir. **Android Gradle derlemesi, gerçek cihaz üzerinde görsel test, firebase çoklu oyuncu ve APK üretimi henüz gerçekleştirilmemiştir.** Bu işler başarılı olmadan sürümü yayınlanmış saymayın.
