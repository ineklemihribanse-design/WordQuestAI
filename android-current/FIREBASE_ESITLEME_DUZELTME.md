# WordQuest AI 1.0.5: kayıtlar eşitleniyor düzeltmesi

- Önceki sürümde hesap kayıtları Cloud Firestore içinde tutulmaya çalışılıyordu.
- Kurulan hizmet Realtime Database olduğu için eşitleme doğrudan `/users/{firebaseAuth.uid}` altına taşındı.
- Her kullanıcının kendi kayıtlarını okuması/yazması dışında erişim yoktur.
- `database.rules.json` Firebase Console → Realtime Database → Rules bölümünde bir kez yayımlanmalıdır; uygulama bu güvenlik adımını kendisi yapamaz.
- Eski `rooms` kuralları değiştirilmedi; kullanıcı kayıtları yeni `users` düğümündedir.
- Google girişinde kullanıcı hesabı korunur. Var olan yerel kayıtlar silinmez.
- Bağlantı yoksa/izin reddedildiyse artık sürekli “Kayıtlar eşitleniyor” denmez; neden gösterilir.
- Bu uygulama / Firebase kuralları gerçek iki cihazda henüz birlikte doğrulanmamıştır.
- OTA/güncelleme: aynı uygulama kimliği `com.kelimebulmacasi.a1.practice.offline101`, versionCode 28. İmzalama anahtarı önceki sürümle eşleşmelidir.
