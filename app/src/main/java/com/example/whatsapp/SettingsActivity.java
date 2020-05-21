package com.example.whatsapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;


import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;


import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {
    private Button saveBtn;
    private CircleImageView profileImageView;
    private EditText usernameET,userBioET;
    private static int gallarypic =1;
    private Uri imageuri;
    private StorageReference userProfileimageRef;
    private String downloadUrl;
    private DatabaseReference userRef;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        userProfileimageRef = FirebaseStorage.getInstance().getReference().child("profile Images");
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");

        saveBtn = findViewById(R.id.bio_settings_btn);
        profileImageView = findViewById(R.id.settings_profile_image);
        userBioET = findViewById(R.id.bio_settings);
        usernameET = findViewById(R.id.username_settings);
        progressDialog = new ProgressDialog(this);

        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryintent = new Intent();
                galleryintent.setAction(Intent.ACTION_GET_CONTENT);
                galleryintent.setType("image/*");
                startActivityForResult(galleryintent,gallarypic);

            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SaveUserData();
            }
        });
        retriveUserInfo();


    }

    private void SaveUserData() {
        final String getUserNAme=usernameET.getText().toString();
        final String getUserStatus=userBioET.getText().toString();

        if(imageuri==null){
            userRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).hasChild("image")){
                        saveInfoOnlyWithoutImage();
                    }
                    else{
                        Toast.makeText(SettingsActivity.this,"Please select image first.",Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
        else if(getUserNAme.equals("")){
            Toast.makeText(SettingsActivity.this,"User name is mandatory",Toast.LENGTH_LONG).show();
        }
        else if(getUserStatus.equals("")){
            Toast.makeText(SettingsActivity.this,"Bio is mandatory",Toast.LENGTH_LONG).show();
        }
        else{
            progressDialog.setTitle("Account Settings");
            progressDialog.setMessage("Please Wait....");
            progressDialog.show();

            final StorageReference filePath = userProfileimageRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid());
            final UploadTask uploadTask = filePath.putFile(imageuri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if(!task.isSuccessful()){
                        throw task.getException();
                    }
                    downloadUrl=filePath.getDownloadUrl().toString();
                    return filePath.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if(task.isSuccessful()){
                        downloadUrl = task.getResult().toString();
                        HashMap<String,Object> profileMap = new HashMap<>();
                        profileMap.put("uid",FirebaseAuth.getInstance().getCurrentUser().getUid());
                        profileMap.put("name",getUserNAme);
                        profileMap.put("status",getUserStatus);
                        profileMap.put("image",downloadUrl);

                        userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                .updateChildren(profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful()){
                                    Intent intent = new Intent(SettingsActivity.this,MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                    progressDialog.dismiss();
                                    Toast.makeText(SettingsActivity.this,"Profile settings has been updated",Toast.LENGTH_LONG).show();
                                }
                            }
                        });

                    }
                }
            });
        }

    }

    private void saveInfoOnlyWithoutImage() {
        final String getUserNAme=usernameET.getText().toString();
        final String getUserStatus=userBioET.getText().toString();

        if(getUserNAme.equals("")){
            Toast.makeText(SettingsActivity.this,"User name is mandatory",Toast.LENGTH_LONG).show();
        }
        else if(getUserStatus.equals("")){
            Toast.makeText(SettingsActivity.this,"Bio is mandatory",Toast.LENGTH_LONG).show();
        }
        else{
            progressDialog.setTitle("Account Settings");
            progressDialog.setMessage("Please Wait....");
            progressDialog.show();

            HashMap<String,Object> profileMap = new HashMap<>();
            profileMap.put("uid",FirebaseAuth.getInstance().getCurrentUser().getUid());
            profileMap.put("name",getUserNAme);
            profileMap.put("status",getUserStatus);
            profileMap.put("image",downloadUrl);

            userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .updateChildren(profileMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Intent intent = new Intent(SettingsActivity.this,MainActivity.class);
                        startActivity(intent);
                        finish();
                        progressDialog.dismiss();
                        Toast.makeText(SettingsActivity.this,"Profile settings has been updated",Toast.LENGTH_LONG).show();
                    }
                }
            });
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==gallarypic&&resultCode==RESULT_OK&&data!=null){
            imageuri=data.getData();

            Picasso.get().load(imageuri).resize(50, 50).
                    centerCrop().into(profileImageView);

        }

    }

    private void retriveUserInfo(){
        userRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()){
                            String imageDb=dataSnapshot.child("image").getValue().toString();
                            String nameDb=dataSnapshot.child("name").getValue().toString();
                            String bioDb=dataSnapshot.child("status").getValue().toString();

                            usernameET.setText(nameDb);
                            userBioET.setText(bioDb);

                            Picasso.get().load(imageDb).placeholder(R.drawable.profile_image).into(profileImageView);

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }
}