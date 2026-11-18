# Ethan 1.0.40 — doğal konuşma ve jest kabul testi (cihaz kontrolü gerekli)

Bu değişiklik **mevcut 3D insan Ethan modelini değiştirmez**; aynı modelin kol eklem hareketlerini yumuşatır, geniş ağız pozlarını sınırlar ve gerçek Android TTS kelime-aralığı olaylarının dışındaki sahte ağız ritmini kaldırır.

## Cihazda mutlaka kontrol edilmesi gerekenler

- [ ] Mikrofonu açınca Ethan dinleme durumuna geçiyor, ağza konuşma hareketi gelmiyor.
- [ ] "Hello. How are you today?" yanıtında dudak kelime olaylarında açılıyor, ses bitince nötr oluyor.
- [ ] Art arda iki aynı sesliyle başlayan kelimede (ör. "apple, apple") her yeni gerçek kelime işaretinde yüz yeniden tetikleniyor.
- [ ] Telefonun TTS motoru `onRangeStart` vermiyorsa ağzın nötr kalması kabul edilir; yanlış zamanda sürekli açılıp kapanması kabul edilmez.
- [ ] "Why is this important? Please explain." gibi uzun bir yanıtta omuz-dirsek-el hareketleri eski animasyondan daha küçük ve sakin görünüyor.
- [ ] Tek kelimelik cevap, uzun cevap ve "Sesi kes" işlemlerinde modelin eli/yüzü garip biçimde donmuyor.
- [ ] 10 kez 3D ekranı aç/kapat, konuşmayı durdur/yeniden başlat: SIGABRT, siyah ekran ve uygulama açılış çökmesi yok.
- [ ] US/UK ses seçenekleri, Gemini/OpenAI, geçmiş, Ethan hafızası, Arena ve YDS değişmedi.

## Dürüst teknik sınırlama

Android `TextToSpeech.onRangeStart` sadece metin aralığı / kelime zamanını verir; fonem düzeyinde dudak-zaman hizalaması veya gerçek ses enerjisi sağlamaz. Dolayısıyla bu yama tam profesyonel lip-sync sağlamaz; gerçek kelime olaylarıyla uyumlu daha ölçülü görsel konuşma hedefler. Modelin fiziksel eklem hareketlerinin ne kadar doğal durduğu cihazdan video ile doğrulanmalıdır. Sessizlikte sürekli viseme üretimi yasaktır.

**Dağıtım:** 1.0.40 şimdilik QA ve imzasız derlemedir. 1.0.38 imzalı sürümün üzerine kurulum yayımlanmadan önce derleme, imza ve gerçek cihaz testi gereklidir. Güncelleme denemesi için kullanıcıdan uygulamayı silmesi ya da API anahtarını yenilemesi istenmemelidir.
