package com.example.pbush.ketchupwithfriends;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by pbush on 12/5/2018.
 */

public class ContactButtonFragment extends Fragment implements MainScreen.ContactButton{

    private ContactData contact;
    private Button contactButton;
    private TextView nameText;
    private TextView timeLeftText;
    private ProgressBar progress;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /** Inflating the layout for this fragment **/
        View v = inflater.inflate(R.layout.contact_button_fragment, container, false);
        nameText = (TextView) v.findViewById(R.id.name);
        timeLeftText = (TextView) v.findViewById(R.id.timeLeft);
        progress = (ProgressBar) v.findViewById(R.id.progressBarTimeLeft);
        contactButton = (Button) v.findViewById(R.id.contact_button);
        return v;
    }

    @Override
    public void resetButton(ContactData c)
    {
        contact = c;
        nameText.setText(c.name);
        long left = c.nextMessageDeadline - Calendar.getInstance().getTimeInMillis();
        if (left <= 0)
        {
            timeLeftText.setText("Message them!!");
            //filling the progress bar
            progress.setProgress(100);
        }
        else {
            long days = TimeUnit.MILLISECONDS.toDays(left);
            long hours, min;
            if (days != 0) {
                hours = (TimeUnit.MILLISECONDS.toHours(left) / days) % 24;
                if (hours != 0) {
                    min = TimeUnit.MILLISECONDS.toMinutes(left) / days / hours % 60;
                }
                else {
                    min = TimeUnit.MILLISECONDS.toMinutes(left) / days % 60;
                }
            }
            else
            {
                hours = (TimeUnit.MILLISECONDS.toHours(left)) % 24;
                if (hours != 0) {
                    min = TimeUnit.MILLISECONDS.toMinutes(left) / hours % 60;
                }
                else {
                    min = TimeUnit.MILLISECONDS.toMinutes(left)% 60;
                }
            }
            timeLeftText.setText("Time Left: " + days + "d, " + hours + "h, " + min + "m");
            long d  = (TimeUnit.HOURS.toMillis((long)c.daysPerDeadline));
            int p = (int)(100-((left)/(float)d)*100);
            //Log.d("for contact", "left: " + left + " days : " + d + " progress: " + p);
            progress.setProgress(p);
        }
    }

    @Override
    public ContactData getButtonContact() {
        return contact;
    }

    public Button getButton()
    {
        return contactButton;
    }

}
