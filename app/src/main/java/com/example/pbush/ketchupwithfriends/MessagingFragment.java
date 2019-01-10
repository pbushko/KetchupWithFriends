package com.example.pbush.ketchupwithfriends;

import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Intent;
import android.telephony.SmsManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

/**
 * Created by pbush on 1/7/2019.
 */

public class MessagingFragment extends Fragment implements MainScreen.MessageFragment {

    private ContactData contact;
    private Button msgButton;
    private Button cancelButton;
    private EditText msgText;
    private TextView contactName;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        /** Inflating the layout for this fragment **/
        View v = inflater.inflate(R.layout.message_screen_fragment, container, false);
        msgButton = (Button)v.findViewById(R.id.msgButton);
        msgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        cancelButton = v.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainScreen.m.setMainScreen();
            }
        });
        msgText = (EditText) v.findViewById(R.id.msgText);
        contactName = (TextView) v.findViewById(R.id.contact_name);
        return v;
    }

    public void resetData(ContactData c)
    {
        contact = c;
        contactName.setText(c.name);
    }

    //sends the message to the contact
    public void sendMessage() {
        if (contact != null) {
            String msg = msgText.getText().toString();

            if (msg != "" || msg != null) {
                try {
                    MainScreen.m.getNewInfo();
                    //Getting intent and PendingIntent instance
                    Intent intent = new Intent(getActivity(), MessagingFragment.class);
                    PendingIntent pi = PendingIntent.getActivity(getActivity(), 0, intent, 0);
                    //Get the SmsManager instance and call the sendTextMessage method to send message
                    SmsManager sms = SmsManager.getDefault();
                    sms.sendTextMessage(contact.phoneNum.get(0), null, msg, pi, null);
                    Log.i("Finished sending SMS...", "");
                    MessageData m = new MessageData();
                    m.timestamp = Calendar.getInstance().getTimeInMillis();
                    contact.addMessage(m);
                    MainScreen.m.lastDataScrape = m.timestamp;
                    Toast.makeText(getActivity(),"Message sent!", Toast.LENGTH_SHORT).show();
                    MainScreen.m.setMainScreen();
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getActivity(),
                            "SMS failed, please try again later.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

}
