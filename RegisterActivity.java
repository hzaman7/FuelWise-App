package com.example.fuelwise;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fuelwise.customComponents.CustomSnackBar;
import com.example.fuelwise.helpers.AppUtils;
import com.example.fuelwise.models.UserModel;
import com.example.fuelwise.firebaseCalls.FireStoreDB;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;


import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    View parentLayout;
    RelativeLayout act_reg_have_acc_rl;
    ProgressBar register_pb;
    Button act_reg_register_btn;
    private boolean isDark = false;
    //FireStoreDB fireStoreDB;
    private UserModel userModel;
    EditText act_reg_user_name_edt, act_reg_email_edt, act_reg_pass_edt, act_reg_con_pass_edt, act_reg_phone_edt;
    FireStoreDB fireStoreDB;
    private void isLoadingData(boolean isLoading) {
        if (isLoading) {
            register_pb.setVisibility(View.VISIBLE);

            act_reg_user_name_edt.setEnabled(false);
            act_reg_email_edt.setEnabled(false);
            act_reg_pass_edt.setEnabled(false);
            act_reg_phone_edt.setEnabled(false);
            act_reg_con_pass_edt.setEnabled(false);
            act_reg_register_btn.setEnabled(false);
            act_reg_have_acc_rl.setEnabled(false);
        } else {
            register_pb.setVisibility(View.GONE);

            act_reg_user_name_edt.setEnabled(true);
            act_reg_email_edt.setEnabled(true);
            act_reg_pass_edt.setEnabled(true);
            act_reg_phone_edt.setEnabled(true);
            act_reg_con_pass_edt.setEnabled(true);
            act_reg_register_btn.setEnabled(true);
            act_reg_have_acc_rl.setEnabled(true);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        getWindow().setStatusBarColor(getResources().getColor(R.color.app_sec_color));

        act_reg_user_name_edt = findViewById(R.id.act_reg_user_name_edt);
        act_reg_email_edt = findViewById(R.id.act_reg_email_edt);
        act_reg_pass_edt = findViewById(R.id.act_reg_pass_edt);
        act_reg_phone_edt = findViewById(R.id.act_reg_phone_edt);
        act_reg_con_pass_edt = findViewById(R.id.act_reg_con_pass_edt);
        register_pb = findViewById(R.id.register_pb);
        parentLayout = findViewById(android.R.id.content);
        userModel = new UserModel();
        fireStoreDB = new FireStoreDB();


        act_reg_register_btn = findViewById(R.id.act_reg_register_btn);
        act_reg_register_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(act_reg_user_name_edt.getText().toString().trim()) &&
                        !TextUtils.isEmpty(act_reg_email_edt.getText().toString().trim()) &&
                        !TextUtils.isEmpty(act_reg_phone_edt.getText().toString().trim()) &&
                        !TextUtils.isEmpty(act_reg_pass_edt.getText().toString().trim()) &&
                        !TextUtils.isEmpty(act_reg_con_pass_edt.getText().toString().trim())) {

                    if (AppUtils.isValidEmail(act_reg_email_edt.getText().toString().trim())) {

                        if (AppUtils.isValidPhone(act_reg_phone_edt.getText().toString().trim())) {

                            if (act_reg_pass_edt.getText().toString().trim().length() > 6 &&
                                    act_reg_pass_edt.getText().toString().trim().length() < 12) {

                                if (act_reg_con_pass_edt.getText().toString().trim().length() > 6 &&
                                        act_reg_con_pass_edt.getText().toString().trim().length() < 12) {

                                    if (act_reg_pass_edt.getText().toString().trim()
                                            .equals(act_reg_con_pass_edt.getText().toString().trim())) {

                                        isLoadingData(true);

                                        FirebaseAuth.getInstance().createUserWithEmailAndPassword(act_reg_email_edt.getText().toString().trim(),
                                                act_reg_pass_edt.getText().toString().trim()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                            @Override
                                            public void onComplete(@NonNull Task<AuthResult> task) {
                                                if (task.isSuccessful()) {

                                                    userModel.setUserEmail(act_reg_email_edt.getText().toString().trim());
                                                    userModel.setUserPassword(act_reg_pass_edt.getText().toString().trim());
                                                    userModel.setUserID(FirebaseAuth.getInstance().getUid());
                                                    //userModel.setDeviceToken(UserDAO.getInstance(RegisterActivity.this).getDeviceToken());
                                                    userModel.setUserFullName(act_reg_user_name_edt.getText().toString().trim());
                                                    userModel.setUserPhone(act_reg_phone_edt.getText().toString().trim());


                                                    fireStoreDB.createUserToFireBaseDB(RegisterActivity.this,
                                                            RegisterActivity.this, userModel);



                                                } else {
                                                    isLoadingData(false);
                                                    CustomSnackBar.showSnackBar(parentLayout, Objects.requireNonNull(task.getException()).getMessage(), isDark);
                                                }
                                            }
                                        });

                                    } else {
                                        CustomSnackBar.showSnackBar(parentLayout, getResources().getString(R.string.password_not_matched), isDark);
                                    }

                                } else {
                                    CustomSnackBar.showSnackBar(parentLayout, getResources().getString(R.string.password_not_valid), isDark);
                                }
                            } else {
                                CustomSnackBar.showSnackBar(parentLayout, getResources().getString(R.string.password_not_valid), isDark);
                            }
                        } else {
                            CustomSnackBar.showSnackBar(parentLayout, getResources().getString(R.string.phone_not_valid), isDark);
                        }


                    } else {
                        CustomSnackBar.showSnackBar(parentLayout, getResources().getString(R.string.email_not_valid), isDark);
                    }

                } else {
                    CustomSnackBar.showSnackBar(parentLayout, getResources().getString(R.string.all_fields_mandatory), isDark);
                }


            }
        });

        act_reg_have_acc_rl = findViewById(R.id.act_reg_have_acc_rl);
        act_reg_have_acc_rl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    public void onUserRegistered(boolean registered, String error) {
        if (registered) {
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

        } else {
            CustomSnackBar.showSnackBar(parentLayout,error , isDark);
        }
    }
}



