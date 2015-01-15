package ca.michaelyagi.recipeapplication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Michael on 1/11/2015.
 */
public class RecipeImagesTabFragment extends Fragment {

    private int recipeId;
    private String recipeUser;

    private ScrollView svLayout;
    private FragmentActivity faActivity;

    private FragmentTransaction fragmentTransaction;
    List<String> imageUrlList = new ArrayList<String>();
    private List<Bitmap> imageBmps = new ArrayList<Bitmap>();
    private List<ImageView> imageViews = new ArrayList<ImageView>();
    private ImageGalleryAdapter imageAdapter;
    private ShareActionProvider mShareActionProvider;
    private String recipeTitle;

    GridView gridViewGallery;

    private int COL_NUM = 2;

    /******************************************************************/
    // On create
    /******************************************************************/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        faActivity = (FragmentActivity) super.getActivity();
        svLayout = (ScrollView) inflater.inflate(R.layout.fragment_recipe_images_detail, container, false);

        //Get arguments passed
        recipeId = getArguments().getInt("recipe_id");
        recipeUser = getArguments().getString("recipe_user");
        imageBmps.clear();

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

                    RecipeImages recipeImagesData = new RecipeImages();

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
                                    recipeImagesData.id = Integer.parseInt(jsonObj.get(tempKey).toString());
                                    break;
                                case "title":
                                    recipeImagesData.title = jsonObj.get(tempKey).toString();
                                    recipeTitle = jsonObj.get(tempKey).toString();
                                    break;
                                case "image_urls":
                                    JSONArray urlList;
                                    if (jsonObj.get(tempKey) instanceof JSONObject) {
                                        urlList = new JSONArray("[" + jsonObj.get(tempKey).toString() + "]");
                                    } else if (jsonObj.get(tempKey) instanceof String) {
                                        urlList = new JSONArray("['" + jsonObj.get(tempKey).toString() + "']");
                                    } else {
                                        urlList = new JSONArray(jsonObj.get(tempKey).toString());
                                    }

                                    for (int i = 0; i < urlList.length(); i++) {
                                        recipeImagesData.imageUrlList.add(urlList.getString(i));
                                    }
                                    break;
                            }


                        }

                        createImageGallery(recipeImagesData);

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

    //Convert Image URL to bitmap
    class UrlToBitmapTask extends AsyncTask<String, Void, List<Bitmap>> {

        List<String> imageUrls;

        public UrlToBitmapTask(List<String> imageUrls) {
            this.imageUrls = imageUrls;
        }

        protected List<Bitmap> doInBackground(String... urls) {
            Bitmap bitmapImage = null;
            List<Bitmap> imageBitmaps = new ArrayList<Bitmap>();
            try {
                Iterator<String> imageUrl = this.imageUrls.iterator();
                while(imageUrl.hasNext()) {
                    String obj = imageUrl.next();
                    bitmapImage = null;

                    InputStream in = new java.net.URL(obj.toString()).openStream();
                    bitmapImage = BitmapFactory.decodeStream(in);

                    imageBitmaps.add(bitmapImage);

                }
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return imageBitmaps;
        }

        protected void onPostExecute(List<Bitmap> result) {
            imageBmps.clear();
            imageBmps.addAll(result);
            gridViewGallery.invalidateViews();
            imageAdapter.notifyDataSetChanged();
        }
    }

    /******************************************************************/
    // Image Gallery
    /******************************************************************/
    //Create the image Gallery
    protected void createImageGallery(RecipeImages recipeImagesData) {

        //Set images in layout placeholder
        RelativeLayout imagesLayout = (RelativeLayout)svLayout.findViewById(R.id.imagesLayout);
        if (recipeImagesData.imageUrlList.size() > 0) {

            gridViewGallery = (GridView)svLayout.findViewById(R.id.gridView);

            gridViewGallery.setNumColumns(COL_NUM);

            DisplayMetrics dm = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            int imagewWidth = (dm.widthPixels*COL_NUM)/2;

            ViewGroup.LayoutParams layoutParams = gridViewGallery.getLayoutParams();
            layoutParams.height = imagewWidth;
            gridViewGallery.setLayoutParams(layoutParams);

            imageBmps.clear();
            //Load preview images first
            Bitmap previewBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            previewBitmap = Utils.adjustBitmapOpacity(previewBitmap,60);
            for (int x = 0;x < recipeImagesData.imageUrlList.size();x++) {
                imageBmps.add(previewBitmap);
            }

            imageAdapter = new ImageGalleryAdapter(svLayout.getContext(), imageBmps);

            //Load actual images and notify the adapter that something has changed
            final AsyncTask urlToBmp = new UrlToBitmapTask(recipeImagesData.imageUrlList).execute();
            gridViewGallery.setAdapter(imageAdapter);

            gridViewGallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView parent, View v, int position, long id) {

                    if (urlToBmp.getStatus() == AsyncTask.Status.FINISHED) {
                        // Get screen size
                        Display display = faActivity.getWindowManager().getDefaultDisplay();
                        Point size = new Point();
                        display.getSize(size);
                        int screenWidth = size.x;
                        int screenHeight = size.y;

                        Bitmap bitmap = imageBmps.get(position);
                        int bitmapHeight = bitmap.getHeight();
                        int bitmapWidth = bitmap.getWidth();

                        while (bitmapHeight > (screenHeight - 200) || bitmapWidth > (screenWidth - 200)) {
                            bitmapHeight = bitmapHeight / 2;
                            bitmapWidth = bitmapWidth / 2;
                        }

                        final Dialog imageDialog = new Dialog(svLayout.getContext());
                        imageDialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                        LayoutInflater inflater = LayoutInflater.from(faActivity.getApplicationContext());

                        imageDialog.setContentView(inflater.inflate(R.layout.image_dialog, null));

                        //set up image view
                        BitmapDrawable imageBmpBackground = new BitmapDrawable(faActivity.getResources(), Bitmap.createScaledBitmap(bitmap, bitmapWidth, bitmapHeight, false));
                        ImageView img = (ImageView) imageDialog.findViewById(R.id.dialogImageView);
                        img.setBackgroundDrawable(imageBmpBackground);

                        //set up button
                        Button button = (Button) imageDialog.findViewById(R.id.dialogOkButton);
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                imageDialog.dismiss();
                            }
                        });

                        imageDialog.show();
                    }
                }
            });
        } else {
            TextView galleryText = (TextView)svLayout.findViewById(R.id.galleryText);
            galleryText.setText("No Images");
        }

    }

    public class ImageGalleryAdapter extends BaseAdapter {

        private Context mContext;
        private final List<Bitmap> imageBmp;

        public ImageGalleryAdapter(Context c, List<Bitmap> imageBmp ) {
            this.mContext = c;
            this.imageBmp = imageBmp;
        }
        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return this.imageBmp.size();
        }
        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return this.imageBmp.get(position);
        }
        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return 0;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View v = convertView;
            View grid;
            LayoutInflater inflater;

            DisplayMetrics dm = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            int imagewWidth = dm.widthPixels / COL_NUM;

            inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            grid = inflater.inflate(R.layout.image_grid, null);
            grid.setLayoutParams(new GridView.LayoutParams(imagewWidth, imagewWidth));
            ImageView imageView = (ImageView)grid.findViewById(R.id.grid_image);
            imageView.setImageBitmap(this.imageBmp.get(position));

            return grid;
        }
    }

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

    private class RecipeImages {
        int id;
        String title;
        List<String> imageUrlList = new ArrayList<String>();

        @Override
        public String toString() {
            return this.title;
        }
    }
}
