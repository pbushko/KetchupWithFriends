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
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
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
import android.view.MotionEvent;
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
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Email;

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

    public static MainScreen m;

    public static int forIds = 0;
    final private int LOADED = 1;
    final private int LOADING = 0;
    final private int SAVING = 1;
    final private int DONE_SAVING = 0;
    private static final int CONTACT_PICKER_RESULT = 1001;
    private static final int MESSAGER_RESULT = 101;
    private int loaded;
    private int saving;
    private long mTimer;
    private boolean mHeld;

    private List<ContactButton> mContactFrags;

    //firebase items
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mDatabaseUserInfo;
    private long lastDataScrape;
    private ImageView loadingScreen;
    // Add this to firebase as well
    private AchievementData achieve;
    private MediaPlayer mediaPlayer;
    private ImageView achievementTomato;
    private List<GetContactsFragment> selectedContacts;
    private Button mContactButton;
    private Button mDeleteContactsButton;

    private String userId;

    private Handler mHandler;
    private Runnable mRunnable;

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

        Button b = findViewById(R.id.get_contacts);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                doLaunchContactPicker(view);
            }
        }
        );

        mContactButton = findViewById(R.id.get_multiple_contacts);
        mContactButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                getMultipleContacts();
            }}
        );
        mContactButton.setVisibility(View.INVISIBLE);

        mDeleteContactsButton = findViewById(R.id.delete_contacts_button);
        mDeleteContactsButton.setVisibility(View.INVISIBLE);

        //getting the achievement tomato to change
        achievementTomato = findViewById(R.id.tomato1);

        selectedContacts = new ArrayList<>();

        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                //checking if a button has been held for long enough
                long time2 = Calendar.getInstance().getTimeInMillis();
                //Log.d("event", "held " + mTimer);
                //Log.d("event", "time2 " + time2);
                if (time2 - mTimer > 800) {
                    Log.d("event", "held");
                    mHeld = true;
                    mDeleteContactsButton.setVisibility(View.VISIBLE);
                    //putting check boxes on all the contact buttons
                    for (ContactButton f : mContactFrags) {
                        f.switchToDelete();
                    }
                    //stop the button from being held anymore and take care of it being held
                    mHandler.removeCallbacks(mRunnable);
                }
                mHandler.postDelayed(this, 10);
            }
        };

        writeDataToScreen();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        m = this;
        super.onCreate(savedInstanceState);
        setMainScreen();

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

            }
        };

        // acheive.
        achieve = new AchievementData();
        achieve.checkday(Calendar.getInstance().getTimeInMillis());

        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.free_music);
        mediaPlayer.start();
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
            lastDataScrape = 1;
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
        saveInfo();
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
            //getting the saved and new info
            mContacts = new ArrayList<ContactData>();
            userId = user.getUid();
            getSavedInfo();
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
        mDatabaseUserInfo = mDatabase.getReference(userId);
        saving = SAVING;
        mDatabaseUserInfo.child("contacts").setValue(mContacts);
        // Read from the database
        mDatabaseUserInfo.child("contacts").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
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
        mDatabaseUserInfo.child("achievements").setValue(achieve);
        // Read from the database
        mDatabaseUserInfo.child("achievements").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
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
        mDatabaseUserInfo.child("lastDataScrape").setValue(lastDataScrape);
        // Read from the database
        mDatabaseUserInfo.child("lastDataScrape").addValueEventListener(new ValueEventListener() {
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
        mDatabaseUserInfo = mDatabase.getReference(userId);
        //Toast.makeText(this, "Getting save info", Toast.LENGTH_SHORT).show();
        mDatabaseUserInfo.child("contacts").orderByValue();
        mDatabaseUserInfo.child("contacts").addValueEventListener(new ValueEventListener() {
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
                    //if there are no saved contacts, get the user to pick them
                    Log.d("sign in", "outside the read contacts function");
                    //putting the data onto the screen
                    if (loaded == LOADED) {
                        if (mContacts.size() == 0)
                        {
                            selectContacts();
                        }
                        else {
                            getNewInfo();
                            setMainScreen();
                        }
                        loaded = LOADING;
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
        mDatabaseUserInfo.child("lastDataScrape").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot snap : dataSnapshot.getChildren()){
                    lastDataScrape = snap.getValue(Long.class);
                    Log.d("sign in", "last data scrape time! it was: " + lastDataScrape);
                }

                Log.d("sign in", "outside the scrape function");
                if(loaded == LOADED) {
                    if (mContacts.size() == 0)
                    {
                        selectContacts();
                    }
                    else {
                        getNewInfo();
                        setMainScreen();
                    }
                    loaded = LOADING;
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
        //Toast.makeText(this, "got save info", Toast.LENGTH_SHORT).show();
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

            //sorting the messages by number and showing them
            for (int i = mMessages.size() - 1; i >= 0; i--)
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
            if (achieve.messageAchievementProgress()){
                achievementTomato.setImageResource(R.drawable.red_achievement_tomato);
            }
            //just need this to get the set dates since that's where their
            //contact deadlines get set rn
            for (ContactData contact : mContacts){
                contact.toString();
            }
            sortContacts();
        }
        Toast.makeText(this, "Got new info", Toast.LENGTH_SHORT).show();
    }

    public void selectContacts() {
        List<ContactData> c = getContacts();
        selectedContacts = new ArrayList<>();

        mContactButton.setVisibility(View.VISIBLE);

        for (ContactData contact : c) {
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            Fragment toSelect = new GetMultipleContacts();
            GetContactsFragment frag = (GetContactsFragment)toSelect;
            fragmentTransaction.add(R.id.scrolllinearlayout, toSelect, "HELLO");
            fragmentTransaction.commitNow();
            frag.resetButton(contact);
            selectedContacts.add(frag);
        }

        loadingScreen.setVisibility(View.INVISIBLE);
    }

    public void getMultipleContacts() {
        for (GetContactsFragment f : selectedContacts) {
            if (f.isChecked()) {
                mContacts.add(f.getContact());
            }
        }
        mContactButton.setVisibility(View.INVISIBLE);
        saveInfo();
        setMainScreen();
    }

    public void sendMessage(Intent messageIntent) {
        startActivityForResult(messageIntent, MESSAGER_RESULT);
    }

    public void writeDataToScreen(){
        //showing the contact data
        if (mContacts != null && mContacts.size() > 0) {
            mContactFrags = new ArrayList<>();
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
                final Button btn = button.getButton();
                //on click, we want it to take us to the input time screen
                btn.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch(event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                // Start
                                mHeld = false;
                                mTimer = Calendar.getInstance().getTimeInMillis();
                                mHandler.post(mRunnable);
                                break;
                            case MotionEvent.ACTION_UP:
                                // End
                                if (mHeld) {
                                    Log.d("event", "held");

                                }
                                else {
                                    Log.d("event", "pushed");
                                    //the user input button
                                    final FragmentManager fragmentManager = getFragmentManager();
                                    final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                                    final Fragment y = new GetUserInput();
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
                                            button.setContactTime();
                                            //setMainScreen();
                                        }
                                    });
                                    Button cancel = frag.getCancelButton();
                                    cancel.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            setMainScreen();
                                        }
                                    });
                                }
                                btn.setEnabled(false);
                                mHeld = false;
                                mHandler.removeCallbacks(mRunnable);
                                break;
                        }
                        return false;
                    }
                });
                mContactFrags.add(button);
            }
        }
        else if (mContacts != null && mContacts.size() == 0) {
            selectContacts();
        }
        loadingScreen.setVisibility(View.INVISIBLE);
        if (loaded == LOADED) {
            saveInfo();
        }
        Log.d("write data to screen", "done writing data");
    }

    public void deleteContacts(View v) {
        for (ContactButton c : mContactFrags) {
            if (c.isChecked()) {
                mContacts.remove(c.getButtonContact());
            }
        }
        saveInfo();
        setMainScreen();
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

    public void doLaunchContactPicker(View view) {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                Contacts.CONTENT_URI);
        contactPickerIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        String DEBUG_TAG = "picker";
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CONTACT_PICKER_RESULT:
                    Cursor cursor = null;
                    try {
                        Uri result = data.getData();
                        Log.v(DEBUG_TAG, "Got a contact result: "
                                + result.toString());

                        // get the contact id from the Uri
                        String id = result.getLastPathSegment();
                        ContentResolver cr = this.getContentResolver();
                        // query for everything email
                        cursor = cr.query(ContactsContract.Contacts.CONTENT_URI,
                                null, ContactsContract.Contacts._ID + "=?",
                                new String[] { id },
                                null);

                        if (cursor.moveToFirst()) {
                            String contact = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
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
                            mContacts.add(c);
                            saveInfo();
                        } else {
                            Log.w(DEBUG_TAG, "No results");
                        }
                    } catch (Exception e) {
                        Log.e(DEBUG_TAG, "Failed to get contact data", e);
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                        setMainScreen();
                    }
                    break;
                case MESSAGER_RESULT:
                    getNewInfo();
                    writeDataToScreen();
                    break;
            }
        } else {
            Log.w(DEBUG_TAG, "Warning: activity result not ok");
        }
    }

    public interface ContactButton{
        public abstract void resetButton(ContactData c, int idx);
        public abstract Button getButton();
        public abstract void setContactTime();
        public abstract ContactData getButtonContact();
        public abstract int getIndex();
        public abstract int getLayout();
        public abstract void switchToDelete();
        public abstract boolean isChecked();
        public abstract Button getMsgButton();
    }

    public interface GetInput{
        public abstract int getNewContactFreq();
        public abstract Button getSubmitButton();
        public abstract Button getCancelButton();
    }

    public interface GetContactsFragment{
        public abstract void resetButton(ContactData c);
        public abstract boolean isChecked();
        public abstract ContactData getContact();
    }
}