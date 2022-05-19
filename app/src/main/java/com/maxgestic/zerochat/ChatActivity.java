package com.maxgestic.zerochat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import com.google.firebase.firestore.EventListener;

import org.json.JSONException;
import org.json.JSONObject;

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
    private Button sendButton, menuButton, mapButton;
    private boolean side = false;
    private String from;
    private String to;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    String userID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
    CollectionReference contacts = FirebaseFirestore.getInstance().collection("users").document(userID).collection("contacts");
    CollectionReference messages;
    CollectionReference messages2;
    DocumentReference convos = FirebaseFirestore.getInstance().collection("convos").document(userID);
    DocumentReference userDoc = FirebaseFirestore.getInstance().collection("users").document(userID);
    String name;

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
        messages2 = FirebaseFirestore.getInstance().collection("convos").document(to).collection(from);

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

        sendButton = findViewById(R.id.messageSendButton);

        listView = findViewById(R.id.messagesList);

        chatAdapter = new ChatAdapter(getApplicationContext(), R.layout.message_sent);
        listView.setAdapter(chatAdapter);

        messageInputText = (EditText) findViewById(R.id.messageInput);
        messageInputText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (event.getAction() == KeyEvent.KEYCODE_ENTER))
                    return sendChatMessage();
                return false;
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendChatMessage();
            }
        });

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

        messages.addSnapshotListener(this);
        messages2.addSnapshotListener(this);
        FirebaseFirestore.getInstance().collection("users").document(to).collection("contacts").document(from).addSnapshotListener(this::updateContactDoc);


    }

    private void updateContactDoc(DocumentSnapshot queryDocumentSnapshots, FirebaseFirestoreException e) {

        /* If exception occurs, don't try to do anything else, just display the error and return */
        if (e != null) {
            Log.e("TEST", "Listen failed.", e);
            return;
        }
            if (queryDocumentSnapshots != null) {

                Boolean sharing = queryDocumentSnapshots.getBoolean("sharingLoc");
                Toast.makeText(this, "UPDATE", Toast.LENGTH_SHORT).show();
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
        String messageText = messageInputText.getText().toString();
        messageInputText.setText("");

        Map<String, Object> data = new HashMap<>();
        data.put("message", messageText);
        data.put("read", false);
        data.put("time_sent", FieldValue.serverTimestamp());
        data.put("sent_by", FirebaseAuth.getInstance().getUid());

        messages.add(data)
                .addOnSuccessListener(documentReference -> {

                    convos.get()
                            .addOnCompleteListener(task -> {

                                if (task.isSuccessful()){
                                    ArrayList<String> array = new ArrayList<>();
                                    AtomicReference<List<String>> group = new AtomicReference<>();

                                    DocumentSnapshot document = task.getResult();
                                    group.set((List<String>) document.get("convos"));

                                    if (group.get() != null) {

                                        array.addAll(group.get());

                                        if (!array.contains(to)) {

                                            convos.update("convos", FieldValue.arrayUnion(to));

                                        }
                                    }
                                    else{

                                        Map<String, Object> newData = new HashMap<>();

                                        newData.put("convos", Collections.singletonList(to));

                                        convos.set(newData);

                                    }

                                    sendMessageNotification(messageText);

                                }

                            });

                    Log.d("TEST", "DocumentSnapshot written with ID: " + documentReference.getId());
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("TEST", "Error adding document", e);
                    }
                });

        contacts.document(to).update(FirestoreContact.FIELD_LASTMESSAGE, FieldValue.serverTimestamp());

        return true;

    }

    private void populateList(){
        messages.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Message message = document.toObject(Message.class);
                            message.setRead(document.getBoolean(Message.FIELD_READ));
                            message.setSentBy(document.getString(Message.FIELD_SENT_BY));
                            if (document.getTimestamp(Message.FIELD_TIME_SENT) == null)
                                message.setTimestamp(Timestamp.now());
                            else {
                                message.setTimestamp(document.getTimestamp(Message.FIELD_TIME_SENT));
                            }
                            chatAdapter.add(message);
                        }
                    } else {
                        Log.w("TEST", "Error getting documents.", task.getException());
                    }
                });
        messages2.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Message message = document.toObject(Message.class);
                            message.setRead(document.getBoolean(Message.FIELD_READ));
                            message.setSentBy(document.getString(Message.FIELD_SENT_BY));
                            if (document.getTimestamp(Message.FIELD_TIME_SENT) == null)
                                message.setTimestamp(Timestamp.now());
                            else {
                                message.setTimestamp(document.getTimestamp(Message.FIELD_TIME_SENT));
                            }
                            chatAdapter.add(message);
                        }
                    } else {
                        Log.w("TEST", "Error getting documents.", task.getException());
                    }
                });
    }

    public void sendMessageNotification(String message){

        String API = "https://fcm.googleapis.com/fcm/send";
        String serverKey = "key=" + getResources().getString(R.string.FCM_SERVER_KEY);;
        String contentType = "application/json";

        JSONObject notification = new JSONObject();
        JSONObject notifcationBody = new JSONObject();

        DocumentReference userDoc = FirebaseFirestore.getInstance().collection("users").document(to);

        userDoc.get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()){

                        DocumentSnapshot doc = task.getResult();

                        List<String> sendToTokens = (List<String>) doc.get("deviceTokens");

                        if (sendToTokens != null) {
                            for (String s : sendToTokens) {

                                try {

                                    notifcationBody.put("title", from);
                                    notifcationBody.put("message", message);
                                    notification.put("to", s);
                                    notification.put("data", notifcationBody);

                                } catch (JSONException e) {
                                    Log.e("TAG", "onCreate: " + e.getMessage());
                                }

                                JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, API, notification, response -> {
                                    Log.i("TEST", response.toString());
                                }, error -> {
                                    Toast.makeText(this, "Request error", Toast.LENGTH_LONG).show();
                                    Log.e("TEST", error.toString());
                                }){

                                    @Override
                                    public Map<String, String> getHeaders() throws AuthFailureError {
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

                switch (c.getType()) {

                    case ADDED:
                        Log.d("TEST", "New Message: " + c.getDocument().getData());
                        Message message = c.getDocument().toObject(Message.class);
                        message.setRead(c.getDocument().getBoolean(Message.FIELD_READ));
                        message.setSentBy(c.getDocument().getString(Message.FIELD_SENT_BY));
                        if (c.getDocument().getTimestamp(Message.FIELD_TIME_SENT) == null)
                            message.setTimestamp(Timestamp.now());
                        else {
                            message.setTimestamp(c.getDocument().getTimestamp(Message.FIELD_TIME_SENT));
                        }
                        chatAdapter.add(message);
                        break;

                }
            }

        }
    }
}
