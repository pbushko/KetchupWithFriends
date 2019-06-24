package com.catgames.pbush.ketchupwithfriends;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

/**
 * Created by pbush on 12/23/2018.
 */

public class GetMultipleContacts extends Fragment implements MainScreen.GetContactsFragment {

    private ContactData contact;
    private CheckBox checkBox;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        /** Inflating the layout for this fragment **/
        View v = inflater.inflate(R.layout.get_contact_fragment, container, false);
        checkBox = (CheckBox) v.findViewById(R.id.checkBox);
        return v;
    }

    public void resetButton(ContactData c) {
        contact = c;
        checkBox.setText("   " + c.name);
    }

    public boolean isChecked() {
        return checkBox.isChecked();
    }

    public void check(boolean toCheck) {
        checkBox.setChecked(toCheck);
    }

    public ContactData getContact() {
        return contact;
    }
}
