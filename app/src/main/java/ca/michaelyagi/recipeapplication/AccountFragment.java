package ca.michaelyagi.recipeapplication;

/******************************************************************/
// Account Options, change email and password
/******************************************************************/

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
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

/**
 * Created by Michael on 12/29/2014.
 */
public class AccountFragment extends Fragment {
    private RelativeLayout llLayout;
    private EditText emailEdit;
    private EditText passwordEdit;
    private EditText passwordConfirmEdit;
    private Button accountButton;
    private String email;
    private String password;
    private String passwordConfirm;
    private String curUsername;
    private String curPassword;
    private String curEmail;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        llLayout    = (RelativeLayout)    inflater.inflate(R.layout.fragment_account, container, false);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("Account");

        //Get the username and password from common SaveSharedPreference
        curUsername = SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext());
        curPassword = SaveSharedPreference.getPassword(RecipeBookApplication.getAppContext());
        curEmail = SaveSharedPreference.getEmail(RecipeBookApplication.getAppContext());

        //Prefill email edit text
        emailEdit = (EditText) llLayout.findViewById(R.id.account_email);
        emailEdit.setText(curEmail);

        //Save the account changes onclick
        accountButton = (Button)llLayout.findViewById(R.id.account_button);
        accountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                passwordEdit = (EditText) llLayout.findViewById(R.id.account_password);
                passwordConfirmEdit = (EditText) llLayout.findViewById(R.id.account_passwordConfirm);

                email = emailEdit.getText().toString();
                password = passwordEdit.getText().toString();
                passwordConfirm = passwordConfirmEdit.getText().toString();

                if (email == null || email.isEmpty()) {
                    Toast.makeText(llLayout.getContext(), "Must enter a valid email...", Toast.LENGTH_SHORT).show();
                } else if ( password != null && !password.isEmpty() && passwordConfirm != null && !passwordConfirm.isEmpty() && !password.equals(passwordConfirm) ) {
                    Toast.makeText(llLayout.getContext(), "Passwords do not match...", Toast.LENGTH_SHORT).show();
                } else {
                    //Verify user credentials
                    //Make GET request and add to list
                    new UpdateAccountTask().execute("http://" + Utils.getApiServer() + "/api/v1/json/user/" + curUsername);

                }
            }
        });

        return llLayout;
    }

    /******************************************************************/
    // AsynkTasks
    /******************************************************************/
    class UpdateAccountTask extends AsyncTask<String, String, String> {

        boolean passwordChanged = false;
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
                if (password != null && !password.isEmpty()) {
                    passwordChanged = true;
                    jsonObj.put("password",password);
                }
                jsonObj.put("email", email);

            } catch (JSONException e) {
                //TODO: Catch json exception
            }

            try {
                String strValue = curUsername + ":" + curPassword;
                String basicAuth = "Basic " + Base64.encodeToString(strValue.getBytes(), Base64.NO_WRAP);
                StringEntity se = new StringEntity(jsonObj.toString(),"UTF-8");
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                put.setHeader("Authorization", basicAuth);
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

                    //If request was successful
                    if (jsonObj.getString("retval").equals("1")) {

                        if (!email.equals(SaveSharedPreference.getEmail(RecipeBookApplication.getAppContext()))) {
                            if (jsonObj.getString("email_changed").equals("1")) {
                                SaveSharedPreference.setEmail(RecipeBookApplication.getAppContext(), email);
                                emailEdit.setText(email, TextView.BufferType.EDITABLE);
                            } else {
                                Toast.makeText(llLayout.getContext(), "Email not available...", Toast.LENGTH_SHORT).show();
                                emailEdit.setText(SaveSharedPreference.getEmail(RecipeBookApplication.getAppContext()), TextView.BufferType.EDITABLE);
                            }
                        }

                        if (passwordChanged && jsonObj.getString("password_changed").equals("1")) {

                            Toast.makeText(llLayout.getContext(), "Account updated, please login again...", Toast.LENGTH_SHORT).show();

                            //Logout
                            SaveSharedPreference.clearCredentials(RecipeBookApplication.getAppContext());

                            //Go to login
                            FragmentManager fragmentManager = getFragmentManager();

                            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                            LoginFragment loginFragment = new LoginFragment();
                            fragmentTransaction.replace(R.id.content_frame, loginFragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();

                            if (dialog.isShowing()) {
                                dialog.dismiss();
                            }
                        }

                        //Hide keyboard after input
                        View target = getView().findFocus();
                        if (target != null) {
                            InputMethodManager imm = (InputMethodManager) target.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(target.getWindowToken(), 0);
                        }

                        Toast.makeText(llLayout.getContext(), "Account Updated...", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(llLayout.getContext(), "Error...", Toast.LENGTH_SHORT).show();
                    }

                } catch (JSONException e) {
                    //TODO
                }
            } else {
                Toast.makeText(llLayout.getContext(), "Connection Error...", Toast.LENGTH_SHORT).show();
                Utils.reconnectDialog(getActivity());
            }

            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

}
