package com.example.pbush.ketchupwithfriends;


import java.util.List;
import java.util.Calendar;

/**
 * Created by pbush on 12/9/2018.
 */

/** kasarn: AcheivementData keep track of achievements of each user.
 *  An instance is in MainScreen. It is saved to Firebase.
 *  Some achievement information is saved to ContactData of each
 *  contact. This class also makes modification to those fields.
 */
public class AchievementData {
    public final static int MS_PER_HOUR = 3600000;
    public final static int MS_PER_DAY = 24 * MS_PER_HOUR;

    public boolean[] nContactAchieve;
    public boolean[] nMessageAchieve;
    public boolean[] loginStreakAchieve;
    public boolean[] deadlineAchieve;
    public boolean[] messageStreakAchieve;


    public static int[] nContactBench = {10, 20, 30, 40, 50, 60};
    public static int[] nMessageBench = {1, 10, 20, 50, 100, 1000};
    public static int[] loginStreakBench = {3, 5, 10, 20, 50, 100};
    public static long loginTime = MS_PER_HOUR / 6; // in milisecond
    public static int[] deadlineBench = {5, 20, 50, 100, 150, 300};
    public static int[] messageStreakBench = {5, 10, 30, 50, 100, 200};


    public int nMessages;
    public int nContacts;
    public int loginStreak;
    public long lastLogin;
    public int deadlinesHit;

    /*
    public int maxStreak;
     */

    public AchievementData() {
        nContactAchieve = new boolean[6];
        nMessageAchieve = new boolean[6];
        loginStreakAchieve = new boolean[6];
        deadlineAchieve = new boolean[6];
        messageStreakAchieve = new boolean[6];

    }

    // increment and check number of contacts added
    public void incrContact() {
        nContacts++;
        benchTest(nContacts, nContactBench, nContactAchieve);
    }

    // increment and check number of messages sent
    public void incrMsg() {
        nMessages++;
        benchTest(nMessages, nMessageBench, nMessageAchieve);
    }

    public boolean messageAchievementProgress() {
        return nMessages > nMessageBench[0];
    }
    // check day
    public void checkday(long currentTime) {
        // first time
        if (lastLogin == 0)
        {
            loginStreak = 1;
            lastLogin = currentTime;
        }
        long miliIntoLastLogin = lastLogin % MS_PER_DAY;
        long diff = currentTime - lastLogin;

        long timeRemainingDay = MS_PER_DAY - miliIntoLastLogin;
        long endOfNextday = lastLogin + timeRemainingDay + MS_PER_DAY;

        // not the next day; reset streak
        if (currentTime >= endOfNextday)
        {
            lastLogin = currentTime;
            loginStreak = 1;
        }
        // next day; continue streak
        else if (currentTime < endOfNextday && diff > timeRemainingDay)
        {
            lastLogin = currentTime;
            loginStreak++;
        }
        // not next day yet; don't do anything

        benchTest(loginStreak, loginStreakBench, loginStreakAchieve);
    }
/*
    public void update(MessageData m, List<ContactData> mContacts) {


    }
    */
    // check if new achivement should be given, if so change it and return true
    // if not return false
    public boolean benchTest(int score, int[] test, boolean[] achieve) {
        boolean change  = false;
        for (int i = 0; i < test.length; i++) {
            // does the score meet the benchmark
            if (score >= test[i]) {
                // if this achievement has not been given
                if (!achieve[i]) {
                    change = true;
                    achieve[i] = true;
                }
                else {
                    achieve[i] = true;
                }
            }
        }
        return change;
    }


}
