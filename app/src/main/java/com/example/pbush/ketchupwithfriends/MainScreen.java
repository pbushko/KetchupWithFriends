package com.example.pbush.ketchupwithfriends;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.widget.ImageView;
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

import retrofit2.http.GET;

public class MainScreen extends AppCompatActivity {

    public static int forIds = 0;

    final private int LOADED = 1;
    final private int SAVING = 1;
    final private int DONE_SAVING = 0;
    private int loaded;
    private int saving;

    //firebase items
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mDatabaseContacts;
    private DatabaseReference mDatabaseLastScrape;
    private Button signInButton;
    private Button saveButton;
    private long lastDataScrape;
    private ImageView loadingScreen;
    // Add this to firebase as well
    private AchievementData achieve;

    //the messages and contacts
    private List<MessageData> mMessages;
    private List<ContactData> mContacts;

    private void setMainScreen()
    {
        setContentView(R.layout.activity_main_screen);
        loadingScreen = findViewById(R.id.loadingScreen);
        TabHost host = (TabHost)findViewById(R.id.tabHost);
        host.setup();
        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("Contacts");
        spec.setContent(R.id.tab1);
        spec.setIndicator("Contacts");
        host.addTab(spec);
        //Tab 2
        spec = host.newTabSpec("Achievements");
        spec.setContent(R.id.achivementPage);
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
        signInButton.setVisibility(View.INVISIBLE);

        /*saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveInfo();
            }
        });*/

        //TextView achievementText = (TextView) findViewById(R.id.achievements);
        String text = "";
        // acheive.
        achieve = new AchievementData();
        achieve.checkday(Calendar.getInstance().getTimeInMillis());
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

                    // process new message for achievement if this is not first installation
                    /* uncomment-kasarn
                    if (lastDataScrape != 0) {
                        achieve.update(singleSms, mContacts);
                        achieve.incrMsg();
                    }
                    */
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
            /* uncomment-kasarn
            achieve.incrContact();
            */
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
            Intent i = new Intent(MainScreen.this, FirebaseUIActivity.class);
            startActivity(i);

        }
        else{
            Log.d("sign in", "signed in!");
            loadingScreen.setVisibility(View.VISIBLE);
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
        saving = SAVING;
        mDatabaseContacts.setValue(mContacts);
        // Read from the database
        mDatabaseContacts.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                for (DataSnapshot snap : dataSnapshot.getChildren()){
                    //mContacts.add(snap.getValue(ContactData.class));
                    //Log.d("write contacts", "Value is: " + mContacts.get(mContacts.size()-1));
                }
                if (saving == SAVING){
                    saving = DONE_SAVING;
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
                if (saving == DONE_SAVING) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    mContacts = new ArrayList<ContactData>();
                    for (DataSnapshot snap : dataSnapshot.getChildren()) {
                        mContacts.add(snap.getValue(ContactData.class));
                        //Log.d("read contacts", "contact: " + mContacts.get(0));
                    }
                    Log.d("sign in", "outside the read contacts function");
                    //putting the data onto the screen
                    if (loaded == LOADED) {
                        getNewInfo();
                        writeDataToScreen();
                    } else
                        loaded = LOADED;
                }
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
                if(loaded == LOADED) {
                    getNewInfo();
                    writeDataToScreen();
                }
                else
                    loaded = LOADED;
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
            List<ContactData> c = getContacts();
            int sze = mContacts.size();
            //only add new contacts if they are not already in our contact list
            if (sze != 0) {
                for (ContactData c1 : c) {
                    for (int i = 0; i < sze; i++) {
                        if (c1.compareTo(mContacts.get(i)) != 0) {
                            mContacts.add(c1);
                            i = sze;
                        }
                    }
                }
            }
            else {
                mContacts = c;
            }

            //sorting the messages by number and showing them
            for (int i = mMessages.size(); i >= 0; i--)
            {
                MessageData message = mMessages.get(i);
                //checking if the phone number already is in a contactData obj
                //this will be fixed later when we just get data from contacts
                for (ContactData contact : mContacts)
                {
                    if(contact.phoneNum.get(0).compareTo(formatPhoneNum(message.phoneNum)) == 0) {
                        contact.addMessage(message);
                        achieve.incrMsg();
                        Log.d("adding messages", "added a new message");
                    }
                }
            }
            //removing contacts to print if they have never been messaged
            int contactOrigLen = mContacts.size();
            int[] indxToRem;
            if (contactOrigLen > 0) {
                indxToRem = new int[contactOrigLen];
                //changing the index so that if 0 needs to be removed, it can be
                indxToRem[0] = -1;
                int idx = 0;
                for (int i = 0; i < contactOrigLen; i++)
                {
                    if(mContacts.get(i).numMessages() < 100) {
                        indxToRem[idx++] = i;
                    }
                }
                idx--;
                //if there are any contacts to change at all
                if (idx >= 0) {
                    for (int i = contactOrigLen - 1; i > 0; i--) {
                        if (idx < 0) {
                            i = 0;
                        }
                        else if (indxToRem[idx] == i) {
                            mContacts.remove(i);
                            idx--;
                        }
                    }
                }
            }
            else {
                mContacts = c;
            }

            Log.d("rem contacts", "original size: " + contactOrigLen);

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
            int buttonIndex = 0;
            for (ContactData contact : mContacts) {

                //make a button that will let you set the time for them
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                Fragment c = new ContactButtonFragment();
                final ContactButton button = (ContactButton)c;
                fragmentTransaction.add(R.id.scrolllinearlayout, c, "HELLO");
                fragmentTransaction.commitNow();
                //setting the info in the button
                button.resetButton(contact, buttonIndex);
                //getting the actual button of the fragment
                Button btn = button.getButton();
                //btn.setId(num);
                //on click, we want it to take us to the input time screen
                btn.setOnClickListener(new View.OnClickListener()
                {
                    public void onClick(View view)
                    {
                        //the user input button
                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        Fragment y = new GetUserInput();
                        final GetUserInput frag = (GetUserInput)y;
                        fragmentTransaction.add(button.getLayout(), y, "HELLO");
                        fragmentTransaction.commitNow();
                        //setting the info in the button
                        Button submit = frag.getSubmitButton();
                        submit.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int toAdd = frag.getNewContactFreq();
                                button.getButtonContact().setContactFrequency(toAdd);
                                setMainScreen();
                            }
                        });
                    }
                });
            }
        }
        loadingScreen.setVisibility(View.INVISIBLE);
        if (loaded == LOADED) {
            saveInfo();
        }
        Log.d("write data to screen", "done writing data");
    }

    //sorts the contacts from most recently messaged to least recently
    private void sortContacts()
    {
        if(mContacts != null) {
            Collections.sort(mContacts, new Comparator<ContactData>() {
                @Override
                public int compare(ContactData c1, ContactData c2) {
                    boolean deadline1 = c1.deadlineHere;
                    boolean deadline2 = c2.deadlineHere;
                    if (deadline1 && !deadline2) {
                        return -1;
                    } else if (!deadline1 && deadline2) {
                        return 1;
                    } else {
                        if (c1.lastMessaged > c2.lastMessaged)
                            return 1;
                        else return 0;
                    }
                }
            });
        }
    }

    public interface ContactButton{
        public abstract void resetButton(ContactData c, int idx);
        public abstract Button getButton();
        public abstract ContactData getButtonContact();
        public abstract int getIndex();
        public abstract int getLayout();
        public abstract Button getMsgButton();
    }

    public interface GetInput{
        public abstract int getNewContactFreq();
        public abstract Button getSubmitButton();
        public abstract Button getCancelButton();
    }
}