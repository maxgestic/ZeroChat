package com.maxgestic.zerochat;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Objects;

import kotlin.random.Random;

public class FCMService extends FirebaseMessagingService {

    private String ADMIN_CHANNEL_ID = "admin_channel";

    @Override
    public void onNewToken(@NonNull String token) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("TEST", "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                });
        super.onNewToken(token);
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        String userID = FirebaseAuth.getInstance().getUid();
        DocumentReference docRef = FirebaseFirestore.getInstance().collection("users").document(userID).collection("contacts").document(Objects.requireNonNull(message.getData().get("title")));

        Log.d("TEST", message.getData().get("title") + " " + message.getData().get("message"));

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("from", userID);
        intent.putExtra("to", message.getData().get("title"));
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Integer notificationID = Random.Default.nextInt(3000);

        setupChannels(notificationManager);

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);


        docRef.get()
                .addOnCompleteListener(task -> {

                   if (task.isSuccessful()){

                       if (task.getResult() != null){

                           DocumentSnapshot doc = task.getResult();
                           String nick = doc.getString("nickname");

                           Uri notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                           NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, ADMIN_CHANNEL_ID)
                                   .setSmallIcon(R.drawable.ic_chat)
                                   .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.chatbubble))
                                   .setContentTitle("Message From: " + nick)
                                   .setContentText(message.getData().get("message"))
                                   .setAutoCancel(true)
                                   .setSound(notificationSoundUri)
                                   .setContentIntent(pendingIntent);

                           notificationBuilder.setColor(R.color.accent1_100);

                           notificationManager.notify(notificationID, notificationBuilder.build());

                       }

                   }

                });
    }

    private void setupChannels(NotificationManager notificationManager) {
        String adminChannelName = "New Notification";
        String adminChannelDesc = "Notification of new message from another user";

        NotificationChannel adminChannel = new NotificationChannel(ADMIN_CHANNEL_ID, adminChannelName, NotificationManager.IMPORTANCE_HIGH);
        adminChannel.setDescription(adminChannelDesc);
        adminChannel.enableLights(true);
        adminChannel.setLightColor(Color.RED);
        adminChannel.enableVibration(true);
        notificationManager.createNotificationChannel(adminChannel);
    }
}
