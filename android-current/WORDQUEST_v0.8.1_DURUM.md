# WordQuest AI v0.8.1 — kaynak geliştirme paketi

**Derlenmiş bir v0.8.1 APK değildir.** Android SDK/Gradle bu çalışma ortamında
bulunmadığından gerçek derleme ve cihaz testi yapılamadı. Mevcut v0.8.0 APK'yı
v0.8.1 olarak yeniden adlandırıp sunmayın.

## Yapılan değişiklikler

- `versionName` 0.8.1, `versionCode` 14.
- Android Studio'da açılabilir Kotlin/Compose kaynak projesi.
- Varsayılan ses modu: Android'in TTS motorunun gerçekten kurulu olduğunu
  bildirdiği, aksanı tam olarak en-US veya en-GB olan en kaliteli ses.
- Otomatik seçim veya Amerikan ve Britanya için ayrı cihaz sesi seçimi;
  seçimler her öğrenci profili için ayrı saklanır.
- Çevrimiçi sesler varsayılan olarak kapalıdır. Cihazın mevcut motorunun
  internet gerektiren seslerini kullanmak ancak isteğe bağlı olarak açılır.
  Uygulamaya ücretli TTS API'si veya abonelik eklenmedi.
- Motor uygun ses bulamazsa karşı aksana geçmek yerine aynı aksanın 0.8.0
  çevrimdışı eSpeak kaydını kullanır; kullanıcıya durum açıkça bildirilir.
- Tam 4.406 eski OGG kayıt, 2.203 ifade ve 2.184 IPA çifti korundu.
- Ses ayarları; bulmacalar, kelime defteri, yarışma ekranı, AI öğretmeni
  yanıtları ve IPA ses düğmelerine bağlandı. Mevcut diğer modüller silinmedi.

## Önemli kalite sınırı

Bu sürümde **yeni bir neural TTS/gerçek insan kaydı oluşturulmadı.** Cihazın
kurulu TTS sesinin daha iyi duyulup duyulmayacağı telefona bağlıdır;
kadın/erkek veya insan sesi kalitesi garanti edilemez. Eski eSpeak kayıtları
halen robotiktir. Asıl hedeflenen doğal kadın/erkek US/UK kayıtları, özgür
lisanslı doğal TTS modeli ve araçları erişilebilir olduğunda yeniden
üretilmelidir; burada o modele ve Android build araçlarına erişim yok.

## Derleme ve eski sürüme güncelleme

Android Studio: proje kök klasörünü açıp `app` modülünü derleyin.
`offline` varyantı v0.8.0'daki aynı paket kimliğini kullanır fakat farklı
imza anahtarıyla üretilirse mevcut uygulamanın üzerine güncelleme olmaz.
Eski kullanıcının verisini korumak için aynı imza anahtarı ve paket kimliği
zorunludur. Gizli anahtarlar ZIP'e konmadı.

Testleri otomatik çalıştırmak için gerekli görevler:
`./gradlew :app:testOfflineUnitTest :app:assembleOffline`

Kaynak statik kontrolü: `python tools/check_v081.py` ve
`python tools/verify_audio_ipa.py`.
