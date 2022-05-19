package com.maxgestic.zerochat;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

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

    private final String NEW_MESSAGE_CHANNEL_ID = "new_message_channel";

    @Override
    public void onNewToken(@NonNull String token) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("TEST", "Fetching FCM registration token failed", task.getException());
                    }
                });
        super.onNewToken(token);
    }

    @SuppressLint("ResourceAsColor")
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        //get userID and documentRef to get nickname
        String userID = FirebaseAuth.getInstance().getUid();
        assert userID != null;
        DocumentReference docRef = FirebaseFirestore.getInstance().collection("users").document(userID).collection("contacts").document(Objects.requireNonNull(message.getData().get("title")));
        //create intent to start app and launch chat with contact that the message came from
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("from", userID);
        intent.putExtra("to", message.getData().get("title"));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //initialise notification manager
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationID = Random.Default.nextInt(3000);
        //setup notification channel
        setupChannels(notificationManager);
        //create pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        //get nickname from firestore to put in notification
        docRef.get()
                .addOnCompleteListener(task -> {
                   if (task.isSuccessful()){
                       if (task.getResult() != null){
                           DocumentSnapshot doc = task.getResult();
                           //build notification
                           String nick = doc.getString("nickname");
                           Uri notificationSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                           NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NEW_MESSAGE_CHANNEL_ID)
                                   .setSmallIcon(R.drawable.ic_chat)
                                   .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.chatbubble))
                                   .setContentTitle("Message From: " + nick)
                                   .setContentText(message.getData().get("message"))
                                   .setAutoCancel(true)
                                   .setSound(notificationSoundUri)
                                   .setContentIntent(pendingIntent);
                           notificationBuilder.setColor(R.color.accent1_100);
                           //notify the user of new message
                           notificationManager.notify(notificationID, notificationBuilder.build());
                       }
                   }
                });
    }

    private void setupChannels(NotificationManager notificationManager) {
        //setup notification channel
        String newMessageChannel = "New Message";
        String adminChannelDesc = "Notification of new message from another user";
        NotificationChannel adminChannel = new NotificationChannel(NEW_MESSAGE_CHANNEL_ID, newMessageChannel, NotificationManager.IMPORTANCE_HIGH);
        adminChannel.setDescription(adminChannelDesc);
        adminChannel.enableLights(true);
        adminChannel.setLightColor(Color.RED);
        adminChannel.enableVibration(true);
        adminChannel.setShowBadge(true);
        notificationManager.createNotificationChannel(adminChannel);
    }
}
