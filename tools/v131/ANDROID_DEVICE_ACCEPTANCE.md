# WordQuest AI 1.0.31 — Android 3D cihaz kabul testi

**Durum:** QA adayı. GitHub Kotlin/JVM derlemesinin başarılı olması gerçek Android GPU testinin yerine geçmez. Emma/Sophia çökme nedeni cihaz hata kaydı görülmeden kesinleşmiş değildir. Bu dalı yayımlamayın/ana dala birleştirmeyin.

## Test ortamı
- Android sürümü, cihaz modeli, kullanılabilir RAM, güncel uygulama sürümü (1.0.31 code56)
- Sahne: gerçek Rocketbox GLB; :teacher3d ayrı işlem; Gemini ve kayıtlı anahtarlar ana uygulamada kalır
- Cihazda önce 1.0.30'un üzerinde uygulamanın açılmasını ve önceki sohbetlerin korunmasını kontrol edin

## Tekrarlanabilir testler
- [ ] Emma 10 kez aç → 3D sahne oluştu → Idle başladı → Kapat. Siyah ekran/çökme/ANR olmadı
- [ ] Sophia 10 kez aynı döngü, aynı kriter
- [ ] Ethan 10 kez aynı döngü, aynı kriter
- [ ] Öğretmen değiştirip her birini açma döngüsünü en az 10 kez tekrarla; eski ses ve GPU kaynakları beklenmedik şekilde kalmasın
- [ ] Her model: Tam Boy / Bel / Yüz, orbit, parmakla yakınlaştırma ve sıfırlama; baş/ayak kesilmesi yok
- [ ] Her model: doğal kol ve omuz konumu, dengeli boy ve bacak oranları, temiz saç/deri materyalleri
- [ ] Her model: Idle animasyonunu başlat/durdur; animasyon öncesi/sonrası donma yok
- [ ] 30 FPS hedefi, gerçek GPU bellek ve PSS ölçümü, ilk kare süresi kaydedildi
- [ ] Uygulama kaldırılmadan güncelleme yapılabildi; Android imzası/US-UK sesleri/Gemini/sohbetler korundu
- [ ] Canlı 3D içinde ses/TTS ve dudak senkronizasyonu: **Henüz geliştirilmedi — bu kutu geçilmiş sayılmaz**
- [ ] Sims/ZEPETO'ya yakın yarı stilize yüz/kalite: **Henüz görsel kabul yapılmadı**

## Cihaz hata kaydı (ADB erişimi varsa)
    adb devices
    adb logcat -c
    # Telefonda: uygulama > konuşma asistanı > Emma/Sophia/Ethan > Gerçek 3D modeli dene
    adb logcat -d -v threadtime -s WQ3DProbe AndroidRuntime DEBUG libc > wordquest_3d_diagnostic.txt
    adb shell dumpsys meminfo com.kelimebulmacasi.a1.practice.offline101:teacher3d

WQ3DProbe ilk kare ms, PSS ve graphics bellek istatistiğini raporlar. Uygulama ApplicationExitInfo ile son yerel çökme sinyalini gösterir. Bütün sistem loglarını veya sohbet/API anahtarlarını paylaşmayın; gerekirse tanımlayıcıları gizleyin.

## Yayım kapısı
Üç karakter için 10/10 başarılı tekrar, görünür doğal vücut oranı, kalıcı animasyon, GPU FPS/bellek ölçümleri ve gerçek cihazda canlı sesli 3D deneyimi doğrulanmadan sürüm **kararlı yayımlanamaz**.
