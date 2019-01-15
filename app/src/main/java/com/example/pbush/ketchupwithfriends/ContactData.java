package com.example.pbush.ketchupwithfriends;

import android.content.Intent;
import android.icu.text.SymbolTable;
import android.provider.ContactsContract;
import android.util.Log;
import android.graphics.Bitmap;
import android.net.Uri;
import android.content.ContentUris;
import android.database.Cursor;
import android.provider.ContactsContract.Contacts;
import android.content.Context;


import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.ByteArrayInputStream;
import java.util.Map;


/**
 * Created by pbush on 10/25/2018.
 */

public class ContactData implements Comparable<ContactData> {
    final public static int MS_PER_HOUR = 3600000;

    public String id; //id number of the contact
    public String name; //name of the contact
    public List<String> phoneNum; //phone number(s) of the contact
    //public List<MessageData> messages; //messages sent to the contact
    public long nextMessageDeadline; //the next time this contact will need to be messaged to keep the streak
    public boolean deadlineHere;
    public String daysPerDeadline; //the amount of days that can pass between messages before breaking the streak
    public int relationshipPoints; //the "points" assigned to this contact
    public int streak; //the streak of consecutive times the contact has been messaged daily
    public long lastMessaged;
    public int totalMessages;

    //concurrent lists to keep track of dates
    public List<String> messageDates;
    public List<Integer> messageNums;

    public ContactData()
    {
        phoneNum = new ArrayList<String>();
        id = "";
        name = "";
        totalMessages = 0;
        //messages = new ArrayList<MessageData>();
        lastMessaged = 0;
        daysPerDeadline = "1 Week";
        nextMessageDeadline = 0;
        deadlineHere = false;
        relationshipPoints = 0;
        streak = 0;
        messageDates = new ArrayList<String>();
        messageNums = new ArrayList<Integer>();
    }

    public ContactData(String num, MessageData m)
    {
        phoneNum = new ArrayList<String>();
        id = "";
        addPhoneNumber(num);
        totalMessages = 0;
        //messages = new ArrayList<MessageData>();
        //messages.add(m);
        lastMessaged = 0;
        daysPerDeadline = "1 Week";
        nextMessageDeadline = 0;
        deadlineHere = false;
        relationshipPoints = 0;
        streak = 0;
        messageDates = new ArrayList<String>();
        messageNums = new ArrayList<Integer>();
    }

    public void addOldMessage(MessageData m) {
         addToList(new Date(m.timestamp));
    }

    public void addToList(Date d) {
        //checking each data point we have
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        String f = fmt.format(d);
        int index = messageDates.indexOf(f);
        //if there is a key that has the same date, increment the counter for that date
        if (index != -1) {
            messageNums.set(index, messageNums.get(index)+1);
        }
        else {
            messageDates.add(f);
            messageNums.add(1);
        }
    }

    public ArrayList<DataPoint> getGraphPoints() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        ArrayList<DataPoint> d = new ArrayList<DataPoint>();
        for (int i = 0; i < messageDates.size(); i++) {
            try {
                //Log.d("getGraphPoints", "" + messageDates.get(i)+ " : " + messageNums.get(i));
                d.add(new DataPoint(fmt.parse(messageDates.get(i)), messageNums.get(i)));
            }
            catch (ParseException e) {
                Log.d("getGraphPoints", "error: " + e);
            }
        }
        return d;
    }

    public BarGraphSeries<DataPoint> getMonthBarGraphPoints(Date firstDay) {
        int sz = messageDates.size();
        Calendar cal = Calendar.getInstance();
        int[] counts = new int[6];
        if (sz != 0) {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
            try {
                int dateIndex = sz - 1;
                Date d = fmt.parse(messageDates.get(dateIndex));
                while (dateIndex > 0 && d.before(new Date(Calendar.getInstance().getTimeInMillis())) && d.after(firstDay)) {
                    cal.setTime(d);
                    int idx = cal.get(Calendar.WEEK_OF_MONTH) - 1;
                    Log.d("monthpts", "" + idx);
                    counts[idx] += messageNums.get(dateIndex);
                    dateIndex--;
                    d = fmt.parse(messageDates.get(dateIndex));
                }
                DataPoint[] dps = new DataPoint[6];
                for (int i = 0; i < 6; i++) {
                    dps[i] = new DataPoint(i+1, counts[i]);
                }
                //after accumulating all the indicies
                return new BarGraphSeries<>(dps);
            }
            catch (ParseException e) {
                //return an empty graph otherwise
                return new BarGraphSeries<>(new DataPoint[]{});
            }
        }
        else {
            //return an empty graph otherwise
            return new BarGraphSeries<>(new DataPoint[]{});
        }
    }

    public BarGraphSeries<DataPoint> getYearBarGraphPoints(Date firstDay) {
        int sz = messageDates.size();
        int[] counts = new int[13];
        if (sz != 0) {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
            try {
                int dateIndex = sz - 1;
                Date d = fmt.parse(messageDates.get(dateIndex));
                Log.d("year", "" + d.before(new Date(Calendar.getInstance().getTimeInMillis())));
                while (dateIndex > 0 && d.before(new Date(Calendar.getInstance().getTimeInMillis())) && d.after(firstDay)) {
                    int idx = Integer.parseInt(messageDates.get(dateIndex).substring(4, 6));
                    Log.d("month of year", "total:" + messageDates.get(dateIndex) + " idx:" + idx);
                    counts[idx] += messageNums.get(dateIndex);
                    dateIndex--;
                    d = fmt.parse(messageDates.get(dateIndex));
                }
                DataPoint[] dps = new DataPoint[12];
                for (int i = 1; i < 13; i++) {
                    dps[i-1] = new DataPoint(i, counts[i]);
                }
                //after accumulating all the indicies
                return new BarGraphSeries<>(dps);
            }
            catch (ParseException e) {
                //return an empty graph otherwise
                return new BarGraphSeries<>(new DataPoint[]{});
            }
        }
        else {
            //return an empty graph otherwise
            return new BarGraphSeries<>(new DataPoint[]{});
        }
    }

    public BarGraphSeries<DataPoint> getBarGraphPoints() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        DataPoint[] d = new DataPoint[messageDates.size()];
        for (int i = 0; i < messageDates.size(); i++) {
            try {
                //Log.d("getGraphPoints", "" + messageDates.get(i)+ " : " + messageNums.get(i));
                d[i] = new DataPoint(fmt.parse(messageDates.get(i)), messageNums.get(i));
            }
            catch (ParseException e) {
                Log.d("getGraphPoints", "error: " + e);
            }
        }
        return new BarGraphSeries<>(d);
    }

    public void addMessage(MessageData m)
    {
        //checking if the last messaged time needs updated
        //need to expand this to also update the next message deadline too
        if (lastMessaged < m.timestamp) {
            lastMessaged = m.timestamp;
            setContactFrequency(daysPerDeadline);
        }
        totalMessages++;
        addToList(new Date(m.timestamp));
        //incrementing the message count for achievements
        MainScreen.m.achieve.incrMsg();
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

    //returns true if successful
    public String setContactFrequency(String timePeriod)
    {
        try {
            daysPerDeadline = timePeriod;
            String[] split = timePeriod.split(" ");
            int time = 1;
            try {
                time = Integer.parseInt(split[0]);
                if (time < 1 || time > 12) {
                    return "Enter a number between 1 and 12";
                }
            }
            catch (Exception e) {
                return "Enter a number between 1 and 12";
            }
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(lastMessaged);
            switch (split[1]) {
                case ("Hour"):
                    c.add(Calendar.HOUR, time);
                    break;
                case ("Day"):
                    c.add(Calendar.DAY_OF_YEAR, time);
                    break;
                case ("Week"):
                    c.add(Calendar.WEEK_OF_YEAR, time);
                    break;
                // do a month otherwise
                case ("Month"):
                    c.add(Calendar.MONTH, time);
                    break;
            }
            nextMessageDeadline = c.getTimeInMillis();
            checkIfDeadlineHere();
            return "";
        }
        catch (Exception e) {
            return "" + e;
        }
    }

    public void checkIfDeadlineHere()
    {
        if (Calendar.getInstance().getTimeInMillis() > (nextMessageDeadline))
            deadlineHere = true;
        else
            deadlineHere = false;
        return;
    }

    /* adopted this code from a post on Stackoverflow */
    /* provide Context to get thumbnail profile pic and save under pic field */
    /**
     * @return the photo URI
     */
    public Uri getPhotoUri(Context context) {
        try {
            Cursor cur = context.getContentResolver().query(
                    ContactsContract.Data.CONTENT_URI,
                    null,
                    ContactsContract.Data.CONTACT_ID + "=" + id + " AND "
                            + ContactsContract.Data.MIMETYPE + "='"
                            + ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE + "'", null,
                    null);
            if (cur != null) {
                if (!cur.moveToFirst()) {
                    return null; // no photo
                }
            } else {
                return null; // error in cursor process
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long
                .parseLong(id));
        Uri temp = Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
        return temp;
    }

    public String toString()
    {
        DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
        if(nextMessageDeadline == 0)
            setContactFrequency("1 Week");
        return "\nName: " + name +
                "\nPhone number: " + phoneNum.get(0) +
                //"\nNum Messages: " + messages.size() +
                "\nRelationship Points: " + relationshipPoints +
                "\nLast Messaged:" + formatter.format(lastMessaged) +
                "\nNext Deadline:" + formatter.format(nextMessageDeadline) +
                "\nDays per Deadline:" + daysPerDeadline +
                "\nDeadline?:" + deadlineHere +
                "\n";
    }


    public int numMessages()
    {
        return totalMessages;
    }


    @Override
    public int compareTo(ContactData c)
    {
        return name.compareTo(c.name);
    }
}
