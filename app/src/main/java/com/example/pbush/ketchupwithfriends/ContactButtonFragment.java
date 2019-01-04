package com.example.pbush.ketchupwithfriends;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.os.AsyncTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by pbush on 12/5/2018.
 */

public class ContactButtonFragment extends Fragment implements MainScreen.ContactButton{

    private ContactData contact;
    private int linearLayout;
    private Button contactButton;
    private Button msgButton;
    private TextView nameText;
    private TextView timeLeftText;
    private ProgressBar progress;
    private CheckBox toDelete;
    private ImageView contactImage;
    private int idx;

    private class RefreshLayout extends AsyncTask<Void, Void, String> {
        protected void onPreExecute()
        {
            resetProgressBar();
        }
        protected String doInBackground(Void... params)
        {
            return "";
        }

        protected void onPostExecute(String res) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        /** Inflating the layout for this fragment **/
        View v = inflater.inflate(R.layout.contact_button_fragment, container, false);
        nameText = (TextView) v.findViewById(R.id.name);
        LinearLayout l = v.findViewById(R.id.contact_button_linear_layout);
        l.setId(MainScreen.forIds);
        linearLayout = MainScreen.forIds++;
        timeLeftText = (TextView) v.findViewById(R.id.timeLeft);
        progress = (ProgressBar) v.findViewById(R.id.progressBarTimeLeft);
        contactButton = (Button) v.findViewById(R.id.contact_button);
        msgButton = (Button)v.findViewById(R.id.msgButton);
        toDelete = v.findViewById(R.id.toDelete);
        contactImage = v.findViewById(R.id.contact_image);
        return v;
    }

    @Override
    public void resetButton(ContactData c, int index)
    {
        idx = index;
        contact = c;
        nameText.setText(c.name);
        resetProgressBar();
    }

    public void resetProgressBar() {
        long left = contact.nextMessageDeadline - Calendar.getInstance().getTimeInMillis();
        if (left <= 0)
        {
            timeLeftText.setText("Message them!!");
            //filling the progress bar
            progress.setProgress(100);
        }
        else {
            long days = TimeUnit.MILLISECONDS.toDays(left);
            long hours = TimeUnit.MILLISECONDS.toHours(left);
            long min = TimeUnit.MILLISECONDS.toMinutes(left);
            long sec = TimeUnit.MILLISECONDS.toSeconds(left);
            if (days != 0) {
                hours = (hours / days) % 24;
                if (hours != 0) {
                    min = min / days / hours % 60;
                    if (min != 0) {
                        sec = sec / min / hours / days % 60;
                    }
                    else {
                        sec = sec / days % 60;
                    }
                }
                else {
                    min = min / days % 60;
                    if (min != 0) {
                        sec = sec / min / days % 60;
                    }
                    else {
                        sec = sec / days % 60;
                    }
                }
            }
            else
            {
                hours = hours % 24;
                if (hours != 0) {
                    min = min / hours % 60;
                    if (min != 0) {
                        sec = sec / min / hours % 60;
                    }
                    else {
                        sec = sec / hours % 60;
                    }
                }
                else {
                    min = min % 60;
                    if (min != 0) {
                        sec = sec / min % 60;
                    }
                    else {
                        sec = sec % 60;
                    }
                }
            }
            timeLeftText.setText("Time Left: " + days + "d, " + hours + "h, " + min + "m, " + sec + "s");
            long d  = (TimeUnit.HOURS.toMillis((long)contact.daysPerDeadline));
            Log.d("time left", "time left: " + left);
            int p = (int)(100-((left)/(float)d)*100);
            //Log.d("for contact", "left: " + left + " days : " + d + " progress: " + p);
            progress.setProgress(p);
        }
    }

    @Override
    public ContactData getButtonContact() {
        return contact;
    }

    public void setContactTime() {
        this.resetButton(contact, idx);
        return;
    }

    public void switchToDelete() {
        toDelete.setVisibility(View.VISIBLE);
        contactImage.setVisibility(View.INVISIBLE);
        return;
    }

    public void switchFromDelete() {
        toDelete.setVisibility(View.INVISIBLE);
        contactImage.setVisibility(View.VISIBLE);
        return;
    }

    public boolean isChecked() {
        return toDelete.isChecked();
    }

    public Button getButton()
    {
        return contactButton;
    }

    @Override
    public int getIndex() {
        return idx;
    }

    public int getLayout()
    {
        return linearLayout;
    }

    public Button getMsgButton() {
        return msgButton;
    }
}
