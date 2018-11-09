package com.example.pbush.ketchupwithfriends;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;

/**
 * Created by pbush on 11/9/2018.
 */

public class Settings extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_screen);
        // get action bar
        //ActionBar actionBar = getActionBar();

        // Enabling Up / Back navigation
        //actionBar.setDisplayHomeAsUpEnabled(true);
    }
}
