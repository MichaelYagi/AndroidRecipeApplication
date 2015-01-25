package ca.michaelyagi.recipeapplication;

/******************************************************************/
// Login action
/******************************************************************/

import android.app.Activity;
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
import android.view.View.OnClickListener;
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
 * Created by Michael on 12/27/2014.
 */
public class LoginFragment extends Fragment {
    private RelativeLayout llLayout;
    private EditText usernameEdit;
    private EditText passwordEdit;
    private Button loginButton;
    private Button registerButton;
    private Button forgotButton;
    private String username;
    private String password;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        llLayout    = (RelativeLayout)    inflater.inflate(R.layout.fragment_login, container, false);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("Login");

        loginButton = (Button)llLayout.findViewById(R.id.login_button);

        //Login button clicked
        loginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                usernameEdit = (EditText)llLayout.findViewById(R.id.username);
                passwordEdit = (EditText)llLayout.findViewById(R.id.password);

                username = usernameEdit.getText().toString();
                password = passwordEdit.getText().toString();

                if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
                    Toast.makeText(llLayout.getContext(), "Must enter credentials...",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(llLayout.getContext(), "Logging in...",Toast.LENGTH_SHORT).show();

                    //Verify user credentials
                    //Make GET request and add to list
                    new VerifyCredTask().execute(SaveSharedPreference.getApiServer(RecipeBookApplication.getAppContext()) + "/api/v1/json/user/" + username + "/verify");
                }
            }
        });

        //Register button clicked
        registerButton = (Button)llLayout.findViewById(R.id.register_button);
        registerButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //Go to registration activity
                FragmentManager fragmentManager = getFragmentManager();

                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                RegisterFragment registerFragment = new RegisterFragment();
                fragmentTransaction.replace(R.id.content_frame, registerFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });

        //Retrieve credentials button clicked
        forgotButton = (Button)llLayout.findViewById(R.id.forgot_button);
        forgotButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fragmentManager = getFragmentManager();

                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                RecoverFragment recoverFragment = new RecoverFragment();
                fragmentTransaction.replace(R.id.content_frame, recoverFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();

            }
        });

        return llLayout;
    }

    public interface OnFragmentInteractionListener {
        public void highlightDrawerItem(int row);
    }

    private OnFragmentInteractionListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    /******************************************************************/
    // AsyncTasks
    /******************************************************************/
    //Verify a users credentials
    class VerifyCredTask extends AsyncTask<String, String, String>{

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
                jsonObj.put("password", password);
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
                    if (jsonObj.getString("retval").equals("1")) {

                        String email = jsonObj.getString("email");
                        int userid = Integer.parseInt(jsonObj.getString("userid"));

                        //Store username password
                        SaveSharedPreference.setUsername(RecipeBookApplication.getAppContext(), username);
                        SaveSharedPreference.setPassword(RecipeBookApplication.getAppContext(), password);
                        if (email != null && !email.isEmpty()) {
                            SaveSharedPreference.setEmail(RecipeBookApplication.getAppContext(), email);
                        }
                        if (userid > 0) {
                            SaveSharedPreference.setUserID(RecipeBookApplication.getAppContext(), userid);
                        }

                        //Hide keyboard after input
                        View target = getView().findFocus();
                        if (target != null) {
                            InputMethodManager imm = (InputMethodManager) target.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(target.getWindowToken(), 0);
                        }

                        mListener.highlightDrawerItem(1);

                        FragmentManager fragmentManager = getFragmentManager();

                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        BrowseFragment browseRecipeFragment = new BrowseFragment();
                        fragmentTransaction.replace(R.id.content_frame, browseRecipeFragment);
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                    } else {
                        Toast.makeText(llLayout.getContext(), jsonObj.getString("message"),Toast.LENGTH_SHORT).show();
                    }

                } catch (JSONException e) {
                    //TODO
                }
            } else {
                Toast.makeText(llLayout.getContext(), "Connection Error...",Toast.LENGTH_SHORT).show();
                Utils.reconnectDialog(getActivity());
            }

            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }

    }
}
