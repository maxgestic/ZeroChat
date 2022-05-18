package com.maxgestic.zerochat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.Predicate;

class ChatAdapter extends ArrayAdapter<Message> {

    private TextView chatText;
    private List<Message> chatMessageList = new ArrayList<Message>();
    private Context context;

    public ChatAdapter(Context context, ArrayList<Message> messages) {
        super(context, 0, messages);
    }

    @Override
    public void add(Message object) {
        chatMessageList.add(object);
        chatMessageList.sort(Message.byDate);
        super.add(object);
        super.sort(Message.byDate);
//        Toast.makeText(context, object.toString(), Toast.LENGTH_SHORT).show();
    }

    public ChatAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        this.context = context;
    }

    @Override
    public void clear() {
        chatMessageList.clear();
        super.clear();
    }

    public void clearSent(){
        chatMessageList.removeIf(new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                String userID = FirebaseAuth.getInstance().getUid();
                assert userID != null;
                return userID.equals(message.getSentBy());
            }
        });
    }

    public void clearRec(){
        chatMessageList.removeIf(new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                String userID = FirebaseAuth.getInstance().getUid();
                assert userID != null;
                return !userID.equals(message.getSentBy());
            }
        });
    }

    public int getCount() {
        return this.chatMessageList.size();
    }

    public Message getItem(int index) {
        return this.chatMessageList.get(index);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String userID = FirebaseAuth.getInstance().getUid();
        Message messageObj = getItem(position);
        View row = convertView;
        LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert userID != null;
//        Toast.makeText(context, messageObj.toString(), Toast.LENGTH_SHORT).show();
        if (userID.equals(messageObj.getSentBy())) {
            row = inflater.inflate(R.layout.message_sent, parent, false);
        }else{
            row = inflater.inflate(R.layout.message_received, parent, false);
        }
        /* Lookup views. */
        TextView messageTxt = (TextView) row.findViewById(R.id.msgr);
        TextView timestampTxt = row.findViewById(R.id.timestamp);
        /* Add the data to the template view. */
        messageTxt.setText(messageObj.getMessage());
        Calendar cal = Calendar.getInstance();
        cal.setTime(messageObj.getTimestamp().toDate());
        Calendar c1 = Calendar.getInstance(); // today
        c1.add(Calendar.DAY_OF_YEAR, -1); // yesterday

        if (c1.get(Calendar.YEAR) >= cal.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) >= cal.get(Calendar.DAY_OF_YEAR)) {
            @SuppressLint("SimpleDateFormat") String format = new SimpleDateFormat("dd.MM.yyyy").format(cal.getTime());
            timestampTxt.setText(format + " " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));
        }else {
            timestampTxt.setText(cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));
        }
        /* Return the completed view to render on screen. */
        return row;
    }
}
