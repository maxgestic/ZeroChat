package com.maxgestic.zerochat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
        TextView display_name = convertView.findViewById(R.id.contact_name);
        TextView message_text = convertView.findViewById(R.id.message_preview);
        ImageView profilePicView = convertView.findViewById(R.id.profilePicture);

//        Toast.makeText(getContext(), ""+contact.getMessagePrev(), Toast.LENGTH_SHORT).show();
        /* Add the data to the template view. */
        display_name.setText(contact.getNick());
        message_text.setText(contact.getMessagePrev());

        if (contact.getImageBytes() != null) {
            byte[] decodedString = contact.getImageBytes();
            InputStream inputStream = new ByteArrayInputStream(decodedString);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            profilePicView.setImageBitmap(bitmap);
        }
        /* Return the completed view to render on screen. */
        return convertView;
    }
}