# KELİME HAFIZAM — ONAYLANMIŞ GÖRSEL REFERANS VE UYGULAMA SÖZLEŞMESİ

Durum: TASARIM KİLİTLİ / uygulanması bekleniyor.
Referans: Kullanıcının 19 Kasım 2026 tarihinde gönderdiği tek ekranlık Kelime Hafızam telefon görseli (sohbetteki görsel). Önceki alternatif tasarımlar geçersizdir.

## Kapsam ve kesin kurallar
- SADECE Kelime Hafızam ve onun alt çalışma sayfaları. Uygulamanın mevcut Ana Sayfasını değiştirme.
- Oyun Odası ayrı bir bölüm olarak kalacak.
- Tasarım yeniden yorumlanmayacak. Aynı açık buz mavisi-beyaz palet, lacivert tipografi, yumuşak mavi gölgeler, kartlar ve ikon hiyerarşisi korunacak.
- Görseli tam ekran resim olarak koyma: tüm metin, istatistik, buton, sekme, kart ve durumlar gerçek erişilebilir Compose bileşenleri olacak.
- Kullanıcı verilerini ve çalışan akışları koru. Örnek görseldeki 248/19/136/24 gibi sayıları sabit kodlama.
- Robot her sayfada zorunlu değil. Kelime Hafızam üst bölümünde kitabı tutan mavi-beyaz WQ robotu görünür.
- Robotun görsel kimliğini değiştirme; basit geometrik Canvas çizimi referans robotun yerine geçemez.

## Kelime Hafızam mobil ana ekranı — görseldeki sıra
1. Üstte WordQuest AI logosu/başlığı, bildirim ve ayarlar; telefonun sistem durum çubuğu ile çakışmayacak.
2. Sol tarafta büyük iki satırlı “Kelime Hafızam”, altında kısa açıklama. Sağda kitap tutan, gülümseyen, mavi-beyaz robot; başlıkla dengeli aynı hero alanında.
3. Tek beyaz istatistik paneli: Kelimem / Tekrar / Öğrenilen, ince dikey ayırıcılarla.
4. “Bugünkü Tekrarın”: takvim ikonu, bekleyen kelime sayısı, ilerleme halkası ve belirgin mavi “Tekrara Başla” butonu.
5. “Çalışma Modları”: 2x2 kompakt kartlar — Kartlarla Çalış, Yazarak Öğren, Tür Eşleştir, Karma Antrenman.
6. “Akıllı Koleksiyonlar”: Favorilerim, Öğrendiklerim, Zorlandıklarım; gerçek sayılarla.
7. Alt gezinme mevcut uygulamanın navigasyon yapısına bağlanmalı; ayrı, işlevsiz ikinci navigasyon üretme.

## Görsel ayrıntılar
- Düz beyaz kart yüzeyleri, yumuşak mavi kenar/gölge, geniş ama ölçülü boşluk.
- Her öğe küçük telefonlarda taşmadan okunacak; tablette devasa kartlar olmayacak.
- Neon parlamaları ve koyu lacivert blokları geri getirme.
- Çalışma kartları gerçek kart destesi hissi verecek; arka/ön yüz, çevirme ve öğrenme dereceleri erişilebilir.
- “only if” gibi çok kelimeli ifadelerde boşlukları koru, kelime sayısını göster, birleşik yanlış yazımı doğru kabul etme.

## Robot teknik kabul kriterleri
1. Ayrı asset/model parçaları: baş kabuğu, yüz ekranı, gözler, boyun, gövde, omuzlar, üst/alt kollar, eller, kitap.
2. Pivot/rig: baş dönüşü, omuz/dirsek/bilek hareketleri, göz bakışı, blink, nefes/idle, selamlama.
3. Kitap ve eller hareket ederken birbirinden kopmamalı. Boyun, göz ve omuzda kayma/titreme olmamalı.
4. Robot görünümü referans görselle tutarlı; düz çizimle geçiştirilmemeli.
5. Hareket azaltma ayarına saygı, performans ve küçük ekran testi.
6. Yalnızca kodda animasyon parametreleri bulunması “rig tamamlandı” anlamına gelmez. Android cihazda doğrula.

## Mevcut durum (19 Kasım 2026)
- tools/v148/VocabularyNeonHome.kt içinde ProfileRobotPortrait Kotlin Canvas ile oluşturuluyor; sinüs tabanlı kol/baş salınımı ve blink bulunuyor.
- Bu robot görsel referansın model kalitesine ulaşmış değil; parçalı 2.5D rig asset'i doğrulanmadı.
- Depoda tools/v1271/AvatarMotionCore.kt ve AvatarFacialLayer.kt mevcut, ancak bunlar Kelime Hafızam'daki robotun bitmiş rig modeli olarak kabul edilmemeli.
- Uygulama içi son tasarımın referans görselle birebir eşleştiği henüz test edilmedi.
- Her küçük commit için APK derleme yok: değişiklikleri biriktir, sonunda tek toplu Android derlemesi ve cihaz doğrulaması yap.

## Doğrulama
Görsel karşılaştırma: kullanıcının sohbette verdiği referansla yan yana telefon ekran görüntüsü.
İşlev: geri, profil, tekrar başlat, dört çalışma modu, koleksiyonlar, kart çevirme ve öğrenme kayıtları.
Rig: en az idle, blink, selam, kitap tutma ve ifade geçişleri cihazda.
