package com.maxgestic.zerochat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.BottomNavigationViewKt;
import androidx.navigation.ui.NavigationUI;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();

        final NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        final NavController navController = navHostFragment.getNavController();
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

    }

    public void logout(View view) {

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
                                if (task1.isSuccessful()) {
                                    userDoc.update("deviceTokens", FieldValue.arrayRemove(token));
                                }

                                mAuth.signOut();

                                Intent intent = new Intent(HomeActivity.this, Auth.class);
                                startActivity(intent);
                                finish();
                            });
                });

    }
}