package com.example.pbush.ketchupwithfriends;

import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by pbush on 10/25/2018.
 */

public class ContactData implements Comparable<ContactData> {
    public String id; //id number of the contact
    public String name; //name of the contact
    public List<String> phoneNum; //phone number(s) of the contact
    public List<MessageData> messages; //messages sent to the contact
    public Calendar nextMessageDeadline; //the next time this contact will need to be messaged to keep the streak
    public boolean deadlineHere;
    public int daysPerDeadline; //the amount of days that can pass between messages before breaking the streak
    public int relationshipPoints; //the "points" assigned to this contact
    public int streak; //the streak of consecutive times the contact has been messaged daily
    public Calendar lastMessaged;

    public ContactData()
    {
        phoneNum = new ArrayList<String>();
        id = "";
        name = "";
        messages = new ArrayList<MessageData>();
        lastMessaged = Calendar.getInstance();
        lastMessaged.setTimeInMillis(0);
        daysPerDeadline = 0;
        nextMessageDeadline = Calendar.getInstance();;
        nextMessageDeadline.setTimeInMillis(0);
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
        lastMessaged = Calendar.getInstance();
        lastMessaged.setTimeInMillis(0);
        daysPerDeadline = 0;
        nextMessageDeadline = Calendar.getInstance();;
        nextMessageDeadline.setTimeInMillis(0);
        deadlineHere = false;
        relationshipPoints = 0;
        streak = 0;
    }

    public void addMessage(MessageData m)
    {
        messages.add(m);
        //checking if the last messaged time needs updated
        //need to expand this to also update the next message deadline too
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(m.timestamp);
        if (lastMessaged.before(cal))
            lastMessaged = cal;
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

    public void setContactFrequency(int days)
    {
        daysPerDeadline = days;
        nextMessageDeadline = (Calendar) lastMessaged.clone();
        nextMessageDeadline.add(Calendar.DATE, days);
        if (nextMessageDeadline.before(Calendar.getInstance()))
            deadlineHere = true;
    }

    //to update the deadline when a new-er message is inputted
    public void updateContactDeadline(long time)
    {
        nextMessageDeadline.setTimeInMillis(time);
        nextMessageDeadline.add(Calendar.DATE, daysPerDeadline);
    }

    public String toString()
    {
        if (messages.size() != 0) {
            DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
            setContactFrequency(5);
            return "\nName: " + name +
                    "\nPhone number: " + phoneNum.get(0) +
                    "\nNum Messages: " + messages.size() +
                    "\nRelationship Points: " + relationshipPoints +
                    "\nLast Messaged:" + formatter.format(lastMessaged.getTime()) +
                    "\nNext Deadline:" + formatter.format(nextMessageDeadline.getTime()) +
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

    @Override
    public int compareTo(ContactData c)
    {
        return name.compareTo(c.name);
    }
}
