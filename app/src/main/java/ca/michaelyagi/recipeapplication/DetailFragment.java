package ca.michaelyagi.recipeapplication;

/******************************************************************/
// Display's a recipe. User can choose to Edit or Delete recipe
/******************************************************************/

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Created by Michael on 12/26/2014.
 */
public class DetailFragment extends Fragment implements ActionBar.TabListener {

    private FragmentTabHost mTabHost;
    private ViewPager viewPager;
    private TabsPagerAdapter mAdapter;
    private ActionBar actionBar;
    private LinearLayout rootView;

    private int recipeId;
    private String recipeUser;

    private int NUM_OF_TABS = 2;

    /******************************************************************/
    // On create
    /******************************************************************/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        rootView = (LinearLayout) inflater.inflate(R.layout.detail_tab_host,container, false);
        ((ActionBarActivity)getActivity()).getSupportActionBar().setTitle("Recipe");

        setHasOptionsMenu(true);

        // update the actionbar to show the up carat
        ((ActionBarActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Get arguments passed
        recipeId = getArguments().getInt("recipe_id");
        recipeUser = getArguments().getString("recipe_user");

        // Initilization
        viewPager = (ViewPager) rootView.findViewById(R.id.pager);
        actionBar = ((ActionBarActivity) getActivity()).getSupportActionBar();
        mAdapter = new TabsPagerAdapter(getFragmentManager());

        viewPager.setAdapter(mAdapter);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Adding Tabs
        actionBar.addTab(actionBar.newTab().setText("Recipe").setTabListener(this));
        actionBar.addTab(actionBar.newTab().setText("Gallery").setTabListener(this));

        /**
         * on swiping the viewpager make respective tab selected
         * */
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                // on changing the page
                // make respected tab selected
                actionBar.setSelectedNavigationItem(position);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });

        return rootView;
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        // on tab selected
        // show respected fragment view
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onDestroyView() {
        actionBar.removeAllTabs();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        super.onDestroyView();
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

    public class TabsPagerAdapter extends FragmentStatePagerAdapter {

        public TabsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int index) {

            Bundle args = new Bundle();
            args.putInt("recipe_id", recipeId);
            args.putString("recipe_user",recipeUser);

            switch (index) {
                case 0:
                    RecipeTabFragment recipeFragment = new RecipeTabFragment();
                    recipeFragment.setArguments(args);
                    return recipeFragment;
                case 1:
                    RecipeImagesTabFragment recipeImageFragment = new RecipeImagesTabFragment();
                    recipeImageFragment.setArguments(args);
                    return recipeImageFragment;
            }

            return null;
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public int getCount() {
            // get item count - equal to number of tabs
            return NUM_OF_TABS;
        }

    }
}
