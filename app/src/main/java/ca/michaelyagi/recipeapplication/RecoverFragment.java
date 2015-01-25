package ca.michaelyagi.recipeapplication;

/******************************************************************/
// Recover user credentials
/******************************************************************/

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

/**
 * Created by Michael on 12/29/2014.
 */
public class RecoverFragment extends Fragment {
    private RelativeLayout llLayout;
    private EditText emailEdit;
    private Button recoverButton;
    private String email;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        llLayout    = (RelativeLayout)    inflater.inflate(R.layout.fragment_recover, container, false);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("Recover");

        //Recover button clicked
        recoverButton = (Button)llLayout.findViewById(R.id.recover_button);
        recoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emailEdit = (EditText) llLayout.findViewById(R.id.recover_email);

                email = emailEdit.getText().toString();

                if (email == null || email.isEmpty()) {
                    Toast.makeText(llLayout.getContext(), "Must enter a valid email...",Toast.LENGTH_SHORT).show();
                } else {
                    //Recover account
                    new RecoverPasswordTask().execute(SaveSharedPreference.getApiServer(RecipeBookApplication.getAppContext()) + "/api/v1/json/recover");

                }
            }
        });

        return llLayout;
    }

    //Recover account, email the user with new recovered password
    class RecoverPasswordTask extends AsyncTask<String, String, String> {

        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = Utils.createProgressDialog(getActivity());
            dialog.show();
        }

        @Override
        protected String doInBackground(String... uri) {

            String responseString = null;

            HttpClient client = new DefaultHttpClient();
            HttpPut put = new HttpPut(uri[0]);

            JSONObject jsonObj = new JSONObject();
            try {
                jsonObj.put("email", email);
            } catch (JSONException e) {
                //TODO: Catch json exception
            }

            try {
                StringEntity se = new StringEntity(jsonObj.toString(),"UTF-8");
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                put.setHeader("Accept", "application/json");
                put.setHeader("Content-type", "application/json");
                put.setEntity(se);
            } catch (UnsupportedEncodingException e) {
                //TODO: Catch url encoding exception
            }

            try {
                HttpResponse response = client.execute(put);
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    responseString = out.toString();
                } else {
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (IOException e) {
                //TODO Handle problems..
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            //Result is responseString from request
            super.onPostExecute(result);

            if (result != null && result.length() > 0) {
                try {
                    //Turn response into JSON object
                    JSONObject jsonObj = new JSONObject(result);

                    //Get all keys of JSON object
                    Iterator keys = jsonObj.keys();

                    //If request was successful
                    if (jsonObj.getString("retval").equals("1") && jsonObj.getString("message").equals("Success")) {

                        Toast.makeText(llLayout.getContext(), "Sending Email...", Toast.LENGTH_SHORT).show();

                        //Hide keyboard after input
                        View view = getView();
                        if (view != null) {
                            View target = view.findFocus();
                            if (target != null) {
                                InputMethodManager imm = (InputMethodManager) target.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                                imm.hideSoftInputFromWindow(target.getWindowToken(), 0);
                            }
                        }

                        //Go back to browse
                        FragmentManager fragmentManager = getFragmentManager();

                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                        BrowseFragment browseRecipeFragment;

                        browseRecipeFragment = new BrowseFragment();
                        fragmentTransaction.replace(R.id.content_frame, browseRecipeFragment);
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                    } else {
                        Toast.makeText(llLayout.getContext(), "Error...", Toast.LENGTH_SHORT).show();
                    }

                } catch (JSONException e) {
                    //TODO
                }
            }  else {
                Toast.makeText(llLayout.getContext(), "Connection Error...", Toast.LENGTH_SHORT).show();
                Utils.reconnectDialog(getActivity());
            }

            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }
}
