package com.example.pbush.ketchupwithfriends;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by pbush on 12/5/2018.
 */

public class ContactButtonFragment extends Fragment implements MainScreen.ContactButton{

    private ContactData contact;
    private TextView nameText;
    private TextView timeLeftText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /** Inflating the layout for this fragment **/
        View v = inflater.inflate(R.layout.contact_button_fragment, container, false);
        nameText = (TextView) v.findViewById(R.id.name);
        return v;
    }

    @Override
    public void resetButton(ContactData c)
    {
        contact = c;
        nameText.setText(c.name);
    }
}
