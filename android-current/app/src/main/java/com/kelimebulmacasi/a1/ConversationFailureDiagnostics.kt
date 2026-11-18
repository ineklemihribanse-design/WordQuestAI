package com.kelimebulmacasi.a1

/** Safe, actionable errors; never interpolate exception messages that may contain private data.
 * WordRequestException is already shown verbatim in the callers, so this handles unexpected
 * failures instead of pretending every exception is a broken internet connection.
 */
internal fun conversationFailureMessage(problem: Throwable): String = when (problem) {
    is java.net.UnknownHostException ->
        "AI sunucusunun adresi çözümlenemedi (DNS). Bağlantı ve DNS ayarlarını kontrol et."
    is java.net.SocketTimeoutException ->
        "AI yanıtı zaman aşımına uğradı. Biraz sonra yeniden dene."
    is java.net.ConnectException ->
        "AI sunucusuna bağlantı açılamadı. Ağ erişimini kontrol et."
    is javax.net.ssl.SSLException ->
        "AI sunucusuyla güvenli TLS bağlantısı kurulamadı. Cihazın tarihini ve ağını kontrol et."
    is java.io.IOException ->
        "AI ağ isteği tamamlanamadı (" + problem.javaClass.simpleName + "). Daha sonra yeniden dene."
    is org.json.JSONException ->
        "AI yanıtı okunamadı (JSON). Sağlayıcı yanıt biçimi kontrol edilmeli."
    is android.database.SQLException ->
        "Sohbet veritabanı işlemi başarısız oldu. Verileri silmeden uygulama hatasını bildir."
    is SecurityException ->
        "AI bağlantısı için bir cihaz veya ağ izni engellendi."
    is IllegalArgumentException ->
        "Konuşma isteği hazırlanırken geçersiz bir değer bulundu. Model ve sohbet ayarlarını kontrol et."
    else ->
        "Konuşma Asistanı hatası (" + problem.javaClass.simpleName + "). Hata türünü bildir; API anahtarını paylaşma."
}
