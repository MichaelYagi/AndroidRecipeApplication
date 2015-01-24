package ca.michaelyagi.recipeapplication;

/******************************************************************/
// Add and Edit a recipe
/******************************************************************/

import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Michael on 12/30/2014.
 */
public class EditRecipeFragment extends Fragment {

    int recipeId;
    String recipeUser;
    String recipeTitle;

    private ScrollView llLayout;
    private FragmentActivity faActivity;
    private static int RESULT_LOAD_IMG = 1;

    private EditText titleEdit;
    private ToggleButton publishToggle;
    private EditText unitEdit;
    private EditText amountEdit;
    private EditText ingredientEdit;
    private EditText stepEdit;
    private EditText servesEdit;
    private EditText prepEdit;
    private EditText cookEdit;
    private MultiAutoCompleteTextView tagsEdit;
    ArrayAdapter<String> tagListAdapter;
    List<String> tagList = new ArrayList<String>();
    private Button addImageButton;
    private Button addIngredientButton;
    private Button addStepButton;
    private Button removeButton;
    private Button updateButton;
    private ImageView recipeImageView;
    private LinearLayout linearLayout;
    private int ingredientCounter;
    private int stepCounter;
    private int imageCounter;
    private List<RecipeImageView> imageViewList = new ArrayList<RecipeImageView>();
    private List<IngredientEditText> ingredientEditTextList = new ArrayList<IngredientEditText>();
    private List<StepEditText> stepEditTextList = new ArrayList<StepEditText>();
    private static final String TIME24HOURS_PATTERN = "([01]?[0-9]|2[0-3]):[0-5][0-9]";
    private Pattern pattern = Pattern.compile(TIME24HOURS_PATTERN);
    private Matcher matcher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        faActivity  = (FragmentActivity)    super.getActivity();
        llLayout    = (ScrollView)    inflater.inflate(R.layout.fragment_edit_recipe, container, false);

        Bundle args = getArguments();
        if (args == null) {
            recipeId = 0;
            recipeUser = SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext());
            ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("Add");
        } else {
            recipeId = args.getInt("recipe_id");
            recipeUser = args.getString("recipe_user");
            ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("Edit");
        }

        servesEdit = (EditText) llLayout.findViewById(R.id.recipe_serves);
        prepEdit = (EditText) llLayout.findViewById(R.id.recipe_prep);
        cookEdit = (EditText) llLayout.findViewById(R.id.recipe_cook);

        /******************************************************************/
        // Tag text autocomplete
        /******************************************************************/
        //Tag Multiautocomplete
        tagListAdapter = new ArrayAdapter<String>(llLayout.getContext(), R.layout.ingredient_simple_row, R.id.rowIngredientTextView);
        tagListAdapter.setNotifyOnChange(true);
        tagsEdit = (MultiAutoCompleteTextView) llLayout.findViewById(R.id.recipe_tags);
        tagsEdit.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        tagsEdit.setAdapter(tagListAdapter);

        //Tag on text change - See if there are matching existing tags
        tagsEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence searchTermSeq, int start, int before, int count) {

                if (searchTermSeq.length() > 1 && !tagsEdit.isPerformingCompletion()) {
                    String searchTerm = searchTermSeq.toString();
                    try {
                        searchTerm = URLEncoder.encode(searchTerm, "UTF-8");
                    } catch(UnsupportedEncodingException e) {
                        //TODO: Catch URLEncoder exception
                    }
                    //Get a list of all tags on searchTerm
                    new TagSearchTask().execute("http://" + Utils.getApiServer() + "/api/v1/json/tag/" + searchTerm);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        //Tag onclick item select, clear the keyboard and dropdown list
        tagsEdit.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                clearKeyboard();
                tagsEdit.clearListSelection();
                tagsEdit.dismissDropDown();
            }
        });

        /******************************************************************/
        // Add or Edit a recipe
        /******************************************************************/
        //Get Recipe Details if editing
        if (recipeId > 0) {
            AsyncTask recipeTask = new RequestRecipeTask().execute("http://" + Utils.getApiServer() + "/api/v1/json/recipe/" + recipeId);
        } else {
            RecipeData dummy = new RecipeData();
            createEditDetailPage(dummy);
        }

        /******************************************************************/
        // Add Image, Add Ingredient, Add Step Button listeners
        /******************************************************************/
        //Add Image button pressed
        addImageButton = (Button) llLayout.findViewById(R.id.recipe_add_image_button);
        addImageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Create intent to Open Image applications like Gallery, Google Photos
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                // Start the Intent
                startActivityForResult(galleryIntent, RESULT_LOAD_IMG);

            }
        });

        //Add Ingredient button pressed
        addIngredientButton = (Button) llLayout.findViewById(R.id.recipe_add_ingredient_button);
        addIngredientButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addNewIngredientRow(null,null,null,true);
                ingredientCounter++;
            }
        });

        //Add Step button pressed
        addStepButton = (Button) llLayout.findViewById(R.id.recipe_add_step_button);
        addStepButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addNewStepRow(null,true);
                stepCounter++;
            }
        });

        //Update/Add button pressed
        updateButton = (Button) llLayout.findViewById(R.id.save_recipe);
        if (recipeId == 0) {
            updateButton.setText("Add Recipe");
        }
        updateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Validate inputs
                titleEdit = (EditText)llLayout.findViewById(R.id.recipe_title);
                ingredientEdit = (EditText)llLayout.findViewById(R.id.recipe_ingredient_1);
                stepEdit = (EditText)llLayout.findViewById(R.id.recipe_step_1);
                boolean hasError = false;

                if (titleEdit.getText().toString().length() == 0) {
                    titleEdit.setError("Recipe Title Require");
                    hasError = true;
                }
                if (ingredientEdit.getText().toString().length() == 0) {
                    ingredientEdit.setError("Recipe Ingredient Required");
                    hasError = true;
                }
                if (stepEdit.getText().toString().length() == 0) {
                    stepEdit.setError("Recipe Step Required");
                    hasError = true;
                }
                if (prepEdit.getText().toString().length() > 0) {
                    matcher = pattern.matcher(prepEdit.getText().toString());
                    if (!matcher.matches()) {
                        prepEdit.setError("Invalid time format, must be HH:MM");
                        hasError = true;
                    }
                }
                if (cookEdit.getText().toString().length() > 0) {
                    matcher = pattern.matcher(cookEdit.getText().toString());
                    if (!matcher.matches()) {
                        cookEdit.setError("Invalid time format, must be HH:MM");
                        hasError = true;
                    }
                }

                if (!hasError) {
                    recipeTitle = titleEdit.getText().toString();

                    //Prepare JSON for recipe for REST call
                    JSONObject jsonRecipeObj = getJsonRecipe();

                    boolean isNewRecipe = false;
                    AsyncTask uploadRecipeTask;
                    if (recipeId > 0) {
                        uploadRecipeTask = new UploadRecipeTask(jsonRecipeObj,isNewRecipe).execute("http://" + Utils.getApiServer() + "/api/v1/json/recipe/" + recipeId);
                    } else {
                        isNewRecipe = true;
                        uploadRecipeTask = new UploadRecipeTask(jsonRecipeObj,isNewRecipe).execute("http://" + Utils.getApiServer() + "/api/v1/json/recipe");
                    }

                    //Image Handling
                    Iterator<RecipeImageView> imageViewIt = imageViewList.iterator();
                    boolean isLastImage = false;
                    int counter = 0;
                    while (imageViewIt.hasNext()) {
                        RecipeImageView imageViewObj = imageViewIt.next();

                        if (!imageViewIt.hasNext()) {
                            isLastImage = true;
                        }

                        //Delete Images
                        if (!imageViewObj.newImage && imageViewObj.removeFlag) {
                            counter++;
                            new DeleteRecipeImageTask(isLastImage).execute("http://" + Utils.getApiServer() + "/api/v1/json/recipe/" + recipeId + "/image/" + imageViewObj.imageId);
                            //Add Image
                        } else if (imageViewObj.newImage) {
                            counter++;
                            if (!isNewRecipe) {
                                new AddRecipeImageTask(imageViewObj.imageBmp, imageViewObj.viewId, isLastImage).execute("http://" + Utils.getApiServer() + "/api/v1/json/recipe/" + recipeId + "/image");
                            }

                        }
                    }

                    clearKeyboard();

                    if (counter == 0) {
                        FragmentManager fragmentManager = getFragmentManager();

                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                        DetailFragment detailRecipeFragment;
                        detailRecipeFragment = new DetailFragment();

                        Bundle args = new Bundle();
                        args.putInt("recipe_id", recipeId);
                        args.putString("recipe_user",SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()));
                        detailRecipeFragment.setArguments(args);

                        fragmentTransaction.replace(R.id.content_frame, detailRecipeFragment);
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                    }
                }

            }
        });

        /******************************************************************/
        // Time picker for cook and prep times
        /******************************************************************/
        //Datepicker when click cook or prep time edittext
        prepEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(llLayout.getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        String hourStr = Integer.toString(selectedHour);
                        String minuteStr = Integer.toString(selectedMinute);

                        if (selectedHour < 10) {
                            hourStr = "0" + Integer.toString(selectedHour);
                        }
                        if (selectedMinute < 10) {
                            minuteStr = "0" + Integer.toString(selectedMinute);
                        }
                        prepEdit.setText( hourStr + ":" + minuteStr);
                    }
                }, hour, minute, true);//Yes 24 hour time
                mTimePicker.setTitle("Select Prep Time");
                mTimePicker.show();
            }
        });

        cookEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(llLayout.getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        String hourStr = Integer.toString(selectedHour);
                        String minuteStr = Integer.toString(selectedMinute);

                        if (selectedHour < 10) {
                            hourStr = "0" + Integer.toString(selectedHour);
                        }
                        if (selectedMinute < 10) {
                            minuteStr = "0" + Integer.toString(selectedMinute);
                        }
                        cookEdit.setText( hourStr + ":" + minuteStr);
                    }
                }, hour, minute, true);//Yes 24 hour time
                mTimePicker.setTitle("Select Cooking Time");
                mTimePicker.show();
            }
        });

        return llLayout;
    }

    /******************************************************************/
    // AsyncTasks
    /******************************************************************/
    //Get recipe data
    class RequestRecipeTask extends AsyncTask<String, String, String> {

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
                                    recipeData.title = Html.fromHtml(jsonObj.get(tempKey).toString()).toString();
                                    break;
                                case "prep_time":
                                    recipeData.prepTime = Html.fromHtml(jsonObj.get(tempKey).toString()).toString();
                                    break;
                                case "cook_time":
                                    recipeData.cookTime = Html.fromHtml(jsonObj.get(tempKey).toString()).toString();
                                    break;
                                case "serves":
                                    recipeData.serves = Integer.parseInt(jsonObj.get(tempKey).toString());
                                    break;
                                case "user":
                                    recipeData.user = Html.fromHtml(jsonObj.get(tempKey).toString()).toString();
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
                                                    ingredientData.amount = Html.fromHtml(ingredientObj.get(tempKey).toString()).toString();
                                                    break;
                                                case "unit":
                                                    ingredientData.unit = Html.fromHtml(ingredientObj.get(tempKey).toString()).toString();
                                                    break;
                                                case "ingredient":
                                                    ingredientData.ingredient = Html.fromHtml(ingredientObj.get(tempKey).toString()).toString();
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
                                                    stepData.description = Html.fromHtml(stepObj.get(tempKey).toString()).toString();
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
                                                    recipeData.tagList.add(Html.fromHtml(tagObj.get(tempKey).toString()).toString());
                                                    break;
                                            }
                                        }
                                    }
                                    break;
                                case "images_info":
                                    JSONArray imageInfo;

                                    if (jsonObj.get(tempKey) instanceof JSONObject) {
                                        imageInfo = new JSONArray("[" + jsonObj.get(tempKey).toString() + "]");
                                    } else if (jsonObj.get(tempKey) instanceof String) {
                                        imageInfo = new JSONArray("['" + jsonObj.get(tempKey).toString() + "']");
                                    } else {
                                        imageInfo = new JSONArray(jsonObj.get(tempKey).toString());
                                    }

                                    String imageUrl;
                                    for (int i = 0; i < imageInfo.length(); i++) {
                                        JSONObject jsonImageObj = new JSONObject(imageInfo.getString(i));

                                        imageUrl = "http://" + Utils.getWebsiteUrl() + "/media/recipeimages/" + recipeId + "/" + jsonImageObj.get("id").toString();
                                        if (jsonImageObj.get("extension") != null && !jsonImageObj.get("extension").toString().isEmpty()) {
                                            imageUrl = imageUrl + "." + jsonImageObj.get("extension").toString();
                                        }

                                        recipeData.imageUrlList.add(imageUrl);
                                    }
                                    break;
                            }


                        }

                        createEditDetailPage(recipeData);

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

    //Added or Updated recipe
    class UploadRecipeTask extends AsyncTask<String, String, String> {

        JSONObject recipeObj;
        boolean isNewRecipe;
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = Utils.createProgressDialog(getActivity());
            dialog.show();
        }

        public UploadRecipeTask(JSONObject recipeJsonObj,boolean isNewRecipe) {
            this.recipeObj = recipeJsonObj;
            this.isNewRecipe = isNewRecipe;
        }

        @Override
        protected String doInBackground(String... uri) {

            String responseString = null;

            HttpClient client = new DefaultHttpClient();
            HttpPut put = new HttpPut(uri[0]);

            try {

                String strValue = SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()) + ":" + SaveSharedPreference.getPassword(RecipeBookApplication.getAppContext());
                String basicAuth = "Basic " + Base64.encodeToString(strValue.getBytes(), Base64.NO_WRAP);
                StringEntity se = new StringEntity(this.recipeObj.toString(),"UTF-8");
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                put.setHeader("Authorization", basicAuth);
                put.setHeader("Accept", "application/json");
                put.setHeader("Content-type", "application/json");
                put.setEntity(se);
            } catch (UnsupportedEncodingException e) {
                System.out.println(e.toString());
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

                    //If request was successful
                    boolean updateSuccess = false;
                    if (recipeId > 0) {
                        if (!jsonObj.getString("user").equals(SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()))) {
                            Toast.makeText(llLayout.getContext(), "Error Updating Recipe...", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(llLayout.getContext(), "Recipe Updated...", Toast.LENGTH_SHORT).show();
                            updateSuccess = true;
                        }
                    } else {
                        if (jsonObj.getString("retval").equals("1") && jsonObj.getString("message").equals("Success")) {
                            Toast.makeText(llLayout.getContext(), "Recipe Added...", Toast.LENGTH_SHORT).show();
                            recipeId = Integer.parseInt(jsonObj.getString("id"));
                            updateSuccess = true;
                        } else {
                            Toast.makeText(llLayout.getContext(), "Error Adding Recipe...", Toast.LENGTH_SHORT).show();
                        }
                    }

                    if (updateSuccess) {
                        //Image Handling
                        Iterator<RecipeImageView> imageViewIt = imageViewList.iterator();
                        int counter = 0;
                        boolean isLastImage = false;
                        while (imageViewIt.hasNext()) {
                            RecipeImageView imageViewObj = imageViewIt.next();

                            if (!imageViewIt.hasNext()) {
                                isLastImage = true;
                            }

                            if (isNewRecipe) {
                                new AddRecipeImageTask(imageViewObj.imageBmp, imageViewObj.viewId, isLastImage).execute("http://" + Utils.getApiServer() + "/api/v1/json/recipe/" + recipeId + "/image");
                            }

                            //Delete Images
                            if (!imageViewObj.newImage && imageViewObj.removeFlag) {
                                counter++;
                            } else if (imageViewObj.newImage) {
                                counter++;
                            }
                        }
                        if (counter == 0) {
                            FragmentManager fragmentManager = getFragmentManager();

                            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                            DetailFragment detailRecipeFragment;
                            detailRecipeFragment = new DetailFragment();

                            Bundle args = new Bundle();
                            args.putInt("recipe_id", recipeId);
                            args.putString("recipe_user", SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()));
                            detailRecipeFragment.setArguments(args);

                            fragmentTransaction.replace(R.id.content_frame, detailRecipeFragment);
                            fragmentTransaction.addToBackStack(null);
                            fragmentTransaction.commit();
                        }
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

    //Download images for a recipe
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

        ImageView bmImage;
        boolean isNewImage;
        String uriDisplay;
        int curImageCount;

        public DownloadImageTask(ImageView bmImage,boolean isNewImage,int curImageCount) {
            this.bmImage = bmImage;
            this.isNewImage = isNewImage;
            this.curImageCount = curImageCount;
        }

        protected Bitmap doInBackground(String... urls) {
            uriDisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(uriDisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (MalformedURLException e) {
                try {
                    File file = new File(uriDisplay);
                    FileInputStream fileInputStream = new FileInputStream(file);
                    mIcon11 = BitmapFactory.decodeStream(fileInputStream);
                } catch(FileNotFoundException f) {
                    System.out.println("File not found:" + f.toString());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);

            RecipeImageView recipeImageView = new RecipeImageView();
            recipeImageView.imageBmp = result;
            recipeImageView.removeFlag = false;
            recipeImageView.newImage = this.isNewImage;
            recipeImageView.viewId = this.curImageCount;
            if (!this.isNewImage) {
                String temp = uriDisplay.substring(uriDisplay.lastIndexOf('/') + 1);
                String fileNameWithOutExt = temp.replaceFirst("[.][^.]+$", "");
                recipeImageView.imageId = Integer.parseInt(fileNameWithOutExt);
            }
            imageViewList.add(recipeImageView);
        }
    }

    //Save new images for recipe
    class AddRecipeImageTask extends AsyncTask<String, String, String> {

        Bitmap imgBmp;
        int viewId;
        boolean isLastImage;
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = Utils.createProgressDialog(getActivity());
            dialog.show();
        }

        public AddRecipeImageTask(Bitmap imgBmp,int viewId,boolean isLastImage) {
            this.imgBmp = imgBmp;
            this.isLastImage = isLastImage;
            this.viewId = viewId;
        }

        @Override
        protected String doInBackground(String... uri) {

            HttpClient client = new DefaultHttpClient();
            HttpResponse response = null;
            String responseString = null;
            HttpEntity entity = null;

            try {
                Bitmap bitmap = this.imgBmp;

                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 75, bos);
                    byte[] data = bos.toByteArray();
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost postRequest = new HttpPost(uri[0]);
                    ByteArrayBody bab = new ByteArrayBody(data, "file.jpg");
                    MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
                    reqEntity.addPart("uploaded", bab);
                    String strValue = SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()) + ":" + SaveSharedPreference.getPassword(RecipeBookApplication.getAppContext());
                    String basicAuth = "Basic " + Base64.encodeToString(strValue.getBytes(), Base64.NO_WRAP);
                    postRequest.setHeader("Authorization", basicAuth);
                    postRequest.setEntity(reqEntity);

                    response = httpClient.execute(postRequest);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
                    String sResponse;
                    StringBuilder s = new StringBuilder();

                    while ((sResponse = reader.readLine()) != null) {
                        s = s.append(sResponse);
                    }
                    responseString = s.toString();
                } catch (Exception e) {
                    // handle exception here
                    System.out.println("Error uploading image:"+e.toString());
                }

            } catch (Exception e) {
                System.out.println("Error uploading image:"+e.toString());
            }

            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            //TEST to show bitmap sent
            /*
            Toast toast = new Toast(llLayout.getContext().getApplicationContext());
            ImageView imageView = new ImageView(llLayout.getContext().getApplicationContext());
            imageView.setTag("recipe_image_" + imageCounter);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(300,200));
            imageView.setImageBitmap(this.imgBmp);
            toast.setView(imageView);
            toast.show();
            */

            //Result is responseString from request
            super.onPostExecute(result);

            if (result != null && result.length() > 0) {
                try {
                    //Turn response into JSON object
                    JSONObject jsonObj = new JSONObject(result);

                    //Get all keys of JSON object
                    Iterator keys = jsonObj.keys();

                    //If request was successful
                    if (jsonObj.getString("retval").equals("true") && jsonObj.getString("message").equals("Image saved")) {
                        Toast.makeText(llLayout.getContext(), "Images Added...", Toast.LENGTH_SHORT).show();

                        //Find the object in the array by viewid and set their isnewflag to false and imageid
                        int imageId = Integer.parseInt(jsonObj.getString("imageid"));

                        //Set flags
                        for (RecipeImageView iView : imageViewList) {
                            if (iView.viewId == this.viewId) {
                                iView.removeFlag = false;
                                iView.newImage = false;
                                iView.imageId = imageId;
                                break;
                            }
                        }
                    } else {
                        Toast.makeText(llLayout.getContext(), "Error Adding Images...", Toast.LENGTH_SHORT).show();
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

            if (isLastImage) {
                FragmentManager fragmentManager = getFragmentManager();

                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                DetailFragment detailRecipeFragment;
                detailRecipeFragment = new DetailFragment();

                Bundle args = new Bundle();
                args.putInt("recipe_id", recipeId);
                args.putString("recipe_user",SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()));
                detailRecipeFragment.setArguments(args);

                fragmentTransaction.replace(R.id.content_frame, detailRecipeFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        }
    }

    //Delete Recipe
    class DeleteRecipeImageTask extends AsyncTask<String, String, String> {

        boolean isLastImage;
        private ProgressDialog dialog;

        @Override
        protected void onPreExecute() {
            dialog = Utils.createProgressDialog(getActivity());
            dialog.show();
        }

        public DeleteRecipeImageTask(boolean isLastImage) {
            this.isLastImage = isLastImage;
        }

        @Override
        protected String doInBackground(String... uri) {

            String responseString = null;

            HttpClient client = new DefaultHttpClient();
            HttpDelete delete = new HttpDelete(uri[0]);

            String strValue = SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()) + ":" + SaveSharedPreference.getPassword(RecipeBookApplication.getAppContext());
            String basicAuth = "Basic " + Base64.encodeToString(strValue.getBytes(), Base64.NO_WRAP);
            delete.setHeader("Authorization", basicAuth);
            delete.setHeader("Accept", "application/json");
            delete.setHeader("Content-type", "application/json");

            try {
                HttpResponse response = client.execute(delete);
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

                    //If request was successful
                    if (jsonObj.getString("retval").equals("1")) {
                        Toast.makeText(llLayout.getContext(), "Deleting Recipe Images...", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(llLayout.getContext(), "Error Deleting Recipe Images...", Toast.LENGTH_SHORT).show();
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

            if (isLastImage) {
                FragmentManager fragmentManager = getFragmentManager();

                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                DetailFragment detailRecipeFragment;
                detailRecipeFragment = new DetailFragment();

                Bundle args = new Bundle();
                args.putInt("recipe_id", recipeId);
                args.putString("recipe_user",SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()));
                detailRecipeFragment.setArguments(args);

                fragmentTransaction.replace(R.id.content_frame, detailRecipeFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        }
    }

    //Search for existing tags
    class TagSearchTask extends AsyncTask<String, String, String>{

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
                        String tempKey = "";

                        tagList.clear();
                        tagListAdapter.clear();
                        String tempKeywordKey;

                        //Loop through objects by key
                        while (keys.hasNext()) {
                            tempKey = keys.next().toString();

                            //Don't include the retval or message objects
                            if (!tempKey.equals("retval") && !tempKey.equals("message")) {

                                JSONObject tagObj = new JSONObject(jsonObj.get(tempKey).toString());

                                Iterator keywordKeys = tagObj.keys();
                                while (keywordKeys.hasNext()) {
                                    tempKeywordKey = keywordKeys.next().toString();

                                    TagData d = new TagData();
                                    d.keyword = tagObj.get(tempKeywordKey).toString();
                                    tagList.add(d.keyword);
                                }


                            }

                        }

                        tagListAdapter.addAll(tagList);

                    }

                } catch (JSONException e) {
                    //TODO
                }
            } else {
                Toast.makeText(llLayout.getContext(), "Connection Error...", Toast.LENGTH_SHORT).show();
                Utils.reconnectDialog(getActivity());
            }
        }
    }

    /******************************************************************/
    // Create JSON object from edittext inputs for use later
    /******************************************************************/
    public JSONObject getJsonRecipe() {
        JSONObject jsonRecipeObj = new JSONObject();
        int counter;

        try {
            //user_id
            jsonRecipeObj.put("user_id", SaveSharedPreference.getUserID(RecipeBookApplication.getAppContext()));

            //publish
            int isPublished = 0;
            if (publishToggle.isChecked()) {
                isPublished = 1;
            }
            jsonRecipeObj.put("published", isPublished);

            //user
            jsonRecipeObj.put("user", recipeUser);

            //Title
            jsonRecipeObj.put("title", titleEdit.getText().toString());

            //Ingredients Text Edit
            JSONArray jsonIngredientArray = new JSONArray();
            Iterator<IngredientEditText> ingredientEditTextIt = ingredientEditTextList.iterator();
            counter = 1;
            while (ingredientEditTextIt.hasNext()) {
                IngredientEditText ingredientEditTextObj = ingredientEditTextIt.next();

                if (ingredientEditTextObj.ingredient.getText().toString() != null && !ingredientEditTextObj.ingredient.getText().toString().isEmpty()) {

                    JSONObject jsonIngredientObj = new JSONObject();
                    jsonIngredientObj.put("sort_order", counter);
                    jsonIngredientObj.put("amount", ingredientEditTextObj.amount.getText().toString());
                    jsonIngredientObj.put("unit", ingredientEditTextObj.unit.getText().toString());
                    jsonIngredientObj.put("ingredient", ingredientEditTextObj.ingredient.getText().toString());
                    jsonIngredientArray.put(jsonIngredientObj);

                    counter++;
                }
            }
            jsonRecipeObj.put("ingredients", jsonIngredientArray);

            //Steps Text Edit
            JSONArray jsonStepArray = new JSONArray();
            Iterator<StepEditText> stepEditTextIt = stepEditTextList.iterator();
            counter = 1;
            while (stepEditTextIt.hasNext()) {
                StepEditText stepEditTextObj = stepEditTextIt.next();

                if (stepEditTextObj.step.getText().toString() != null && !stepEditTextObj.step.getText().toString().isEmpty()) {
                    JSONObject jsonStepObj = new JSONObject();
                    jsonStepObj.put("sort_order", counter);
                    jsonStepObj.put("description", TextUtils.htmlEncode(stepEditTextObj.step.getText().toString()));
                    jsonStepArray.put(jsonStepObj);

                    counter++;
                }
            }
            jsonRecipeObj.put("steps", jsonStepArray);

            //Serves
            if (servesEdit.getText().toString() != null && !servesEdit.getText().toString().isEmpty()) {
                jsonRecipeObj.put("serves", servesEdit.getText().toString());
            } else {
                jsonRecipeObj.put("serves", "");
            }

            //Prep
            if (prepEdit.getText().toString() != null && !prepEdit.getText().toString().isEmpty()) {
                jsonRecipeObj.put("prep_time", prepEdit.getText().toString() + ":00");
            } else {
                jsonRecipeObj.put("prep_time", "");
            }

            //Cook
            if (cookEdit.getText().toString() != null && !cookEdit.getText().toString().isEmpty()) {
                jsonRecipeObj.put("cook_time", cookEdit.getText().toString() + ":00");
            } else {
                jsonRecipeObj.put("cook_time", "");
            }

            //Tags
            JSONArray jsonTagsArray = new JSONArray();
            List<String> tagItems = Arrays.asList(tagsEdit.getText().toString().split("\\s*,\\s*"));
            Iterator<String> tagItemsIt = tagItems.iterator();
            while(tagItemsIt.hasNext()) {
                String tagItemObj = tagItemsIt.next();

                if (tagItemObj.toString() != null && !tagItemObj.toString().isEmpty()) {
                    JSONObject jsonStepObj = new JSONObject();
                    jsonStepObj.put("keyword", tagItemObj.toString());
                    jsonTagsArray.put(jsonStepObj);
                }
            }
            jsonRecipeObj.put("tags", jsonTagsArray);


        } catch(JSONException e) {
            //TODO: JSON exception when JSONifying recipe
        }

        return jsonRecipeObj;
    }

    // When Image is selected from Gallery, display it with a delete button
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        try {
            // When an Image is picked
            if (requestCode == RESULT_LOAD_IMG && resultCode == faActivity.RESULT_OK && null != data) {

                // Get the Image from data
                Uri selectedImage = data.getData();

                String filePath = FileUtils.getPath(llLayout.getContext(),selectedImage);

                addNewImageRow(filePath, true);

                imageCounter++;

            } else {
                Toast.makeText(llLayout.getContext(), "You haven't chosen an image",Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            System.out.println(e.toString() + " " + e.getStackTrace());
            Toast.makeText(llLayout.getContext(), "Error...", Toast.LENGTH_LONG).show();
        }

    }

    /******************************************************************/
    // Edit prefill form
    /******************************************************************/
    protected void createEditDetailPage(RecipeData recipeData) {

        int counter = 1;
        LinearLayout ll;

        //Set the title
        titleEdit = (EditText) llLayout.findViewById(R.id.recipe_title);
        titleEdit.setText(recipeData.title);

        //Set the published switch
        publishToggle = (ToggleButton) llLayout.findViewById(R.id.publish_toggle);
        if (recipeData.published == 1) {
            publishToggle.setChecked(true);
        } else {
            publishToggle.setChecked(false);
        }

        //Set Images
        imageCounter = 1;

        Iterator<String> imageUrl = recipeData.imageUrlList.iterator();
        while(imageUrl.hasNext()) {
            String obj = imageUrl.next();
            String urlStr = obj;

            addNewImageRow(urlStr,false);

            imageCounter++;
        }

        //Set the ingredients list
        ingredientCounter = 1;
        ll = (LinearLayout) faActivity.findViewById(R.id.recipe_ingredients);
        Iterator<IngredientData> ingredientIt = recipeData.ingredientList.iterator();
        if (!ingredientIt.hasNext()) {
            IngredientEditText ingredientEditText = new IngredientEditText();
            ingredientEditText.id = ingredientCounter;

            amountEdit = (EditText) llLayout.findViewWithTag("recipe_amount_" + ingredientCounter);
            amountEdit.setText("");
            ingredientEditText.amount = amountEdit;

            unitEdit = (EditText) llLayout.findViewWithTag("recipe_unit_" + ingredientCounter);
            unitEdit.setText("");
            ingredientEditText.unit = unitEdit;

            ingredientEdit = (EditText) llLayout.findViewWithTag("recipe_ingredient_" + ingredientCounter);
            ingredientEdit.setText("");
            ingredientEditText.ingredient = ingredientEdit;

            ingredientEditTextList.add(ingredientEditText);
        }
        while(ingredientIt.hasNext()) {
            IngredientData obj = ingredientIt.next();

            if (ingredientCounter == 1) {
                IngredientEditText ingredientEditText = new IngredientEditText();
                ingredientEditText.id = ingredientCounter;

                amountEdit = (EditText) llLayout.findViewWithTag("recipe_amount_" + ingredientCounter);
                amountEdit.setText(obj.amount);
                ingredientEditText.amount = amountEdit;

                unitEdit = (EditText) llLayout.findViewWithTag("recipe_unit_" + ingredientCounter);
                unitEdit.setText(obj.unit);
                ingredientEditText.unit = unitEdit;

                ingredientEdit = (EditText) llLayout.findViewWithTag("recipe_ingredient_" + ingredientCounter);
                ingredientEdit.setText(obj.ingredient);
                ingredientEditText.ingredient = ingredientEdit;

                ingredientEditTextList.add(ingredientEditText);
            } else {
                addNewIngredientRow(obj.amount,obj.unit,obj.ingredient,false);
            }

            ingredientCounter++;

        }

        //Set the step list
        stepCounter = 1;
        ll = (LinearLayout) faActivity.findViewById(R.id.recipe_steps);
        Iterator<StepData> stepIt = recipeData.stepList.iterator();
        if (!stepIt.hasNext()) {
            StepEditText stepEditText = new StepEditText();
            stepEditText.id = stepCounter;

            stepEdit = (EditText) llLayout.findViewWithTag("recipe_step_" + stepCounter);
            stepEdit.setText("");
            stepEditText.step = stepEdit;

            stepEditTextList.add(stepEditText);
        }
        while(stepIt.hasNext()) {
            StepData obj = stepIt.next();

            if (stepCounter == 1) {
                StepEditText stepEditText = new StepEditText();
                stepEditText.id = stepCounter;

                stepEdit = (EditText) llLayout.findViewWithTag("recipe_step_" + stepCounter);
                stepEdit.setText(obj.description);
                stepEditText.step = stepEdit;

                stepEditTextList.add(stepEditText);
            } else {
                addNewStepRow(obj.description,false);
            }
            stepCounter++;
        }

        //Times and serving
        if(Integer.toString(recipeData.serves) != null && !Integer.toString(recipeData.serves).isEmpty() && recipeData.serves > 0) {
            String servingStr = Integer.toString(recipeData.serves);
            servesEdit.setText(servingStr);
        } else {
            servesEdit.setText("");
        }

        //Prep
        prepEdit = (EditText) llLayout.findViewById(R.id.recipe_prep);
        if((recipeData.prepTime != null && !recipeData.prepTime.isEmpty()) && !recipeData.prepTime.equals("00:00:00")) {
            String prep = recipeData.prepTime;
            prep = prep.substring(0, prep.length() - 3);
            prepEdit.setText(prep);
        } else {
            prepEdit.setText("");
        }

        //Cooking
        cookEdit = (EditText) llLayout.findViewById(R.id.recipe_cook);
        if((recipeData.cookTime != null && !recipeData.cookTime.isEmpty()) && !recipeData.cookTime.equals("00:00:00")) {
            String cook = recipeData.cookTime;
            cook = cook.substring(0, cook.length() - 3);
            cookEdit.setText(cook);
        } else {
            cookEdit.setText("");
        }

        //Set the tag list
        if (recipeData.tagList.size() > 0) {
            String tagEditStr = "";
            Iterator<String> tagIt = recipeData.tagList.iterator();

            while (tagIt.hasNext()) {
                final String tagObjStr = tagIt.next();

                tagEditStr = tagEditStr + tagObjStr;

                if (tagIt.hasNext()) {
                    tagEditStr = tagEditStr + ", ";
                }
            }
            tagsEdit.setText(tagEditStr);
        } else {
            tagsEdit.setText("");
        }

    }

    /******************************************************************/
    // Add EditText boxes and images dynamically
    /******************************************************************/
    //Add a new image and remove button
    private void addNewImageRow(String imagePath,boolean isNewImage) {

        LinearLayout ll = (LinearLayout) faActivity.findViewById(R.id.recipe_images);
        String temp;

        //Add new EditText items with delete button
        final LinearLayout newll = new LinearLayout(llLayout.getContext());

        newll.setTag("recipe_images_" + imageCounter);
        newll.setOrientation(LinearLayout.HORIZONTAL);

        ImageView imageView = new ImageView(super.getActivity().getApplicationContext());
        imageView.setTag("recipe_image_" + imageCounter);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(300,200));
        //Load preview images first while loading in AsyncTask
        Bitmap previewBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        previewBitmap = Utils.adjustBitmapOpacity(previewBitmap,60);
        imageView.setImageBitmap(previewBitmap);
        newll.addView(imageView);

        removeButton = new Button(llLayout.getContext());
        removeButton.setTag("recipe_image_remove_" + imageCounter);
        removeButton.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        removeButton.setBackgroundColor(Color.TRANSPARENT);
        removeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_cancel, 0, 0, 0);
        removeButton.setPadding(0, 0, 0, 0);
        newll.addView(removeButton);

        ll.addView(newll);

        int curImageCount = imageCounter;
        final AsyncTask downloadImage = new DownloadImageTask((ImageView) llLayout.findViewWithTag("recipe_image_" + imageCounter),isNewImage,curImageCount).execute(imagePath);

        removeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (downloadImage.getStatus() == AsyncTask.Status.FINISHED) {
                    String removeButtonTag = v.getTag().toString();
                    String idNumber = removeButtonTag.substring(removeButtonTag.length() - 1);

                    recipeImageView = (ImageView) llLayout.findViewWithTag("recipe_image_" + idNumber);
                    recipeImageView.setVisibility(WebView.GONE);
                    recipeImageView = null;
                    removeButton = (Button) llLayout.findViewWithTag("recipe_image_remove_" + idNumber);
                    removeButton.setVisibility(Button.GONE);
                    removeButton = null;

                    //Set flags
                    for (RecipeImageView iView : imageViewList) {
                        if (iView.viewId == Integer.parseInt(idNumber)) {
                            iView.removeFlag = true;
                            break;
                        }
                    }
                }
            }
        });
    }

    //Add new ingredient and remove button and button listener
    private void addNewIngredientRow(String amountValue,String unitValue,String ingredientValue, boolean addButtonPressed) {
        LinearLayout ll = (LinearLayout) faActivity.findViewById(R.id.recipe_ingredients);
        IngredientEditText ingredientEditText = new IngredientEditText();
        ingredientEditText.id = ingredientCounter;

        //Add new EditText items with delete button
        final LinearLayout newll = new LinearLayout(llLayout.getContext());

        newll.setTag("recipe_ingredients_" + ingredientCounter);
        newll.setOrientation(LinearLayout.HORIZONTAL);

        Resources r = getResources();
        //float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 333, r.getDisplayMetrics());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        newll.setLayoutParams(params);

        //Add amount edit
        amountEdit = new EditText(llLayout.getContext());
        amountEdit.setTag("recipe_amount_" + ingredientCounter);
        amountEdit.setHint("Amount");
        amountEdit.setSingleLine(true);
        amountEdit.setHorizontallyScrolling(true);
        if (amountValue != null && !amountValue.isEmpty()) {
            amountEdit.setText(amountValue);
        }
        newll.addView(amountEdit);
        ingredientEditText.amount = amountEdit;
        if (addButtonPressed) {
            //Set focus on new field
            amountEdit.requestFocus();
        }

        //Add unit edit
        unitEdit = new EditText(llLayout.getContext());
        unitEdit.setTag("recipe_unit_" + ingredientCounter);
        unitEdit.setHint("Unit");
        unitEdit.setSingleLine(true);
        unitEdit.setHorizontallyScrolling(true);
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 70, r.getDisplayMetrics());
        unitEdit.setWidth(Math.round(px));
        if (unitValue != null && !unitValue.isEmpty()) {
            unitEdit.setText(unitValue);
        }
        newll.addView(unitEdit);
        ingredientEditText.unit = unitEdit;

        //Add ingredient edit
        ingredientEdit = new EditText(llLayout.getContext());
        ingredientEdit.setTag("recipe_ingredient_" + ingredientCounter);
        ingredientEdit.setHint("Ingredient");
        ingredientEdit.setSingleLine(true);
        ingredientEdit.setHorizontallyScrolling(true);
        LinearLayout.LayoutParams iparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT,1f);
        iparams.setMargins(0, 0, 120, 0);
        ingredientEdit.setLayoutParams(iparams);
        if (ingredientValue != null && !ingredientValue.isEmpty()) {
            ingredientEdit.setText(ingredientValue);
        }
        newll.addView(ingredientEdit);
        ingredientEditText.ingredient = ingredientEdit;

        ingredientEditTextList.add(ingredientEditText);

        removeButton = new Button(llLayout.getContext());
        removeButton.setTag("recipe_ingredient_remove_" + ingredientCounter);
        removeButton.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        removeButton.setBackgroundColor(Color.TRANSPARENT);
        removeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_cancel, 0, 0, 0);
        LinearLayout.LayoutParams bparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        bparams.setMargins(-120,0,-120,0);
        removeButton.setLayoutParams(bparams);
        removeButton.setPadding(0,0,0,0);
        newll.addView(removeButton);

        ll.addView(newll);

        removeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String removeButtonTag = v.getTag().toString();
                String[] splits = removeButtonTag.split("_");
                String idNumber = splits[splits.length - 1];

                amountEdit = (EditText) llLayout.findViewWithTag("recipe_amount_" + idNumber);
                amountEdit.setVisibility(EditText.GONE);
                amountEdit = null;
                unitEdit = (EditText) llLayout.findViewWithTag("recipe_unit_" + idNumber);
                unitEdit.setVisibility(EditText.GONE);
                unitEdit = null;
                ingredientEdit = (EditText) llLayout.findViewWithTag("recipe_ingredient_" + idNumber);
                ingredientEdit.setVisibility(EditText.GONE);
                ingredientEdit = null;
                linearLayout = (LinearLayout) llLayout.findViewWithTag("recipe_ingredients_" + idNumber);
                linearLayout.setVisibility(LinearLayout.GONE);
                removeButton = (Button) llLayout.findViewWithTag("recipe_ingredient_remove_" + idNumber);
                removeButton.setVisibility(Button.GONE);
                removeButton = null;
                //Remove from list
                IngredientEditText objRemove = null;
                for (IngredientEditText iEditText : ingredientEditTextList) {
                    if (iEditText.id == Integer.parseInt(idNumber)) {
                        objRemove = iEditText;
                        break;
                    }
                }
                if (objRemove != null) {
                    ingredientEditTextList.remove(objRemove);
                }
            }
        });
    }

    //Add new step and remove button and button listener
    private void addNewStepRow(String description,boolean addButtonPressed) {
        LinearLayout ll = (LinearLayout) faActivity.findViewById(R.id.recipe_steps);
        StepEditText stepEditText = new StepEditText();
        stepEditText.id = stepCounter;

        //Add new EditText items with delete button
        final LinearLayout newll = new LinearLayout(llLayout.getContext());

        newll.setTag("recipe_steps_" + stepCounter);
        newll.setOrientation(LinearLayout.HORIZONTAL);

        Resources r = getResources();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        newll.setLayoutParams(params);

        stepEdit = new EditText(llLayout.getContext());
        stepEdit.setTag("recipe_step_" + stepCounter);
        stepEdit.setHint("Step");
        stepEdit.setHorizontallyScrolling(true);
        LinearLayout.LayoutParams iparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT,1f);
        iparams.setMargins(0, 0, 120, 0);
        stepEdit.setLayoutParams(iparams);
        stepEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        if (description != null && !description.isEmpty()) {
            stepEdit.setText(description);
        }
        stepEditText.step = stepEdit;
        newll.addView(stepEdit);
        stepEditTextList.add(stepEditText);
        if (addButtonPressed) {
            //Set focus on new field
            stepEdit.requestFocus();
        }

        removeButton = new Button(llLayout.getContext());
        removeButton.setTag("recipe_step_remove_" + stepCounter);
        removeButton.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        removeButton.setBackgroundColor(Color.TRANSPARENT);
        removeButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_cancel, 0, 0, 0);
        LinearLayout.LayoutParams bparams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        bparams.setMargins(-120,0,-120,0);
        removeButton.setLayoutParams(bparams);
        removeButton.setPadding(0,0,0,0);
        newll.addView(removeButton);

        ll.addView(newll);

        removeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String removeButtonTag = v.getTag().toString();
                String[] splits = removeButtonTag.split("_");
                String idNumber = splits[splits.length - 1];

                stepEdit = (EditText) llLayout.findViewWithTag("recipe_step_" + idNumber);
                stepEdit.setVisibility(EditText.GONE);
                stepEdit = null;
                linearLayout = (LinearLayout) llLayout.findViewWithTag("recipe_steps_" + idNumber);
                linearLayout.setVisibility(LinearLayout.GONE);
                removeButton = (Button) llLayout.findViewWithTag("recipe_step_remove_" + idNumber);
                removeButton.setVisibility(Button.GONE);
                removeButton = null;
                //Remove from list
                StepEditText objRemove = null;
                for (StepEditText sEditText : stepEditTextList) {
                    if (sEditText.id == Integer.parseInt(idNumber)) {
                        objRemove = sEditText;
                        break;
                    }
                }
                if (objRemove != null) {
                    stepEditTextList.remove(objRemove);
                }
            }
        });

    }

    /******************************************************************/
    // Classes
    /******************************************************************/
    class RecipeData {
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
        List<String> imageUrlList = new ArrayList<String>();

        @Override
        public String toString() {
            return this.title;
        }
    }

    class IngredientData {
        int sortOrder;
        String amount;
        String unit;
        String ingredient;

        @Override
        public String toString() {
            return this.amount + " " + this.unit + " " + this.ingredient;
        }
    }

    class StepData {
        int sortOrder;
        String description;

        @Override
        public String toString() {
            return this.description;
        }
    }

    class IngredientEditText {
        int id;
        EditText amount;
        EditText unit;
        EditText ingredient;

        @Override
        public String toString() {
            return this.amount.getText().toString() + " " + this.unit.getText().toString() + " " + this.ingredient.getText().toString();
        }
    }

    class StepEditText {
        int id;
        EditText step;

        @Override
        public String toString() {
            return this.step.getText().toString();
        }
    }

    class RecipeImageView {
        int viewId;
        int imageId;
        Bitmap imageBmp;
        boolean removeFlag;
        boolean newImage;

        @Override
        public String toString() {
            return "View ID:" + Integer.toString(viewId) + " Image ID:" + Integer.toString(this.imageId) + " New image:" + newImage + " Remove flag:" + removeFlag;
        }
    }

    class TagData {
        String keyword;

        @Override
        public String toString() {
            return "Keyword:" + keyword;
        }
    }

    public void clearKeyboard() {
        //Hide keyboard after input
        View target = llLayout.findFocus();
        if (target != null) {
            InputMethodManager imm = (InputMethodManager) target.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(target.getWindowToken(), 0);
        }
    }

}
