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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
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
    ContactsAdapter adapter;
    ListenerRegistration listenerRegistration;

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
                                               contact.setID(doc.getId());
                                               contact.setNick(doc.getString("nickname"));
                                               contact.setLastMessage(doc.getTimestamp("lastMessage"));
                                               data.add(contact);
                                           }
                                       }
                                       adapter = new ContactsAdapter(getActivity(), data);
                                       adapter.sort(FirestoreContact.byDate);
                                       list.setAdapter(adapter);
                                       listenerRegistration = convos.whereEqualTo("id", userID).addSnapshotListener(this);
                                   }
                               });
                   }
               }
            });
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
                    Log.d("TEST", "New Convos: " + c.getDocument().getData().get("convos"));
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
                                                contact.setID(doc.getId());
                                                contact.setNick(doc.getString("nickname"));
                                                contact.setLastMessage(doc.getTimestamp("lastMessage"));
                                                data.add(contact);
                                            }
                                        }
                                        adapter = new ContactsAdapter(getActivity(), data);
                                        adapter.sort(FirestoreContact.byDate);
                                        list.setAdapter(adapter);
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
                                                contact.setID(doc.getId());
                                                contact.setNick(doc.getString("nickname"));
                                                contact.setLastMessage(doc.getTimestamp("lastMessage"));
                                                data.add(contact);
                                            }
                                        }
                                        adapter = new ContactsAdapter(getActivity(), data);
                                        adapter.sort(FirestoreContact.byDate);
                                        list.setAdapter(adapter);
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
        super.onPause();
    }

    @Override
    public void onDestroy() {
        listenerRegistration.remove();
        super.onDestroy();
    }
}