package com.maxgestic.zerochat;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;

public class settingsFragment extends Fragment {

    ActivityResultLauncher<Intent> activityResultLauncher;
    Uri selectedImage;
    ImageView imageView;
    byte[] data1;

    public settingsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        Button editPicButton = requireView().findViewById(R.id.editPictureButton);

        editPicButton.setOnClickListener(v -> {
            editPictureDialog();
        });

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        // doSomeOperations();
                        Intent data = result.getData();
                        selectedImage = Objects.requireNonNull(data).getData();
                        InputStream imageStream = null;
                        try {
                            imageStream = requireContext().getContentResolver().openInputStream(selectedImage);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }

//                        Bitmap bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(imageStream), 100, 100, true);
                        Bitmap bitmap1 = BitmapFactory.decodeStream(imageStream);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap1.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                        data1 = baos.toByteArray();

                        imageView.setImageURI(selectedImage);
                    }
                });

        super.onViewCreated(view, savedInstanceState);

        //TODO make it so profile pic is in ImageView when dialog opens
    }

    public void editPictureDialog(){

        final Dialog dialog = new Dialog(getActivity());

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.edit_photo_dialog);

        Button edit = dialog.findViewById(R.id.editPhotoButton);

        final EditText nick = dialog.findViewById(R.id.editContactNick);
        Button select = dialog.findViewById(R.id.selectButton);
        imageView = dialog.findViewById(R.id.pictureView);

        select.setOnClickListener(v -> {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            activityResultLauncher.launch(photoPickerIntent);
        });

        edit.setOnClickListener(v -> {

            String userId = FirebaseAuth.getInstance().getUid();

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            StorageReference path = storageRef.child("profilePics/"+userId+".jpg");

            path.putBytes(data1);

            dialog.dismiss();

        });

        dialog.show();

    }

}