package org.mozilla.accountsexample;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;

import static android.content.Context.CLIPBOARD_SERVICE;
import static org.mozilla.accountsexample.AppGlobals.REC_USERNAME;

public class ClipboardDialog {
    private static void setClip(String text, Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
        clipboard.setText(text);
    }

    public static void show(String site, final String username, final String password, final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(site);
        builder.setMessage("Copy the username or password for this site to the clipboard?");
        builder.setNegativeButton("Username",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setClip(username, context);
                        dialog.cancel();
                    }
                });

        builder.setPositiveButton("Password",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        setClip(password, context);
                        dialog.cancel();
                    }
                });

        builder.setNeutralButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        builder.show();
    }
}
