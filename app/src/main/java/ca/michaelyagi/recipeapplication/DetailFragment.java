package ca.michaelyagi.recipeapplication;

/******************************************************************/
// Display's a recipe. User can choose to Edit or Delete recipe
/******************************************************************/

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Michael on 12/26/2014.
 */
public class DetailFragment extends Fragment {

    private FragmentTabHost mTabHost;

    private int recipeId;
    private String recipeUser;
    private boolean recipeIsPublished;

    /******************************************************************/
    // On create
    /******************************************************************/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.detail_tab_host,container, false);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("Recipe");
        // update the actionbar to show the up carat
        ((ActionBarActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setHasOptionsMenu(true);

        //Get arguments passed
        recipeId = getArguments().getInt("recipe_id");
        recipeUser = getArguments().getString("recipe_user");
        recipeIsPublished = getArguments().getBoolean("recipe_ispublished");

        Bundle args = new Bundle();
        args.putInt("recipe_id", recipeId);
        args.putString("recipe_user",recipeUser);
        args.putBoolean("recipe_ispublished",recipeIsPublished);

        mTabHost = (FragmentTabHost)rootView.findViewById(android.R.id.tabhost);
        mTabHost.setup(getActivity(),getChildFragmentManager(), R.id.tab_content);

        mTabHost.addTab(mTabHost.newTabSpec("recipeFragmentTab").setIndicator("Recipe"),RecipeTabFragment.class, args);
        mTabHost.addTab(mTabHost.newTabSpec("recipeImagesFragmentTab").setIndicator("Gallery"), RecipeImagesTabFragment.class, args);

        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Get item selected and deal with it
        switch (item.getItemId()) {
            case android.R.id.home:
                //called when the up carat in actionbar is pressed
                getActivity().onBackPressed();
                return true;
            //Share button
            case R.id.menu_item_share:
                return false;
            //Edit a recipe
            case R.id.menu_edit:
                return false;
            //Delete a recipe with dialog
            case R.id.menu_delete:
                return false;
        }

        return true;
    }

}
