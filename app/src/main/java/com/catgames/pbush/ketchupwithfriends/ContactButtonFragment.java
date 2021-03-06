package com.catgames.pbush.ketchupwithfriends;

import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.os.AsyncTask;

import java.util.Calendar;

/**
 * Created by pbush on 12/5/2018.
 */

public class ContactButtonFragment extends Fragment implements MainScreen.ContactButton{

    private ContactData contact;
    private int linearLayout;
    private Button contactButton;
    private Button msgButton;
    private Button directMsgButton;
    private TextView nameText;
    private TextView timeLeftText;
    private TextView msgDeadlineText;
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
        msgDeadlineText = v.findViewById(R.id.contactDeadline);
        progress = (ProgressBar) v.findViewById(R.id.progressBarTimeLeft);
        contactButton = (Button) v.findViewById(R.id.contact_button);
        msgButton = (Button)v.findViewById(R.id.msgButton);
        directMsgButton = (Button) v.findViewById(R.id.directMessage);
        toDelete = v.findViewById(R.id.toDelete);
        contactImage = v.findViewById(R.id.contact_image);
        return v;
    }

    @Override
    public void resetButton(ContactData c, int index)
    {
        idx = index;
        contact = c;
        Uri pic = c.getPhotoUri(getActivity());
        if (pic != null) {
            contactImage.setImageURI(pic);
        }

        nameText.setText(c.name);
        String[] split = c.daysPerDeadline.split(" ");
        if (Integer.parseInt(split[0]) != 1) {
            msgDeadlineText.setText("Message every " + c.daysPerDeadline.toLowerCase() + "s");
        }
        else {
            msgDeadlineText.setText("Message every " + c.daysPerDeadline.toLowerCase());
        }
        resetProgressBar();
    }

    public void resetProgressBar() {
        long diff = contact.nextMessageDeadline - Calendar.getInstance().getTimeInMillis();
        if (diff <= 0)
        {
            timeLeftText.setText("Message them!!");
            //filling the progress bar
            progress.setProgress(100);
        }
        else {
            long x = diff / 1000;
            long sec = x % 60;
            x /= 60;
            long min = x % 60;
            x /= 60;
            long hours = x % 24;
            x /= 24;
            long days = x;

            timeLeftText.setText("Time Left: " + days + "d, " + hours + "h, " + min + "m, " + sec + "s");
            long d  = (contact.nextMessageDeadline - contact.lastMessaged);
            //Log.d("time left", "time left: " + diff);
            int p = (int)(100 - (diff/(float)d)*100);
            //Log.d("for contact", "left: " + diff + " days : " + d + " progress: " + p);
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

    public Button getDirectMsgButton() {
        return directMsgButton;
    }
}
