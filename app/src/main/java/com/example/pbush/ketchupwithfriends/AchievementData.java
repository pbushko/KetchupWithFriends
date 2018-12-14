package com.example.pbush.ketchupwithfriends;


import java.util.List;

/**
 * Created by pbush on 12/9/2018.
 */

/** kasarn: AcheivementData keep track of achievements of each user.
 *  An instance is in MainScreen. It is saved to Firebase.
 *  Some achievement information is saved to ContactData of each
 *  contact. This class also makes modification to those fields.
 */
public class AchievementData {
    public boolean[] nContactAchieve;
    public boolean[] nMessageAchieve;
    public boolean[] loginStreakAchieve;
    public boolean[] messageStreakAchieve;
    public boolean[] deadlineAchieve;

    public static int[] nContactBench = {10, 20, 30, 40, 50, 60};
    public static int[] nMessageBench = {1, 20, 50, 100, 1000};
    public static int[] loginStreakBench = {5, 10, 20, 100};
    public static int[] messageStreakBench = {5, 10, 30, 200};
    public static int[] deadlineBench = {5, 20, 50, 100};

    public int nMessages;
    public int nContacts:
    public int loginStreak;
    public int deadlinesHit;
    /*
    public int maxStreak;
     */

    public AchievementData {
        nContactAchieve = new boolean[6];
        nMessageAchieve = new boolean[5];
        loginStreakAchieve = new boolean[4];
        messageStreakAchieve = new boolean[4];
        deadlineAchieve = new boolean[6];
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

    public void update(MessageData m, List<ContactData> mContacts) {


    }
    // check if new achivement should be given, if so change it and return true
    // if not return false
    public boolean benchTest(int score, int[] test, boolean[] achieve) {
        boolean change  = false
        for (int i; i < test.length; i++) {
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
