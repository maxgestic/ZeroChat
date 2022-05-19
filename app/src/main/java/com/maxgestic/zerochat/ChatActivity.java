package com.maxgestic.zerochat;

import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class ChatActivity extends AppCompatActivity implements EventListener<QuerySnapshot>, View.OnClickListener, PopupMenu.OnMenuItemClickListener {

    private ChatAdapter chatAdapter;
    private ListView listView;
    private EditText messageInputText;
    private Button menuButton;
    private Button mapButton;
    private String from;
    private String to;
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final String userID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
    private final CollectionReference contacts = FirebaseFirestore.getInstance().collection("users").document(userID).collection("contacts");
    private CollectionReference messages;
    private final DocumentReference convos = FirebaseFirestore.getInstance().collection("convos").document(userID);
    private final DocumentReference userDoc = FirebaseFirestore.getInstance().collection("users").document(userID);
    private String name;
    private ListenerRegistration lr1, lr2, lr3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        listView = findViewById(R.id.messagesList);

        Button backButton = findViewById(R.id.chatBackButton);
        TextView nameField = findViewById(R.id.chatName);
        menuButton = findViewById(R.id.chatMoreButton);
        mapButton = findViewById(R.id.chatMapButton);

        backButton.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        from = intent.getStringExtra("from");
        to = intent.getStringExtra("to");

        FirebaseFirestore.getInstance().collection("users").document(to).collection("contacts").document(from).get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()){

                        DocumentSnapshot doc = task.getResult();

                        if (doc.exists()){

                            Boolean sharing = doc.getBoolean("sharingLoc");

                            if (sharing != null){

                                if (sharing){

                                    mapButton.setVisibility(Button.VISIBLE);

                                }

                            }

                        }

                    }

                });

        messages = FirebaseFirestore.getInstance().collection("convos").document(from).collection(to);
        CollectionReference messages2 = FirebaseFirestore.getInstance().collection("convos").document(to).collection(from);

        DocumentReference docRef = contacts.document(to);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    name = Objects.requireNonNull(Objects.requireNonNull(document.getData()).get("nickname")).toString();
                    nameField.setText(name);
                } else {
                    Log.d("TEST", "No such document");
                }
            } else {
                Log.d("TEST", "get failed with ", task.getException());
            }
        });

        Button sendButton = findViewById(R.id.messageSendButton);

        listView = findViewById(R.id.messagesList);

        chatAdapter = new ChatAdapter(getApplicationContext(), R.layout.message_sent);
        listView.setAdapter(chatAdapter);

        messageInputText = (EditText) findViewById(R.id.messageInput);
        messageInputText.setOnKeyListener((v, keyCode, event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (event.getAction() == KeyEvent.KEYCODE_ENTER))
                return sendChatMessage();
            return false;
        });

        sendButton.setOnClickListener(v -> sendChatMessage());

        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatAdapter);

        chatAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatAdapter.getCount() - 1);
            }
        });
        menuButton.setOnClickListener(this);
        mapButton.setOnClickListener(this);

//        populateList();

        lr1 = messages.addSnapshotListener(this);
        lr2 = messages2.addSnapshotListener(this);
        lr3 = FirebaseFirestore.getInstance().collection("users").document(to).collection("contacts").document(from).addSnapshotListener(this::updateContactDoc);


    }

    private void updateContactDoc(DocumentSnapshot queryDocumentSnapshots, FirebaseFirestoreException e) {

        /* If exception occurs, don't try to do anything else, just display the error and return */
        if (e != null) {
            Log.e("TEST", "Listen failed.", e);
            return;
        }
            if (queryDocumentSnapshots != null) {

                Boolean sharing = queryDocumentSnapshots.getBoolean("sharingLoc");
//                Toast.makeText(this, "UPDATE", Toast.LENGTH_SHORT).show();
                if (sharing != null){

                    if (sharing){

                        mapButton.setVisibility(Button.VISIBLE);

                    }
                    else{

                        mapButton.setVisibility(Button.INVISIBLE);

                    }
                }

            }

    }

    private boolean sendChatMessage() {
        //get message text and clear input field
        String messageText = messageInputText.getText().toString();
        messageInputText.setText("");
        //initialise map to be uploaded to firestore and add message data
        Map<String, Object> data = new HashMap<>();
        data.put("message", messageText);
        data.put("read", false);
        data.put("time_sent", FieldValue.serverTimestamp());
        data.put("sent_by", FirebaseAuth.getInstance().getUid());
        //add message to firestore
        messages.add(data)
                .addOnSuccessListener(documentReference -> {
                    //add id of receiver into the "convos" array to be able to fetch them in the conversation list fragment
                    convos.get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()){
                                    AtomicReference<List<String>> group = new AtomicReference<>();
                                    DocumentSnapshot document = task.getResult();
                                    group.set((List<String>) document.get("convos"));
                                    //check if the "convos" array exists, if not create it and add it to document
                                    if (group.get() != null) {
                                        ArrayList<String> array = new ArrayList<>(group.get());
                                        //check if receiver id is already in "convos" array and if not add it
                                        if (!array.contains(to)) {
                                            convos.update("convos", FieldValue.arrayUnion(to));
                                        }
                                    }
                                    else{
                                        Map<String, Object> newData = new HashMap<>();
                                        newData.put("convos", Collections.singletonList(to));
                                        convos.set(newData);
                                    }
                                    //function to send notification to receiver devices
                                    sendMessageNotification(messageText);
                                }
                            });
                    Log.d("TEST", "DocumentSnapshot written with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> Log.w("TEST", "Error adding document", e));
        //update document to show when the latest message was in the conversation, this is used to sort list of convos in the conversation list fragment
        contacts.document(to).update(FirestoreContact.FIELD_LASTMESSAGE, FieldValue.serverTimestamp());
        return true;
    }

    public void sendMessageNotification(String message){
        String API = "https://fcm.googleapis.com/fcm/send";
        String serverKey = "key=" + getResources().getString(R.string.FCM_SERVER_KEY);
        String contentType = "application/json";
        JSONObject notification = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        DocumentReference userDoc = FirebaseFirestore.getInstance().collection("users").document(to);

        //Getting device tokens of receiver
        userDoc.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        DocumentSnapshot doc = task.getResult();
                        List<String> sendToTokens = (List<String>) doc.get("deviceTokens");
                        if (sendToTokens != null) {
                            for (String s : sendToTokens) {
                                try {
                                    notificationBody.put("title", from);
                                    notificationBody.put("message", message);
                                    notification.put("to", s);
                                    notification.put("data", notificationBody);
                                } catch (JSONException e) {
                                    Log.e("TAG", "onCreate: " + e.getMessage());
                                }
                                JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, API, notification, response -> Log.i("TEST", response.toString()), error -> Log.e("TEST", error.toString())){
                                    @Override
                                    public Map<String, String> getHeaders(){
                                        HashMap<String, String> params = new HashMap<>();
                                        params.put("Authorization", serverKey);
                                        params.put("Content-Type", contentType);
                                        return params;
                                    }
                                };
                                Volley.newRequestQueue(this).add(jsonRequest);
                            }
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        lr1.remove();
        lr2.remove();
        lr3.remove();
        super.onDestroy();
    }

    public void showMenu(View v){
        PopupMenu moreMenu = new PopupMenu(this, v);
        moreMenu.setOnMenuItemClickListener(this);
        MenuInflater inflater = moreMenu.getMenuInflater();
        inflater.inflate(R.menu.chat_menu, moreMenu.getMenu());
        contacts.document(to).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()){
                            Boolean sharing = doc.getBoolean("sharingLoc");
                            if (sharing != null){
                                if (sharing){
                                    moreMenu.getMenu().getItem(0).setChecked(true);
                                }
                            }
                        }
                    }
                });
        moreMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        item.setChecked(!item.isChecked());

        contacts.document(to).update("sharingLoc", item.isChecked());

        if (item.isChecked()) {

            userDoc.update("sharedWith", FieldValue.arrayUnion(to));
            Intent locationStartIntent = new Intent(this, GPS.class);
            locationStartIntent.putExtra("userID", from);
            startService(locationStartIntent);
        }else{
            userDoc.update("sharedWith", FieldValue.arrayRemove(to));
            Intent locationStartIntent = new Intent(this, GPS.class);
            stopService(locationStartIntent);
        }

        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
        item.setActionView(new View(getApplicationContext()));
        item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return false;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return false;
            }
        });
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v == menuButton){

            showMenu(v);

        }
        else if (v == mapButton){

            Intent mapIntent = new Intent(this, MapsActivity.class);
            mapIntent.putExtra("otherPerson", to);
            mapIntent.putExtra("otherPersonName", name);
            startActivity(mapIntent);

        }
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
        /* If exception occurs, don't try to do anything else, just display the error and return */
        if (e != null) {
            Log.e("TEST", "Listen failed.", e);
            return;
        }

        if (chatAdapter != null) {
            assert queryDocumentSnapshots != null;
            for (DocumentChange c : queryDocumentSnapshots.getDocumentChanges()) {
                if (c.getType() == DocumentChange.Type.ADDED) {
//                    Log.d("TEST", "New Message: " + c.getDocument().getData());
                    Message message = c.getDocument().toObject(Message.class);
                    message.setRead(c.getDocument().getBoolean(Message.FIELD_READ));
                    message.setSentBy(c.getDocument().getString(Message.FIELD_SENT_BY));
                    if (c.getDocument().getTimestamp(Message.FIELD_TIME_SENT) == null)
                        message.setTimestamp(Timestamp.now());
                    else {
                        message.setTimestamp(c.getDocument().getTimestamp(Message.FIELD_TIME_SENT));
                    }
                    chatAdapter.add(message);
                }
            }
        }
    }
}
