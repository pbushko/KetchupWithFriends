package com.example.pbush.ketchupwithfriends;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class MainScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
            Calendar calendar = Calendar.getInstance();
            List<MessageData> messages = getSentMessages();
            List<ContactData> contacts = getContacts();
            String myText = "";

            //printing some of the messages
            /*
            for (int i = 0; i < 50; i++)
            {
                calendar.setTimeInMillis(messages.get(i).timestamp);

                myText += "time: " + formatter.format(calendar.getTime()) + " number: " +
                        messages.get(i).phoneNum + "\n";
            }
            */

            int count = 0;
            //sorting the messages by number and showing them
            for (MessageData message : messages)
            {
                //checking if the phone number already is in a contactData obj
                //this will be fixed later when we just get data from contacts
                boolean newNum = true;
                for (ContactData contact : contacts)
                {
                    if(contact.phoneNum.get(0).compareTo(formatPhoneNum(message.phoneNum)) == 0) {
                        newNum = false;
                        contact.addMessage(message);
                    }
                }
                /* messages with no contact
                if (newNum) {
                    contacts.add(new ContactData(message.phoneNum, message));
                }
                */
                count++;
            }

            //just need this to get the set dates since that's where their
            //contact deadlines get set rn
            for (ContactData contact : contacts){
                contact.toString();
            }
            //sorts the contacts from most recently messaged to least recently
            Collections.sort(contacts, new Comparator<ContactData>() {
                @Override
                public int compare(ContactData c1, ContactData c2)
                {
                    if (c1.deadlineHere && !c2.deadlineHere)
                    {
                        return -1;
                    }
                    else if (!c1.deadlineHere && c2.deadlineHere)
                    {
                        return 1;
                    }
                    else if (c1.deadlineHere && c2.deadlineHere)
                    {
                        return c1.lastMessaged.compareTo(c2.lastMessaged);
                    }
                    else
                    {
                        return c2.lastMessaged.compareTo(c1.lastMessaged);
                    }
                }
            });

            //showing the contact data
            for (ContactData contact : contacts)
            {
                myText += contact.toString();
            }

            LinearLayout lView = (LinearLayout)findViewById(R.id.scrolllinearlayout);
            TextView toShow = new TextView(this);
            toShow.setText(myText);

            lView.addView(toShow);
        }
    }

    public String formatPhoneNum(String num)
    {
        //only get the last 9 digits if there are more than the normal amount
        if (num.length() > 10) {
            return num.substring(num.length() - 10, num.length());
        }
        else return num;
    }

    //github try1
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
        if(null == cursor || 0 == cursor.getCount())
        {
            return new ArrayList<MessageData>();
        }
        List<MessageData> messages = new ArrayList<MessageData>();
        try
        {
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
            {
                MessageData singleSms = new MessageData();
                singleSms.id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
                String temp = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                //need to remove any dashes from the phone number
                singleSms.phoneNum = formatPhoneNumber(temp);
                singleSms.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("date"));

                messages.add(singleSms);
            }

        }
        catch (Exception e)
        {
            Log.e("parse messages", e.getMessage());
        }

        cursor.close();

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
}
