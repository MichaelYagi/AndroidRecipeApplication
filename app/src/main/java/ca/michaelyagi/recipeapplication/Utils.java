package ca.michaelyagi.recipeapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.WindowManager;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Michael on 1/5/2015.
 */
//General util class used by application
public class Utils {
    public static String apiServer;
    public static String webUrl;

    //Custom Progress Spinner with white background
    public static ProgressDialog createProgressDialog(Context mContext) {
        ProgressDialog dialog = new ProgressDialog(mContext,R.style.ProgressSpinnerTheme);

        try {
            dialog.show();
        } catch(WindowManager.BadTokenException e) {

        }

        dialog.setCancelable(false);

        dialog.setContentView(R.layout.progress_spinner);

        return dialog;
    }

    //Used for preview images before an image loads
    public static Bitmap adjustBitmapOpacity(Bitmap bitmap, int opacity) {
        Bitmap mutableBitmap = bitmap.isMutable()
                ? bitmap
                : bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        int colour = (opacity & 0xFF) << 24;
        canvas.drawColor(colour, PorterDuff.Mode.DST_IN);
        return mutableBitmap;
    }

    //Check internet connectivity
    public boolean isOnline(Context mContext) {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    //Check if a server is reachable
    public static boolean serverReachable(String serverUrl) {
        try {
            URL url = new URL(serverUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            int code = connection.getResponseCode();

            if (code == 200) {
                return true;
            } else {
                return false;
            }
        } catch(MalformedURLException e) {
            //TODO: catch malformed url exception
        } catch(IOException e) {
            //TODO: catch ioexception
        }

        return false;
    }

    public static AlertDialog reconnectDialog(Context mContext) {
        final Context context = mContext;
        final Activity activity = (Activity) context;

        AlertDialog dialog = new AlertDialog.Builder(mContext)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setTitle("Connection Error")
                .setMessage("Reconnect?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    //Reconnect to the website
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        System.out.println(which);
                        //Retry and recreate activity
                        activity.recreate();
                    }

                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {

                    //Exit the application
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                    }

                })
                .show();

        return dialog;
    }
}

