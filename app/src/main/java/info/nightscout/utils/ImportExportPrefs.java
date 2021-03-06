package info.nightscout.utils;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.events.EventAppExit;

/**
 * Created by mike on 03.07.2016.
 */

public class ImportExportPrefs {
    private static Logger log = LoggerFactory.getLogger(ImportExportPrefs.class);
    static File path = new File(Environment.getExternalStorageDirectory().toString());
    static final File file = new File(path, MainApp.sResources.getString(R.string.app_name) + "Preferences");

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);


        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public static void exportSharedPreferences(final Activity c) {

        new AlertDialog.Builder(c)
                .setMessage(MainApp.sResources.getString(R.string.export_to) + " " + file + " ?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
                        try {
                            FileWriter fw = new FileWriter(file);
                            PrintWriter pw = new PrintWriter(fw);
                            Map<String, ?> prefsMap = prefs.getAll();
                            for (Map.Entry<String, ?> entry : prefsMap.entrySet()) {
                                pw.println(entry.getKey() + "::" + entry.getValue().toString());
                            }
                            pw.close();
                            fw.close();
                            ToastUtils.showToastInUiThread(c, MainApp.sResources.getString(R.string.exported));
                        } catch (FileNotFoundException e) {
                            ToastUtils.showToastInUiThread(c, MainApp.sResources.getString(R.string.filenotfound) + " " + file);
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    public static void importSharedPreferences(final Activity c) {
        new AlertDialog.Builder(c)
                .setMessage(MainApp.sResources.getString(R.string.import_from) + " " + file + " ?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
                        SharedPreferences.Editor editor = prefs.edit();
                        String line;
                        String[] lineParts;
                        try {
                            editor.clear();
                            editor.commit();

                            BufferedReader reader = new BufferedReader(new FileReader(file));
                            while ((line = reader.readLine()) != null) {
                                lineParts = line.split("::");
                                if (lineParts.length == 2) {
                                    if (lineParts[1].equals("true") || lineParts[1].equals("false")) {
                                        editor.putBoolean(lineParts[0], Boolean.parseBoolean(lineParts[1]));
                                    } else {
                                        editor.putString(lineParts[0], lineParts[1]);
                                    }
                                }
                            }
                            reader.close();
                            editor.commit();
                            OKDialog.show(c, MainApp.sResources.getString(R.string.setting_imported), MainApp.sResources.getString(R.string.restartingapp), new Runnable() {
                                @Override
                                public void run() {
                                    log.debug("Exiting");
                                    MainApp.instance().stopKeepAliveService();
                                    MainApp.bus().post(new EventAppExit());
                                    MainApp.closeDbHelper();
                                    c.finish();
                                    System.runFinalization();
                                    System.exit(0);
                                }
                            });
                        } catch (FileNotFoundException e) {
                            ToastUtils.showToastInUiThread(c, MainApp.sResources.getString(R.string.filenotfound) + " " + file);
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

}
