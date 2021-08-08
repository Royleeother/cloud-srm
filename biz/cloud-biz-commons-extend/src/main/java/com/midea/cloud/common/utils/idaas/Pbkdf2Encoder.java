package com.midea.cloud.common.utils.idaas;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.midea.cloud.common.utils.idaas.EncodingUtils.*;

/**
 * <pre>
 * (idaas平台生成token值用)
 * </pre>
 *
 * @author wuwl18@meicloud.com
 * @version 1.00.00
 *
 * <pre>
 *  修改记录
 *  修改后版本:
 *  修改人:
 *  修改日期: 2020/5/9
 *  修改内容:
 * </pre>
 */
public class Pbkdf2Encoder {
    private static final int DEFAULT_HASH_WIDTH = 256;
    private static final int DEFAULT_ITERATIONS = 5000;
    private final RandomBytesKeyGenerator saltGenerator = new RandomBytesKeyGenerator();
    private String algorithm = SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA1.name();
    private final byte[] secret;
    private final int hashWidth;
    private final int iterations;

    public Pbkdf2Encoder() {
        this("");
    }

    public Pbkdf2Encoder(String secret) {
        this(secret, DEFAULT_ITERATIONS, DEFAULT_HASH_WIDTH);
    }

    public Pbkdf2Encoder(String secret, int iterations, int hashWidth) {
        this.secret = encodeUtf8(secret);
        this.iterations = iterations;
        this.hashWidth = hashWidth;
    }

    public void setAlgorithm(SecretKeyFactoryAlgorithm secretKeyFactoryAlgorithm) {
        if (secretKeyFactoryAlgorithm == null) {
            throw new IllegalArgumentException("secretKeyFactoryAlgorithm cannot be null");
        }
        String algorithmName = secretKeyFactoryAlgorithm.name();
        try {
            SecretKeyFactory.getInstance(algorithmName);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Invalid algorithm '" + algorithmName + "'.", e);
        }
        this.algorithm = algorithmName;
    }

    public String encode(String rawPassword) {
        byte[] salt = this.saltGenerator.generateKey();
        byte[] encoded = encode(rawPassword, salt);
        return encode(encoded);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        byte[] digested = decode(encodedPassword);
        byte[] salt = subArray(digested, 0, this.saltGenerator.getKeyLength());
        return MessageDigest.isEqual(digested, encode(rawPassword, salt));
    }

    private String encode(byte[] bytes) {
        return String.valueOf(encodeHex(bytes));
    }

    private byte[] decode(String encodedBytes) {
        return decodeHex(encodedBytes);
    }

    private byte[] encode(String rawPassword, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(),
                    concatenate(salt, this.secret), this.iterations, this.hashWidth);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(this.algorithm);
            return concatenate(salt, skf.generateSecret(spec).getEncoded());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not create hash", e);
        }
    }

    public enum SecretKeyFactoryAlgorithm {
        PBKDF2WithHmacSHA1,
        PBKDF2WithHmacSHA256,
        PBKDF2WithHmacSHA512
    }

}
