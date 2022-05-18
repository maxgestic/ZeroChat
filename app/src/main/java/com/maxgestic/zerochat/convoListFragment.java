package com.maxgestic.zerochat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class convoListFragment extends Fragment implements EventListener<QuerySnapshot> {

    private ListView list;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    String userID = mAuth.getCurrentUser().getUid();
    CollectionReference convos = FirebaseFirestore.getInstance().collection("convos");
    CollectionReference contacts = FirebaseFirestore.getInstance().collection("users").document(userID).collection("contacts");
    ArrayList<FirestoreContact> data = new ArrayList<>();
    ConvoListAdapter adapter;
    ListenerRegistration listenerRegistration, listenerRegistration2;
    Boolean listenerOn = false, listenerOn2 = false;

    public convoListFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_convo_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        list = requireView().findViewById(R.id.convoListView);
        list.setOnItemClickListener((parent, view1, position, id) -> {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("from", mAuth.getUid());
            intent.putExtra("to", data.get(position).getID());
            startActivity(intent);
        });
//        populateList();
    }

    private void populateList(){
        ArrayList<String> array = new ArrayList<>();
        AtomicReference<List<String>> group = new AtomicReference<>();
        if (adapter != null){
            adapter.clear();
        }
            convos.document(userID).get().addOnCompleteListener(task -> {
               if (task.isSuccessful()) {
                   DocumentSnapshot document = task.getResult();
                   group.set((List<String>) document.get("convos"));
                   if (group.get() != null) {
                       array.addAll(group.get());
                       contacts.get()
                               .addOnCompleteListener(task1 -> {
                                   if (task1.isSuccessful()) {
                                       for (QueryDocumentSnapshot doc : task1.getResult()) {
                                           String contactID = doc.getId();
                                           if (array.contains(contactID)) {
                                               FirestoreContact contact = doc.toObject(FirestoreContact.class);
                                               convos.document(userID).collection(contactID).orderBy("time_sent", Query.Direction.DESCENDING).limit(1).get()
                                                       .addOnCompleteListener(task2 -> {
                                                           if (task2.isSuccessful() && task2.getResult() != null){
                                                               for (QueryDocumentSnapshot doc2 : task2.getResult()) {
                                                                   convos.document(contactID).collection(userID).orderBy("time_sent", Query.Direction.DESCENDING).limit(1).get()
                                                                           .addOnCompleteListener(task3 -> {
                                                                               if (task3.isSuccessful() && task3.getResult() != null){
                                                                                   for (QueryDocumentSnapshot doc3 : task3.getResult()) {
                                                                                       Message message = doc2.toObject(Message.class);
                                                                                       message.setTimestamp(doc2.getTimestamp("time_sent"));
                                                                                       Message message2 = doc3.toObject(Message.class);
                                                                                       message2.setTimestamp(doc3.getTimestamp("time_sent"));
                                                                                       if (message.getTimestamp().compareTo(message2.getTimestamp()) > 0) {
                                                                                           contact.setMessagePrev(message.getMessage());
                                                                                       }else{
                                                                                           contact.setMessagePrev(message2.getMessage());
                                                                                       }
                                                                                       contact.setID(doc.getId());
                                                                                       contact.setNick(doc.getString("nickname"));
                                                                                       contact.setLastMessage(doc.getTimestamp("lastMessage"));
                                                                                       data.add(contact);
                                                                                   }
                                                                                   adapter = new ConvoListAdapter(getActivity(), data);
                                                                                   adapter.sort(FirestoreContact.byDate);
                                                                                   list.setAdapter(adapter);
                                                                                   if (!listenerOn) {
                                                                                       listenerRegistration = convos.whereEqualTo("id", userID).addSnapshotListener(this);
                                                                                       listenerOn = true;
                                                                                   }
                                                                                   if (!listenerOn2) {
                                                                                       listenerRegistration2 = convos.document(contactID).collection(userID).addSnapshotListener(this::messageListener);
                                                                                       listenerOn2 = false;
                                                                                   }
                                                                               }
                                                                           });
                                                               }
                                                           }
                                                       });
                                           }
                                       }
                                   }
                               });
                   }
               }
            });
    }

    private void messageListener(QuerySnapshot queryDocumentSnapshots, FirebaseFirestoreException e) {
        if (queryDocumentSnapshots != null) {
            for (DocumentChange c : queryDocumentSnapshots.getDocumentChanges()) {
                switch (c.getType()) {
                    case ADDED:
                        Message message = c.getDocument().toObject(Message.class);
                        message.setMessage(c.getDocument().getString("message"));
                        message.setTimestamp(c.getDocument().getTimestamp("time_sent"));
                        message.setSentBy(c.getDocument().getString("sent_by"));
                        for (int i = 0; i < adapter.getCount(); i++) {
                            FirestoreContact contact = adapter.getItem(i);
                            String id = contact.getID();
                            if (message.getSentBy().equals(id)) {
                                if (message.getTimestamp().compareTo(contact.getLastMessage()) > 0) {
                                    adapter.remove(contact);
                                    contact.setMessagePrev(message.getMessage());
                                    contact.setLastMessage(message.getTimestamp());
                                    adapter.add(contact);
                                    adapter.sort(FirestoreContact.byDate);
                                    list.setAdapter(adapter);
                                }
                            }
                        }
                }
            }
        }
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException error) {
        ArrayList<String> array = new ArrayList<>();
        AtomicReference<List<String>> group = new AtomicReference<>();
        assert queryDocumentSnapshots != null;
        for (DocumentChange c: queryDocumentSnapshots.getDocumentChanges()) {
            if (adapter != null){
                adapter.clear();
            }
            switch (c.getType()) {
                case ADDED:
                    group.set((List<String>) c.getDocument().getData().get("convos"));
                    if (group.get() != null) {
                        array.addAll(group.get());
                        contacts.get()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        for (QueryDocumentSnapshot doc : task1.getResult()) {
                                            String contactID = doc.getId();
                                            if (array.contains(contactID)) {
                                                FirestoreContact contact = doc.toObject(FirestoreContact.class);
                                                convos.document(userID).collection(contactID).orderBy("time_sent", Query.Direction.DESCENDING).limit(1).get()
                                                        .addOnCompleteListener(task2 -> {
                                                            if (task2.isSuccessful() && task2.getResult() != null){
                                                                for (QueryDocumentSnapshot doc2 : task2.getResult()) {
                                                                    convos.document(contactID).collection(userID).orderBy("time_sent", Query.Direction.DESCENDING).limit(1).get()
                                                                            .addOnCompleteListener(task3 -> {
                                                                                if (task3.isSuccessful() && task3.getResult() != null){
                                                                                    for (QueryDocumentSnapshot doc3 : task3.getResult()) {
                                                                                        Message message = doc2.toObject(Message.class);
                                                                                        message.setTimestamp(doc2.getTimestamp("time_sent"));
                                                                                        Message message2 = doc3.toObject(Message.class);
                                                                                        message2.setTimestamp(doc3.getTimestamp("time_sent"));
                                                                                        if (message.getTimestamp().compareTo(message2.getTimestamp()) > 0) {
                                                                                            contact.setMessagePrev(message.getMessage());
                                                                                        }else{
                                                                                            contact.setMessagePrev(message2.getMessage());
                                                                                        }
                                                                                        contact.setID(doc.getId());
                                                                                        contact.setNick(doc.getString("nickname"));
                                                                                        contact.setLastMessage(doc.getTimestamp("lastMessage"));
                                                                                        data.add(contact);
                                                                                    }
                                                                                    adapter = new ConvoListAdapter(getActivity(), data);
                                                                                    adapter.sort(FirestoreContact.byDate);
                                                                                    list.setAdapter(adapter);
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    }
                                });
                    }
                    break;

                case MODIFIED:
                    Log.d("TEST", "Edit Convos: " + c.getDocument().getData().get("convos"));
                    group.set((List<String>) c.getDocument().getData().get("convos"));
                    if (group.get() != null) {
                        array.addAll(group.get());
                        contacts.get()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        for (QueryDocumentSnapshot doc : task1.getResult()) {
                                            String contactID = doc.getId();
                                            if (array.contains(contactID)) {
                                                FirestoreContact contact = doc.toObject(FirestoreContact.class);
                                                convos.document(userID).collection(contactID).orderBy("time_sent", Query.Direction.DESCENDING).limit(1).get()
                                                        .addOnCompleteListener(task2 -> {
                                                            if (task2.isSuccessful() && task2.getResult() != null){
                                                                for (QueryDocumentSnapshot doc2 : task2.getResult()) {
                                                                    convos.document(contactID).collection(userID).orderBy("time_sent", Query.Direction.DESCENDING).limit(1).get()
                                                                            .addOnCompleteListener(task3 -> {
                                                                                if (task3.isSuccessful() && task3.getResult() != null){
                                                                                    for (QueryDocumentSnapshot doc3 : task3.getResult()) {
                                                                                        Message message = doc2.toObject(Message.class);
                                                                                        message.setTimestamp(doc2.getTimestamp("time_sent"));
                                                                                        Message message2 = doc3.toObject(Message.class);
                                                                                        message2.setTimestamp(doc3.getTimestamp("time_sent"));
                                                                                        if (message.getTimestamp().compareTo(message2.getTimestamp()) > 0) {
                                                                                            contact.setMessagePrev(message.getMessage());

                                                                                        }else{
                                                                                            contact.setMessagePrev(message2.getMessage());
                                                                                        }
                                                                                        contact.setID(doc.getId());
                                                                                        contact.setNick(doc.getString("nickname"));
                                                                                        contact.setLastMessage(doc.getTimestamp("lastMessage"));
                                                                                        data.add(contact);
                                                                                    }
                                                                                    adapter = new ConvoListAdapter(getActivity(), data);
                                                                                    adapter.sort(FirestoreContact.byDate);
                                                                                    list.setAdapter(adapter);
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    }
                                });
                        break;
                    }
            }
        }
    }

    @Override
    public void onResume() {
        populateList();
        super.onResume();
    }

    @Override
    public void onPause() {
        listenerRegistration.remove();
        listenerRegistration2.remove();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        listenerRegistration.remove();
        listenerRegistration2.remove();
        super.onDestroy();
    }
}