package com.example.pbush.ketchupwithfriends;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Created by pbush on 11/9/2018.
 */

public class Settings extends Activity {

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
