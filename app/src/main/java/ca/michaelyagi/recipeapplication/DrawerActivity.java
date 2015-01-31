package ca.michaelyagi.recipeapplication;
//TODO: Set drawer title

/******************************************************************/
// The left side drawer persistent throughout the app
/******************************************************************/

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.nsd.NsdManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Michael on 12/27/2014.
 */
public class DrawerActivity extends ActionBarActivity implements BrowseFragment.OnFragmentInteractionListener,LoginFragment.OnFragmentInteractionListener,RegisterFragment.OnFragmentInteractionListener,RecipeTabFragment.OnFragmentInteractionListener {
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    DrawerAdapter drawerListAdapter;
    List<DrawerListData> drawerItems = new ArrayList<DrawerListData>();
    private ActionBarDrawerToggle drawerToggle;

    ArrayAdapter<RecipeListData> listAdapter;
    // Create and populate a List of recipes
    List<RecipeListData> recipeList = new ArrayList<RecipeListData>();

    public static int LAST_DRAWER_HIGHLIGHT_POSITION = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_drawer);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setBackgroundDrawable(getResources().getDrawable(R.color.actionbar_background));

        //Initial drawer items
        DrawerListData d;

        if (SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()).length() == 0) {
            d = new DrawerListData();
            d.setDrawerItemText("Browse Recipe");
            d.setDrawerItemIcon(R.drawable.ic_action_view_as_list);
            drawerItems.add(d);

            d = new DrawerListData();
            d.setDrawerItemText("Login");
            d.setDrawerItemIcon(R.drawable.ic_action_person);
            drawerItems.add(d);

            d = new DrawerListData();
            d.setDrawerItemText("Register");
            d.setDrawerItemIcon(R.drawable.ic_action_add_person);
            drawerItems.add(d);
        } else {
            d = new DrawerListData();
            d.setDrawerItemText("Browse Recipe");
            d.setDrawerItemIcon(R.drawable.ic_action_view_as_list);
            drawerItems.add(d);

            d = new DrawerListData();
            d.setDrawerItemText("My Recipes");
            d.setDrawerItemIcon(R.drawable.ic_action_important);
            drawerItems.add(d);

            d = new DrawerListData();
            d.setDrawerItemText("Add Recipe");
            d.setDrawerItemIcon(R.drawable.ic_action_new);
            drawerItems.add(d);

            d = new DrawerListData();
            d.setDrawerItemText("My Account");
            d.setDrawerItemIcon(R.drawable.ic_action_accounts);
            drawerItems.add(d);
        }

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(drawerToggle);

        drawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the list's click listener
        drawerList.setOnItemClickListener(new DrawerItemClickListener());

        // Set the adapter for the list view
        drawerListAdapter = new DrawerAdapter(getSupportActionBar().getThemedContext(),R.layout.drawer_list_item,drawerItems);
        // Set the ArrayAdapter as the ListView's adapter.
        drawerList.setAdapter(drawerListAdapter);
        highlightDrawerItem(1);

        LayoutInflater inflater = getLayoutInflater();
        final ViewGroup drawerHeader = (ViewGroup) inflater.inflate(R.layout.drawer_list_header,drawerList,false);

        //Set Drawer header
        TextView headerUser = (TextView) drawerHeader.findViewById(R.id.header_user);
        TextView headerEmail = (TextView) drawerHeader.findViewById(R.id.header_email);
        String headerUserStr;
        String headerEmailStr;
        if (SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()).length() == 0) {
            headerUserStr = "Guest";
            headerEmailStr = "";
        } else {
            headerUserStr = SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext());
            headerEmailStr = SaveSharedPreference.getEmail(RecipeBookApplication.getAppContext());
        }
        headerUser.setText(headerUserStr);
        headerEmail.setText(headerEmailStr);
        drawerList.addHeaderView(drawerHeader, null, false);

        drawerToggle = new ActionBarDrawerToggle(this,drawerLayout,R.string.open_drawer,R.string.close_drawer) {

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                if (slideOffset != 0) {
                    drawerList.getChildAt(LAST_DRAWER_HIGHLIGHT_POSITION).setBackgroundColor(getResources().getColor(R.color.drawer_highlight));
                    drawerListAdapter.setSelected(LAST_DRAWER_HIGHLIGHT_POSITION);
                }
                super.onDrawerSlide(drawerView, slideOffset);
            }

            public void onDrawerStateChanged(int newState) {

                //Set Drawer header
                TextView headerUser = (TextView) drawerHeader.findViewById(R.id.header_user);
                TextView headerEmail = (TextView) drawerHeader.findViewById(R.id.header_email);
                String curHeaderUser = headerUser.getText().toString();
                String curHeaderEmail = headerEmail.getText().toString();
                String headerUserStr;
                String headerEmailStr;

                if (SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()).length() == 0) {
                    headerUserStr = "Guest";
                    headerEmailStr = "";
                } else {
                    headerUserStr = SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext());
                    headerEmailStr = SaveSharedPreference.getEmail(RecipeBookApplication.getAppContext());
                }

                if (!curHeaderUser.equals(headerUserStr) && !curHeaderEmail.equals(headerEmailStr)) {
                    headerUser.setText(headerUserStr);
                    headerEmail.setText(headerEmailStr);
                    drawerList.removeHeaderView(drawerHeader);
                    drawerList.addHeaderView(drawerHeader, null, false);
                }

            }

            public void onDrawerClosed(View drawerView) {
                ActivityCompat.invalidateOptionsMenu(DrawerActivity.this);
            }

            public void onDrawerOpened(View drawerView) {
                ActivityCompat.invalidateOptionsMenu(DrawerActivity.this);
            }

        };

        drawerList.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    drawerLayout.closeDrawers();
                } else {
                    clearKeyboard();
                }
            }
        });

        drawerLayout.setDrawerListener(drawerToggle);

        if (savedInstanceState == null) {
            //Default fragment is to browse recipes
            FragmentManager fragmentManager = getSupportFragmentManager();

            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            BrowseFragment browseRecipeFragment = new BrowseFragment();
            fragmentTransaction.add(R.id.content_frame, browseRecipeFragment);
            fragmentTransaction.commit();
        }

    }

    public void showDrawerToggle(boolean showDrawerIndicator) {
        drawerToggle.setDrawerIndicatorEnabled(showDrawerIndicator);
    }

    public void highlightDrawerItem(int row) {
        if (LAST_DRAWER_HIGHLIGHT_POSITION != row && drawerList.getChildAt(LAST_DRAWER_HIGHLIGHT_POSITION) != null) {
            drawerList.getChildAt(LAST_DRAWER_HIGHLIGHT_POSITION).setBackgroundColor(getResources().getColor(R.color.drawer_background));
        }
        LAST_DRAWER_HIGHLIGHT_POSITION = row;
    }

    /******************************************************************/
    // Define when user presses back button
    /******************************************************************/
    @Override
    public final void onBackPressed() {

        drawerToggle.setDrawerIndicatorEnabled(true);

        FragmentManager fragmentManager = getSupportFragmentManager();

        //If nothing on the stack, close the app
        if(fragmentManager.getBackStackEntryCount() == 0){
            finish();
            //Else pop the stack
        } else {
            fragmentManager.popBackStack();
        }
    }

    /******************************************************************/
    // Drawer selection logic
    /******************************************************************/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        drawerLayout.closeDrawer(drawerList);

        FragmentManager fragmentManager = getSupportFragmentManager();

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        if (drawerToggle.onOptionsItemSelected(item)) {
            //Not logged in
            if (SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()).length() == 0) {

                drawerItems.clear();

                DrawerListData d = new DrawerListData();
                d.setDrawerItemText("Browse Recipe");
                d.setDrawerItemIcon(R.drawable.ic_action_view_as_list);
                drawerItems.add(d);

                d = new DrawerListData();
                d.setDrawerItemText("Login");
                d.setDrawerItemIcon(R.drawable.ic_action_person);
                drawerItems.add(d);

                d = new DrawerListData();
                d.setDrawerItemText("Register");
                d.setDrawerItemIcon(R.drawable.ic_action_add_person);
                drawerItems.add(d);

                drawerListAdapter = new DrawerAdapter(getSupportActionBar().getThemedContext(),R.layout.drawer_list_item,drawerItems);
                drawerList.setAdapter(drawerListAdapter);
            } else {
                drawerItems.clear();

                DrawerListData d = new DrawerListData();
                d.setDrawerItemText("Browse Recipe");
                d.setDrawerItemIcon(R.drawable.ic_action_view_as_list);
                drawerItems.add(d);

                d = new DrawerListData();
                d.setDrawerItemText("My Recipes");
                d.setDrawerItemIcon(R.drawable.ic_action_important);
                drawerItems.add(d);

                d = new DrawerListData();
                d.setDrawerItemText("Add Recipe");
                d.setDrawerItemIcon(R.drawable.ic_action_new);
                drawerItems.add(d);

                d = new DrawerListData();
                d.setDrawerItemText("My Account");
                d.setDrawerItemIcon(R.drawable.ic_action_accounts);
                drawerItems.add(d);

                drawerListAdapter = new DrawerAdapter(getSupportActionBar().getThemedContext(),R.layout.drawer_list_item,drawerItems);
                drawerList.setAdapter(drawerListAdapter);
            }

            return true;
        }

        switch (item.getItemId()) {
            case R.id.menu_login:
                highlightDrawerItem(2);
                LoginFragment loginFragment = new LoginFragment();
                fragmentTransaction.replace(R.id.content_frame, loginFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                return true;
            case R.id.menu_register:
                highlightDrawerItem(3);
                RegisterFragment registerFragment = new RegisterFragment();
                fragmentTransaction.replace(R.id.content_frame, registerFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                return true;
            case R.id.menu_logout:
                highlightDrawerItem(1);
                Toast.makeText(this, "Logging Out...",Toast.LENGTH_SHORT).show();
                SaveSharedPreference.clearCredentials(RecipeBookApplication.getAppContext());

                BrowseFragment browseRecipeFragment = new BrowseFragment();
                fragmentTransaction.replace(R.id.content_frame, browseRecipeFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    /******************************************************************/
    // Overflow menu change on fragment changes
    /******************************************************************/
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (menu.size() == 0) {
            getMenuInflater().inflate(R.menu.overflow_menu, menu);
            getMenuInflater().inflate(R.menu.detail_menu, menu);
        }

        //Non overflow items
        /******************************************************************/
        // Search Recipes
        /******************************************************************/
        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        searchItem.setVisible(true);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.menu_item_search).getActionView();
        if (null != searchView ) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() > 3) {
                    /*
                    searchSuggestions.clear();
                    try {
                        newText = URLEncoder.encode(newText, "UTF-8");
                    } catch(UnsupportedEncodingException e) {
                        //TODO: Catch URLEncoder exception
                    }
                    */
                } else {
                    return false;
                }
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                String searchTerm = query;

                //Pass search term to BrowseActivity with Bundle
                Bundle args = new Bundle();
                args.putBoolean("viewbysearch_filter", true);
                args.putString("searchterm",searchTerm);

                clearKeyboard();

                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                BrowseFragment browseRecipeFragment = new BrowseFragment();
                browseRecipeFragment.setArguments(args);
                fragmentTransaction.replace(R.id.content_frame, browseRecipeFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();

                return true;

            }
        });

        MenuItem shareItem = menu.findItem(R.id.menu_item_share);
        shareItem.setVisible(false);

        MenuItem deleteRecipesItem = menu.findItem(R.id.menu_item_delete_recipes);
        deleteRecipesItem.setVisible(false);

        MenuItem deleteImagesItem = menu.findItem(R.id.menu_item_delete_images);
        deleteImagesItem.setVisible(false);

        //Overflow items
        MenuItem logoutItem = menu.findItem(R.id.menu_logout);
        logoutItem.setVisible(false);

        MenuItem loginItem = menu.findItem(R.id.menu_login);
        loginItem.setVisible(false);

        MenuItem registerItem = menu.findItem(R.id.menu_register);
        registerItem.setVisible(false);

        MenuItem editItem = menu.findItem(R.id.menu_edit);
        editItem.setVisible(false);

        MenuItem deleteRecipeItem = menu.findItem(R.id.menu_delete);
        deleteRecipeItem.setVisible(false);

        //Not logged in
        if (SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()).length() == 0) {
            loginItem.setVisible(true);
            registerItem.setVisible(true);
        } else {
            logoutItem.setVisible(true);
        }

        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    //Selected item in drawer
    private void selectItem(int position) {

        //Take into account the Layout added above
        position = position - 1;

        FragmentManager fragmentManager = getSupportFragmentManager();

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        BrowseFragment browseRecipeFragment;

        if (SaveSharedPreference.getUsername(RecipeBookApplication.getAppContext()).length() == 0) {
            switch (position) {
                //Browse Recipes
                case 0:
                    browseRecipeFragment = new BrowseFragment();
                    fragmentTransaction.replace(R.id.content_frame, browseRecipeFragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
                case 1:
                    LoginFragment loginFragment = new LoginFragment();
                    fragmentTransaction.replace(R.id.content_frame, loginFragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
                case 2:
                    //Register
                    RegisterFragment registerFragment = new RegisterFragment();
                    fragmentTransaction.replace(R.id.content_frame, registerFragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
                default:
                    browseRecipeFragment = new BrowseFragment();
                    fragmentTransaction.replace(R.id.content_frame, browseRecipeFragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
            }
        } else {
            switch (position) {
                //Browse Recipes
                case 0:
                    browseRecipeFragment = new BrowseFragment();
                    fragmentTransaction.replace(R.id.content_frame, browseRecipeFragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
                case 1:
                    //My Recipes
                    Bundle args = new Bundle();
                    args.putBoolean("viewbyuser_filter", true);

                    browseRecipeFragment = new BrowseFragment();
                    browseRecipeFragment.setArguments(args);
                    fragmentTransaction.replace(R.id.content_frame, browseRecipeFragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
                case 2:
                    //Add Recipe
                    EditRecipeFragment addRecipeFragment = new EditRecipeFragment();
                    fragmentTransaction.replace(R.id.content_frame, addRecipeFragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
                case 3:
                    //My Account
                    AccountFragment accountFragment = new AccountFragment();
                    fragmentTransaction.replace(R.id.content_frame, accountFragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
                case 4:
                    //Log out and redirect to Browse
                    SaveSharedPreference.clearCredentials(RecipeBookApplication.getAppContext());

                    browseRecipeFragment = new BrowseFragment();
                    fragmentTransaction.replace(R.id.content_frame, browseRecipeFragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
                default:
                    browseRecipeFragment = new BrowseFragment();
                    fragmentTransaction.replace(R.id.content_frame, browseRecipeFragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                    break;
            }
        }

        // Highlight the selected item, update the title, and close the drawer
        highlightDrawerItem(position+1);
        setTitle(drawerItems.get(position).getDrawerItemText());
        drawerLayout.closeDrawer(drawerList);
    }

    /******************************************************************/
    // Classes
    /******************************************************************/
    class RecipeListData {
        private int id;
        private String title;
        private String user;

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

        public String getUser() { return this.user; }

        public void setId(int anId) {
            this.id = anId;
        }

        public void setTitle(String aTitle) {
            this.title = aTitle;
        }

        public void setUser(String aUser) {
            this.user = aUser;
        }
    }

    class DrawerListData {
        private int drawerItemIcon;
        private String drawerItemText;

        public String getDrawerItemText() {return this.drawerItemText;}
        public int getDrawerItemIcon() {return this.drawerItemIcon;}
        public void setDrawerItemText(String anItemText) {this.drawerItemText = anItemText;}
        public void setDrawerItemIcon(int anItemIcon) {this.drawerItemIcon = anItemIcon;}
    }

    public class DrawerAdapter extends ArrayAdapter<DrawerListData> {
        private final Context context;
        private final List<DrawerListData> data;
        private final int layoutResourceId;
        private int selected;

        public DrawerAdapter(Context context, int layoutResourceId, List<DrawerListData> data) {
            super(context, layoutResourceId, data);
            this.context = context;
            this.data = data;
            this.layoutResourceId = layoutResourceId;
        }

        public void setSelected(int selected) {
            this.selected = selected;
            notifyDataSetChanged();
        }

        private class ViewHolder {
            ImageView drawerIcon;
            TextView drawerText;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View cView = convertView;
            final int vPosition = position;
            final ViewGroup vParent = parent;

            ViewHolder holder = null;
            DrawerListData rowItem = getItem(position);

            LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.drawer_list_item, null);
                holder = new ViewHolder();
                holder.drawerIcon = (ImageView) convertView.findViewById(R.id.drawerItemIcon);
                holder.drawerText = (TextView) convertView.findViewById(R.id.drawerItemView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if ((position+1) == selected) {
                holder.drawerText.setTextColor(getResources().getColor(R.color.drawer_text_highlight));
                holder.drawerIcon.setColorFilter(getResources().getColor(R.color.drawer_text_highlight));
            } else {
                holder.drawerText.setTextColor(Color.DKGRAY);
                holder.drawerIcon.setColorFilter(Color.DKGRAY);
            }

            holder.drawerText.setText(rowItem.getDrawerItemText());
            holder.drawerIcon.setImageResource(rowItem.getDrawerItemIcon());

            return convertView;
        }

    }

    public void clearKeyboard() {
        //Hide keyboard after input
        View target = getCurrentFocus();
        if (target != null) {
            InputMethodManager imm = (InputMethodManager) target.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(target.getWindowToken(), 0);
        }
    }

}
