package net.nwnetsolutions.holidayengine.util;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Security {


    /**
     * This method takes in a code hash value and a String, and returns true if the hashed version
     * of that String matches the given hash, or false otherwise.
     *
     * @param key
     * @param hash
     * @return
     */
    public boolean stringMatchesHash(String key, String hash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(key.getBytes());
            byte[] resultHash = digest.digest();

            // Convert to HEX
            StringBuffer hexString = new StringBuffer();
            for (byte b : resultHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }

            Log.d("Security.java", "Orig: " + key + ", Hash: " + hexString.toString());

            // Compare new hash to original
            if (hexString.toString().equalsIgnoreCase(hash)) {
                return true;
            }

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return false;
    }

    public String getHash(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(key.getBytes());
            byte[] resultHash = digest.digest();

            // Convert to HEX
            StringBuffer hexString = new StringBuffer();
            for (byte b : resultHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
