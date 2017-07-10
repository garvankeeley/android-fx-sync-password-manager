package org.mozilla.accountsexample;

import android.content.Context;
import android.content.SharedPreferences;

import org.mozilla.gecko.sync.repositories.domain.PasswordRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AppGlobals {

    private static final AtomicInteger counter = new AtomicInteger();
    public static int uniqueIntForApp() {
        return counter.getAndIncrement();
    }

    public static final String REC_GUID = "guid";
    public static final String REC_HOSTNAME = "hostname";
    public static final String REC_USERNAME = "username";
    public static final String REC_PASSWORD = "password";

    public static Map<String, String> passwordRecordToMap(PasswordRecord record) {
        Map<String, String> map = new HashMap<>();
        map.put(REC_GUID, record.guid);
        map.put(REC_HOSTNAME, record.hostname);
        map.put(REC_USERNAME, record.encryptedUsername);
        map.put(REC_PASSWORD, record.encryptedPassword);
        return map;
    }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("moz.lockbox.prefs", Context.MODE_PRIVATE);
    }

}
