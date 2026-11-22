# WordQuest AI 1.0.85 — Arkadaşın kişisel maskotunu gösterme (Firebase)

## Durum

- 1.0.85 APK derleme ve 1.0.84 üzerinde emülatör güncelleme testlerinden geçti.
- Küçük WebP (en fazla 160px) önizleme, **ayrı izin** ile ve sadece karşılıklı arkadaşların erişebileceği `friendProfiles/${uid}/avatarPreview` yolunda tutulur.
- Maskotun asıl ZIP dosyası cihazdan gönderilmez.
- `firebase/database.rules.v185.json` yerel RTDB emülatör izin testlerinden geçti.
- **Canlı Firebase kuralları henüz dağıtılmadı.** GitHub'daki `WQ_FIREBASE_SERVICE_ACCOUNT_JSON` gizli anahtarı eksik; bu anahtar olmadan dağıtım iş akışı güvenli biçimde reddediyor.

## Firebase Console üzerinden güvenli yayınlama

1. [Firebase Console](https://console.firebase.google.com/) içinde `wordquest-ai` projesini, **Realtime Database → Rules** bölümünü aç.
2. Mevcut kuralların bir kopyasını dışarıda **yedekle**. Tam veritabanını veya kuralları sıfırlama.
3. Var olan `rules → friendProfiles → $uid` altındaki `.read`, `.write`, `.validate`, `avatar`, `points`, `completed`, `lastActiveAt` ve `$other` öğelerine dokunma.
4. Aynı `$uid` nesnesine aşağıdaki **tek yeni alanı** ekle:

```json
"avatarPreview": {
  ".validate": "newData.isString() && newData.val().length >= 140 && newData.val().length <= 18050 && newData.val().matches(/^custom_[a-f0-9]{32}:[A-Za-z0-9+\\/=]+$/)"
}
```

5. Mevcut `$uid/.read` mutlaka şu koşula eşdeğer olmalıdır (her iki yönlü arkadaşlık gerekir):
```text
auth != null && (auth.uid === $uid || (
  root.child('friends').child(auth.uid).child($uid).val() === true &&
  root.child('friends').child($uid).child(auth.uid).val() === true
))
```
Mevcut politika bundan farklıysa, üstüne yazmadan önce farklılığın nedenini incele.

6. Sadece doğrulama başarılı ve diğer kurallar değişmemişse **Publish** yap. Genel `.read:true`, `.write:true` veya `friends` verilerini herkese açma.
7. İki farklı gerçek hesapla karşılıklı arkadaş ol. Profilde **Maskotumu arkadaşlarıma göster** iznini etkinleştir; her iki tarafta da kartların kendi seçilen maskotu gösterdiğini doğrula. İzni kapatınca arkadaş kartındaki özel görselin kaldırıldığını doğrula.

**Tam karşılaştırma kaynağı:** `firebase/database.rules.v185.json` ve `firebase/database.rules.v183.json`. Fark yalnızca `friendProfiles/$uid/avatarPreview` alanıdır; tüm kuralları topluca değiştirmek yerine bu tek eklemeyi yap.

## GitHub Actions ile otomatik yayınlamak için

`Settings → Secrets and variables → Actions` altına yalnızca repository yöneticisi tarafından **Firebase'e yetkili hizmet hesabı** `WQ_FIREBASE_SERVICE_ACCOUNT_JSON` olarak güvenli secret biçiminde tanımlanabilir. Bu gizli anahtarı sohbetlerde veya kaynak kodunda paylaşma. Ardından `deploy-friend-preview-v185.yml` iş akışını manuel çalıştır. İş akışı, canlı kurallar eski denetlenmiş tabanla eşleşmiyorsa güvenlik amacıyla işlem yapmaz.

## Doğrulanan sınırlar

Kotlin/JVM APK derlemesi, sürüm imzası ve emülatör yükseltmesi başarılıdır. Ancak gerçek cihazda iki ayrı kullanıcıyla paylaşım testi ve canlı RTDB kuralının uygulanması henüz doğrulanmış değildir. Bu adımlar gerçekleşmeden arkadaşların kişisel maskotu kesin göründüğünü varsayma.
