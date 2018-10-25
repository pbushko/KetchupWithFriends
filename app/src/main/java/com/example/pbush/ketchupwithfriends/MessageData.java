package com.example.pbush.ketchupwithfriends;

/**
 * Created by pbush on 10/24/2018.
 */

public class MessageData implements Comparable<MessageData> {
    public int id; //the id of the message
    public String phoneNum; //to whom the message was sent
    public long timestamp; //sent time

    //nothing in the constructor; we will assign the items separately
    public MessageData()
    {
    }

    //compares the phone numbers
    public int compareTo(MessageData m)
    {
        return this.phoneNum.compareTo(m.phoneNum);
    }
}
