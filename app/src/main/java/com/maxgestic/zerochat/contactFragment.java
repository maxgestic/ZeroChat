package com.maxgestic.zerochat;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.util.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class contactFragment extends Fragment implements EventListener<QuerySnapshot>{

    private ListView list;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    String userID = mAuth.getCurrentUser().getUid();
    CollectionReference contacts = FirebaseFirestore.getInstance().collection("users").document(userID).collection("contacts");
    ArrayList<FirestoreContact> data = new ArrayList<>();
    ContactsAdapter adapter;
    ListenerRegistration listenerRegistration;
    Boolean paused = false;


    public contactFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        list = requireView().findViewById(R.id.contactList);
        registerForContextMenu(list);
        Log.d("TEST", "Path:" + contacts);
        listenerRegistration = contacts.addSnapshotListener(this);
        populateList();

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(getActivity(), "Test: " + data.get(position).getEmail(), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("from", mAuth.getUid());
                intent.putExtra("to", data.get(position).getID());
                startActivity(intent);
            }
        });

        FloatingActionButton addContact = requireView().findViewById(R.id.addButton);
        addContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addContactDialog();
            }
        });


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_contact, container, false);
    }

    private void populateList(){
        contacts.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !paused) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d("TEST", document.getId() + " => " + document.getData());
                            FirestoreContact contact = document.toObject(FirestoreContact.class);
                            contact.setID(document.getId());
                            contact.setNick(document.getString("nickname"));
                            data.add(contact);
                        }
                        if (data != null && !paused) {
                            adapter = new ContactsAdapter(getActivity(), data);
                            list.setAdapter(adapter);
                        }
                    } else {
                        Log.w("TEST", "Error getting documents.", task.getException());
                    }
                });
    }

    public void addContactDialog(){

        final Dialog dialog = new Dialog(getActivity());

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.add_contact_dialog);

        final EditText nick = dialog.findViewById(R.id.addContactNick);
        final EditText email = dialog.findViewById(R.id.addContactEmail);
        Button add = dialog.findViewById(R.id.addContactButton);

        add.setOnClickListener(v -> {
            String nickName = nick.getText().toString();
            String emailAddress = email.getText().toString();

            if(!nickName.isEmpty() && !emailAddress.isEmpty()){
                addContactToFirebase(nickName, emailAddress);
                dialog.dismiss();
            }else{
                Toast.makeText(getActivity(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();

    }

    public void editContactDialog(String email){

        final Dialog dialog = new Dialog(getActivity());

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.edit_contact_dialog);

        final EditText nick = dialog.findViewById(R.id.editContactNick);
        Button edit = dialog.findViewById(R.id.editContactButton);

        edit.setOnClickListener(v -> {
            String nickName = nick.getText().toString();
            if(!nickName.isEmpty()){
                editContactInFirebase(email, nickName);
                dialog.dismiss();
            }
            else{
                Toast.makeText(getActivity(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();

    }

    private void addContactToFirebase(String nick, String email){
        CollectionReference users = FirebaseFirestore.getInstance().collection("users");
        /* Construct a map of key-value pairs */
        Map<String, Object> contact = new HashMap<>();
        contact.put(FirestoreContact.FIELD_NICKNAME, nick);
        contact.put(FirestoreContact.FIELD_EMAIL, email);

        //check if contact with email already exists
        contacts.whereEqualTo(FirestoreContact.FIELD_EMAIL, email).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            users.whereEqualTo(FirestoreContact.FIELD_EMAIL, email).get()
                                    .addOnCompleteListener(task2 -> {
                                        if (task2.isSuccessful()) {
                                            if (task2.getResult().isEmpty()) {
                                                Toast.makeText(getActivity(), "User does not exist", Toast.LENGTH_SHORT).show();
                                            }
                                            for (QueryDocumentSnapshot document : task2.getResult()) {
                                                Log.d("TEST", document.getId() + " => " + document.getData());
                                                contacts.document(document.getId()).set(contact);
                                                Toast.makeText(getActivity(), "Contact added!", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Log.d("TEST", "Error getting documents.", task2.getException());
                                        }
                                    });
                        }
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d("TEST", document.getId() + " => " + document.getData());
                            Toast.makeText(getActivity(), "Contact already exists", Toast.LENGTH_SHORT).show();
                        }
                    }
                });


    }

    private void editContactInFirebase(String email, String nick){

        contacts.whereEqualTo(FirestoreContact.FIELD_EMAIL, email).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            Toast.makeText(getActivity(), "Contact does not exist", Toast.LENGTH_SHORT).show();
                        }
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d("TEST", document.getId() + " => " + document.getData());
                            contacts.document(document.getId()).update(FirestoreContact.FIELD_NICKNAME, nick);
                            Toast.makeText(getActivity(), "Contact edited!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d("TEST", "Error getting documents.", task.getException());
                    }
                });

    }

    private void deleteContact(String email){
        contacts.whereEqualTo(FirestoreContact.FIELD_EMAIL, email).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            Toast.makeText(getActivity(), "Contact does not exist", Toast.LENGTH_SHORT).show();
                        }
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            contacts.document(document.getId()).delete();
                            Toast.makeText(getActivity(), "Contact deleted!", Toast.LENGTH_SHORT).show();
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
        if (error != null) {
            Log.w("TEST", "Listen failed.", error);
        }

        if (data != null && adapter != null && !paused) {

            adapter.clear();

            if (value != null && !value.isEmpty() && !paused) {
                data = new ArrayList<>();
                if (!paused) {

                    for (QueryDocumentSnapshot document : value) {

                        FirestoreContact contact = document.toObject(FirestoreContact.class);
                        contact.setID(document.getId());
                        contact.setNick(document.getString("nickname"));
                        data.add(contact);

                    }

                    adapter.addAll(data);
                }

            }

            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId()==R.id.contactList) {
            MenuInflater inflater = requireActivity().getMenuInflater();
            inflater.inflate(R.menu.contact_long_menu, menu);
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch(item.getItemId()) {
            case R.id.edit_contact:
                Toast.makeText(getActivity(), "Edit", Toast.LENGTH_SHORT).show();
                editContactDialog(data.get(info.position).getEmail());
                return true;
            case R.id.delete_contact:
                Toast.makeText(getActivity(), "Delete", Toast.LENGTH_SHORT).show();
                deleteContact(data.get(info.position).getEmail());
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onStop() {
        listenerRegistration.remove();
        paused = true;
        super.onStop();
    }

    @Override
    public void onDestroy() {
        listenerRegistration.remove();
        paused = true;
        super.onDestroy();
    }
}