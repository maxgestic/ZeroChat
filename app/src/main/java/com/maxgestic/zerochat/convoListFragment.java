package com.maxgestic.zerochat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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
    Boolean paused = false;

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

    private void messageListener(QuerySnapshot queryDocumentSnapshots, FirebaseFirestoreException e) {
        Log.d("TEST", "UPDATE");
        if (queryDocumentSnapshots != null && !paused) {
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
                                    if (!paused) {
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
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException error) {
        ArrayList<String> array = new ArrayList<>();
        AtomicReference<List<String>> group = new AtomicReference<>();
        AtomicReference<Integer> counter = new AtomicReference<>(0);
        AtomicReference<Integer> counter2 = new AtomicReference<>(0);
        AtomicReference<Integer> counter3 = new AtomicReference<>(0);
        AtomicReference<Integer> counter4 = new AtomicReference<>(0);
        assert queryDocumentSnapshots != null;
        for (DocumentChange change: queryDocumentSnapshots.getDocumentChanges()) {
            if (adapter != null){
                adapter.clear();
            }
            switch (change.getType()) {
                case ADDED:
                    group.set((List<String>) change.getDocument().getData().get("convos"));
                    if (group.get() != null && !paused) {
                        array.addAll(group.get());
                        contacts.get()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful() && !paused) {
                                        for (QueryDocumentSnapshot doc : task1.getResult()) {
                                            String contactID = doc.getId();
                                            if (array.contains(contactID) && !paused) {
                                                counter.set(counter.get()+1);
                                                FirestoreContact contact = doc.toObject(FirestoreContact.class);
                                                convos.document(userID).collection(contactID).orderBy("time_sent", Query.Direction.DESCENDING).limit(1).get()
                                                        .addOnCompleteListener(task2 -> {
                                                            if (task2.isSuccessful() && task2.getResult() != null && !paused){
                                                                for (QueryDocumentSnapshot doc2 : task2.getResult()) {
                                                                    convos.document(contactID).collection(userID).orderBy("time_sent", Query.Direction.DESCENDING).limit(1).get()
                                                                            .addOnCompleteListener(task3 -> {
                                                                                if (task3.getResult().isEmpty() && !paused){
//                                                                                    Log.d("TEST", "ADDING");
                                                                                    counter2.set(counter2.get()+1);
                                                                                }
                                                                                if (task3.isSuccessful() && task3.getResult() != null && !paused) {
                                                                                    for (QueryDocumentSnapshot doc3 : task3.getResult()) {
                                                                                        Message message = doc2.toObject(Message.class);
                                                                                        message.setTimestamp(doc2.getTimestamp("time_sent"));
                                                                                        Message message2 = doc3.toObject(Message.class);
                                                                                        message2.setTimestamp(doc3.getTimestamp("time_sent"));
                                                                                        if (message.getTimestamp().compareTo(message2.getTimestamp()) > 0) {
                                                                                            contact.setMessagePrev(message.getMessage());
                                                                                        } else {
                                                                                            contact.setMessagePrev(message2.getMessage());
                                                                                        }
                                                                                        contact.setID(doc.getId());
                                                                                        contact.setNick(doc.getString("nickname"));
                                                                                        contact.setLastMessage(doc.getTimestamp("lastMessage"));
                                                                                        data.add(contact);
//                                                                                        Log.d("TEST", "ADDING");
                                                                                        counter2.set(counter2.get()+1);
                                                                                        counter4.set(counter4.get()+1);
                                                                                    }
                                                                                }

//                                                                                Log.d("TEST", "C1 " + counter + " C2 " + counter2);

                                                                                if (counter.get().equals(counter2.get()) && !paused) {

                                                                                    FirebaseStorage storage = FirebaseStorage.getInstance();
                                                                                    StorageReference storageRef = storage.getReference();
                                                                                    counter3.set(0);

                                                                                    for (FirestoreContact c : data){

                                                                                        StorageReference pathRef = storageRef.child("profilePics/"+c.getID()+".jpg");
//                                                                                        Log.d("TEST", pathRef.toString());

                                                                                        final long ONE_MEGABYTE = 1024 * 1024;

                                                                                        pathRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(bytes -> {
                                                                                            if (bytes != null && !paused){

                                                                                                FirestoreContact cont = c;
//                                                                                                Log.d("TEST", bytes.length+ "");
                                                                                                data.remove(c);
                                                                                                cont.setImageBytes(bytes);
                                                                                                data.add(cont);
                                                                                                counter3.set(counter3.get()+1);

                                                                                                if (counter3.get().equals(counter4.get())) {

                                                                                                  if (!paused) {
                                                                                                      adapter = new ConvoListAdapter(getActivity(), data);
                                                                                                      adapter.sort(FirestoreContact.byDate);
                                                                                                      list.setAdapter(adapter);
                                                                                                  }

                                                                                                }

                                                                                            }
                                                                                        }).addOnFailureListener(exception -> {
                                                                                            Log.e("TEST", exception.getMessage());
                                                                                            counter3.set(counter3.get()+1);

                                                                                            if (counter3.get().equals(counter4.get())) {

                                                                                                if (!paused) {
                                                                                                    adapter = new ConvoListAdapter(getActivity(), data);
                                                                                                    adapter.sort(FirestoreContact.byDate);
                                                                                                    list.setAdapter(adapter);
                                                                                                }

                                                                                            }
                                                                                        });
                                                                                    };

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
                    Log.d("TEST", "Edit Convos: " + change.getDocument().getData().get("convos"));
                    group.set((List<String>) change.getDocument().getData().get("convos"));
                    if (group.get() != null && !paused) {
                        array.addAll(group.get());
                        contacts.get()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful() && !paused) {
                                        for (QueryDocumentSnapshot doc : task1.getResult()) {
                                            String contactID = doc.getId();
                                            if (array.contains(contactID) && !paused) {
                                                counter.set(counter.get()+1);
                                                FirestoreContact contact = doc.toObject(FirestoreContact.class);
                                                convos.document(userID).collection(contactID).orderBy("time_sent", Query.Direction.DESCENDING).limit(1).get()
                                                        .addOnCompleteListener(task2 -> {
                                                            if (task2.isSuccessful() && task2.getResult() != null && !paused){
                                                                for (QueryDocumentSnapshot doc2 : task2.getResult()) {
                                                                    convos.document(contactID).collection(userID).orderBy("time_sent", Query.Direction.DESCENDING).limit(1).get()
                                                                            .addOnCompleteListener(task3 -> {
                                                                                if (task3.getResult().isEmpty() && !paused){
//                                                                                    Log.d("TEST", "ADDING");
                                                                                    counter2.set(counter2.get()+1);
                                                                                }
                                                                                if (task3.isSuccessful() && task3.getResult() != null && !paused) {
                                                                                    for (QueryDocumentSnapshot doc3 : task3.getResult()) {
                                                                                        Message message = doc2.toObject(Message.class);
                                                                                        message.setTimestamp(doc2.getTimestamp("time_sent"));
                                                                                        Message message2 = doc3.toObject(Message.class);
                                                                                        message2.setTimestamp(doc3.getTimestamp("time_sent"));
                                                                                        if (message.getTimestamp().compareTo(message2.getTimestamp()) > 0) {
                                                                                            contact.setMessagePrev(message.getMessage());
                                                                                        } else {
                                                                                            contact.setMessagePrev(message2.getMessage());
                                                                                        }
                                                                                        contact.setID(doc.getId());
                                                                                        contact.setNick(doc.getString("nickname"));
                                                                                        contact.setLastMessage(doc.getTimestamp("lastMessage"));
                                                                                        data.add(contact);
//                                                                                        Log.d("TEST", "ADDING");
                                                                                        counter2.set(counter2.get()+1);
                                                                                        counter4.set(counter4.get()+1);
                                                                                    }
                                                                                }

//                                                                                Log.d("TEST", "C1 " + counter + " C2 " + counter2);

                                                                                if (counter.get().equals(counter2.get()) && !paused) {

                                                                                    FirebaseStorage storage = FirebaseStorage.getInstance();
                                                                                    StorageReference storageRef = storage.getReference();
                                                                                    counter3.set(0);

                                                                                    for (FirestoreContact c : data){

                                                                                        StorageReference pathRef = storageRef.child("profilePics/"+c.getID()+".jpg");
//                                                                                        Log.d("TEST", pathRef.toString());

                                                                                        final long ONE_MEGABYTE = 1024 * 1024;

                                                                                        pathRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(bytes -> {
                                                                                            if (bytes != null && !paused){

                                                                                                FirestoreContact cont = c;
//                                                                                                Log.d("TEST", bytes.length+ "");
                                                                                                data.remove(c);
                                                                                                cont.setImageBytes(bytes);
                                                                                                data.add(cont);
                                                                                                counter3.set(counter3.get()+1);

                                                                                                if (counter3.get().equals(counter4.get())) {

                                                                                                    if (!paused) {
                                                                                                        adapter = new ConvoListAdapter(getActivity(), data);
                                                                                                        adapter.sort(FirestoreContact.byDate);
                                                                                                        list.setAdapter(adapter);
                                                                                                    }

                                                                                                }

                                                                                            }
                                                                                        }).addOnFailureListener(exception -> {
                                                                                            Log.e("TEST", exception.getMessage());
                                                                                            counter3.set(counter3.get()+1);

                                                                                            if (counter3.get().equals(counter4.get())) {

                                                                                                if (!paused) {
                                                                                                    adapter = new ConvoListAdapter(getActivity(), data);
                                                                                                    adapter.sort(FirestoreContact.byDate);
                                                                                                    list.setAdapter(adapter);
                                                                                                }

                                                                                            }
                                                                                        });
                                                                                    };

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
        if (!listenerOn) {
//            Log.d("TEST", "STARTIN LIST");
            listenerRegistration = convos.whereEqualTo("id", userID).addSnapshotListener(this);
            listenerOn = true;
        }
//        Log.d("TEST", "STARTIN LIST");
        if (!listenerOn2) {
            for (FirestoreContact contact2 : data) {
                listenerRegistration2 = convos.document(contact2.getID()).collection(userID).addSnapshotListener(this::messageListener);
            }
            listenerOn2 = true;
        }
        paused = false;
        super.onResume();
    }

    @Override
    public void onStop() {
//        Log.d("TEST", "PAUSE");
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerOn = false;
        }
        if(listenerRegistration2!=null) {
            listenerRegistration2.remove();
            listenerOn2 = false;
        }
        paused = true;
        super.onStop();
    }

    @Override
    public void onDestroy() {
        paused = true;
//        Log.d("TEST", "STOP");
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerOn = false;
        }
        if(listenerRegistration2!=null) {
            listenerRegistration2.remove();
            listenerOn2 = false;
        }
        super.onDestroy();
    }
}