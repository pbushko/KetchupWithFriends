package com.example.pbush.ketchupwithfriends;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.jar.Attributes;

public class MainScreen extends AppCompatActivity {

    //firebase items
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mDatabaseContacts;
    private DatabaseReference mDatabaseLastScrape;
    private GoogleSignInClient mGoogleSignInClient;
    private Button signInButton;
    private Button saveButton;
    private Button getInputButton;
    private Button submitInputButton;

    private long lastDataScrape;

    private EditText userNum;

    private int contactButtonId;

    //the messages and contacts
    private List<MessageData> mMessages;
    private List<ContactData> mContacts;

    private void setMainScreen()
    {
        setContentView(R.layout.activity_main_screen);

        TabHost host = (TabHost)findViewById(R.id.tabHost);
        host.setup();
        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("Contacts");
        spec.setContent(R.id.tab1);
        spec.setIndicator("Contacts");
        host.addTab(spec);
        //Tab 2
        spec = host.newTabSpec("Achievements");
        spec.setContent(R.id.tab2);
        spec.setIndicator("Achievements");
        host.addTab(spec);

        signInButton = findViewById(R.id.signInButton);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainScreen.this, FirebaseUIActivity.class);
                startActivity(i);
            }
        });

        getInputButton = findViewById(R.id.set_time_button);
        getInputButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.get_contact_frequency_screen);
                final Spinner staticSpinner = (Spinner) findViewById(R.id.time_option_spinner);
                userNum = (EditText) findViewById(R.id.user_num_input);
                userNum.setTransformationMethod(null);
                GetUserInput.setInputScreen(MainScreen.this, staticSpinner);
                submitInputButton = findViewById(R.id.ok_button);
                submitInputButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String spinner_item = staticSpinner.getSelectedItem().toString();
                        setMainScreen();
                        String num = userNum.getText().toString();
                        Log.d("userNum", "num is: " + num + " spinner is: " + spinner_item);
                    }
                });
            }
        });

        //setting the save button to save info when pressed
        saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveInfo();
            }
        });

        signInButton.setVisibility(View.INVISIBLE);

        writeDataToScreen();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setMainScreen();

        mAuth = FirebaseAuth.getInstance();

        mDatabase = FirebaseDatabase.getInstance();
        mDatabaseContacts = mDatabase.getReference("contacts");
        mDatabaseLastScrape = mDatabase.getReference("lastScrape");

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                //they are signed in
                if(firebaseAuth.getCurrentUser() != null)
                {
                    //hide the sign in button
                    signInButton.setVisibility(View.INVISIBLE);
                }
            }
        };

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings)
        {
            Intent i = new Intent(this, Settings.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public String formatPhoneNum(String num)
    {
        //only get the last 9 digits if there are more than the normal amount
        if (num.length() > 10) {
            return num.substring(num.length() - 10, num.length());
        }
        else return num;
    }

    public List<MessageData> getSentMessages()
    {
        Uri uriSms = Uri.parse("content://sms/sent");
        Cursor cursor = this.getContentResolver().query(uriSms, null,null,null,null);
        List<MessageData> outboxSms = parseCursorArray(cursor);
        if(!cursor.isClosed())
        {
            cursor.close();
        }
        return outboxSms;
    }

    public List<MessageData> parseCursorArray(Cursor cursor)
    {

        long now = Calendar.getInstance().getTimeInMillis();
        Log.d("message data", "last data scrape: " + lastDataScrape + " Now: " + now);
        if(null == cursor || 0 == cursor.getCount())
        {
            return new ArrayList<MessageData>();
        }
        List<MessageData> messages = new ArrayList<MessageData>();
        try
        {
            long lastTimeStamp = now + 1;
            //only getting the messages that weren't scraped since last opening of the app

            int numNewMessages = 0;
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                if (lastTimeStamp > lastDataScrape) {
                    MessageData singleSms = new MessageData();
                    singleSms.id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
                    String temp = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    //need to remove any dashes from the phone number
                    singleSms.phoneNum = formatPhoneNumber(temp);
                    singleSms.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                    lastTimeStamp = singleSms.timestamp;
                    messages.add(singleSms);
                    numNewMessages++;
                    //Log.d("newMessageScrape", "Num new messages: " + numNewMessages);
                }
                else {
                    cursor.moveToLast();
                }
            }
            Log.d("newMessageScrape", "Num new messages: " + numNewMessages);
        }
        catch (Exception e)
        {
            Log.e("parse messages", e.getMessage());
        }

        cursor.close();
        lastDataScrape = now;
        return messages;
    }

    public List<ContactData> getContacts()
    {
        List<ContactData> contacts = new ArrayList<ContactData>();
        ContentResolver cr = this.getContentResolver();
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
        {
            String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            int nameFieldColumnIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            String name = cursor.getString(nameFieldColumnIndex);
            String number = "";
            //getting the phone number for everyone
            if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(
                    ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                Cursor phoneCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                        new String[]{id}, null);
                phoneCursor.moveToNext();
                int numberFieldColumnIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                number = formatPhoneNumber(phoneCursor.getString(numberFieldColumnIndex));
                phoneCursor.close();
            }
            ContactData c = new ContactData();
            c.id = id;
            c.name = name;
            c.addPhoneNumber(number);
            contacts.add(c);
        }

        cursor.close();

        return contacts;
    }

    private String formatPhoneNumber(String number)
    {
        return number.replace("-", "").replace("+", "")
            .replace(" ", "").replace(")", "")
                .replace("(", "");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_actions, menu);
        return true;
    }

    public void updateUI(FirebaseUser user)
    {
        if(user == null)
        {
            //sign in/create an account with google in the firebase UI
            //creating the firebase sign in page
            signInButton.setVisibility(View.VISIBLE);

        }
        else{
            Log.d("sign in", "signed in!");
            signInButton.setVisibility(View.INVISIBLE);
            //getting the saved and new info
            mContacts = new ArrayList<ContactData>();
            getSavedInfo();
            //lastDataScrape = 0;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Toast.makeText(this, "main", Toast.LENGTH_SHORT).show();
        mAuth.addAuthStateListener(mAuthListener);
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    public void saveInfo()
    {
        mDatabaseContacts.setValue(mContacts);
        // Read from the database
        mDatabaseContacts.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                for (DataSnapshot snap : dataSnapshot.getChildren()){
                    mContacts.add(snap.getValue(ContactData.class));
                    //Log.d("write contacts", "Value is: " + mContacts.get(mContacts.size()-1));
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("read contacts", "Failed to read value.", error.toException());
            }
        });
        mDatabaseLastScrape.child("lastDataScrape").setValue(lastDataScrape);
        // Read from the database
        mDatabaseLastScrape.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                for (DataSnapshot snap : dataSnapshot.getChildren()){
                    lastDataScrape = snap.getValue(Long.class);
                    Log.d("save data", "saved last data scrape time! it was: " + lastDataScrape);
                }
                Log.d("save data", "outside the scrape function");
            }
            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("read contacts", "Failed to read value.", error.toException());
            }
        });
    }

    public void getSavedInfo()
    {
        Toast.makeText(this, "Getting save info", Toast.LENGTH_SHORT).show();
        mDatabaseContacts.orderByValue();
        mDatabaseContacts.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                for (DataSnapshot snap : dataSnapshot.getChildren()){
                    mContacts.add(snap.getValue(ContactData.class));
                    //Log.d("read contacts", "contact: " + mContacts.get(0));
                }
                Log.d("sign in", "outside the read contacts function");
                //putting the data onto the screen
                getNewInfo();
                writeDataToScreen();
            }
            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("read contacts", "Failed to read value.", error.toException());
            }
        });
        //trying to get the last scraped time
        mDatabaseLastScrape.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snap : dataSnapshot.getChildren()){
                    lastDataScrape = snap.getValue(Long.class);
                    Log.d("sign in", "last data scrape time! it was: " + lastDataScrape);
                }
                Log.d("sign in", "outside the scrape function");
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("read contacts", "Failed to read value.", databaseError.toException());
            }
        });
        Log.d("sign in", "last data scrape: " + lastDataScrape);
        Toast.makeText(this, "got save info", Toast.LENGTH_SHORT).show();
    }

    public void getNewInfo() {
        Toast.makeText(this, "Getting new info", Toast.LENGTH_SHORT).show();
        Log.d("sign in", "last data scrape: " + lastDataScrape);
        //getting permission from the user to access message and contact data
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    1);
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS},
                    2);
        }
        //read in messages if we have access to it
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                        == PackageManager.PERMISSION_GRANTED)
        {

            mMessages = getSentMessages();
            mContacts = getContacts();

            //sorting the messages by number and showing them
            for (MessageData message : mMessages)
            {
                //checking if the phone number already is in a contactData obj
                //this will be fixed later when we just get data from contacts
                boolean newNum = true;
                for (ContactData contact : mContacts)
                {
                    if(contact.phoneNum.get(0).compareTo(formatPhoneNum(message.phoneNum)) == 0) {
                        newNum = false;
                        contact.addMessage(message);
                    }
                }
            }
            //removing contacts to print if they have never been messaged
            int contactOrigLen = mContacts.size();
            int[] indxToRem = new int[contactOrigLen];
            //changing the index so that if 0 needs to be removed, it can be
            indxToRem[0] = -1;
            int idx = 0;
            for (int i = 0; i < contactOrigLen; i++)
            {
                if(mContacts.get(i).lastMessaged == 0) {
                    indxToRem[idx++] = i;
                }
            }
            Log.d("rem contacts", "original size: " + contactOrigLen);
            idx--;
            for (int i = contactOrigLen-1; i > 0; i--)
            {
                if(indxToRem[idx] == i) {
                    mContacts.remove(i);
                    idx--;
                }
            }
            Log.d("rem contacts", "new size: " + mContacts.size());
            //just need this to get the set dates since that's where their
            //contact deadlines get set rn
            for (ContactData contact : mContacts){
                contact.toString();
            }
            sortContacts();
        }
        Toast.makeText(this, "Got new info", Toast.LENGTH_SHORT).show();
    }

    public void writeDataToScreen(){
        Log.d("write data to screen", "writing data...");
        String myText = "";
        LinearLayout lView = (LinearLayout)findViewById(R.id.scrolllinearlayout);
        //showing the contact data
        if (mContacts != null) {
            for (ContactData contact : mContacts) {

                //make a button that will let you set the time for them
                Button btn= new Button(this);
                btn.setText(contact.toString());
                int num = Integer.parseInt(contact.phoneNum.get(0).substring(1));
                btn.setId(num);
                //on click, we want it to take us to the input time screen
                btn.setOnClickListener(new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        //your write code
                        setContentView(R.layout.get_contact_frequency_screen);
                        final Spinner staticSpinner = (Spinner) findViewById(R.id.time_option_spinner);
                        userNum = (EditText) findViewById(R.id.user_num_input);
                        userNum.setTransformationMethod(null);
                        GetUserInput.setInputScreen(MainScreen.this, staticSpinner);
                        contactButtonId = view.getId();
                        submitInputButton = findViewById(R.id.ok_button);
                        submitInputButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int num = Integer.parseInt(userNum.getText().toString());
                                for (ContactData contact : mContacts){
                                    if (Integer.parseInt(contact.phoneNum.get(0).substring(1)) == contactButtonId) {
                                        int multiplier = 0;
                                        switch (staticSpinner.getSelectedItem().toString())
                                        {
                                            case("Hour"):
                                                multiplier = 1;
                                                break;
                                            case("Day"):
                                                multiplier = 24;
                                                break;
                                            case("Week"):
                                                multiplier = 7*24;
                                                break;
                                            // NOT IMPLEMENTED RIGHT NOW
                                            case("Month"):
                                                multiplier = 1;
                                                break;
                                            default:
                                                multiplier = 1;
                                                break;
                                        }
                                        contact.setContactFrequency(num*multiplier);
                                        setMainScreen();
                                    }
                                }
                            }
                        });
                    }
                });
                lView.addView(btn);
                //Log.d("userNum", "contact data: " + contact.name + " " + contact.nextMessageDeadline);
            }
        }

        Log.d("write data to screen", "done writing data");
    }

    //sorts the contacts from most recently messaged to least recently
    private void sortContacts()
    {
        Collections.sort(mContacts, new Comparator<ContactData>() {
            @Override
            public int compare(ContactData c1, ContactData c2)
            {
                boolean deadline1 = c1.deadlineHere;
                boolean deadline2 = c2.deadlineHere;
                if (deadline1 && !deadline2)
                {
                    return -1;
                }
                else if (!deadline1 && deadline2)
                {
                    return 1;
                }
                else if (deadline1 && deadline2)
                {
                    if (c1.lastMessaged > c2.lastMessaged)
                        return 1;
                    else return 0;
                }
                else
                {
                    if (c1.lastMessaged < c2.lastMessaged)
                        return 1;
                    else return 0;
                }
            }
        });
    }
}
