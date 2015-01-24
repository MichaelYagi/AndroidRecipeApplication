package ca.michaelyagi.recipeapplication;

/******************************************************************/
// Browse all recipes, by user or by tag
/******************************************************************/

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Michael on 12/27/2014.
 */
public class BrowseFragment extends Fragment {
    ListView browseListView;
    BrowseAdapter listAdapter;
    // Create and populate a List of recipes
    List<RecipeListData> recipeList = new ArrayList<RecipeListData>();
    MenuItem deleteRecipesItem;
    Bundle args;

    private RelativeLayout        llLayout;
    private boolean popNext = false;
    TextView browseText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        llLayout    = (RelativeLayout)    inflater.inflate(R.layout.fragment_browse, container, false);
        setHasOptionsMenu(true);

        //Find the ListView
        browseListView = (ListView) llLayout.findViewById( R.id.browseListView );
        browseListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        browseText = (TextView) llLayout.findViewById(R.id.browseText);
        browseText.setText("");

        //Clear the recipe list
        recipeList.clear();

        //Create ArrayAdapter of RecipeList
        listAdapter = new BrowseAdapter(super.getActivity(),R.layout.browse_recipe_row,recipeList);

        // Set the ArrayAdapter as the ListView's adapter.
        browseListView.setAdapter(listAdapter);

        /******************************************************************/
        // Get recipe list based on arguments passed
        /******************************************************************/
        //Get recipes by user logged in
        if (getArguments() != null) {
            args = getArguments();
        }
        if (getArguments() != null && getArguments().getString("user") != null && getArguments().getBoolean("viewbyuser_filter") && getArguments().getString("user").length() > 0) {
            ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("User Recipes");
            new RequestBrowseTask(true).execute("http://" + Utils.getApiServer() + "/api/v1/json/recipesByType/user/" + getArguments().getString("user"));
        } else if (getArguments() != null && getArguments().getString("keyword") != null && getArguments().getBoolean("viewbytag_filter") && getArguments().getString("keyword").length() > 0) {
            ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("\"" + getArguments().getString("keyword") + "\"");
            String tag = "";
            try {
                tag = URLEncoder.encode(getArguments().getString("keyword"), "utf-8");
            } catch(UnsupportedEncodingException e) {
                //TODO: Catch URLEncoder exception
            }
            new RequestBrowseTask(true).execute("http://" + Utils.getApiServer() + "/api/v1/json/recipesByType/tag/" + tag);
            //Get recipes by search term
        } else if (getArguments() != null && getArguments().getString("searchterm") != null && getArguments().getBoolean("viewbysearch_filter") && getArguments().getString("searchterm").length() > 0) {
            ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle("\"" + getArguments().getString("searchterm") + "\"");
            String searchTerm = "";
            try {
                searchTerm = URLEncoder.encode(getArguments().getString("searchterm"), "utf-8");
            } catch (UnsupportedEncodingException e) {
                //TODO: Catch URLEncoder exception
            }
            new RequestBrowseTask(true).execute("http://" + Utils.getApiServer() + "/api/v1/json/recipesByType/search/" + searchTerm);
        } else if (getArguments() != null && getArguments().getBoolean("viewbyuser_filter")) {
            ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("My Recipes");
            new RequestBrowseTask(false).execute("http://" + Utils.getApiServer() + "/api/v1/json/recipesByType/user/" + SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()));
        //Get all recipes
        } else {
            ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("Browse");
            new RequestBrowseTask(true).execute("http://" + Utils.getApiServer() + "/api/v1/json/recipes");
        }

        //On click listener when user clicks on a browse item
        browseListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,int position, long id) {

                ((ListView) parent).setItemChecked(position, false);

                mListener.showDrawerToggle(false);

                //Get Data at position selected
                RecipeListData recipeData = (RecipeListData)parent.getItemAtPosition(position);

                FragmentManager fragmentManager = getFragmentManager();

                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

                DetailFragment detailRecipeFragment = new DetailFragment();

                Bundle args = new Bundle();
                args.putInt("recipe_id", recipeData.getId());
                args.putString("recipe_title",recipeData.getTitle());
                args.putString("recipe_user",recipeData.getUser());
                detailRecipeFragment.setArguments(args);

                fragmentTransaction.replace(R.id.content_frame, detailRecipeFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });

        return llLayout;
    }

    public interface OnFragmentInteractionListener {
        public void showDrawerToggle(boolean showDrawerToggle);
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

    @Override
    public void onPrepareOptionsMenu(Menu menu) {

        MenuItem deleteRecipes = menu.findItem(R.id.menu_item_delete_recipes);
        deleteRecipes.setVisible(false);

        MenuItem searchRecipes = menu.findItem(R.id.menu_item_search);
        searchRecipes.setVisible(true);

        mListener.showDrawerToggle(true);

        if (((ListView) browseListView).getCheckedItemCount() > 0) {
            deleteRecipes.setVisible(true);
            searchRecipes.setVisible(false);
            mListener.showDrawerToggle(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        final MenuItem anItem = item;

        switch (anItem.getItemId()) {
            case R.id.menu_item_delete_recipes:

                //Check if at least one item checked and show garbage icon in actionbar
                if (((ListView) browseListView).getCheckedItemCount() > 0) {

                    new AlertDialog.Builder(llLayout.getContext())
                            .setIconAttribute(android.R.attr.alertDialogIcon)
                            .setTitle("Confirmation")
                            .setMessage("Delete recipes?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                                //Confirm delete
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    for (int x = listAdapter.getCount() - 1; x >= 0; x--) {
                                        if (((ListView) browseListView).isItemChecked(x)) {
                                            ((ListView) browseListView).setItemChecked(x,false);
                                            //DELETE request to delete this recipe
                                            new DeleteRequestTask().execute("http://" + Utils.getApiServer() + "/api/v1/json/recipe/" + recipeList.get(x).getId());
                                            recipeList.remove(listAdapter.getItem(x));
                                        }

                                    }

                                    Toast.makeText(llLayout.getContext(), "Recipes Deleted...", Toast.LENGTH_SHORT).show();

                                    //Refresh the browsefragment
                                    listAdapter = new BrowseAdapter(getActivity(),R.layout.browse_recipe_row,recipeList);

                                    // Set the ArrayAdapter as the ListView's adapter.
                                    browseListView.setAdapter(listAdapter);
                                    if (listAdapter.getCount() == 0) {
                                        browseText.setText("No Recipes");
                                    }
                                    listAdapter.notifyDataSetChanged();
                                    anItem.setVisible(false);
                                }

                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {

                                //Confirm cancel
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    for (int x = listAdapter.getCount() - 1; x >= 0; x--) {
                                        if (((ListView) browseListView).isItemChecked(x)) {
                                            ((ListView) browseListView).setItemChecked(x, false);
                                            ((ListView) browseListView).getChildAt(x).setBackgroundColor(Color.WHITE);
                                        }

                                    }
                                    anItem.setVisible(false);
                                }

                            })
                            .show();

                }

                break;
            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /******************************************************************/
    // AsyncTasks
    /******************************************************************/
    //Request the list of recipes
    class RequestBrowseTask extends AsyncTask<String, String, String>{

        private ProgressDialog dialog;
        private boolean ShowOnlyPublished;

        public RequestBrowseTask(boolean aShowOnlyPublished) {
            this.ShowOnlyPublished = aShowOnlyPublished;
        }

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
                int counter = 0;
                try {
                    //Turn response into JSON object
                    JSONObject jsonObj = new JSONObject(result);

                    //Get all keys of JSON object
                    Iterator keys = jsonObj.keys();

                    listAdapter.clear();
                    recipeList.clear();

                    //If request was successful
                    if (jsonObj.getString("retval").equals("1") && jsonObj.getString("message").equals("Success")) {
                        if (jsonObj.has("id")) {
                            if ((this.ShowOnlyPublished && jsonObj.getString("published").equals("1")) || !this.ShowOnlyPublished) {
                                RecipeListData d = new RecipeListData();
                                Integer recipeId = Integer.parseInt(jsonObj.get("id").toString());
                                d.setId(recipeId);
                                d.setTitle(jsonObj.get("title").toString());
                                String draftText = "";
                                if (!this.ShowOnlyPublished) {
                                    draftText = "DRAFT";
                                }
                                d.setDraft(draftText);
                                d.setUser(jsonObj.get("user").toString());
                                Integer serves = Integer.parseInt(jsonObj.get("serves").toString());
                                d.setServes(serves);
                                d.setCookTime(jsonObj.get("cook_time").toString());
                                d.setPrepTime(jsonObj.get("prep_time").toString());
                                listAdapter.add(d);

                                if (jsonObj.get("image_id") != null && !jsonObj.get("image_id").toString().isEmpty()) {
                                    String imageUrl = "http://" + Utils.getWebsiteUrl() + "/media/recipeimages/" + recipeId + "/" + jsonObj.get("image_id").toString();
                                    if (jsonObj.get("extension") != null && !jsonObj.get("extension").toString().isEmpty()) {
                                        imageUrl = imageUrl + "." + jsonObj.get("extension").toString();
                                    }

                                    //Call AsyncTask to convert Url to Bmp, pass json object
                                    new DownloadImageTask(counter, d).execute(imageUrl);

                                }
                            }

                        } else {
                            String tempKey = "";

                            //Loop through objects by key
                            while (keys.hasNext()) {
                                tempKey = keys.next().toString();

                                //Don't include the retval or message objects
                                if (!tempKey.equals("retval") && !tempKey.equals("message")) {

                                    RecipeListData d = new RecipeListData();

                                    JSONObject recipeObj = new JSONObject(jsonObj.get(tempKey).toString());

                                    if ((this.ShowOnlyPublished && recipeObj.getString("published").equals("1")) || !this.ShowOnlyPublished) {

                                        Integer recipeId = Integer.parseInt(recipeObj.get("id").toString());
                                        d.setId(recipeId);
                                        d.setTitle(recipeObj.get("title").toString());
                                        String draftText = "";
                                        if (!this.ShowOnlyPublished) {
                                            draftText = "DRAFT";
                                        }
                                        d.setDraft(draftText);
                                        d.setUser(recipeObj.get("user").toString());
                                        Integer serves = Integer.parseInt(recipeObj.get("serves").toString());
                                        d.setServes(serves);
                                        d.setCookTime(recipeObj.get("cook_time").toString());
                                        d.setPrepTime(recipeObj.get("prep_time").toString());
                                        listAdapter.add(d);

                                        if (!recipeObj.get("image_id").toString().equals("null") && !recipeObj.get("image_id").toString().isEmpty()) {

                                            String imageUrl = "http://" + Utils.getWebsiteUrl() + "/media/recipeimages/" + recipeId + "/" + recipeObj.get("image_id").toString();
                                            if (recipeObj.get("extension").toString() != null && !recipeObj.get("extension").toString().isEmpty()) {
                                                imageUrl = imageUrl + "." + recipeObj.get("extension").toString();
                                            }

                                            //Call AsyncTask to convert Url to Bmp, pass json object
                                            new DownloadImageTask(counter, d).execute(imageUrl);

                                        }

                                        counter++;
                                    }
                                }
                            }
                        }

                    } else {
                        browseText.setText("No Recipes");
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
                    } else {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        Toast.makeText(llLayout.getContext(), "Could Not Delete Recipe...", Toast.LENGTH_SHORT).show();
                    }

                } catch (JSONException e) {
                    //TODO
                    System.out.println(e.toString());
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

        RecipeListData recipeListData;
        String imageUrl;
        int count;

        public DownloadImageTask(int count, RecipeListData d) {
            this.count = count;
            this.recipeListData = d;
        }

        protected Bitmap doInBackground(String... urls) {
            imageUrl = urls[0];
            Bitmap imageBmp = null;
            try {
                InputStream in = new java.net.URL(imageUrl).openStream();
                imageBmp = BitmapFactory.decodeStream(in);
            } catch (MalformedURLException e) {
                try {
                    File file = new File(imageUrl);
                    FileInputStream fileInputStream = new FileInputStream(file);
                    imageBmp = BitmapFactory.decodeStream(fileInputStream);
                } catch(FileNotFoundException f) {
                    System.out.println("File not found:" + f.toString());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return imageBmp;
        }

        protected void onPostExecute(Bitmap result) {
            recipeListData.setImage(result);
            listAdapter.remove(recipeListData);
            listAdapter.insert(recipeListData,count);
        }
    }

    /******************************************************************/
    // Classes
    /******************************************************************/
    public class SquareImageView extends ImageView {
        public SquareImageView(Context context) {
            super(context);
        }

        public SquareImageView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public SquareImageView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, widthMeasureSpec);

            int width = getMeasuredWidth();
            setMeasuredDimension(width, width);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);

            if (getLayoutParams() != null && w != h) {
                getLayoutParams().height = w;
                setLayoutParams(getLayoutParams());
            }
        }
    }

    public class BrowseAdapter extends ArrayAdapter<RecipeListData> {
        private final Context context;
        private final List<RecipeListData> data;
        private final int layoutResourceId;
        private Animation animation1;
        private Animation animation2;
        private ImageView flipImage;
        private boolean isChecked = false;

        public BrowseAdapter(Context context, int layoutResourceId, List<RecipeListData> data) {
            super(context, layoutResourceId, data);
            this.context = context;
            this.data = data;
            this.layoutResourceId = layoutResourceId;

            animation1 = AnimationUtils.loadAnimation(context, R.anim.to_middle);
            animation2 = AnimationUtils.loadAnimation(context, R.anim.from_middle);
        }

        private class ViewHolder {
            ImageView image;
            TextView titleText;
            TextView draftText;
            TextView userText;
            TextView servesText;
            TextView prepText;
            TextView cookText;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View cView = convertView;
            final int vPosition = position;
            final ViewGroup vParent = parent;

            ViewHolder holder = null;
            RecipeListData rowItem = getItem(position);

            LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.browse_recipe_row, null);
                holder = new ViewHolder();
                holder.titleText = (TextView) convertView.findViewById(R.id.rowTitle);
                holder.draftText = (TextView) convertView.findViewById(R.id.rowDraft);
                holder.userText = (TextView) convertView.findViewById(R.id.rowUser);
                holder.prepText = (TextView) convertView.findViewById(R.id.prepTime);
                holder.servesText = (TextView) convertView.findViewById(R.id.serves);
                holder.cookText = (TextView) convertView.findViewById(R.id.cookTime);
                holder.image = (ImageView) convertView.findViewById(R.id.rowImage);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.titleText.setText(rowItem.getTitle());
            holder.userText.setText(rowItem.getUser());
            holder.draftText.setText(rowItem.getDraft());

            if (rowItem.getServes() > 0) {
                String serves = Integer.toString(rowItem.getServes());
                holder.servesText.setText(serves);
            }

            if ((rowItem.getPrepTime() != null && !rowItem.getPrepTime().isEmpty()) && !rowItem.getPrepTime().equals("00:00:00")) {
                String prep = rowItem.getPrepTime();
                prep = prep.substring(0, prep.length() - 3);
                holder.prepText.setText(prep);
            }

            if ((rowItem.getCookTime() != null && !rowItem.getCookTime().isEmpty()) && !rowItem.getCookTime().equals("00:00:00")) {
                String cook = rowItem.getCookTime();
                cook = cook.substring(0, cook.length() - 3);
                holder.cookText.setText(cook);
            }

            if (rowItem.getImage() == null) {
                char firstCharacterTitle = rowItem.getTitle().toUpperCase().charAt(0);
                int color = ColorGenerator.DEFAULT.getColor(firstCharacterTitle);
                CharacterDrawable drawable = new CharacterDrawable(firstCharacterTitle, color);
                holder.image.setImageDrawable(drawable);
            } else {
                holder.image.setImageBitmap(rowItem.getImage());
            }

            if (args != null && args.getBoolean("viewbyuser_filter") && holder.userText.getText().toString().equals(SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()))) {

                //Delete items when clicking image
                holder.image.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        flipImage = (ImageView) view;
                        final RecipeListData rowItem = getItem(vPosition);

                        getActivity().invalidateOptionsMenu();

                        flipImage.clearAnimation();
                        flipImage.setAnimation(animation1);
                        flipImage.startAnimation(animation1);

                        Animation.AnimationListener animListener = new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                                if (animation==animation1) {
                                    if (isChecked) {
                                        if (rowItem.getImage() == null) {
                                            char firstCharacterTitle = rowItem.getTitle().toUpperCase().charAt(0);
                                            int color = ColorGenerator.DEFAULT.getColor(firstCharacterTitle);
                                            CharacterDrawable drawable = new CharacterDrawable(firstCharacterTitle, color);
                                            flipImage.setImageDrawable(drawable);
                                        } else {
                                            flipImage.setImageBitmap(rowItem.getImage());
                                        }
                                    } else {
                                        flipImage.setBackgroundColor(Color.LTGRAY);
                                        flipImage.setImageResource(R.drawable.ic_action_accept);
                                    }
                                    flipImage.clearAnimation();
                                    flipImage.setAnimation(animation2);
                                    flipImage.startAnimation(animation2);
                                } else {
                                    isChecked=!isChecked;
                                }
                            }
                            @Override
                            public void onAnimationRepeat(Animation animation) {
                                // TODO Auto-generated method stub
                            }
                            @Override
                            public void onAnimationEnd(Animation animation) {
                                // TODO Auto-generated method stub
                            }

                        };

                        animation1.setAnimationListener(animListener);
                        animation2.setAnimationListener(animListener);

                        if (((ListView) vParent).isItemChecked(vPosition)) {
                            ((ListView) vParent).setItemChecked(vPosition, false);
                        } else {
                            ((ListView) vParent).setItemChecked(vPosition, true);
                        }
                    }

                });
            }

            return convertView;
        }

    }

    public class CharacterDrawable extends ColorDrawable {

        private final char character;
        private final Paint textPaint;
        private final Paint borderPaint;
        private static final int STROKE_WIDTH = 10;
        private static final float SHADE_FACTOR = 0.9f;

        public CharacterDrawable(char character, int color) {
            super(color);
            this.character = character;
            this.textPaint = new Paint();
            this.borderPaint = new Paint();

            // text paint settings
            textPaint.setColor(Color.WHITE);
            textPaint.setAntiAlias(true);
            textPaint.setFakeBoldText(true);
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setTextAlign(Paint.Align.CENTER);

            // border paint settings
            borderPaint.setColor(getDarkerShade(color));
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(STROKE_WIDTH);
        }

        private int getDarkerShade(int color) {
            return Color.rgb((int)(SHADE_FACTOR * Color.red(color)),
                    (int)(SHADE_FACTOR * Color.green(color)),
                    (int)(SHADE_FACTOR * Color.blue(color)));
        }

        @Override
        public void draw(Canvas canvas) {
            super.draw(canvas);


            // draw border
            canvas.drawRect(getBounds(), borderPaint);

            // draw text
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            textPaint.setTextSize(height / 2);
            canvas.drawText(String.valueOf(character), width/2, height/2 - ((textPaint.descent() + textPaint.ascent()) / 2) , textPaint);
        }

        @Override
        public void setAlpha(int alpha) {
            textPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
            textPaint.setColorFilter(cf);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    class RecipeListData {
        private int id;
        private String title;
        private String draft;
        private String user;
        private Bitmap image;
        private int serves;
        private String prepTime;
        private String cookTime;

        @Override
        public String toString() {
            return this.title;
        }

        public int getId() {
            return this.id;
        }

        public String getTitle() {
            return this.title;
        }

        public String getDraft() { return this.draft; }

        public String getUser() { return this.user; }

        public Bitmap getImage() { return this.image; }

        public int getServes() { return this.serves; }

        public String getPrepTime() { return this.prepTime; }

        public String getCookTime() { return this.cookTime; }

        public void setId(int anId) {
            this.id = anId;
        }

        public void setTitle(String aTitle) {
            this.title = aTitle;
        }

        public void setUser(String aUser) {
            this.user = aUser;
        }

        public void setDraft(String aDraft) { this.draft = aDraft; }

        public void setImage(Bitmap aImage) {
            this.image = aImage;
        }

        public void setServes(int aServes) {
            this.serves = aServes;
        }

        public void setPrepTime(String aPrepTime) { this.prepTime = aPrepTime; }

        public void setCookTime(String aCookTime) { this.cookTime = aCookTime; }
    }
}
