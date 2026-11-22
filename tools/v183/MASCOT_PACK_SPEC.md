# WordQuest Mascot Pack v1 — Güvenli ZIP standardı

**Kapsam:** 2D PNG/WebP görselleri. Harici kod, HTML, GLB/GLTF veya sınırsız animasyon kabul edilmez.
Paketler uygulama özel depolamasına alınır; Firebase'e otomatik yüklenmez.

## ZIP dosyaları

```text
manifest.json
images/idle.png
images/happy.png          (isteğe bağlı)
images/sad.png            (isteğe bağlı)
images/thinking.png       (isteğe bağlı)
images/win.png            (isteğe bağlı)
README_TR.txt             (isteğe bağlı, en fazla 64 KiB)
```

PNG yerine WebP kullanılabilir. `images/frame_00.png` ile `images/frame_99.webp`
arasında adlandırılan isteğe bağlı kareler doğrulanabilir; ilk sürümde bunların tüm
ekranlarda oynatılacağı garanti edilmez. 'loss' tepkisi yoksa 'idle' kullanılır.

## Manifest

```json
{
  "formatVersion": 1,
  "id": "benim-robotum",
  "name": "Benim Robotum",
  "packageVersion": 1,
  "animation": "static",
  "images": {
    "idle": "images/idle.png",
    "happy": "images/happy.png",
    "sad": "images/sad.png",
    "thinking": "images/thinking.png",
    "win": "images/win.png"
  }
}
```

Sadece `images.idle` zorunludur. `formatVersion` tam sayı **1** olmalı;
`id` 1–40 karakterlik güvenli ASCII kimliktir; `name` 1–40 karakterdir.
`animation` `static` veya `frames` olabilir. Eksik ifadelerde `idle` kullanılır.
Paket kimliği dosya sistemi yolu olarak kullanılmaz; rastgele oluşturulan güvenli
yerel kimlikle saklanır.

## Sınırlar

- ZIP dosyasının tamamı: en fazla 25 MiB
- Açılan verinin toplamı: en fazla 80 MiB
- Dosya sayısı: en fazla 40
- Her dosya: en fazla 25 MiB, manifest/README en fazla 64 KiB
- Görselin her kenarı: en fazla 2048 piksel
- Sabit dosya yolu izin listesi; yol geçişi, dosya tekrarları, yabancı/çalıştırılabilir
  içerik ve geçersiz görüntü imzası reddedilir
- İçerik, cihazın özel dosya alanında geçici klasöre açılır; başarısız ithalatın
  geçici dosyaları silinir; diğer hesapların kayıtlarına dokunulmaz.

## Şablon

`python3 tools/v183/build_mascot_template_183.py` komutuyla
`WordQuest_Mascot_Template_v1.zip` üretilir. Şablon içerisindeki PNG **boş
şeffaf tuvaldir**, kullanmadan önce gerçek karakter görseliyle değiştirilmeli.

## Çevrimiçi görünürlük

Özel maskotun görseli karşı cihazda yoksa hazır 'piko' maskotuna dönülür.
ZIP otomatik olarak sunucuya gönderilmez. İnternet üstünden görsel paylaşımı
bu formatın ilk sürümünde etkin değildir.
