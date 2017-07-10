package org.mozilla.accountsexample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import org.mozilla.accounts.FirefoxAccount;
import org.mozilla.accounts.FirefoxAccountDevelopmentStore;
import org.mozilla.accounts.FirefoxAccountEndpointConfig;
import org.mozilla.accounts.login.FirefoxAccountLoginWebViewActivity;
import org.mozilla.accounts.sync.FirefoxAccountSyncClient;
import org.mozilla.accounts.sync.commands.SyncCollectionCallback;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecord;

import java.lang.ref.WeakReference;
import java.util.List;

public class FxASync {

    public static interface FxASyncListener {
        void onReceivedPasswordRecords(List<PasswordRecord> receivedRecords);
    }
    public static FxASyncListener listener;

    private static final String LOGTAG = "FxASync";
    private static final int dialogRequestCode = AppGlobals.uniqueIntForApp();

    private static Context context;

    static void show(Activity activity) {
        context = activity.getApplicationContext();

        final Intent intent = new Intent(context, FirefoxAccountLoginWebViewActivity.class);
        intent.putExtra(FirefoxAccountLoginWebViewActivity.EXTRA_ACCOUNT_CONFIG,
                FirefoxAccountEndpointConfig.getProduction());
        activity.startActivityForResult(intent, dialogRequestCode);
    }

    static Boolean handleActivityResult(final int requestCode, final int resultCode) {
        if (requestCode != dialogRequestCode) {
            return false;
        }

        if (resultCode == FirefoxAccountLoginWebViewActivity.RESULT_OK) {
            final FirefoxAccount account = new FirefoxAccountDevelopmentStore(context).loadFirefoxAccount();
            if (account == null) {
                Log.d("lol", "Nothing.");
            } else {
                Log.d("lol", account.uid);
                sync(account);
            }
        } else if (resultCode == FirefoxAccountLoginWebViewActivity.RESULT_CANCELED) {
            Log.d("lol", "User canceled login");
        } else {
            Log.d("lol", "error!");
        }
        return true;
    }

    private static void sync(final FirefoxAccount account) {
        FirefoxAccountSyncClient client = new FirefoxAccountSyncClient(account);
        client.getPasswords(context, new SyncCollectionCallback<PasswordRecord>() {
            @Override
            public void onReceive(final List<PasswordRecord> receivedRecords) {
                Log.e(LOGTAG, "onReceive: passwords!");
                if (listener != null) {
                    listener.onReceivedPasswordRecords(receivedRecords);
                }
//                for (final PasswordRecord record : receivedRecords) {
//                    Log.d(LOGTAG, record.encryptedPassword + ": " + record.encryptedUsername);
//                }
            }

            @Override public void onError(final Exception e) { Log.e(LOGTAG, "onError: error!", e); }
        });
    }
}
