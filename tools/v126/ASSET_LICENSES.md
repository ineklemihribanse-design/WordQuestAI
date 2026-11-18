# 1.0.26 Insansı avatarların kökeni ve lisansı

Bu sürümdeki insan modelleri **Quaternius** tarafından hazırlanan, CC0 1.0 kapsamındaki *Ultimate Modular Women Pack* ve *Ultimate Modular Men Pack* kaynaklarından gelir.

- Emma: Ultimate Modular Women Pack / Formal.glb
- Sophia: Ultimate Modular Women Pack / Casual.glb
- Ethan: Ultimate Modular Men Pack / Suit.glb
- Kaynak sayfalar: https://quaternius.com/packs/ultimatemodularwomen.html ve https://quaternius.com/packs/ultimatemodularcharacters.html
- Lisans: https://creativecommons.org/publicdomain/zero/1.0/ (CC0-1.0)
- Açık kaynak GLB düzenleme/dağıtım kopyası: https://github.com/trebeljahr/quaternius-showcase
- Sabit kaynak commit: `0128ca510d653bb04d6d1e4885bd8890b5050b4f`

Dosyalar APK derlenirken sabit commit üzerinden indirilir. `tools/v126/install_humanoid_assets.py`, GLB başlığını, boyutu, üçgen ağını, kemik/skin bağlantısını, animasyonları ve materyalleri kontrol eder. APK'nın `assets/avatars` klasörüne gömülür; uygulama çalışırken indirme yapılmaz ve abonelik/ücretli avatar servisi gerekmez.

**Kalite sınırı:** Bunlar hazırlanmış animasyonlu 3D insansı varlıklardır fakat fotoğraf gerçekçiliğinde MetaHuman/AAA yapımlar değillerdir. Kaynak model konuşma için özel viseme/blendshape içermiyorsa ağzın kelime-ses düzeyinde senkronize olduğu söylenemez. 1.0.26 uygun animasyon klipleriyle konuşma jesti kullanır, fonem düzeyinde lip-sync iddiası yoktur. Android emülatörü/gerçek cihaz görüntü testi ayrıca gereklidir.
