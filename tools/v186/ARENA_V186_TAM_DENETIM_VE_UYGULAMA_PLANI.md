# WORDQUEST AI 1.0.86 — ARENA, TAHTA, TUR AKISI VE ARKADAS PROFILI
Bu bir gelistirme plani ve test kapisidir. 1.0.85 yerine yayinlanmis APK degildir. Canli Firebase kurallari bu dal tarafindan degistirilmez.

## Bulgular (22.11.2026 ekran goruntuleri ve Kotlin kaynaklari)
P0: ArenaBoardV2 klavye acikken compact=250dp yukseklik sinirini uyguluyor; hucre boyutu da yukseklikten hesaplandigindan tahta okunamayacak kadar kuculuyor.
P0: Sabit kesisim harfleri, ipucu harfi ve taslak metin tek (row,column) koordinatina dayanarak birlestirilmeli; farkli slotlarda gorsel kayma engellenmeli.
P0: Cift submit, ayni anda pas/ipucu, tur surenin bitmesi, iki oyuncunun da tum kelimeleri kacirmasi, replay ve baglanti kaybindan sonra ilerleyis kilitlenmemeli.
P1: Sadece tahta sığdırma degil buyutme, iki yonde kaydirma ve secilen kelimeyi merkeze getirme gerekli. IME'de hucre boyutu ayni kalmali; cevap+gonder+ipucu gorunur olmali.
P1: Ipuclari kalan hak ve sunucu yetkisi varsa secilen her uygun kelimede erisilebilir; tekrar ipucu ayni harf icin hak harcamamali.
P1: Ayarlar, lobi, skor ve sira bilgisi tekrarlanmadan tek gorunur alanda tutulmali.
P2: Arkadas kartinda Profili incele; buyuk maskot, izinli puan, oyun sayisi ve son etkinlik. Kaynak ZIP, e-posta, gizli veri paylasma.

## Teknoloji kararlari
1. Hucre genisligi ekranin yatay boyutuna veya zoom'a bagli hesaplanacak; klavye acilinca hucre boyutu degismeyecek, sadece tahta goruntuleme alani kisalabilecek.
2. Dar telefon varsayilan yakin gorunum ve tek pan/zoom viewport. Tablet tam tahta + istege bagli zoom. Secili kelime otomatik gorunur bolgeye kaydirilacak.
3. Letter display precedence: SOLVED(cell) > HINT(cell) > DRAFT(cell). Tek kaynak: Cell(row,column), immutable secim kimligi; staleness testi.
4. IME: WindowInsets.ime / imePadding; board ve cevap paneli birbirinden bagimsiz, klavye ustunde gorunur cevap+gonder+ipucu; geri tusu once IME'yi kapatir.
5. Sunucu bazli state machine: LOBBY, READY, TURN_ACTIVE, TURN_SUBMITTING, TURN_TRANSITION, REPLAY, FINISHED. CLIENT local timer yalniz gosterim icin.
6. Her submit/pas/hint bir idempotent action id ve sunucu revision kontroluyle bir defa uygulanir; reconnect'te gercek room state yeniden yuklenir.
7. Word states: UNTRIED, SOLVED, FAILED, PASSED, WAITING_REPLAY, REPLAY_AVAILABLE, REPLAY_SPENT. Tum kelimeler hesaplandiginda erisimli bir tur/sonuc durumu saglanir.
8. Ipuclari: hamle yetkisi/limit server dogrular; secili kelimede zaten acilan harf yeniden harcanmaz; replay ve multiplayer hakkinda emülatör test zorunlu.
9. Arkadas detaylari sadece karsilikli arkadaslik ve yerel paylasim izniyle gosteren ozet veridir. Tam maskot ZIP'leri telefon disina cikmaz.
10. Firebase kurallarini gevsetme yok. Gerekirse sadece minimal fark ve yerel rules-unit-testing.

## Test matrisi — yayin kosulu
- Telefon 320/360/393/420 dp ve tablet 7/10/13 inc; dikey/yatay, font %100/%150, ime acik/kapali, ekran cevirme.
- Izgara 7/10/15/20 sutun; fit/zoom/kaydirma ve secili kelime; input taslagi acik kesisim ve ipucu harfleriyle ayni koordinatta.
- Yanlis/dogru/tekrar cevap, bos cevap, emoji, klavye geri tusu, sirada degilken yanit, gec gelen Firebase snapshot.
- 2 kisi ayni anda Submit, Pass, Hint; reconnect, baglanti yok, sunucu onay gecikmesi, timeout, host/guest cikisi.
- Her ikisi pas gecmis, son sans aktif, son sans bitmis, tum kelimeler cevaplanmis, tum kelimeler tuketilmis, sonsuz sure modu.
- Arkadas onayli/onaysiz, paylasim izni acik/kapali, arkadas silme, profil gorunumu, kisisel maskot silme.
- 1.0.85 -> 1.0.86 mevcut imza/paket/veri korunarak kurulumu ve gercek iki cihaz manuel kontrolu.

## Kabul
Hucreler IME acilinca kuculmeyecek; metin hucreden kaymayacak. Cevap butonu ve ipucu klavye acikken erisilebilir. Her tur bitis yolu FINISHED veya yetkili baska aktif hamleye ulasacak. Profil detayinda buyuk maskot olacak. Gecerli APK ve canlı oyun testi olmadan yayim yapilmayacak.

## Sıra
A) Responsive Board + Friend Profile; B) Sticky IME answer panel; C) authoritative turn/replay state machine; D) all hints; E) device/emulator release gate. Firebase ve kullanici verisi silinmeyecek.
