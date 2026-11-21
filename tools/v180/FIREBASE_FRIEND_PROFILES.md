# WordQuest AI 1.0.80 — Arkadaş profil özetleri

Yeni RTDB düğümü: `friendProfiles/$uid`. Mevcut `users`, `directory`, `friends` ve Arena kuralları **değiştirilmedi**.

* Kullanıcı kendi maskotunu, toplam puanını, tamamladığı oyun sayısını ve en son etkinlik zamanını paylaşır.
* Okuma izni yalnızca profil sahibinde veya **karşılıklı arkadaşlık** bağı olan hesaplarda bulunur.
* Yazma yetkisi yalnızca `auth.uid == $uid` olan hesapta bulunur.
* E-posta, oyun cevapları, çalışma geçmişi veya kişisel belgeler paylaşılmaz.
* `lastActiveAt` bir son etkinlik zamanıdır. Gerçek zamanlı çevrimiçi varlık olarak yorumlanmaz.
* Firebase Konsolu'nda `wordquest-ai-default-rtdb` Rules bölümünde mevcut canlı JSON önce karşılaştırılmalıdır. Farklı mevcut kuralları silerek dosyanın tamamını körlemesine yapıştırmayın. Yalnızca `friendProfiles` alt düğümünü mevcut kurallara ekleyin.
* Yeni kural henüz yayımlanmamışsa arkadaş kartları gizli ayrıntılar için boş durum gösterir; hesap/oyun verileri bozulmaz.
* Hesap silme sırasında `friendProfiles/$uid` de kaldırılır.

Rules: https://console.firebase.google.com/project/wordquest-ai/database/wordquest-ai-default-rtdb/rules
