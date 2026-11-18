# WordQuest AI — GitHub Releases otomatik güncelleme (v1.0.16)

Bu proje mevcut Kotlin/Compose WordQuest AI'ye eklenmiştir; sıfırdan yeniden oluşturulmamıştır. Android uygulaması `com.kelimebulmacasi.a1.practice.offline101` paketini kullanır. Sürüm kodu 39'dur; minSdk 26, targetSdk 35. Eski kullanıcının hesapları, oyun verileri ve ayarları korunur. Android'in kendi kurulum onayı **zorunludur**.

## 1. İki ayrı depo neden gerekli?

**Özel kaynak kodu:** `mihribansezik5-lgtm/WordQuestAI` (zaten var, private). Burada kaynaklar ve GitHub Actions çalışır. İmzalama anahtarı bu depoya dosya olarak konulmaz.

**Herkese açık indirme:** `mihribansezik5-lgtm/WordQuestAI-Updates` (**bunu sen oluşturmalısın; hazır olduğunu varsayma**). Sadece `WordQuestAI.apk` ve `update.json` Release ekleri; ayrıca normal GitHub README tutulur. Bu deponun **Public** olması, uygulamanın kimlik doğrulama anahtarı olmadan güncelleme indirmesini sağlar. Herkes yayımlanan APK dosyasını indirebilir; kaynak kodu ve parolalar yayımlanmaz. Özel bir güncelleme deposu kullanmak istenirse APK içine GitHub token'ı gömülemez; izinli bir kimlik doğrulama sunucusu veya güvenli zaman sınırlı indirme aracısı gerekir; bu kurulum ücretsiz kamuya açık Release modelini seçer.

Ücretsiz kota ve kullanım sınırları GitHub planına bağlıdır; özel repo Actions dakikaları sınırlıdır. Ücretsiz sunucu satın alınması gerekmez.

## 2. GitHub'da ilk kez yapacakların (bir defalık)

1. https://github.com/new adresini aç. Owner `mihribansezik5-lgtm`, Repository name `WordQuestAI-Updates`, Visibility `Public`; "Add README" seç ve oluştur. Deponun adı **harf harf aynı** olmalı, yoksa `UpdateProtocol.kt` sabitini ve Actions `UPDATE_REPO` alanını birlikte değiştirmen gerekir.
2. https://github.com/settings/personal-access-tokens adresinden **Fine-grained personal access token** oluştur. Repository access **Only select repositories** → **WordQuestAI-Updates**; permission **Contents: Read and write**. Başka depo yetkisi verme. Token'ı hiçbir APK'ya, sohbet mesajına veya kaynak koduna koyma.
3. Özel kaynak deposunda `Settings → Secrets and variables → Actions → New repository secret` bölümüne şu 5 sır yükle:
   - `WQ_UPDATES_TOKEN` = önceki adımın token değeri.
   - `WQ_RELEASE_KEYSTORE_B64` = **önceden kullandığın aynı orijinal PKCS#12 (.p12) imzalama dosyasının base64 içeriği** (örneğin yerel makinede `base64 -w0 key.p12`, macOS'te `base64 < key.p12 | tr -d '\n'`). **Anahtar dosyasını Git geçmişine veya kamuya açık depoya koyma.**
   - `WQ_UPGRADE_STORE_PASSWORD` = orijinal keystore parolası.
   - `WQ_UPGRADE_KEY_ALIAS` = orijinal anahtar takma adı (`wordquest-v082` sertifika yedeğinde doğrulandı).
   - `WQ_UPGRADE_KEY_PASSWORD` = orijinal özel anahtar parolası.
4. Bu kaynak güncellemesi (`v1.0.16`) ve yeni `.github/workflows/*.yml` dosyaları GitHub özel depoda `main` dalına geldikten sonra `Actions → Build WordQuest AI v1.0.16 GitHub Updater APK` iş akışını çalıştır. PR üzerine çalıştırılan build'lerin yayımlanmaması güvenlik için kasıtlıdır.
5. Başarılı build çalışmasının **Run ID** numarasını açtığın Actions sayfasından kopyala. `Actions → Publish signed APK to public Updates Releases → Run workflow` bölümünde `source_run_id` alanına bu değeri yaz. İmzalı APK ve `update.json` public Release `v1.0.16` altında oluşmalı.
6. https://github.com/mihribansezik5-lgtm/WordQuestAI-Updates/releases adresinde `v1.0.16` Release altında **iki dosya** olduğunu kontrol et. **Buradaki yayın başarıyla yapılmadıkça uygulamaya yeni sürüm bulma garantisi yoktur.**

## 3. İlk APK'nın kurulması

Yeni güncelleme istemcisi cihazında bulunmadığı için **v1.0.16 APK'nın yalnızca bir kez normal Android yükleme ekranından kurulması gerekir**. Aynı package name / imzalama sertifikası / daha yüksek versionCode korunduğundan önceki v1.0.15'in üzerine kurulur. Kurulum sırasında mevcut uygulamayı kaldırma. Güncellemeden sonra `Ayarlar → Güncellemeler` bölümünü aç. İlk kontrolde Release yoksa hata mesajı görünür; uygulamanın geri kalanı çalışmaya devam eder.

## 4. Sonraki sürümler (v1.0.17, v1.0.18…)

1. Mevcut özel kaynak projeyi güncelle; `app/build.gradle.kts` içindeki `versionCode` değerini **mutlaka artır**, `versionName` değerini `1.0.17-...` gibi yeni sürüme ayarla.
2. `RELEASE_NOTES.md` değişikliklerini güncelle; özel GitHub deposundaki `main` dalına değişiklikleri aktar.
3. `build-apk.yml` **main push** build'ini çalıştırır. Başarılı `main` build'inden sonra `publish-updates.yml` otomatik başlar, özel anahtarla APK'yı imzalar, paket/imza/version kontrol eder, `update.json` üretir, APK ile Release oluşturur.
4. Başarılı yayımlamadan sonra `tools/prune_public_releases.py` yalnızca en yeni **3 tam APK Release'ini** tutar. Git geçmişini veya Git tag'lerini **silmez**. Haftalık `cleanup-updates.yml` ek güvenlik için tekrar denetler.
5. Kullanıcının telefonundaki uygulama açıldığında en fazla **12 saatte bir** sürüm kontrol eder. Kullanıcı istediğinde Ayarlar'dan zorlayabilir. Güncelleme HTTPS ile indirilir, boyutu ve SHA-256 kontrol edilir, aynı paket/sürüm/imza teyit edilir, Android kurulum onayı açılır.

**Not:** Her yeni versionName için build workflow'unun görünen adı `Build WordQuest AI v1.0.16 GitHub Updater APK` olarak kalmalı veya `publish-updates.yml` içindeki `workflow_run.workflows` adıyla birlikte güncellenmeli. APK artefakt adı `WordQuestAI-Unsigned-APK` sabit kalmalıdır.

## 5. Güvenlik ve depolama

- İmzalama anahtarı, şifreler ve GitHub token uygulamanın koduna veya APK'ya gömülmez.
- GitHub Releases API ve indirmeler HTTPS kullanır. Release SHA-256'sı, boyut, paket adı, yeni sürüm kodu ve cihazdaki imza sertifikası karşılaştırılır.
- Başarısız veya yarım kalan `.part` dosyaları silinir. Geçerli APK dosyası kurulum iptal edildiyse önbellekte bir kez saklanır; tekrar kullanılmadan önce tekrar doğrulanır.
- Güncelleme başarıyla cihazda açıldığında önceki APK dosyaları uygulamanın özel `cacheDir/updates` dizininden silinir; mevcut Firebase, veri dosyaları ve tercihler silinmez.
- Cihaz işletim sisteminin `Bilinmeyen uygulama yükleme` izni ve kurulum onayı gerekebilir. **Tamamen sessiz yükleme yoktur.**
- Uygulamanın paket kimliği değişmez: hiçbir ikinci uygulama oluşturulmaz.
- Bazı cihazlarda APK sertifikası ön doğrulaması yapılamazsa sistem kuruluma başlamaz; bu bilinçli olarak kapalı güvenlik kuralıdır. Android Package Installer ayrıca kontrol eder.

## 6. Ne test edildi, ne edilmedi?

Otomatik testler: sürüm karşılaştırma, yeni sürüm bulunması/bulunmaması, geçersiz paket kimliği, hatalı SHA veya eksik boyut, eksik Release asset'i, HTTPS host doğrulaması; temizleme otomasyonunda en yeni 3 sürümün korunması. CI derlemesi `:app:testOfflineUnitTest :app:assembleOffline` ile doğrulanır.

**Canlı cihaz testleri** ayrıca yapılmalı: Android 8/12/14/16 kurulum izni ve iptal akışı, 95 MB indirmede internet kopması, gerçek SHA doğrulaması, sonrasında veri koruma, başarısız imza, gerçek GitHub Release ile iki art arda güncelleme. Bu testler cihaz ve public Release olmadan doğrulanmış sayılmaz.

## 7. Hızlı bağlantılar

- Kaynak Actions: https://github.com/mihribansezik5-lgtm/WordQuestAI/actions
- Kaynak Secrets: https://github.com/mihribansezik5-lgtm/WordQuestAI/settings/secrets/actions
- Public updates repository (kullanıcı oluşturmalı): https://github.com/mihribansezik5-lgtm/WordQuestAI-Updates
- Releases: https://github.com/mihribansezik5-lgtm/WordQuestAI-Updates/releases