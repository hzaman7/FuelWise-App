package com.example.fuelwise;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.fuelwise.customComponents.CustomSnackBar;
import com.example.fuelwise.helpers.AppUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {


    Button act_login_login_btn;
    TextView act_log_forget_pass;
    RelativeLayout act_log_no_acc_rl;
    View parentLayout;
    ProgressBar login_progressbar;
    EditText act_login_pass_edt, act_login_email_edt;
    private FirebaseAuth mAuth;
    private boolean isDark = false;

    private void isLoadingData(boolean isLoading) {
        if (isLoading) {
            login_progressbar.setVisibility(View.VISIBLE);

            act_log_no_acc_rl.setEnabled(false);
            act_login_login_btn.setEnabled(false);
            act_log_forget_pass.setEnabled(false);
            act_login_pass_edt.setEnabled(false);
            act_login_email_edt.setEnabled(false);
        }else{
            login_progressbar.setVisibility(View.GONE);

            act_log_no_acc_rl.setEnabled(true);
            act_login_login_btn.setEnabled(true);
            act_log_forget_pass.setEnabled(true);
            act_login_pass_edt.setEnabled(true);
            act_login_email_edt.setEnabled(true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();

        if(mAuth.getCurrentUser()!=null){
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        getWindow().setStatusBarColor(getResources().getColor(R.color.app_sec_color));
        parentLayout = findViewById(android.R.id.content);



        act_login_pass_edt = findViewById(R.id.act_login_pass_edt);
        act_login_email_edt = findViewById(R.id.act_login_email_edt);
        login_progressbar = findViewById(R.id.login_progressbar);
        act_log_no_acc_rl = findViewById(R.id.act_log_no_acc_rl);
        act_log_no_acc_rl.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        act_log_forget_pass = findViewById(R.id.act_log_forpass);
        act_log_forget_pass.setOnClickListener(view -> {
            startActivity(new Intent(LoginActivity.this, ForgetActivity.class));
        });

        act_login_login_btn = findViewById(R.id.act_login_login_btn);
        act_login_login_btn.setOnClickListener(view -> {
            if (!TextUtils.isEmpty(act_login_email_edt.getText().toString().trim()) &&
                    !TextUtils.isEmpty(act_login_pass_edt.getText().toString().trim())) {

                if (AppUtils.isValidEmail(act_login_email_edt.getText().toString())) {
                    if (act_login_pass_edt.getText().toString().trim().length() > 6 &&
                            act_login_pass_edt.getText().toString().trim().length() < 12) {


                        isLoadingData(true);


                        mAuth.signInWithEmailAndPassword(act_login_email_edt.getText().toString().trim(),
                                act_login_pass_edt.getText().toString().trim()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {

                                if (task.isSuccessful()) {
                                    isLoadingData(false);
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                } else {
                                    isLoadingData(false);
                                    CustomSnackBar.showSnackBar(parentLayout, Objects.requireNonNull(task.getException()).getMessage(), isDark);
                                }
                            }
                        });
                    } else {
                        CustomSnackBar.showSnackBar(parentLayout, getResources().getString(R.string.password_not_valid), isDark);
                    }
                } else {
                    CustomSnackBar.showSnackBar(parentLayout, getResources().getString(R.string.email_not_valid), isDark);
                }
            } else {
                CustomSnackBar.showSnackBar(parentLayout, getResources().getString(R.string.both_fields_mandatory), isDark);

            }

        });




        act_log_forget_pass = findViewById(R.id.act_log_forpass);
        act_log_forget_pass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this,ForgetActivity.class);
                startActivity(intent);
            }
        });

        act_log_no_acc_rl = findViewById(R.id.act_log_no_acc_rl);
        act_log_no_acc_rl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this,RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}
