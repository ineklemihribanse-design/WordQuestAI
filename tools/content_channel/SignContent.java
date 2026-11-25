import java.nio.file.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.HexFormat;

class SignContent {
    public static void main(String[] args) throws Exception {
        KeyStore ks=KeyStore.getInstance("PKCS12");
        try(var in=Files.newInputStream(Path.of(args[0]))) {
            ks.load(in,System.getenv("WQ_STORE_PASS").toCharArray());
        }
        String alias=System.getenv("WQ_KEY_ALIAS");
        Certificate cert=ks.getCertificate(alias);
        String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
        if(!digest.equals("29f9449cef86e511bb8256ca1db58580ff88dd1badd034ff79b81c6df662bce6"))
            throw new SecurityException("Unexpected certificate");
        PrivateKey key=(PrivateKey)ks.getKey(alias,System.getenv("WQ_KEY_PASS").toCharArray());
        String algo=switch(key.getAlgorithm()) {case "RSA" -> "SHA256withRSA";case "EC" -> "SHA256withECDSA";default -> throw new SecurityException("Unsupported key");};
        byte[] payload=Files.readAllBytes(Path.of(args[1]));
        Signature s=Signature.getInstance(algo);
        s.initSign(key);s.update(payload);byte[] signature=s.sign();
        s.initVerify(cert.getPublicKey());s.update(payload);
        if(!s.verify(signature))throw new SecurityException("Signature verification failed");
        Base64.Encoder b=Base64.getEncoder();
        Files.writeString(Path.of(args[2]),"{\"payload\":\""+b.encodeToString(payload)+"\",\"signature\":\""+b.encodeToString(signature)+"\"}\n");
        System.out.println("Verified content signature matches installed APK signer.");
    }
}
