package ca.michaelyagi.recipeapplication;

/******************************************************************/
// Display's a recipe. User can choose to Edit or Delete recipe
/******************************************************************/

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

//import android.app.Fragment;
//import android.app.FragmentTransaction;

/**
 * Created by Michael on 12/26/2014.
 */
public class RecipeTabFragment extends Fragment {

    private int recipeId;
    private String recipeUser;

    private ScrollView svLayout;
    private FragmentActivity faActivity;

    private FragmentTransaction fragmentTransaction;
    private ShareActionProvider mShareActionProvider;
    private String recipeTitle;

    private int COL_NUM = 2;

    /******************************************************************/
    // On create
    /******************************************************************/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        faActivity  = (FragmentActivity)    super.getActivity();
        svLayout    = (ScrollView)    inflater.inflate(R.layout.fragment_recipe_detail, container, false);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("Recipe");

        //Get arguments passed
        recipeId = getArguments().getInt("recipe_id");
        recipeUser = getArguments().getString("recipe_user");

        //Override overflow menu so user can edit this recipe
        setHasOptionsMenu(true);

        //Make GET request to see this recipe
        new GetRecipeRequestTask().execute("http://" + Utils.getApiServer() + "/api/v1/json/recipe/" + recipeId);

        return svLayout;
    }

    /******************************************************************/
    // Menu Setup
    /******************************************************************/
    //Enable/disable items in overflow menu or otherwise dynamically modify the contents
    @Override
    public void onPrepareOptionsMenu(Menu menu) {

        MenuItem shareItem = menu.findItem(R.id.menu_item_share);
        shareItem.setVisible(true);

        MenuItem editItem = menu.findItem(R.id.menu_edit);
        editItem.setVisible(false);

        MenuItem deleteRecipeItem = menu.findItem(R.id.menu_delete);
        deleteRecipeItem.setVisible(false);

        //Not logged in
        if (SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()).length() > 0 && recipeUser.length() > 0 && recipeUser.equals(SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()))) {
            editItem.setVisible(true);
            deleteRecipeItem.setVisible(true);
        }
    }

    //Item selection in the Overflow menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //Share button
            case R.id.menu_item_share:
                Intent email = new Intent(Intent.ACTION_SEND);
                String subject = SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()) + " wants to share a recipe";
                email.putExtra(Intent.EXTRA_SUBJECT, subject);
                String message;
                if (SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()).length() == 0) {
                    message = SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()) + " wants to share '" + recipeTitle + "'<br><br>View this recipe <a href='http://" + Utils.getWebsiteUrl() + "/food/recipe/id/" + recipeId + "'>here</a>";
                } else {
                    message = "Someone has shared '" + recipeTitle + "'<br><br>View this recipe <a href='http://" + Utils.getWebsiteUrl() + "/food/recipe/id/" + recipeId + "'>here</a>";
                }
                email.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(new StringBuilder().append(message).toString()));
                email.setType("text/html");
                startActivity(Intent.createChooser(email, "Select an application:"));
                break;
            //Edit a recipe
            case R.id.menu_edit:

                fragmentTransaction = getParentFragment().getFragmentManager().beginTransaction();

                EditRecipeFragment editFragment = new EditRecipeFragment();

                Bundle args = new Bundle();
                args.putInt("recipe_id", recipeId);
                args.putString("recipe_user", recipeUser);
                editFragment.setArguments(args);

                fragmentTransaction.replace(R.id.content_frame, editFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                break;
            //Delete a recipe with dialog
            case R.id.menu_delete:

                new AlertDialog.Builder(svLayout.getContext())
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setTitle("Confirmation")
                        .setMessage("Delete this recipe?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                            //Confirm delete
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //DELETE request to delete this recipe
                                new DeleteRequestTask().execute("http://" + Utils.getApiServer() + "/api/v1/json/recipe/" + recipeId);
                            }

                        })
                        .setNegativeButton("No", null)
                        .show();
                break;
        }

        return true;
    }

    /******************************************************************/
    // AsyncTasks
    /******************************************************************/

    //DELETE this recipe
    class DeleteRequestTask extends AsyncTask<String, String, String> {

        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = Utils.createProgressDialog(getActivity());
            dialog.show();
        }

        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString = null;
            HttpDelete delete = null;
            try {
                delete = new HttpDelete(uri[0]);

                String strValue = SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()) + ":" + SaveSharedPreference.getPassword(RecipeBookApplication.getAppContext());
                String basicAuth = "Basic " + Base64.encodeToString(strValue.getBytes(), Base64.NO_WRAP);
                delete.setHeader("Authorization", basicAuth);
                delete.setHeader("Accept", "application/json");
                delete.setHeader("Content-type", "application/json");

                response = httpclient.execute(delete);
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    responseString = out.toString();
                } else{
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (IOException e) {
                //TODO Handle problems..
                System.out.println(e.toString());
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

                    if (jsonObj.getString("retval").equals("1") && jsonObj.getString("message").equals("Success")) {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        Toast.makeText(svLayout.getContext(), "Recipe Deleted...", Toast.LENGTH_SHORT).show();

                        //Redirect to My Recipes Page
                        Bundle args = new Bundle();
                        args.putBoolean("viewbyuser_filter", true);

                        fragmentTransaction = getParentFragment().getFragmentManager().beginTransaction();
                        BrowseFragment browseRecipeFragment;
                        browseRecipeFragment = new BrowseFragment();
                        browseRecipeFragment.setArguments(args);
                        fragmentTransaction.replace(R.id.content_frame, browseRecipeFragment);
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                    } else {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        Toast.makeText(svLayout.getContext(), "Could Not Delete Recipe...", Toast.LENGTH_SHORT).show();
                    }

                } catch (JSONException e) {
                    //TODO
                    System.out.println(e.toString());
                }
            } else {
                Toast.makeText(svLayout.getContext(), "Connection Error...", Toast.LENGTH_SHORT).show();
                Utils.reconnectDialog(getActivity());
            }

            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

    //GET recipe details
    class GetRecipeRequestTask extends AsyncTask<String, String, String> {

        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = Utils.createProgressDialog(getActivity());
            dialog.show();
        }

        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString = null;
            try {
                response = httpclient.execute(new HttpGet(uri[0]));
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    out.close();
                    responseString = out.toString();
                } else{
                    //Closes the connection.
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (IOException e) {
                //TODO Handle problems..
                System.out.println(e.toString());
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

                    RecipeData recipeData = new RecipeData();

                    //Get all keys of JSON object
                    Iterator keys = jsonObj.keys();

                    //If request was successful
                    if (jsonObj.getString("retval").equals("1") && jsonObj.getString("message").equals("Success")) {
                        String tempKey = "";

                        //Loop through objects by key
                        while (keys.hasNext()) {
                            tempKey = keys.next().toString();

                            switch (tempKey) {
                                case "id":
                                    recipeData.id = Integer.parseInt(jsonObj.get(tempKey).toString());
                                    break;
                                case "published":
                                    recipeData.published = Integer.parseInt(jsonObj.get(tempKey).toString());
                                    break;
                                case "title":
                                    recipeData.title = jsonObj.get(tempKey).toString();
                                    recipeTitle = jsonObj.get(tempKey).toString();
                                    break;
                                case "prep_time":
                                    recipeData.prepTime = jsonObj.get(tempKey).toString();
                                    break;
                                case "cook_time":
                                    recipeData.cookTime = jsonObj.get(tempKey).toString();
                                    break;
                                case "serves":
                                    recipeData.serves = Integer.parseInt(jsonObj.get(tempKey).toString());
                                    break;
                                case "user":
                                    recipeData.user = jsonObj.get(tempKey).toString();
                                    break;
                                case "ingredients":
                                    JSONArray ingredientList;
                                    if (jsonObj.get(tempKey) instanceof JSONObject) {
                                        ingredientList = new JSONArray("[" + jsonObj.get(tempKey).toString() + "]");
                                    } else {
                                        ingredientList = new JSONArray(jsonObj.get(tempKey).toString());
                                    }

                                    for (int i = 0; i < ingredientList.length(); i++) {
                                        JSONObject ingredientObj = new JSONObject(ingredientList.getString(i));
                                        IngredientData ingredientData = new IngredientData();

                                        //Get all keys of JSON object
                                        Iterator ingredientKeys = ingredientObj.keys();
                                        while (ingredientKeys.hasNext()) {
                                            tempKey = ingredientKeys.next().toString();

                                            switch (tempKey) {
                                                case "sort_order":
                                                    ingredientData.sortOrder = Integer.parseInt(ingredientObj.get(tempKey).toString());
                                                    break;
                                                case "amount":
                                                    ingredientData.amount = ingredientObj.get(tempKey).toString();
                                                    break;
                                                case "unit":
                                                    ingredientData.unit = ingredientObj.get(tempKey).toString();
                                                    break;
                                                case "ingredient":
                                                    ingredientData.ingredient = ingredientObj.get(tempKey).toString();
                                                    break;
                                            }
                                        }
                                        recipeData.ingredientList.add(ingredientData);

                                    }
                                    break;
                                case "steps":
                                    JSONArray stepList;
                                    if (jsonObj.get(tempKey) instanceof JSONObject) {
                                        stepList = new JSONArray("[" + jsonObj.get(tempKey).toString() + "]");
                                    } else {
                                        stepList = new JSONArray(jsonObj.get(tempKey).toString());
                                    }

                                    for (int i = 0; i < stepList.length(); i++) {
                                        JSONObject stepObj = new JSONObject(stepList.getString(i));
                                        StepData stepData = new StepData();

                                        //Get all keys of JSON object
                                        Iterator stepKeys = stepObj.keys();
                                        while (stepKeys.hasNext()) {
                                            tempKey = stepKeys.next().toString();

                                            switch (tempKey) {
                                                case "sort_order":
                                                    stepData.sortOrder = Integer.parseInt(stepObj.get(tempKey).toString());
                                                    break;
                                                case "description":
                                                    stepData.description = stepObj.get(tempKey).toString();
                                                    break;
                                            }
                                        }
                                        recipeData.stepList.add(stepData);
                                    }
                                    break;
                                case "tags":
                                    JSONArray tagList;
                                    if (jsonObj.get(tempKey) instanceof JSONObject) {
                                        tagList = new JSONArray("[" + jsonObj.get(tempKey).toString() + "]");
                                    } else {
                                        tagList = new JSONArray(jsonObj.get(tempKey).toString());
                                    }

                                    for (int i = 0; i < tagList.length(); i++) {
                                        JSONObject tagObj = new JSONObject(tagList.getString(i));

                                        //Get all keys of JSON object
                                        Iterator tagKeys = tagObj.keys();
                                        while (tagKeys.hasNext()) {
                                            tempKey = tagKeys.next().toString();

                                            switch (tempKey) {
                                                case "keyword":
                                                    recipeData.tagList.add(tagObj.get(tempKey).toString());
                                                    break;
                                            }
                                        }
                                    }
                                    break;
                            }


                        }

                        createDetailPage(recipeData);

                    }

                } catch (JSONException e) {
                    //TODO
                    System.out.println(e.toString());
                }
            } else {
                Toast.makeText(svLayout.getContext(), "Connection Error...", Toast.LENGTH_SHORT).show();
                Utils.reconnectDialog(getActivity());
            }

            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

    //Create the detail page
    protected void createDetailPage(RecipeData recipeData) {

        int counter = 1;

        LinearLayout ll;

        if (recipeData.published == 0) {
            TextView draftText = (TextView)svLayout.findViewById(R.id.detailDraftTextView);
            draftText.setText("Draft");
        }

        //Set the title
        TextView titleText = (TextView)svLayout.findViewById(R.id.detailTitleTextView);
        titleText.setText(Html.fromHtml(recipeData.title));

        //Set user
        TextView userText = (TextView)svLayout.findViewById(R.id.detailUserTextView);
        final String userStr = recipeData.user;
        Paint paint = new Paint();
        paint.setARGB(125,125,125,125);
        userText.setTextColor(Color.parseColor("#009688"));
        userText.setPaintFlags( paint.getFlags());
        userText.setText(Html.fromHtml(userStr));

        userText.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                //Pass tag keyword to BrowseActivity with args
                Bundle args = new Bundle();
                args.putBoolean("viewbyuser_filter", true);
                args.putString("user",userStr);

                fragmentTransaction = getParentFragment().getFragmentManager().beginTransaction();
                BrowseFragment browseRecipeFragment = new BrowseFragment();
                browseRecipeFragment.setArguments(args);
                fragmentTransaction.replace(R.id.content_frame, browseRecipeFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });

        //Set the ingredients list
        Iterator<IngredientData> ingredientIt = recipeData.ingredientList.iterator();
        LinearLayout igredientLayout = (LinearLayout)svLayout.findViewById(R.id.ingredientsLayout);
        counter = 1;
        while(ingredientIt.hasNext()) {
            IngredientData obj = ingredientIt.next();
            String ingredientStr = obj.amount + " " + obj.unit + " " + obj.ingredient;
            ingredientStr = ingredientStr.trim();

            if (counter == 1) {
                TextView ingredientText = (TextView)svLayout.findViewById(R.id.ingredientTextView);
                ingredientText.setTextColor(Color.DKGRAY);
                ingredientText.setText(Html.fromHtml(ingredientStr));
            } else {
                TextView ingredientText = new TextView(super.getActivity().getApplicationContext());
                ingredientText.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
                ingredientText.setTextColor(Color.DKGRAY);
                float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
                ingredientText.setPadding(Math.round(px), Math.round(px), Math.round(px), Math.round(px));
                ingredientText.setTextSize(16);
                ingredientText.setText(Html.fromHtml(ingredientStr));
                igredientLayout.addView(ingredientText);
            }

            View hrLine = new View(super.getActivity().getApplicationContext());
            hrLine.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            hrLine.getLayoutParams().height = 2;
            hrLine.setBackgroundColor(Color.LTGRAY);
            igredientLayout.addView(hrLine);

            counter++;

        }

        //Set the step list
        Iterator<StepData> stepIt = recipeData.stepList.iterator();
        LinearLayout stepLayout = (LinearLayout)svLayout.findViewById(R.id.stepsLayout);
        counter = 1;
        while(stepIt.hasNext()) {
            StepData obj = stepIt.next();
            String stepStr = obj.description;

            if (counter == 1) {
                TextView stepText = (TextView)svLayout.findViewById(R.id.stepTextView);
                stepText.setTextColor(Color.BLACK);
                stepText.setText(Html.fromHtml(stepStr));
            } else {
                TextView stepText = new TextView(super.getActivity().getApplicationContext());
                stepText.setTextColor(Color.BLACK);
                float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
                stepText.setPadding(Math.round(px), Math.round(px), Math.round(px), Math.round(px));
                stepText.setTextSize(16);
                stepText.setText(Html.fromHtml(stepStr));
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);

                stepLayout.addView(stepText, layoutParams);
            }

            counter++;
        }

        //Times and serving
        String servingStr = Integer.toString(recipeData.serves);
        if(servingStr != null && !servingStr.isEmpty() && recipeData.serves > 0) {
            servingStr = "Serves: " + servingStr;
            TextView servingText = (TextView)svLayout.findViewById(R.id.servesTextView);
            servingText.setText(servingStr);
        }

        //Prep
        if(recipeData.prepTime != null && !recipeData.prepTime.isEmpty() && !recipeData.prepTime.equals("00:00:00")) {
            String prepStr = recipeData.prepTime;
            prepStr = prepStr.substring(0, prepStr.length() - 3);
            prepStr = "Prep Time: " + prepStr;
            TextView prepText = (TextView)svLayout.findViewById(R.id.prepTextView);
            prepText.setText(prepStr);
        }

        //Cooking
        if(recipeData.cookTime != null && !recipeData.cookTime.isEmpty() && !recipeData.cookTime.equals("00:00:00")) {
            String cookStr = recipeData.cookTime;
            cookStr = cookStr.substring(0, cookStr.length() - 3);
            cookStr = "Cook Time: " + cookStr;
            TextView prepText = (TextView)svLayout.findViewById(R.id.cookTextView);
            prepText.setText(cookStr);
        };

        //Set the tag list
        if (recipeData.tagList.size() > 0) {
            RelativeLayout tagLayout = (RelativeLayout)svLayout.findViewById(R.id.tagsLayout);

            LayoutInflater inflater = LayoutInflater.from(super.getActivity().getApplicationContext());
            View view = inflater.inflate(R.layout.fragment_recipe_detail, null);
            ll = new LinearLayout(super.getActivity());
            ll.setOrientation(LinearLayout.HORIZONTAL);
            ll.setGravity(Gravity.CENTER);
            Iterator<String> tagIt = recipeData.tagList.iterator();

            TextView tagText = new TextView(super.getActivity().getApplicationContext());
            tagText.setText("Tags: ");
            tagText.setTextColor(Color.BLACK);
            ((LinearLayout) ll).addView(tagText);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(0, 15, 0, 15);

            while (tagIt.hasNext()) {
                final String tagObjStr = tagIt.next();

                tagText = new TextView(super.getActivity().getApplicationContext());
                tagText.setTextColor(Color.parseColor("#009688"));
                tagText.setPaintFlags( paint.getFlags());

                tagText.setText(Html.fromHtml(tagObjStr));

                tagText.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v){
                        //Pass tag keyword to BrowseActivity with args
                        Bundle args = new Bundle();
                        args.putBoolean("viewbytag_filter", true);
                        args.putString("keyword",tagObjStr);

                        fragmentTransaction = getParentFragment().getFragmentManager().beginTransaction();
                        BrowseFragment browseRecipeFragment = new BrowseFragment();
                        browseRecipeFragment.setArguments(args);
                        fragmentTransaction.replace(R.id.content_frame, browseRecipeFragment);
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                    }
                });

                ((LinearLayout) ll).addView(tagText);

                if (tagIt.hasNext()) {
                    tagText = new TextView(super.getActivity().getApplicationContext());
                    tagText.setText(", ");
                    tagText.setTextColor(Color.BLACK);
                    ((LinearLayout) ll).addView(tagText);
                }
            }

            tagLayout.addView(ll, layoutParams);

        }

    }

    /******************************************************************/
    // Classes
    /******************************************************************/
    private class RecipeData {
        int id;
        int published;
        String title;
        String prepTime;
        String cookTime;
        int serves;
        String user;
        List<IngredientData> ingredientList = new ArrayList<IngredientData>();
        List<StepData> stepList = new ArrayList<StepData>();
        List<String> tagList = new ArrayList<String>();

        @Override
        public String toString() {
            return this.title;
        }
    }

    private class IngredientData {
        int sortOrder;
        String amount;
        String unit;
        String ingredient;

        @Override
        public String toString() {
            return this.ingredient;
        }
    }

    private class StepData {
        int sortOrder;
        String description;

        @Override
        public String toString() {
            return this.description;
        }
    }

}
