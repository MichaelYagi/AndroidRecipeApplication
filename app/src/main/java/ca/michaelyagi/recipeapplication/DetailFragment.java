package ca.michaelyagi.recipeapplication;

/******************************************************************/
// Display's a recipe. User can choose to Edit or Delete recipe
/******************************************************************/

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Michael on 12/26/2014.
 */
public class DetailFragment extends Fragment {

    private FragmentTabHost mTabHost;
    private ViewPager mPager;

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

        mPager = (ViewPager) rootView.findViewById(R.id.pager);

        mTabHost = (FragmentTabHost)rootView.findViewById(android.R.id.tabhost);
        mTabHost.setup(getActivity(),getChildFragmentManager(), R.id.tab_content);

        mTabHost.addTab(mTabHost.newTabSpec("recipeFragmentTab").setIndicator("Recipe"),RecipeTabFragment.class, args);
        mTabHost.addTab(mTabHost.newTabSpec("recipeImagesFragmentTab").setIndicator("Gallery"), RecipeImagesTabFragment.class, args);

        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int pageNumber) {
                mTabHost.setCurrentTab(pageNumber);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });


        mTabHost.setOnTabChangedListener(new FragmentTabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                int pageNumber = 0;
                if (tabId.equals("recipeFragmentTab")) {
                    pageNumber = 0;
                } else if (tabId.equals("recipeImagesFragmentTab")) {
                    pageNumber = 1;
                }

                mPager.setCurrentItem(pageNumber);
            }
        });

        mPager.setAdapter(new CustomerPagerAdapter(getChildFragmentManager()));

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

    private class CustomerPagerAdapter extends FragmentPagerAdapter {

        public CustomerPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            Bundle args = new Bundle();
            args.putInt("recipe_id", recipeId);
            args.putString("recipe_user",recipeUser);
            args.putBoolean("recipe_ispublished",recipeIsPublished);

            if (position == 0) {
                RecipeTabFragment recipeTabFragment = new RecipeTabFragment();
                recipeTabFragment.setArguments(args);
                return recipeTabFragment;
            } else if (position == 1) {
                RecipeImagesTabFragment recipeImagesTabFragment = new RecipeImagesTabFragment();
                recipeImagesTabFragment.setArguments(args);
                return recipeImagesTabFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

    }


}
