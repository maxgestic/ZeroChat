package com.maxgestic.zerochat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class ConvoListAdapter extends ArrayAdapter<FirestoreContact> {

    public ConvoListAdapter(Context context, ArrayList<FirestoreContact> contacts){
        super(context, 0, contacts);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        /* Get the contacts data for this position. */
        FirestoreContact contact = getItem(position);
        /* Check if an existing view is being reused, otherwise inflate the view. */
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.convolist_item, parent, false);
        }
        /* Lookup views. */
        TextView display_name = (TextView) convertView.findViewById(R.id.contact_name);
        TextView message_text = (TextView) convertView.findViewById(R.id.message_preview);

//        Toast.makeText(getContext(), ""+contact.getMessagePrev(), Toast.LENGTH_SHORT).show();
        /* Add the data to the template view. */
        display_name.setText(contact.getNick());
        message_text.setText(contact.getMessagePrev());
        /* Return the completed view to render on screen. */
        return convertView;
    }
}