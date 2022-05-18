package com.maxgestic.zerochat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class ContactsAdapter extends ArrayAdapter<FirestoreContact> {

    public ContactsAdapter(Context context, ArrayList<FirestoreContact> contacts){
        super(context, 0, contacts);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        /* Get the contacts data for this position. */
        FirestoreContact contact = getItem(position);
        /* Check if an existing view is being reused, otherwise inflate the view. */
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.contact_item, parent, false);
        }
        /* Lookup views. */
        TextView display_name = (TextView) convertView.findViewById(R.id.contact_name);
        /* Add the data to the template view. */
        display_name.setText(contact.getNick());
        /* Return the completed view to render on screen. */
        return convertView;
    }
}