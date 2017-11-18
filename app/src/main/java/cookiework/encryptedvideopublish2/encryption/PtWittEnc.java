/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cookiework.encryptedvideopublish2.encryption;

import android.content.Context;
import android.util.Log;

import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.digests.MD5Digest;
import org.spongycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.spongycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.spongycastle.jce.interfaces.ECPrivateKey;
import org.spongycastle.jce.interfaces.ECPublicKey;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECPrivateKeySpec;
import org.spongycastle.jce.spec.ECPublicKeySpec;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.UrlBase64;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.RSAPrivateKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import cookiework.encryptedvideopublish2.util.DBHelper;

import static cookiework.encryptedvideopublish2.Constants.ECCURVE_NAME;
import static cookiework.encryptedvideopublish2.Constants.TIME_LOG_TAG;

/**
 * @author Andrew This class it the main workhorse that does all encryption,
 *         decryption and key management. It provides tools to Subscription Processor as
 *         well as being directly accessed by the Firefox extension to encode/decode
 *         messages.
 *         <p>
 *         Message encryption is defined as:
 *         <p>
 *         (message, t) → (ct, t*)
 *         <p>
 *         temp = Sha1-hash[RSA-Signature(message)] k = MD5(temp || 0) ct =
 *         AES_ENCk(message)
 *         <p>
 *         send (ct, t*) to database to recover search on t*
 *         <p>
 *         Message decryption is defined as:
 *         <p>
 *         (ct, t*) → (message, t)
 *         <p>
 *         t* = MD5(temp || 1) k = MD5(temp || 0) message = AES_DECk(ct)
 */
public class PtWittEnc {

    private PrivateKey sk = null;
    private PublicKey pk = null;
    public final DBHelper dbHelper;

    //constructs object nothing special
    public PtWittEnc(Context context) {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        this.dbHelper = new DBHelper(context);
    }

    //This class uses the java.security.KeyPairGenerator to create a keypair.  This set is
//saved to the local computer at the location of file loc.
    public boolean makeKeys(String username) {
        try {
            if(dbHelper.getN(username) == null) {
                long beginTime = System.currentTimeMillis();

                KeyPair kp = KeyPairGenerator.getInstance("RSA", "SC").generateKeyPair();
                sk = kp.getPrivate();
                pk = kp.getPublic();

                long endTime = System.currentTimeMillis();
                Log.i(TIME_LOG_TAG, "makeKeys(): " + (endTime - beginTime));

                dbHelper.addKey(username, getNasString(), getDasString());
            } else {
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //helper function
    public BigInteger getN() {
        return ((RSAPrivateKey) sk).getModulus();
    }

    //helper function
    public BigInteger getE() {
        return ((RSAPublicKey) pk).getPublicExponent();
    }

    //helper function
    public BigInteger getD() {
        return ((RSAPrivateKey) sk).getPrivateExponent();
    }

    //helper function
    public String getNasString() {
        return ((RSAPrivateKey) sk).getModulus().toString();
    }

    //helper function
    public String getEasString() {
        return ((RSAPublicKey) pk).getPublicExponent().toString();
    }

    //helper function
    public String getDasString() {
        return ((RSAPrivateKey) sk).getPrivateExponent().toString();
    }

    //helper function
    public PrivateKey getPrivateKey() {
        return sk;
    }

    //helper function
    public PublicKey getPublicKey() {
        return pk;
    }

    //This function is used when loading a key from the keyfile that is stored locally
//it sets the public key to be used later.
    public void setPrivateKey(BigInteger N, BigInteger d) {
        try {
            RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(N, d);
            KeyFactory factory = KeyFactory.getInstance("RSA", "SC");
            sk = factory.generatePrivate(keySpec);
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    //Performs an MD5 hash with a 0 appended to the end of the input value.
    //this method is used during the message encryption to generate the K value
    //that is used for the AES encryption
    public byte[] hashMD5(byte[] input) {
        byte[] input2 = appendBytes(input, "0".getBytes());

        MD5Digest md5 = new MD5Digest();
        md5.update(input2, 0, input2.length);

        byte[] digest = new byte[md5.getDigestSize()];
        md5.doFinal(digest, 0);

        return digest;

    }

    //Does AES encryption on plain text using the provided key, this method uses
    //Bouncycastle as a provider and uses the "AES/ECB/PKCS7Padding" cipher instance
    //It returns a base64 encoded string of the ciphertext for more compact storage
    //in the database.
    public String encryptAES(String plainText, byte[] key) {
        long beginTime = System.currentTimeMillis();

        byte[] input = plainText.getBytes();
        byte[] cipherText = null;
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

        try {
            Cipher ci = Cipher.getInstance("AES/ECB/PKCS7Padding", "SC");
            ci.init(Cipher.ENCRYPT_MODE, secretKey);

            //cipherText = new byte[ci.getOutputSize(input.length)];
            //int ctLength = ci.update(input, 0, input.length, cipherText, 0);
            //ci.doFinal(cipherText, ctLength);
            cipherText = ci.doFinal(input);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String result = new String(UrlBase64.encode(cipherText));

        long endTime = System.currentTimeMillis();
        Log.i(TIME_LOG_TAG, "encryptAES(): " + (endTime - beginTime));

        return result;
    }

    //This creates the T* value which is a common step during the encryption
    //decryption and subscription processes it is simply MD5(Temp || 1)
    public String createTStar(byte[] temp) {
        byte[] temp2 = appendBytes(temp, "1".getBytes());

        return new String(UrlBase64.encode(hashMD5(temp2)));
    }

    //helper function
    public byte[] appendBytes(byte[] temp, byte[] zeroOrOne) {
        byte[] temp2 = new byte[temp.length + zeroOrOne.length];
        System.arraycopy(temp, 0, temp2, 0, temp.length);
        System.arraycopy(zeroOrOne, 0, temp2, zeroOrOne.length, zeroOrOne.length);

        return temp2;
    }

    //Decrypts the cipher text into plain text using bouncycastle as the provider
    //and using the AES/ECB/PKCS7Padding cipher instance.
    public String decryptAES(String text, byte[] key) {
        long beginTime = System.currentTimeMillis();

        byte[] cipherText = null;
        try {
            cipherText = UrlBase64.decode(text);
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }


        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        int ctLength = cipherText.length;
        ;
        byte[] pt = null;
        try {
            Cipher ci = Cipher.getInstance("AES/ECB/PKCS7Padding", "SC");
            ci.init(Cipher.DECRYPT_MODE, secretKey);
            //pt = new byte[ci.getOutputSize(ctLength)];
            //int ptLength = ci.update(cipherText, 0, ctLength, pt, 0);
            //ptLength += ci.doFinal(pt, ptLength);
            pt = ci.doFinal(cipherText);
        } catch (Exception e) {
            e.printStackTrace();
            return e.toString();
        }

        long endTime = System.currentTimeMillis();
        Log.i(TIME_LOG_TAG, "decryptAES(): " + (endTime - beginTime));

        return new String(pt);
    }

    //This method loads a key file into the system setting the public and private key in the class
    public void loadKeyFile(String username) {
        try {
            String nString = dbHelper.getN(username);
            String dString = dbHelper.getD(username);
            BigInteger N = new BigInteger(nString);
            BigInteger d = new BigInteger(dString);
            setPrivateKey(N, d);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //This method is used when sending a message, it uses the encryptAES method
    //and returns a plaintext and tStar in an array.
    public VideoInfo send(String[] tags, String plainTitle, String plainIntro, String plainAddr, String status) {
        VideoInfo info = new VideoInfo();
        try {
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            byte[] k = new byte[16];
            random.nextBytes(k);
            info.setCipherTitle(encryptAES(plainTitle, k));
            info.setCipherIntro(encryptAES(plainIntro, k));
            info.setCipherAddr(encryptAES(plainAddr, k));
            info.setStatus(status);
            String key = new String(UrlBase64.encode(k));
            info.setKey(key);
            for (int i = 0; i < tags.length; i++) {
                byte[] temp = createTemp(getD(), getN(), tags[i]);
                System.out.println(Arrays.toString(temp));
                String encKey = encryptAES(key, hashMD5(temp));
                String tStar = createTStar(temp);
                info.getTagsAndEncKeys().put(tStar, encKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return info;
    }

    //This method creates the 'temp' value which is defeind as SHA1(RSA-Signature(message))
    private byte[] createTemp(BigInteger d, BigInteger N, String tag) {
        byte[] temp = null;
        try {
            long beginTime = System.currentTimeMillis();

            MessageDigest digest = MessageDigest.getInstance("SHA1");
            digest.update(tag.getBytes());
            byte[] result = digest.digest();
            BigInteger sha1Final = new BigInteger(1, result);

            BigInteger sigma = sha1Final.modPow(d, N);
            temp = sigma.toByteArray();

            long endTime = System.currentTimeMillis();
            Log.i(TIME_LOG_TAG, "createTemp(): " + (endTime - beginTime));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return temp;
    }

    public static String hashPw(String pass) {
        try {
            byte[] passbytes = pass.getBytes();
            MessageDigest mdigest = MessageDigest.getInstance("MD5");
            mdigest.update(passbytes);
            byte hashBytes[] = mdigest.digest();
            StringBuffer sbuffer = new StringBuffer();
            for (int i = 0; i < hashBytes.length; i++) {
                String temp = Integer.toHexString(0xff & hashBytes[i]);
                if (temp.length() == 1)
                    sbuffer.append('0');
                sbuffer.append(temp);
            }

            pass = sbuffer.toString();
        } catch (Exception e) {
            return pass;
        }

        return pass;
    }
}
