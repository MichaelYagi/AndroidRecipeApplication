package ca.michaelyagi.recipeapplication;

/******************************************************************/
// Register a new user
/******************************************************************/

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
 * Created by Michael on 12/28/2014.
 */
public class RegisterFragment extends Fragment {
    private RelativeLayout llLayout;
    private EditText usernameEdit;
    private EditText emailEdit;
    private EditText passwordEdit;
    private EditText passwordConfirmEdit;
    private Button registerButton;
    private String username;
    private String email;
    private String password;
    private String passwordConfirm;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        llLayout    = (RelativeLayout)    inflater.inflate(R.layout.fragment_register, container, false);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("Register");

        //Register button onclick
        registerButton = (Button)llLayout.findViewById(R.id.register_button);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                usernameEdit = (EditText)llLayout.findViewById(R.id.register_username);
                emailEdit = (EditText)llLayout.findViewById(R.id.register_email);
                passwordEdit = (EditText)llLayout.findViewById(R.id.register_password);
                passwordConfirmEdit = (EditText)llLayout.findViewById(R.id.register_passwordConfirm);

                username = usernameEdit.getText().toString();
                email = emailEdit.getText().toString();
                password = passwordEdit.getText().toString();
                passwordConfirm = passwordConfirmEdit.getText().toString();

                if (username == null || username.isEmpty() ||
                        email == null || email.isEmpty() ||
                        password == null || password.isEmpty() ||
                        passwordConfirm == null || passwordConfirm.isEmpty()) {
                    Toast.makeText(llLayout.getContext(), "Must enter all values...",Toast.LENGTH_SHORT).show();
                } else if(!password.equals(passwordConfirm)) {
                    Toast.makeText(llLayout.getContext(), "Passwords do not match...",Toast.LENGTH_SHORT).show();
                } else {
                    new RegisterUserTask().execute("http://" + Utils.getApiServer() + "/api/v1/json/user");
                }
            }
        });

        return llLayout;
    }

    /******************************************************************/
    // AsyncTasks
    /******************************************************************/
    class RegisterUserTask extends AsyncTask<String, String, String> {

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
            HttpPut put= new HttpPut(uri[0]);

            JSONObject jsonObj = new JSONObject();
            try {
                jsonObj.put("username", username);
                jsonObj.put("password", password);
                jsonObj.put("email", email);
            } catch(JSONException e) {
                //TODO: Catch json exception
            }

            try {
                StringEntity se = new StringEntity( jsonObj.toString(),"UTF-8");
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                put.setHeader("Accept", "application/json");
                put.setHeader("Content-type", "application/json");
                put.setEntity(se);
            } catch(UnsupportedEncodingException e) {
                //TODO: Catch url encoding exception
            }

            try {
                HttpResponse response = client.execute(put);
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_OK){
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
                    if (jsonObj.getString("retval").length() > 0 && Integer.parseInt(jsonObj.getString("retval")) > 0) {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }

                        int userId = Integer.parseInt(jsonObj.getString("retval"));

                        //Store username password
                        SaveSharedPreference.setUsername(RecipeBookApplication.getAppContext(), username);
                        SaveSharedPreference.setPassword(RecipeBookApplication.getAppContext(), password);
                        SaveSharedPreference.setEmail(RecipeBookApplication.getAppContext(),email);
                        SaveSharedPreference.setUserID(RecipeBookApplication.getAppContext(),userId);

                        //Hide keyboard after input
                        View target = getView().findFocus();
                        if (target != null) {
                            InputMethodManager imm = (InputMethodManager) target.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(target.getWindowToken(), 0);
                        }

                        Toast.makeText(llLayout.getContext(), "Logging in...", Toast.LENGTH_SHORT).show();

                        //Go back to browse
                        FragmentManager fragmentManager = getFragmentManager();

                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                        BrowseFragment browseRecipeFragment;

                        browseRecipeFragment = new BrowseFragment();
                        fragmentTransaction.replace(R.id.content_frame, browseRecipeFragment);
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                    } else {
                        Toast.makeText(llLayout.getContext(), "Username or email already taken", Toast.LENGTH_SHORT).show();
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
