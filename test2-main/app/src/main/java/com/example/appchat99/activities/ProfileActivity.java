package com.example.appchat99.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.example.appchat99.R;
import com.example.appchat99.databinding.ActivityProfileBinding;
import com.example.appchat99.models.User;
import com.example.appchat99.utilities.Constants;
import com.example.appchat99.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {
    private FirebaseFirestore database;
    ActivityProfileBinding binding;
    PreferenceManager preferenceManager;
    private String endcodedImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        database = FirebaseFirestore.getInstance();

        loadProfile();
        binding.textBack.setOnClickListener(v -> onBackPressed());
        binding.textAddImage.setOnClickListener(v -> {
            Intent intent =new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });

        binding.buttonEditProfile.setOnClickListener(v -> {
            if (isValidProfileDetails()){
                updateProfile();
            }
        });
    }

    private void updateProfile() {
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .update(Constants.KEY_NAME,binding.inputName.getText().toString(),
                        Constants.KEY_IMAGE,endcodedImage)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    preferenceManager.putString(Constants.KEY_NAME,binding.inputName.getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE,endcodedImage);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error while updating profile", Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isValidProfileDetails() {
        if (binding.inputName.getText().toString().trim().isEmpty()){
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (endcodedImage == null){
            Toast.makeText(this, "Please select your image", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (binding.inputEmail.getText().toString().trim().isEmpty()){
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void loadProfile(){
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()){
                        User user = documentSnapshot.toObject(User.class);
                        if(user.image != null){
                            byte[] bytes = Base64.decode(user.image,Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                            binding.imageProfile.setImageBitmap(bitmap);
                        }
                        binding.inputName.setText(user.name);
                        binding.inputEmail.setText(user.email);
                        preferenceManager.putString(Constants.KEY_NAME,binding.inputName.getText().toString());
                        preferenceManager.putString(Constants.KEY_IMAGE,endcodedImage);
                        preferenceManager.putString(Constants.KEY_EMAIL,binding.inputEmail.getText().toString());
                    }
                });
    }

    private String endcodedImage(Bitmap bitmap){
        int previewWidth =150;
        int previewHeigh=bitmap.getHeight() *previewWidth /bitmap.getWidth();
        Bitmap previewBitmap =Bitmap.createScaledBitmap(bitmap,previewWidth,previewHeigh,false);
        ByteArrayOutputStream byteArrayOutputStream =new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG,50,byteArrayOutputStream);
        byte[] bytes=byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes,Base64.DEFAULT);
    }
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK){
                    if (result.getData() !=null){
                        Uri imageUri =result.getData().getData();
                        try {
                            InputStream inputStream=getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.textAddImage.setVisibility(View.GONE);
                            endcodedImage=endcodedImage(bitmap);
                        }catch (FileNotFoundException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
    );
}