import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.spec.KeySpec;

import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

class ClusterInfoCrypter {

    private Cipher encrypter;
    private Cipher decrypter;

    ClusterInfoCrypter(String password, String salt) {
        encrypter = Cipher.getInstance("AES/ECB/PKCS5Padding");
        decrypter = Cipher.getInstance("AES/ECB/PKCS5Padding");

        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 128);
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] rawKey = f.generateSecret(spec).getEncoded();
        Key m_keySpec = new SecretKeySpec(rawKey, "AES");

        encrypter.init(ENCRYPT_MODE, m_keySpec);
        decrypter.init(DECRYPT_MODE, m_keySpec);
    }

    public String encrypt(String sensitiveData) {
        String encryptedSensitiveData = new String(
                Base64.getEncoder().encodeToString(
                        encrypter.doFinal(
                                sensitiveData.getBytes()
                        )
                )
        );

        return encryptedSensitiveData
    }

    public String decrypt(String encryptedSensitiveData) {
        String decryptedSensitiveData = new String(
                decrypter.doFinal(
                        Base64.getDecoder().decode(
                                encryptedSensitiveData.getBytes()
                        )
                )
        );

        return decryptedSensitiveData;
    }

}