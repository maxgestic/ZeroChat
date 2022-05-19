package com.maxgestic.zerochat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Auth extends AppCompatActivity {

    private static final String TAG = "TEST";
    private EditText inputEmail, inputPassword;
    private Button btnLogin, btnRegister;
    private final String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
    private ProgressBar progressBar;
    private TextView socialDevTxt;
    private ConstraintLayout layout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mStore;
    private SignInClient oneTapClient;
    private Boolean showOneTapUI = true;

    private final ActivityResultLauncher<IntentSenderRequest> loginResultHandler = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
        // handle intent result here
        if (result.getResultCode() == RESULT_OK) Log.d(TAG, "RESULT_OK.");
        if (result.getResultCode() == RESULT_CANCELED) Log.d(TAG, "RESULT_CANCELED.");
        if (result.getResultCode() == RESULT_FIRST_USER) Log.d(TAG, "RESULT_FIRST_USER.");
        try {
            SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(result.getData());
            String idToken = credential.getGoogleIdToken();
            String username = credential.getId();
            String password = credential.getPassword();
            //login if saved username and password filled in from one tap
            if (password != null){
                progressBar.setVisibility(View.VISIBLE);
                btnLogin.setVisibility(View.GONE);
                btnRegister.setVisibility(View.GONE);
                ConstraintSet set = new ConstraintSet();
                set.clone(layout);
                set.connect(socialDevTxt.getId(), ConstraintSet.TOP, progressBar.getId(), ConstraintSet.BOTTOM, 100);
                set.applyTo(layout);

                mAuth.signInWithEmailAndPassword(username, password).addOnCompleteListener(task -> {
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
            //login if token from google login from one tap
            if (idToken != null){
                progressBar.setVisibility(View.VISIBLE);
                btnLogin.setVisibility(View.GONE);
                btnRegister.setVisibility(View.GONE);
                ConstraintSet set = new ConstraintSet();
                set.clone(layout);
                set.connect(socialDevTxt.getId(), ConstraintSet.TOP, progressBar.getId(), ConstraintSet.BOTTOM, 100);
                set.applyTo(layout);
                // Got an ID token from Google. Use it to authenticate
                // with Firebase.
                AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                mAuth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(Auth.this, "Login Successfully", Toast.LENGTH_SHORT).show();
                                    DocumentReference documentReference = mStore.collection("users").document(Objects.requireNonNull(mAuth.getCurrentUser()).getUid());
                                    documentReference.get()
                                            .addOnCompleteListener(task1 ->{

                                                if (!task1.getResult().exists()){
                                                    Map<String, Object> user = new HashMap<>();
                                                    user.put("email", username);
                                                    documentReference.set(user);
                                                }

                                            });
                                    StartNewActivity();
                                } else {
                                    // If sign in fails, display a message to the user.
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(Auth.this, ""+task.getException(), Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "" + task.getException());
                                    btnLogin.setVisibility(View.VISIBLE);
                                    btnRegister.setVisibility(View.VISIBLE);
                                    set.clone(layout);
                                    set.connect(socialDevTxt.getId(), ConstraintSet.TOP, btnRegister.getId(), ConstraintSet.BOTTOM, 78);
                                    set.applyTo(layout);
                                }
                            }
                        });
            }


        } catch (ApiException e) {
            switch (e.getStatusCode()) {
                case CommonStatusCodes.CANCELED:
                    Log.d(TAG, "One-tap dialog was closed.");
                    showOneTapUI = false;
                    break;
                case CommonStatusCodes.NETWORK_ERROR:
                    Log.d(TAG, "One-tap encountered a network error.");
                    break;
                default:
                    Log.d(TAG, "Couldn't get credential from result."
                            + e.getLocalizedMessage());
                    break;
            }
        }
    });
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        //find all the views and setup the page
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
        FirebaseUser mUser = mAuth.getCurrentUser();
        btnRegister.setOnClickListener(v -> RegisterUser());
        btnLogin.setOnClickListener(v -> LoginUser());
        //check if user is logged in and if so start HomeActivity via intent
        if(mUser != null){
            //TODO: Program if device token is not in deviceTokens array in firestore then logout user and make them log in again, this is to implement a device manager later on where users can logout of other devices remotely
            StartNewActivity();
        }

        SignInButton googleSignInBtn = findViewById(R.id.googleSignInButton);
        googleSignInBtn.setSize(SignInButton.SIZE_ICON_ONLY);
        googleSignInBtn.setColorScheme(SignInButton.COLOR_AUTO);
        googleSignInBtn.setOnClickListener(v -> GoogleLogin());

        if (showOneTapUI) {
            GoogleLogin();
        }

    }

    private void GoogleLogin(){
        oneTapClient = Identity.getSignInClient(this);
        BeginSignInRequest signInRequest = BeginSignInRequest.builder()
                .setPasswordRequestOptions(BeginSignInRequest.PasswordRequestOptions.builder()
                        .setSupported(true)
                        .build())
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId("746704851947-8764i6nl3ipop62fptctaihct2rf1nga.apps.googleusercontent.com")
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .setAutoSelectEnabled(true)
                .build();

        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this, beginSignInResult -> {
                    try {
                        loginResultHandler.launch(new IntentSenderRequest.Builder(beginSignInResult.getPendingIntent().getIntentSender()).build());
                    } catch(android.content.ActivityNotFoundException e){
                        e.printStackTrace();
                        Log.e(TAG, "Couldn't start One Tap UI: " + e.getLocalizedMessage());
                    }
                }).addOnFailureListener(this, e -> Log.d(TAG, e.getLocalizedMessage()));
    }

    private void LoginUser() {
        String email = inputEmail.getText().toString().toLowerCase();
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
                    Log.e(TAG, "" + task.getException());
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
        String email = inputEmail.getText().toString().toLowerCase();
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
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
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
                    Log.e(TAG, "" + task.getException());
                    btnLogin.setVisibility(View.VISIBLE);
                    btnRegister.setVisibility(View.VISIBLE);
                    set.clone(layout);
                    set.connect(socialDevTxt.getId(), ConstraintSet.TOP, btnRegister.getId(), ConstraintSet.BOTTOM, 78);
                    set.applyTo(layout);
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