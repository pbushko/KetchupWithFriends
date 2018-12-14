package com.example.pbush.ketchupwithfriends;

import android.util.Log;
import android.graphics.Bitmap;
import android.net.Uri;
import android.content.ContentUris;
import android.database.Cursor;
import android.provider.ContactsContract.Contacts;
import android.content.Context;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.ByteArrayInputStream;


/**
 * Created by pbush on 10/25/2018.
 */

public class ContactData implements Comparable<ContactData> {
    final public static int MS_PER_HOUR = 3600000;

    public String id; //id number of the contact
    public String name; //name of the contact
    public List<String> phoneNum; //phone number(s) of the contact
    public List<MessageData> messages; //messages sent to the contact
    public long nextMessageDeadline; //the next time this contact will need to be messaged to keep the streak
    public boolean deadlineHere;
    public int daysPerDeadline; //the amount of days that can pass between messages before breaking the streak
    public int relationshipPoints; //the "points" assigned to this contact
    public int streak; //the streak of consecutive times the contact has been messaged daily
    public long lastMessaged;
    /* profile picture */
    public ByteArrayInputStream pic;


    public ContactData()
    {
        phoneNum = new ArrayList<String>();
        id = "";
        name = "";
        messages = new ArrayList<MessageData>();
        lastMessaged = 0;
        daysPerDeadline = 0;
        nextMessageDeadline = 0;
        deadlineHere = false;
        relationshipPoints = 0;
        streak = 0;
    }

    public ContactData(String num, MessageData m)
    {
        phoneNum = new ArrayList<String>();
        id = "";
        addPhoneNumber(num);
        messages = new ArrayList<MessageData>();
        messages.add(m);
        lastMessaged = 0;
        daysPerDeadline = 0;
        nextMessageDeadline = 0;
        deadlineHere = false;
        relationshipPoints = 0;
        streak = 0;
    }

    public void addMessage(MessageData m)
    {
        messages.add(m);
        //checking if the last messaged time needs updated
        //need to expand this to also update the next message deadline too
        if (lastMessaged < m.timestamp) {
            lastMessaged = m.timestamp;
            setContactFrequency(daysPerDeadline);
        }
        return;
    }

    public void addPhoneNumber(String num)
    {
        //only get the last 9 digits if there are more than the normal amount
        if (num.length() > 10) {
            phoneNum.add(num.substring(num.length() - 10, num.length()));
        }
        else phoneNum.add(num);
        return;
    }

    public void setContactFrequency(int hours)
    {
        daysPerDeadline = hours;
        nextMessageDeadline = lastMessaged + (hours * MS_PER_HOUR);
        checkIfDeadlineHere();
        Log.d("contact data", "set contact frequency for " + name + "!");
    }

    public void checkIfDeadlineHere()
    {
        if (Calendar.getInstance().getTimeInMillis() > (nextMessageDeadline))
            deadlineHere = true;
        else
            deadlineHere = false;
        return;
    }

    /*public void messageDeadline(MessageData message)
    {
        if (message.timestamp > (nextMessageDeadline))
            return true;
        else
            return false;
        return;
    }*/

    /* adopted this code from a post on Stackoverflow */
    /* provide Context to get thumbnail profile pic and save under pic field */
    public void updatePicture(Context context)
    {
        long number = Long.parseLong(phoneNum.get(0));
        Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, number);
        Uri photoUri = Uri.withAppendedPath(contactUri, Contacts.Photo.CONTENT_DIRECTORY);
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] {Contacts.Photo.PHOTO}, null, null, null);
        if (cursor == null) {
            return;
        }
        try {
            if (cursor.moveToFirst()) {
                byte[] data = cursor.getBlob(0);
                if (data != null) {
                    pic = new ByteArrayInputStream(data);
                    return;
                }
            }
        } finally {
            cursor.close();
        }
    }

    public String toString()
    {
        if (messages.size() != 0) {
            DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
            if(nextMessageDeadline == 0)
                setContactFrequency(5);
            return "\nName: " + name +
                    "\nPhone number: " + phoneNum.get(0) +
                    "\nNum Messages: " + messages.size() +
                    "\nRelationship Points: " + relationshipPoints +
                    "\nLast Messaged:" + formatter.format(lastMessaged) +
                    "\nNext Deadline:" + formatter.format(nextMessageDeadline) +
                    "\nDays per Deadline:" + daysPerDeadline +
                    "\nDeadline?:" + deadlineHere +
                    "\n";
        }
        else {
            return "\nName: " + name +
                    "\nPhone number: " + phoneNum.get(0) +
                    "\nNum Messages: " + messages.size() +
                    "\nRelationship Points: " + relationshipPoints +
                    "\nLast Messaged: Never" + "\n";
        }
    }

    public int numMessages()
    {
        return messages.size();
    }

    @Override
    public int compareTo(ContactData c)
    {
        return name.compareTo(c.name);
    }
}
