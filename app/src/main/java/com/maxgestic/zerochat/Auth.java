package com.maxgestic.zerochat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class Auth extends AppCompatActivity {

    EditText inputEmail, inputPassword;
    Button btnLogin, btnRegister;
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    ProgressBar progressBar;
    TextView socialDevTxt;
    ConstraintLayout layout;
    FirebaseAuth mAuth;
    FirebaseUser mUser;
    FirebaseFirestore mStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        socialDevTxt = findViewById(R.id.socialDevTxt);
        layout = findViewById(R.id.constraintLayout);
        ConstraintSet set = new ConstraintSet();
        progressBar = new ProgressBar(this);
        progressBar.setId(View.generateViewId());
        layout.addView(progressBar,0);
        set.clone(layout);
        set.connect(progressBar.getId(), ConstraintSet.TOP, R.id.inputPassword, ConstraintSet.BOTTOM, 100);
        set.connect(progressBar.getId(), ConstraintSet.LEFT, layout.getId(), ConstraintSet.LEFT, 0);
        set.connect(progressBar.getId(), ConstraintSet.RIGHT, layout.getId(), ConstraintSet.RIGHT, 0);
        set.applyTo(layout);
        progressBar.setVisibility(View.GONE);
        mAuth = FirebaseAuth.getInstance();
        mStore = FirebaseFirestore.getInstance();
        mUser = mAuth.getCurrentUser();
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RegisterUser();
            }
        });
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { LoginUser(); }
        });
        //check if user is logged in and if so start HomeActivity via intent
        if(mUser != null){
            StartNewActivity();
        }
    }

    private void LoginUser() {
        String email = inputEmail.getText().toString();
        String password = inputPassword.getText().toString();
        if(!email.matches(emailPattern))
        {
            inputEmail.setError("Invalid Email");
        }else if (password.isEmpty() || password.length() < 8) {
            inputPassword.setError("Enter a valid password");
        }
        else {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.GONE);
            btnRegister.setVisibility(View.GONE);
            ConstraintSet set = new ConstraintSet();
            set.clone(layout);
            set.connect(socialDevTxt.getId(), ConstraintSet.TOP, progressBar.getId(), ConstraintSet.BOTTOM, 100);
            set.applyTo(layout);
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(Auth.this, "Login Successfully", Toast.LENGTH_SHORT).show();
                    StartNewActivity();
                } else {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(Auth.this, ""+task.getException(), Toast.LENGTH_SHORT).show();
                    btnLogin.setVisibility(View.VISIBLE);
                    btnRegister.setVisibility(View.VISIBLE);
                    set.clone(layout);
                    set.connect(socialDevTxt.getId(), ConstraintSet.TOP, btnRegister.getId(), ConstraintSet.BOTTOM, 78);
                    set.applyTo(layout);
                }
            });
        }
    }

    private void RegisterUser() {
        String email = inputEmail.getText().toString();
        String password = inputPassword.getText().toString();
        if(!email.matches(emailPattern))
        {
            inputEmail.setError("Invalid Email");
        }else if (password.isEmpty() || password.length() < 8) {
            inputPassword.setError("Enter password that is more than 8 characters");
        }
        else {
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setVisibility(View.GONE);
            btnRegister.setVisibility(View.GONE);
            ConstraintSet set = new ConstraintSet();
            set.clone(layout);
            set.connect(socialDevTxt.getId(), ConstraintSet.TOP, progressBar.getId(), ConstraintSet.BOTTOM, 100);
            set.applyTo(layout);
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(Auth.this, "Registered Successfully", Toast.LENGTH_SHORT).show();
                        DocumentReference documentReference = mStore.collection("users").document(Objects.requireNonNull(mAuth.getCurrentUser()).getUid());
                        Map<String, Object> user = new HashMap<>();
                        user.put("email", email);
                        documentReference.set(user);
                        StartNewActivity();
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(Auth.this, ""+task.getException(), Toast.LENGTH_SHORT).show();
                        btnLogin.setVisibility(View.VISIBLE);
                        btnRegister.setVisibility(View.VISIBLE);
                        set.clone(layout);
                        set.connect(socialDevTxt.getId(), ConstraintSet.TOP, btnRegister.getId(), ConstraintSet.BOTTOM, 78);
                        set.applyTo(layout);
                    }
                }
            });

        }

    }

    private void StartNewActivity() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("TEST", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();

                    // Get new FCM registration token
                    String userID = FirebaseAuth.getInstance().getUid();
                    assert userID != null;
                    DocumentReference userDoc = FirebaseFirestore.getInstance().collection("users").document(userID);
                    userDoc.get()
                            .addOnCompleteListener(task1 -> {
                                if (task1.isSuccessful()){
                                    List<String> tokens = (List<String>) task1.getResult().get("deviceTokens");
                                    if (tokens != null){
                                        if (!tokens.contains(token)){
                                            userDoc.update("deviceTokens", FieldValue.arrayUnion(token));
                                        }
                                    }
                                    else{
                                        userDoc.update("deviceTokens", FieldValue.arrayUnion(token));
                                    }
                                }
                            });
                });
        Intent intent = new Intent(Auth.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}