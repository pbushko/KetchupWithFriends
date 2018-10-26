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
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import android.provider.ContactsContract.CommonDataKinds.Phone;

public class MainScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);
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

        //setting a button
        Button button = (Button) findViewById(R.id.Settings);
        button.setOnClickListener( new View.OnClickListener(){
            public void onClick (View v){
                // Code here executes on main thread after user presses button
                setContentView(R.layout.settings_screen);
            }
        });

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED)
        {
            DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
            Calendar calendar = Calendar.getInstance();
            List<MessageData> messages = getSentMessages();
            //for (MessageData message : messages)
            //{
                //Date d = new Date(message.timestamp);
                //Log.d("message dates", d.toString());
            //    Log.d("message dates", formatter.format(message.timestamp));
            //}
            String myText = "";

            //printing some of the messages
            for (int i = 0; i < 50; i++)
            {
                calendar.setTimeInMillis(messages.get(i).timestamp);
                /*Log.d("message dates",
                        formatter.format(calendar.getTime()) + " " +
                                messages.get(i).address);
                                */
                myText += "time: " + formatter.format(calendar.getTime()) + " number: " +
                        messages.get(i).phoneNum + "\n";
            }
            List<ContactData> contacts = getContacts();
            int count = 0;
            //sorting the messages by number and showing them
            for (MessageData message : messages)
            {
                //checking if the phone number already is in a contactData obj
                //this will be fixed later when we just get data from contacts
                boolean newNum = true;
                for (ContactData contact : contacts)
                {
                    if(contact.phoneNum.compareTo(message.phoneNum) == 0) {
                        newNum = false;
                        contact.messages.add(message);
                    }
                }
                if (newNum) {
                    contacts.add(new ContactData(message.phoneNum, message));
                }
                count++;
                if (count % 500 == 0)
                {
                    Log.d("tag", "" + count);
                }
            }
            Log.d("meow", "" + contacts.size());
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
            c.phoneNum = number;
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
}
