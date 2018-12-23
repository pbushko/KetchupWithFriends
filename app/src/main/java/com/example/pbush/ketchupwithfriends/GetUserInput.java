package com.example.pbush.ketchupwithfriends;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Created by pbush on 11/29/2018.
 */

public class GetUserInput extends Fragment implements MainScreen.GetInput{

    private Button submitButton;
    private Button cancelButton;
    private Spinner spinner;
    private EditText num;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        /** Inflating the layout for this fragment **/
        View v = inflater.inflate(R.layout.get_contact_frequency_screen, container, false);

        submitButton = (Button)v.findViewById(R.id.ok_button);
        cancelButton = (Button)v.findViewById(R.id.cancel_button);
        spinner = (Spinner)v.findViewById(R.id.time_option_spinner);
        num = (EditText) v.findViewById(R.id.user_num_input);
        num.setTransformationMethod(null);

        // Create an ArrayAdapter using the string array and a default spinner
        ArrayAdapter<CharSequence> staticAdapter = ArrayAdapter
                .createFromResource(getActivity(), R.array.time_options_array,
                        android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        staticAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(staticAdapter);

        return v;
    }

    public Button getCancelButton() {
        return cancelButton;
    }

    public Button getSubmitButton() {
        return submitButton;
    }

    public int getNewContactFreq() {
        int multiplier = 1;
        switch (spinner.getSelectedItem().toString())
        {
            case("Hour"):
                multiplier = 1;
                break;
            case("Day"):
                multiplier = 24;
                break;
            case("Week"):
                multiplier = 7*24;
                break;
            // NOT IMPLEMENTED RIGHT NOW
            case("Month"):
                multiplier = 1;
                break;
            default:
                multiplier = 1;
                break;
        }
        return Integer.parseInt(num.getText().toString())* multiplier;
    }
}