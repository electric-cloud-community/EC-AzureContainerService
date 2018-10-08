import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.AlgorithmParameters
import java.security.spec.KeySpec

class ClusterInfoCrypter {

    public class EncryptionResult {
        private String encryptedSensitiveData;
        private String iv;

        EncryptionResult(String encryptedSensitiveData, String iv) {
            this.encryptedSensitiveData = encryptedSensitiveData;
            this.iv = iv;
        }

        String getEncryptedSensitiveData() {
            return encryptedSensitiveData;
        }

        String getIv() {
            return iv;
        }
    }

    public EncryptionResult encrypt(String sensitiveData, String password, String salt) {
        SecretKey secretKey = getSecretKey(password, salt);

        /* Encrypt the message. */
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        AlgorithmParameters params = cipher.getParameters();
        byte[] ivBytes = params.getParameterSpec(IvParameterSpec.class).getIV();
        byte[] encryptedSensitiveDataBytes = cipher.doFinal(sensitiveData.getBytes());

        // bytes to string
        String iv = Base64.getEncoder().encodeToString(ivBytes);
        String encryptedSensitiveData = Base64.getEncoder().encodeToString(encryptedSensitiveDataBytes);

        return new EncryptionResult(encryptedSensitiveData, iv);
    }

    public String decrypt(String encryptedSensitiveData, String password, String salt, String iv) {
        SecretKey secretKey = getSecretKey(password, salt);

        // string to bytes
        byte[] ivBytes = Base64.getDecoder().decode(iv.getBytes());
        byte[] encryptedSensitiveDataBytes = Base64.getDecoder().decode(encryptedSensitiveData.getBytes());

        /* Decrypt the message, given derived key and initialization vector. */
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));

        String decryptedSensitiveData = new String(cipher.doFinal(encryptedSensitiveDataBytes), "UTF-8");

        return decryptedSensitiveData;
    }

    private static SecretKey getSecretKey(String password, String salt) {
        /* Derive the key, given password and salt. */
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.getChars(), salt.getBytes(), 65536, 128);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
}