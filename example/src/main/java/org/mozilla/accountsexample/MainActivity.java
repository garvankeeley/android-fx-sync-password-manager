package org.mozilla.accountsexample;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.mozilla.accountsexample.keystore.KeystoreWrapper;
import org.mozilla.accountsexample.patternlock.PatternLockActivity;
import org.mozilla.accountsexample.recycler.RecyclerFragment;
import org.mozilla.gecko.background.common.log.writers.StringLogWriter;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecord;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mozilla.accountsexample.AppGlobals.REC_GUID;


public class MainActivity extends AppCompatActivity {

    KeystoreWrapper keystore;
    private String RECORD_PREFIX = "_record_";
    RecyclerFragment tableFragment;
    Date timeInactive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        tableFragment = (RecyclerFragment) getFragmentManager().findFragmentById(R.id.recycler_table);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (keystore == null) {
            keystore = new KeystoreWrapper(this);
            keystore.symmetricTest();

            showPINScreen();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (timeInactive != null) {
            long diffInMillisec = (new Date()).getTime() - timeInactive.getTime();
            if (diffInMillisec > 1000 * 10) {
                showPINScreen();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        timeInactive = new Date();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    void showPINScreen() {
        PatternLockActivity.listener = new PatternLockActivity.PatternLockListener() {
            @Override
            public void patternLockCreated(String patternCode) {
                keystore.setPINCode(patternCode);
                showFxASetup();
                timeInactive = null;
            }

            @Override public void patternUnlockValidated() {
                showFxASetup();
                timeInactive = null;
            }

            @Override
            public void patternUnlockTooManyAttempts() {
                AppGlobals.prefs(getApplicationContext()).edit().clear().commit();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        System.exit(0);
                    }
                }, 400);

            }
        };
        PatternLockActivity.unlockCode = keystore.getPINCode();
        startActivity(new Intent(this, PatternLockActivity.class));
    }

    void showFxASetup() {
        List<Map<String, String>> records = readAllRecords();
        if (records.size() > 0) {
            displayRecords(records);
            return;
        }

        FxASync.show(this);
        FxASync.listener = new FxASync.FxASyncListener() {
            @Override
            public void onReceivedPasswordRecords(final List<PasswordRecord> receivedRecords) {
                new Handler(getMainLooper()).post(new Runnable() {
                    @Override public void run() {
                        processIncomingRecords(receivedRecords);
                    }});
            }
        };
    }


    private void processIncomingRecords(List<PasswordRecord> receivedRecords) {
        List<Map<String, String>> records = new ArrayList<>();
        for (PasswordRecord r : receivedRecords) {
            Map<String, String> map = AppGlobals.passwordRecordToMap(r);
            records.add(map);
            Log.d("HOST", r.hostname);
        }
        savePasswordRecords(records);
        displayRecords(records);
    }

    private void displayRecords(List<Map<String, String>> records) {
        tableFragment.loadData(records);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FxASync.handleActivityResult(requestCode, resultCode);
    }

    void savePasswordRecords(List<Map<String, String>> records) {
        SharedPreferences.Editor editor = AppGlobals.prefs(this).edit();
        for (Map<String, String> record : records) {
            saveRecord(record, editor);
        }
        editor.apply();
    }

    void saveRecord(Map<String, String> record, SharedPreferences.Editor editor) {
        JSONObject obj = new JSONObject();
        obj.putAll(record);
        String str = obj.toJSONString();
        editor.putString(RECORD_PREFIX + record.get(REC_GUID), keystore.symmetricEncrypt(str));
    }

    public List<Map<String,String>> readAllRecords() {
        List<Map<String, String>> records = new ArrayList<>();
        Map<String, ?> all = AppGlobals.prefs(this).getAll();
        for (final String k : all.keySet()) {
            if (!k.startsWith(RECORD_PREFIX)) {
                continue;
            }
            //String id = k.replace("_record_", "");
            String cipherText = AppGlobals.prefs(this).getString(k, "");
            String jsonString = keystore.symmetricDecrypt(cipherText);

            JSONParser parser = new JSONParser();
            JSONObject json = null;
            try {
                json = (JSONObject) parser.parse(jsonString);
            } catch (org.json.simple.parser.ParseException e) {
                e.printStackTrace();
            }
            if (json == null) {
                continue;
            }

            Map<String, String> record = new HashMap<>();
            for (Object key : json.keySet()) {
                record.put(key.toString(), json.get(key).toString());
            }
            records.add(record);
        }
        return records;
    }


}