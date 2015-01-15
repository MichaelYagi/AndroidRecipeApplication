package ca.michaelyagi.recipeapplication;

/******************************************************************/
// Get context for whole app
/******************************************************************/

import android.app.Application;
import android.content.Context;

/**
 * Created by Michael on 12/28/2014.
 */
public class RecipeBookApplication extends Application {
    private static Context context;

    public void onCreate(){
        super.onCreate();
        RecipeBookApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return RecipeBookApplication.context;
    }
}
