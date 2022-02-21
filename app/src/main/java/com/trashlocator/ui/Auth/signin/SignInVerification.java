package com.trashlocator.ui.Auth.signin;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.trashlocator.R;
import com.trashlocator.ui.MainDashboard;
import com.trashlocator.ui.firebase.FirebaseInit;

import java.util.concurrent.TimeUnit;

import static android.widget.Toast.LENGTH_LONG;

public class SignInVerification extends AppCompatActivity {

    public Button Verify;
    public TextView otpsentText;
    EditText OtpEditText;
    String phonenumber;
    String codebysystem;
    ProgressDialog progressDialog;
    boolean sentCode=false;

    //Firebase Variables
    DatabaseReference dbRef;
    FirebaseAuth mAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_verification);

        //HOOKS
        otpsentText=findViewById(R.id.otp_sent_number_signin_verification_textview);
        Verify=(Button) findViewById(R.id.sign_in_verify_button);
        OtpEditText=findViewById(R.id.otp_signin_verification_edittext);

        //VARIABLES
        Bundle bundle=getIntent().getExtras();
        phonenumber=bundle.getString("Phone");

        //SET OTP SENT TO PHONE NUMBER TEXT
        otpsentText.setText(String.format("Sending OTP to %s", phonenumber));

        //firebase instance
        mAuth= FirebaseAuth.getInstance();

        //send OTP
        sendVerificationCodeToUser(phonenumber);

        Verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String codeByUser = OtpEditText.getText().toString().trim();
                if (!codeByUser.equals("") && sentCode) {
                    verifyCode(codeByUser);
                }
                else
                {
                    Toast.makeText(SignInVerification.this,"Code incorrect", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    //Code to send OTP message
    void sendVerificationCodeToUser(String phoneNo) {

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber("+91"+phoneNo)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(SignInVerification.this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);

    }
    //Call Back after phone auth
    PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                    super.onCodeSent(s, forceResendingToken);
                    codebysystem = s;
                    sentCode=true;
                }

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {

                    String code = phoneAuthCredential.getSmsCode();
                    if (code != null) {

                        verifyCode(code);
                    }

                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Toast.makeText(SignInVerification.this, e.getMessage(), Toast.LENGTH_LONG).show();

                }
            };

    //Code to verify the OTP
    void verifyCode(String code) {

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(codebysystem, code);
        signInTheUserByCredentials(credential);

    }

    void signInTheUserByCredentials(PhoneAuthCredential credential) {


        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(SignInVerification.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {

                            //Set ProgressDialog
                            progressDialog=new ProgressDialog(SignInVerification.this);
                            //show progressDialog
                            progressDialog.show();
                            //set contentView for progressDialog
                            progressDialog.setContentView(R.layout.progress_dialog);
                            //set transparent background
                            progressDialog.getWindow().setBackgroundDrawableResource(
                                    android.R.color.transparent
                            );
                            getUserDataFromDatabase();

                        } else {
                            Toast.makeText(SignInVerification.this, "Incorrect code..", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void getUserDataFromDatabase() {
        Log.d("TAG", "getUserDataFromDatabase: BEFORE GETUSERDATA FROM DB");
        dbRef= FirebaseInit.getDatabase().getReference("USERS/"+phonenumber);
        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnashot) {
                if (userSnashot.exists()) {
                    Log.d("TAG", "onDataChange: SNAPSHOT"+userSnashot);
                    storeInSharedPreferences();

                } else {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "Account doesn't exist!", LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

    }

    private void storeInSharedPreferences() {

        //Instantiate
        SharedPreferences sharedPreferences=getSharedPreferences("com.trashlocator.userdetails", Context.MODE_PRIVATE);
        //Editor
        SharedPreferences.Editor editor;
        editor = sharedPreferences.edit();
        //STRING
        editor.putString("PHONE",phonenumber);
        //BOOLEAN
        editor.putBoolean("LOG_STATE",true);
        //save
        editor.apply();

        Log.d("SharedPreference","Phone::"+sharedPreferences.getString("PHONE",null));

        progressDialog.dismiss();
        Log.d("TAG", "storeInSharedPreferences: INSIDE SAVEINSHARED PREF");

        Intent intent=new Intent(SignInVerification.this, MainDashboard.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Toast.makeText(SignInVerification.this, "Signed in successfully!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        new AlertDialog.Builder(SignInVerification.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Please wait")
                .setMessage("Just wait a moment while we are verifying your mobile")
                .setPositiveButton("OK",null)
                .show();
    }
}