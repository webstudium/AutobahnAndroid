package de.tavendo.autobahn;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import android.util.Base64;
import android.util.Log;

public class WampCraConnection extends WampConnection implements WampCra {

    public void authenticate(final AuthHandler authHandler, final String authKey, final String authSecret, Object... authExtra) {
        call(Wamp.URI_WAMP_PROCEDURE + "authreq", String.class, new CallHandler(){

            public void onResult(Object challenge) {
                
                String sig = null;
                try {
                    sig = authSignature((String)challenge, authSecret);
                } catch (SignatureException e) {
                    Log.e("WampCraConnection:authenicate",e.toString());
                }
                
                call(Wamp.URI_WAMP_PROCEDURE + "auth", WampCraPermissions.class, new CallHandler(){

                    public void onResult(Object result) {
                        authHandler.onAuthSuccess(result);
                    }

                    public void onError(String errorUri, String errorDesc) {
                        authHandler.onAuthError(errorUri,errorDesc);                
                    }
                    
                }, sig);
                
                
            }

            public void onError(String errorUri, String errorDesc) {
                authHandler.onAuthError(errorUri,errorDesc);                
            }
            
        }, authKey, authExtra);
    }

    public String authSignature(String authChallenge, String authSecret) throws SignatureException{
        try {
            Key sk = new SecretKeySpec(authSecret.getBytes(), HASH_ALGORITHM);
            Mac mac = Mac.getInstance(sk.getAlgorithm());
            mac.init(sk);
            final byte[] hmac = mac.doFinal(authChallenge.getBytes());
            return Base64.encodeToString(hmac,Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e1) {
            throw new SignatureException("error building signature, no such algorithm in device " + HASH_ALGORITHM);
        } catch (InvalidKeyException e) {
            throw new SignatureException("error building signature, invalid key " + HASH_ALGORITHM);
        }
    }

    private static final String HASH_ALGORITHM = "HmacSHA256";
    
}