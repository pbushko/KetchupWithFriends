package com.example.pbush.ketchupwithfriends;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pbush on 10/25/2018.
 */

public class ContactData {
    public String id;
    public String name;
    public String phoneNum;
    public List<MessageData> messages;
    public int relationshipPoints;

    public ContactData()
    {
        id = "";
        name = "";
        phoneNum = "";
        messages = new ArrayList<MessageData>();
        relationshipPoints = 0;
    }

    public ContactData(String num, MessageData m)
    {
        id = "";
        phoneNum = num;
        messages = new ArrayList<MessageData>();
        messages.add(m);
        relationshipPoints = 0;
    }

    public String toString()
    {
        return "\nName: " + name +
                "\nPhone number: " + phoneNum +
                "\nNum Messages: " + messages.size() +
                "\nRelationship Points: " + relationshipPoints + "\n";
    }
}
