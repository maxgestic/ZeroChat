package com.maxgestic.zerochat;

import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class settingsFragment extends Fragment {

    private ActivityResultLauncher<Intent> activityResultLauncher;
    private Uri selectedImage;
    private ImageView imageView;
    private byte[] data1;

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
        editPicButton.setOnClickListener(v -> editPictureDialog());

        super.onViewCreated(view, savedInstanceState);
        //TODO make it so profile pic is in ImageView when dialog opens
    }

    public void editPictureDialog(){
        final Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.edit_photo_dialog);
        Button edit = dialog.findViewById(R.id.editPhotoButton);
        Button select = dialog.findViewById(R.id.selectButton);
        imageView = dialog.findViewById(R.id.pictureView);
        select.setOnClickListener(v -> {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            activityResultLauncher.launch(photoPickerIntent);
        });

        edit.setOnClickListener(v -> {
            if (data1 != null){
                String userId = FirebaseAuth.getInstance().getUid();
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReference();
                StorageReference path = storageRef.child("profilePics/"+userId+".jpg");
                path.putBytes(data1);
                dialog.dismiss();
            }
        });

        dialog.show();

    }

}