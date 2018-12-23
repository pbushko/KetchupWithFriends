package com.example.pbush.ketchupwithfriends;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by pbush on 12/23/2018.
 */

public class GetMultipleContacts extends Fragment implements MainScreen.GetContactsFragment {

    private ContactData contact;
    private TextView nameText;
    private CheckBox checkBox;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        /** Inflating the layout for this fragment **/
        View v = inflater.inflate(R.layout.get_contact_fragment, container, false);
        nameText = (TextView) v.findViewById(R.id.name);
        checkBox = (CheckBox) v.findViewById(R.id.checkBox);
        return v;
    }

    public void resetButton(ContactData c) {
        contact = c;
        nameText.setText(c.name);
    }

    public boolean isChecked() {
        return checkBox.isChecked();
    }

    public ContactData getContact() {
        return contact;
    }
}
