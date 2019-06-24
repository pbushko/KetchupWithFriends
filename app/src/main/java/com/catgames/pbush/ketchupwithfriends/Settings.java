package com.catgames.pbush.ketchupwithfriends;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Created by pbush on 11/9/2018.
 */

public class Settings extends AppCompatActivity {

    private Button signOutButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_screen);
        // get action bar
        //ActionBar actionBar = getActionBar();
        mAuth = FirebaseAuth.getInstance();
        // Enabling Up / Back navigation
        //actionBar.setDisplayHomeAsUpEnabled(true);
        signOutButton = findViewById(R.id.logOutButton);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent i = new Intent(Settings.this, FirebaseUIActivity.class);
                startActivity(i);
            }
        });
    }
}
