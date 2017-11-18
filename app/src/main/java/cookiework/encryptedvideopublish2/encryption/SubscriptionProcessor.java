/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cookiework.encryptedvideopublish2.encryption;
import android.content.Context;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.spongycastle.math.ec.ECFieldElement;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.UrlBase64;

import static cookiework.encryptedvideopublish2.Constants.ECCURVE;
import static cookiework.encryptedvideopublish2.Constants.ECCURVE_NAME;
import static cookiework.encryptedvideopublish2.Constants.TIME_LOG_TAG;

/**
 *
 * @author Andrew
 *
 * This class is designed encapsulate methods that are used in the subscription process.
 * Subscribe, Approve and Finalize.  It uses the methods from PtwittEnc to accomplish
 * this task and provide the Firefox extension an simplier way to do different subscription methods.
 */
public class SubscriptionProcessor {

    private PtWittEnc enc;
    
    //Constructor generates random 'r' value from a using the java secruity secure random class with specified number of bits
    public SubscriptionProcessor(Context context)
    {
        this.enc = new PtWittEnc(context);
    }

    public BigInteger generateResponse(BigInteger d, BigInteger M, BigInteger N)
    {
        ////////////////////////////////////////////////////////////////
        /////////////////         PART TWO   ///////////////////////////
        //Do the Following Action: M' = (M^d) mod N/////////////////////
        ////////////////////////////////////////////////////////////////

        BigInteger mPrime = M.modPow(d, N);
        return mPrime;
    }

    public String getResponseString(String dStr, String MStr, String NStr)
    {
        String mPrimeStr = "";

        try
        {
            long beginTime = System.currentTimeMillis();

            BigInteger d =  new BigInteger(dStr);
            BigInteger N =  new BigInteger(NStr);
            BigInteger M = new BigInteger(UrlBase64.decode(MStr));
            BigInteger mPrime = generateResponse(d, M, N);
            mPrimeStr = new String(UrlBase64.encode(mPrime.toByteArray()));

            long endTime = System.currentTimeMillis();
            Log.i(TIME_LOG_TAG, "generateResponse(): " + (endTime - beginTime));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return mPrimeStr;
    }
}
