package com.maxgestic.zerochat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();

        final NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        assert navHostFragment != null;
        final NavController navController = navHostFragment.getNavController();
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        checkPerms();

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

    protected void checkPerms() {

        int checkResult1 = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
        int checkResult2 = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);

        if (checkResult1 != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        } else if (checkResult2 != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 2);

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantedResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantedResults);
        switch (requestCode) {
            case 1: {
                if (grantedResults.length > 0 && grantedResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Thanks for that", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "You need to grant this", Toast.LENGTH_SHORT).show();
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                }
                break;
            }
            case 2: {
                if (grantedResults.length > 0 && grantedResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Thanks for that", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "You need to grant this", Toast.LENGTH_SHORT).show();
                    requestPermissions(new String[]{Manifest.permission.READ_SMS}, 2);
                }
                break;
            }
        }
    }
}