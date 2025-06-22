package com.spkt.libraSys;

import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.util.io.pem.PemReader;
import java.io.StringReader;
import java.security.*;
import java.security.spec.RSAPublicKeySpec;
import javax.crypto.Cipher;
import java.math.BigInteger;
import java.util.Base64;

public class RsaPkcs1Encryptor {

    public static PublicKey getPublicKeyFromPkcs1(String pem) throws Exception {
        // Bước 1: Đọc phần nội dung base64 từ PEM
        PemReader pemReader = new PemReader(new StringReader(pem));
        byte[] content = pemReader.readPemObject().getContent();
        pemReader.close();

        // Bước 2: Parse modulus và exponent từ DER sequence sử dụng BouncyCastle
        RSAPublicKey rsaPublicKey = RSAPublicKey.getInstance(content);
        BigInteger modulus = rsaPublicKey.getModulus();
        BigInteger exponent = rsaPublicKey.getPublicExponent();

        // Bước 3: Tạo PublicKey
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(keySpec);
    }

    public static String encrypt(String plaintext, String pemPublicKey) throws Exception {
        PublicKey publicKey = getPublicKeyFromPkcs1(pemPublicKey);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public static void main(String[] args) throws Exception {
        String pem = "-----BEGIN RSA PUBLIC KEY-----\n" +
                "MIIBCgKCAQEAs7onDF8V9osmx05yAD4r5C/99Cfb4dwL35JypZ6jRX3vAy89DFyT\n" +
                "oE7A48xJXOF5F3nIwIbiFT2RwVUwf6oLyHKM7NIIqxUI/KCvQvfQar2Em9rbtATa\n" +
                "FSQIVWh4aKrO6pAgcnWz8XN54MrFVZuCIsTDm5QgAK0K2+ztaZG+gRKO/9I0I0RL\n" +
                "uZ1D2V56eYjK/398CDMb4FBo+TVi9c03JbTglCE9VeAyLoK7DeaTURNSgH+xw6sh\n" +
                "Ge11D+HAsEHBRUu7cuN2GKuCm6HSVtG2IqFSaTO0Ku7fHr7FcOKWsTKaIZLcl9JZ\n" +
                "9+oj3ELQOwqEUBeG5ICcDleGVj4qpvNIHQIDAQAB\n" +
                "-----END RSA PUBLIC KEY-----";

        String ciphertext = encrypt("Hello DRM World", pem);
        System.out.println("Encrypted (Base64): " + ciphertext);
    }
}
