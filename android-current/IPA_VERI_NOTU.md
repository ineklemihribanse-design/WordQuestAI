# WordQuest AI 0.8.0 IPA veri kaynağı

- En-US/en-GB ses kayıtları: 2.203 sözcük/ifade, 4.406 farklı Ogg dosyası.
- `app/src/main/assets/pronunciation/ipa.json`: 2.184 sözcük/ifade için iki ayrı IPA dökümü. Bir sözcüğün anlamı belirtilmeden kesin telaffuzunun seçilemediği 19 girdi ayrı `unresolved` alanında tutulur.
- IPA kayıtları, **OGG'leri seslendiren aynı eSpeak US/GB profillerinin fonem çıktısından otomatik türetilmiştir**. İnsan denetimli, evrensel yetkili sözlük veya fonetik uzman doğrulaması olarak sunulamaz. Bu bir kısıtlamadır.
- US tarafında CMU Pronouncing Dictionary 0.7b, kelimenin sözlükte bulunup bulunmadığını ve olası birden fazla okunuşu saptamak için kullanılmıştır (sözlüğü kopyalamıyoruz). UK transkripsiyonları ayrı eSpeak en-gb modeline dayalıdır.
- Anlam ve dilbilgisel göreve göre farklı seslendirilebilen sözcükler için uygulama belirsizlik uyarısı gösterir. Başka kaynaklardan doğrulanmamış IPA üretmez.
- Türkçe yaklaşık okunuş yalnızca elle seçilmiş beş tanıdık sözcükte bulunur; ayrı aç/kapa ayarı vardır ve IPA'nın yerine geçmez.
- Yeni IPA tercihleri mevcut profil-bazlı `wordquest_preferences` içine geriye uyumlu şekilde eklenir; veri tabanı silinmez.
- `python3 tools/verify_audio_ipa.py` yerel veri tutarlılığını ve US/GB ayrımını statik denetler. Seslerin kulağa nasıl geldiği veya APK'nın açıldığı hakkında kanıt değildir.
