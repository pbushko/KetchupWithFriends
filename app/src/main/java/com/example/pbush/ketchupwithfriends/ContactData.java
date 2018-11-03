package com.example.pbush.ketchupwithfriends;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pbush on 10/25/2018.
 */

public class ContactData {
    public String id; //id number of the contact
    public String name; //name of the contact
    public List<String> phoneNum; //phone number(s) of the contact
    public List<MessageData> messages; //messages sent to the contact
    public long lastMessaged; //last day/time this contact was messaged
    public long nextMessageDeadline; //the next time this contact will need to be messaged to keep the streak
    public int daysPerDeadline; //the amount of days that can pass between messages before breaking the streak
    public int relationshipPoints; //the "points" assigned to this contact
    public int streak; //the streak of consecutive times the contact has been messaged daily

    public ContactData()
    {
        phoneNum = new ArrayList<String>();
        id = "";
        name = "";
        phoneNum.add("");
        messages = new ArrayList<MessageData>();
        lastMessaged = 0;
        daysPerDeadline = 0;
        nextMessageDeadline = 0;
        relationshipPoints = 0;
        streak = 0;
    }

    public ContactData(String num, MessageData m)
    {
        phoneNum = new ArrayList<String>();
        id = "";
        phoneNum.add(num);
        messages = new ArrayList<MessageData>();
        messages.add(m);
        lastMessaged = m.timestamp;
        daysPerDeadline = 0;
        nextMessageDeadline = 0;
        relationshipPoints = 0;
        streak = 0;
    }

    public void addMessage(MessageData m)
    {
        messages.add(m);
        //checking if the last messaged time needs updated
        //need to expand this to also update the next message deadline too
        if (lastMessaged < m.timestamp)
            lastMessaged = m.timestamp;
        return;
    }

    public String toString()
    {
        return "\nName: " + name +
                "\nPhone number: " + phoneNum.get(0) +
                "\nNum Messages: " + messages.size() +
                "\nRelationship Points: " + relationshipPoints + "\n";
    }
}
