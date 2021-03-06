package com.catgames.pbush.ketchupwithfriends;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.ContactsContract.Contacts;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.ValueDependentColor;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;

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
    private boolean mFirstLoading;

    private List<ContactButton> mContactFrags;
    private long timeToNextDeadline;

    private String firebaseRegistrationToken;

    //firebase items
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mDatabaseUserInfo;
    public long lastDataScrape;
    private ImageView loadingScreen;
    // Add this to firebase as well
    public AchievementData achieve;
    private ImageView achievementTomato;
    private List<GetContactsFragment> selectedContacts;
    private Button mContactButton;
    private Button mDeleteContactsButton;
    private Button mGetSingleContact;

    private LinearLayout mForBulkContacts;
    private CheckBox mCheckAll;
    private TabWidget mTabs;

    private GraphView mContactGraph;

    private String userId;

    private Handler mHandler;
    private Runnable mRunnable;

    //the messages and contacts
    private List<MessageData> mMessages;
    private List<ContactData> mContacts;

    public void setMainScreen()
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

        mTabs = findViewById(android.R.id.tabs);

        Button b = findViewById(R.id.get_contacts);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                doLaunchContactPicker(view);
            }
        }
        );

        mForBulkContacts = findViewById(R.id.for_bulk_contacts);
        mContactButton = findViewById(R.id.get_multiple_contacts);
        mContactButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                getMultipleContacts();
            }}
        );
        mCheckAll = findViewById(R.id.check_all);
        mCheckAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                selectAllContacts();
            }}
        );
        mTabs.setVisibility(View.VISIBLE);
        mForBulkContacts.setVisibility(View.INVISIBLE);

        mDeleteContactsButton = findViewById(R.id.delete_contacts_button);
        mDeleteContactsButton.setVisibility(View.INVISIBLE);

        mGetSingleContact = findViewById(R.id.get_contacts);
        mGetSingleContact.setVisibility(View.INVISIBLE);

        //getting the achievement tomato to change
        if (achieve != null){
            //getting the achievement tomato to change
            achieve.update();
            achievementTomato = findViewById(R.id.message_tomato);
            achievementTomato.setImageResource(achieve.getMessageRipeningStage());
        }

        selectedContacts = new ArrayList<>();

        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                //checking if a button has been held for long enough
                long time2 = Calendar.getInstance().getTimeInMillis();
                if (time2 - mTimer > 1000) {
                    mHeld = true;
                    mDeleteContactsButton.setVisibility(View.VISIBLE);
                    mGetSingleContact.setVisibility(View.INVISIBLE);
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
        achieve.checkday(now);
        Log.d("message data", "last data scrape: " + lastDataScrape + " Now: " + now);
        if(null == cursor || 0 == cursor.getCount())
        {
            return new ArrayList<MessageData>();
        }
        List<MessageData> messages = new ArrayList<MessageData>();
        try
        {
            long lastTimeStamp = now + 1;
            //only getting the messages that weren't scraped since last time

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
                    //only getting the new messages
                    if (lastTimeStamp > lastDataScrape) {
                        messages.add(singleSms);
                        numNewMessages++;
                    }
                }
                else {
                    cursor.moveToLast();
                }
            }
        }
        catch (Exception e)
        {
            Log.e("parse messages", e.getMessage());
        }

        cursor.close();
        //here in case no messages were found; this is needed to be an offset for the lag
        //between checking and sending messages
        if (messages.size() != 0) {
            lastDataScrape = now;
        }
        return messages;
    }

    public List<ContactData> getContacts()
    {
        List<ContactData> contacts = new ArrayList<ContactData>();
        if (ContextCompat.checkSelfPermission(this,

                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    1);
            return getContacts();
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS},
                    2);
            return getContacts();
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {

            ContentResolver cr = this.getContentResolver();
            Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                int nameFieldColumnIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                String name = cursor.getString(nameFieldColumnIndex);
                String number = "";
                //getting the phone number for everyone
                if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor phoneCursor = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
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
        }

        return contacts;
    }

    private String formatPhoneNumber(String number)
    {
        return number.replace("-", "").replace("+", "")
                .replace(" ", "").replace(")", "")
                .replace("(", "").replace("\"", "");
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
            FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                @Override
                public void onComplete(@NonNull Task<InstanceIdResult> task) {
                    if (!task.isSuccessful()) {
                        Log.w("Firebase", "getInstanceId failed", task.getException());
                        return;
                    }

                    // Get new Instance ID token
                    firebaseRegistrationToken = task.getResult().getToken();

                }
            });
            getSavedInfo();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    public void saveInfo()
    {
        mDatabaseUserInfo = mDatabase.getReference(userId);
        saving = SAVING;
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
        mDatabaseUserInfo.child("contacts").setValue(mContacts);
        mDatabaseUserInfo.child("achievements").setValue(achieve);
        mDatabaseUserInfo.child("lastDataScrape").setValue(lastDataScrape);
        mDatabaseUserInfo.child("firebaseToken").setValue(firebaseRegistrationToken);
        long timeToNextDeadline = Long.MAX_VALUE;
        for (ContactData c : mContacts) {
            if (timeToNextDeadline > c.nextMessageDeadline) {
                timeToNextDeadline = c.nextMessageDeadline;
                Log.d("contact", ":" + c.toString());
            }
        }
        long timeLeft = timeToNextDeadline - Calendar.getInstance().getTimeInMillis();
        String msg = "";
        if (timeLeft < 50) {
            msg = "yes";
        }
        else {
            msg = "no";
        }
        Log.d("timeLeft", "" + timeLeft);
        //trying to get analytics for sending notifications
        FirebaseAnalytics.getInstance(this)
                .setUserProperty("timeToNextDeadline",
                        Long.toString(timeLeft));
        FirebaseAnalytics.getInstance(this)
                .setUserProperty("needsToBeMessaged", msg);
    }

    public void getSavedInfo()
    {
        int achievementsLoaded = LOADING;
        loaded = LOADING;
        mDatabaseUserInfo = mDatabase.getReference(userId);
        //Toast.makeText(this, "Getting save info", Toast.LENGTH_SHORT).show();
        mDatabaseUserInfo.child("contacts").orderByValue();
        mDatabaseUserInfo.child("contacts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (saving == DONE_SAVING) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    mContacts = new ArrayList<ContactData>();
                    for (DataSnapshot snap : dataSnapshot.getChildren()) {
                        mContacts.add(snap.getValue(ContactData.class));
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

                try {
                    lastDataScrape = dataSnapshot.getValue(Long.class);
                    Log.d("sign in", "outside the scrape function");
                }
                catch (Exception e) {
                    lastDataScrape = 0;
                }
                if(loaded == LOADED) {
                    if (mContacts.size() == 0)
                    {
                        selectContacts();
                    }
                    else {
                        //getNewInfo();
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
        mDatabaseUserInfo.child("lastDataScrape").orderByValue();

        mDatabaseUserInfo.child("achievements").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                achieve = new AchievementData();
                AchievementData achieveTemp = dataSnapshot.getValue(AchievementData.class);
                if (achieveTemp != null) {
                    achieve = achieveTemp;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        mDatabaseUserInfo.child("achievements").orderByValue();

    }

    public void getNewInfo() {
        //getting permission from the user to access message and contact data
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    1);
            getNewInfo();
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS},
                    2);
            getNewInfo();
        }
        //read in messages if we have access to it
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                        == PackageManager.PERMISSION_GRANTED)
        {

            mMessages = getSentMessages();

            Log.d("msg", "mMessages" + mMessages.size());

            //sorting the messages by number and showing them
            for (int i = mMessages.size() - 1; i >= 0; i--)
            {
                MessageData message = mMessages.get(i);
                //checking if the phone number already is in a contactData obj
                //this will be fixed later when we just get data from contacts
                for (ContactData contact : mContacts)
                {
                    if(contact.phoneNum.get(0).compareTo(formatPhoneNum(message.phoneNum)) == 0) {
                        Log.d("msg", "added!");
                        contact.addMessage(message);
                    }
                }
            }

            //just need this to get the set dates since that's where their
            //contact deadlines get set rn
            for (ContactData contact : mContacts){
                contact.toString();
            }
            sortContacts();
        }
        saveInfo();
    }

    public void selectContacts() {
        List<ContactData> c = getContacts();
        selectedContacts = new ArrayList<>();

        mTabs.setVisibility(View.INVISIBLE);
        mForBulkContacts.setVisibility(View.VISIBLE);
        mGetSingleContact.setVisibility(View.INVISIBLE);

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

    //when bulk selecting contacts, either enable all the check boxes or disable them
    public void selectAllContacts() {
        boolean enable = false;
        //checking to see if we will enable or disable them
        for (int i = 0; i < selectedContacts.size(); i++) {
            //we will enable them all if there are any unchecked boxes
            if (!selectedContacts.get(i).isChecked()) {
                enable = true;
            }
        }
        for (GetContactsFragment c : selectedContacts) {
            c.check(enable);
        }
    }

    public void getMultipleContacts() {
        mContacts = new ArrayList<ContactData>();
        for (GetContactsFragment f : selectedContacts) {
            if (f.isChecked()) {
                mContacts.add(f.getContact());
            }
        }
        mTabs.setVisibility(View.VISIBLE);
        mForBulkContacts.setVisibility(View.INVISIBLE);
        mGetSingleContact.setVisibility(View.INVISIBLE);
        lastDataScrape = 0;
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
                    contact.addOldMessage(message);
                }
            }
        }
        saveInfo();
        setMainScreen();
    }

    public void sendMessage(String num) {
        Intent sendIntent = new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", num, null));
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(sendIntent, MESSAGER_RESULT);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void writeDataToScreen(){
        mGetSingleContact.setVisibility(View.VISIBLE);
        //showing the contact data
        if (mContacts != null && mContacts.size() > 0) {
            mContactFrags = new ArrayList<>();
            int buttonIndex = 0;
            for (final ContactData contact : mContacts) {
                //make a button that will let you set the time for them
                FragmentManager fragmentManager = getFragmentManager();
                final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                Fragment c = new ContactButtonFragment();
                final ContactButton button = (ContactButton)c;
                fragmentTransaction.add(R.id.scrolllinearlayout, c, "HELLO");
                fragmentTransaction.commitNow();
                //setting the info in the button
                button.resetButton(contact, buttonIndex);
                //getting the actual button of the fragment
                final Button btn = button.getButton();
                final int index = buttonIndex;

                //on click, we want it to take us to the input time screen
                btn.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch(event.getAction()) {
                            case MotionEvent.ACTION_SCROLL:
                                btn.setEnabled(false);
                                mHandler.removeCallbacks(mRunnable);
                                mHeld = false;
                                break;
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
                                    setContactScreen(index, "Week");
                                }
                                btn.setEnabled(false);
                                mHandler.removeCallbacks(mRunnable);
                                mHeld = false;
                                break;
                            default:
                                btn.setEnabled(false);
                                mHandler.removeCallbacks(mRunnable);
                                mHeld = false;
                                break;
                        }
                        return true;
                    }
                });

                Button msgButton = button.getMsgButton();
                msgButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (ContextCompat.checkSelfPermission(MainScreen.this,
                                Manifest.permission.SEND_SMS)
                                != PackageManager.PERMISSION_GRANTED) {
                            if (ActivityCompat.shouldShowRequestPermissionRationale(MainScreen.this,
                                    Manifest.permission.SEND_SMS)) {

                            } else {
                                ActivityCompat.requestPermissions(MainScreen.this,
                                        new String[]{Manifest.permission.SEND_SMS},
                                        3);
                            }
                        }
                        sendMessage(button.getButtonContact().phoneNum.get(0));
                    }
                });

                //this enables the in-app messaging feature
                final Button directMsgButton = button.getDirectMsgButton();
                directMsgButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (ContextCompat.checkSelfPermission(MainScreen.this,
                                Manifest.permission.SEND_SMS)
                                != PackageManager.PERMISSION_GRANTED) {
                            if (ActivityCompat.shouldShowRequestPermissionRationale(MainScreen.this,
                                    Manifest.permission.SEND_SMS)) {
                                ActivityCompat.requestPermissions(MainScreen.this,
                                        new String[]{Manifest.permission.SEND_SMS},
                                        3);
                            } else {
                                ActivityCompat.requestPermissions(MainScreen.this,
                                        new String[]{Manifest.permission.SEND_SMS},
                                        3);
                            }
                        }
                        FragmentManager fragmentManager = getFragmentManager();
                        final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        Fragment m = new MessagingFragment();
                        final MessageFragment msg = (MessageFragment)m;
                        fragmentTransaction.add(button.getLayout(), m, "HELLO");
                        msg.resetData(contact);
                        fragmentTransaction.commitNow();
                        directMsgButton.setEnabled(false);
                    }
                });
                mContactFrags.add(button);
                buttonIndex++;
            }
        }
        else if (mContacts != null && mContacts.size() == 0) {
            selectContacts();
        }
        loadingScreen.setVisibility(View.INVISIBLE);
        if (loaded == LOADED) {
            Log.d("saving","saved!");
            saveInfo();
        }
    }

    @Override
    public void onBackPressed() {
        setMainScreen();
    }

    public void setContactScreen(final int idx, final String graphSpinnerString) {
        mFirstLoading = true;
        ContactData c = mContacts.get(idx);
        setContentView(R.layout.contact_data_screen);
        //setting the custom contact image
        ImageView image = findViewById(R.id.contact_image);
        Uri pic = c.getPhotoUri(MainScreen.this);
        if (pic != null) {
            image.setImageURI(pic);
        }
        TextView name = (TextView) findViewById(R.id.contact_name);
        TextView totMessages = (TextView) findViewById(R.id.total_num_messages);
        name.setText(c.name);
        totMessages.setText(c.numMessages() + " total messages sent since you started using this app");
        final Spinner graphSpinner = (Spinner) findViewById(R.id.graph_sorting);
        mContactGraph = findViewById(R.id.graph);
        ArrayList<DataPoint> dps = c.getGraphPoints();
        graphSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //this gets called on initialization as well
                if (!mFirstLoading) {
                    setContactScreen(idx, graphSpinner.getSelectedItem().toString());
                }
                else {
                    mFirstLoading = false;
                }
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                return;
            }
        });

        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();
        Date last = cal.getTime();

        mContactGraph.getGridLabelRenderer().setVerticalAxisTitle("# Sent Messages");
        switch (graphSpinnerString) {

            case ("Week") :
                graphSpinner.setSelection(0);
                mContactGraph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this));
                mContactGraph.getGridLabelRenderer().setNumHorizontalLabels(9);
                try {
                    String now = fmt.format(new Date(cal.getTimeInMillis()));
                    today = fmt.parse(now);
                    //formatting for week
                    cal.add(Calendar.DAY_OF_YEAR, -7);
                    last = fmt.parse(fmt.format(new Date(cal.getTimeInMillis())));
                } catch (ParseException e) {

                }
                BarGraphSeries<DataPoint> s = c.getWeekBarGraphPoints(last);
                s.setSpacing(30);
                // styling
                s.setValueDependentColor(new ValueDependentColor<DataPoint>() {
                    @Override
                    public int get(DataPoint data) {
                        return Color.rgb((int) data.getX() * 255 / 4, (int) Math.abs(data.getY() * 255 / 6), 100);
                    }
                });
                s.setDrawValuesOnTop(true);
                s.setValuesOnTopColor(Color.RED);
                mContactGraph.addSeries(s);
                mContactGraph.getGridLabelRenderer().setHumanRounding(false, true);
                mContactGraph.getViewport().setMinX(last.getTime());
                mContactGraph.getViewport().setMinY(0);
                mContactGraph.getViewport().setMaxX(today.getTime() + 86400000);
                mContactGraph.getViewport().setXAxisBoundsManual(true);
                mContactGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                    @Override
                    public String formatLabel(double value, boolean isValueX) {
                        if (isValueX) {
                            // show new x depending on the day
                            Calendar calendar = Calendar.getInstance();
                            Calendar cal = Calendar.getInstance();
                            Date last = cal.getTime();
                            SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
                            try {
                                //formatting for week
                                cal.add(Calendar.DAY_OF_YEAR, -6);
                                last = fmt.parse(fmt.format(new Date(cal.getTimeInMillis())));
                            } catch (ParseException e) {

                            }
                            calendar.setTime(new Date((long) value));
                            String dow = "";
                            if (calendar.getTimeInMillis() < last.getTime())
                            {
                                return "";
                            }
                            switch (calendar.get(Calendar.DAY_OF_WEEK)) {
                                case (Calendar.SUNDAY):
                                    dow = "Su";
                                    break;
                                case (Calendar.MONDAY):
                                    dow = "M";
                                    break;
                                case (Calendar.TUESDAY):
                                    dow = "T";
                                    break;
                                case (Calendar.WEDNESDAY):
                                    dow = "W";
                                    break;
                                case (Calendar.THURSDAY):
                                    dow = "Th";
                                    break;
                                case (Calendar.FRIDAY):
                                    dow = "F";
                                    break;
                                case (Calendar.SATURDAY):
                                    dow = "Sa";
                                    break;
                                default:
                                    dow = "";
                                    break;
                            }
                            return dow;
                        } else {
                            // show currency for y values
                            return super.formatLabel(value, isValueX);
                        }
                    }
                });
                mContactGraph.getGridLabelRenderer().setHorizontalAxisTitle("Day");
                break;
            case("Month"):
                graphSpinner.setSelection(1);
                mContactGraph.getGridLabelRenderer().setHorizontalAxisTitle("Week");
                mContactGraph.getGridLabelRenderer().setNumHorizontalLabels(7);
                try {
                    String now = fmt.format(new Date(cal.getTimeInMillis()));
                    //getting the beginning of the month
                    cal.add(Calendar.DAY_OF_MONTH, -Integer.parseInt(now.substring(6)));
                    last = fmt.parse(fmt.format(new Date(cal.getTimeInMillis())));
                } catch (ParseException e) {
                }

                s = c.getMonthBarGraphPoints(last);
                //grouping the
                s.setSpacing(50);
                // styling
                s.setValueDependentColor(new ValueDependentColor<DataPoint>() {
                    @Override
                    public int get(DataPoint data) {
                        return Color.rgb((int) data.getX() * 255 / 4, (int) Math.abs(data.getY() * 255 / 6), 100);
                    }
                });
                s.setDrawValuesOnTop(true);
                s.setValuesOnTopColor(Color.RED);

                mContactGraph.addSeries(s);
                mContactGraph.getGridLabelRenderer().setHumanRounding(false, true);
                mContactGraph.getViewport().setMinX(0);
                mContactGraph.getViewport().setMinY(0);
                mContactGraph.getViewport().setMaxX(6);
                mContactGraph.getViewport().setXAxisBoundsManual(true);
                mContactGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                    @Override
                    public String formatLabel(double value, boolean isValueX) {
                        if (isValueX) {
                            // show new x depending on the month
                            switch ((int)Math.round(value)) {
                                case (1):
                                    return "1";
                                case (2):
                                    return "2";
                                case (3):
                                    return "3";
                                case (4):
                                    return "4";
                                case (5):
                                    return "5";
                                default:
                                    return "";
                            }
                        } else {
                            // show currency for y values
                            return super.formatLabel(value, isValueX);
                        }
                    }
                });
                break;
            //default is the year
            default:
                graphSpinner.setSelection(2);
                mContactGraph.getGridLabelRenderer().setHorizontalAxisTitle("Month");
                mContactGraph.getGridLabelRenderer().setNumHorizontalLabels(14);
                try {
                    //getting the end of the year
                    cal.add(Calendar.DAY_OF_YEAR, -364);
                    last = fmt.parse(fmt.format(new Date(cal.getTimeInMillis())));
                    Log.d("newdate", "" + last);
                } catch (ParseException e) {
                }

                s = c.getYearBarGraphPoints(last);
                //grouping the
                s.setSpacing(20);
                // styling
                s.setValueDependentColor(new ValueDependentColor<DataPoint>() {
                    @Override
                    public int get(DataPoint data) {
                        return Color.rgb((int) data.getX() * 255 / 4, (int) Math.abs(data.getY() * 255 / 6), 100);
                    }
                });
                s.setDrawValuesOnTop(true);
                s.setValuesOnTopColor(Color.RED);

                mContactGraph.addSeries(s);
                mContactGraph.getGridLabelRenderer().setHumanRounding(false, true);
                mContactGraph.getViewport().setMinX(0);
                mContactGraph.getViewport().setMinY(0);
                mContactGraph.getViewport().setMaxX(13);
                mContactGraph.getViewport().setXAxisBoundsManual(true);
                mContactGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                    @Override
                    public String formatLabel(double value, boolean isValueX) {
                        if (isValueX) {
                            // show new x depending on the month
                            switch ((int)Math.round(value)) {
                                case (1):
                                case (6):
                                case (7):
                                    return "J";
                                case (2):
                                    return "F";
                                case (3):
                                case (5):
                                    return "M";
                                case (4):
                                case (8):
                                    return "A";
                                case (9):
                                    return "S";
                                case (10):
                                    return "O";
                                case (11):
                                    return "N";
                                case (12):
                                    return "D";
                                default:
                                    return "";
                            }
                        } else {
                            // show currency for y values
                            return super.formatLabel(value, isValueX);
                        }
                    }
                });
                break;
        }

        final Spinner spinner = (Spinner)findViewById(R.id.time_option_spinner);
        final EditText num = (EditText) findViewById(R.id.user_num_input);
        num.setTransformationMethod(null);
        // Create an ArrayAdapter using the string array and a default spinner
        ArrayAdapter<CharSequence> staticAdapter = ArrayAdapter
                .createFromResource(this, R.array.time_options_array,
                        android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        staticAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(staticAdapter);

        Button submit = findViewById(R.id.ok_button);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String res = mContacts.get(idx).setContactFrequency(num.getText().toString() + " " + spinner.getSelectedItem().toString());
                if (res == "") {
                    Toast.makeText(MainScreen.this, "Deadline updated successfully!", Toast.LENGTH_SHORT).show();
                    saveInfo();
                }
                else {
                    Toast.makeText(MainScreen.this, res, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void deleteContacts(View v) {
        for (ContactButton c : mContactFrags) {
            if (c.isChecked()) {
                mContacts.remove(c.getButtonContact());
            }
        }
        mHandler.removeCallbacks(mRunnable);
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

                            //populating the messages from the past with this contact
                            lastDataScrape = 0;
                            mMessages = getSentMessages();
                            //sorting the messages by number and showing them
                            for (int i = mMessages.size() - 1; i >= 0; i--)
                            {
                                MessageData message = mMessages.get(i);
                                if(c.phoneNum.get(0).compareTo(formatPhoneNum(message.phoneNum)) == 0) {
                                    c.addOldMessage(message);
                                }
                            }
                            saveInfo();
                            setMainScreen();

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
                    Log.d("new message", "new message!");
                    getNewInfo();
                    setMainScreen();
                    break;
            }
        } else {
            if (requestCode == MESSAGER_RESULT) {
                Log.d("new message", "new message!");
                getNewInfo();
                setMainScreen();
            }
            Log.w(DEBUG_TAG, "Warning: activity result not ok");
        }
    }

    public interface ContactButton{
        public abstract void resetButton(ContactData c, int idx);
        public abstract Button getButton();
        public abstract void setContactTime();
        public abstract ContactData getButtonContact();
        public abstract int getIndex();
        public abstract void resetProgressBar();
        public abstract int getLayout();
        public abstract void switchToDelete();
        public abstract boolean isChecked();
        public abstract Button getMsgButton();
        public abstract Button getDirectMsgButton();
    }

    public interface GetInput{
        public abstract int getNewContactFreq();
        public abstract Button getSubmitButton();
    }

    public interface MessageFragment {
        public abstract void resetData(ContactData c);
    }

    public interface GetContactsFragment{
        public abstract void resetButton(ContactData c);
        public abstract boolean isChecked();
        public abstract void check(boolean b);
        public abstract ContactData getContact();
    }
}