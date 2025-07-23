package com.example.fuelwise;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.fuelwise.customComponents.CustomSnackBar;
import com.example.fuelwise.helpers.AppUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class ForgetActivity extends AppCompatActivity {

    Button act_forget_pass_sent_btn;
    View parentLayout;
    TextView back_to_login_tv;
    private boolean isDark = false;
    EditText act_forget_pass_email_edt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget);

        getWindow().setStatusBarColor(getResources().getColor(R.color.app_sec_color));

        back_to_login_tv = findViewById(R.id.back_to_login_tv);
        back_to_login_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ForgetActivity.this,LoginActivity.class);
                startActivity(intent);
            }
        });
        act_forget_pass_email_edt = findViewById(R.id.act_forget_pass_email_edt);
        act_forget_pass_sent_btn = findViewById(R.id.act_forget_pass_sent_btn);
        act_forget_pass_sent_btn.setOnClickListener(view -> {

            if (AppUtils.isValidEmail(act_forget_pass_email_edt.getText().toString().trim())) {

                act_forget_pass_email_edt.setEnabled(false);
                act_forget_pass_sent_btn.setEnabled(false);
                FirebaseAuth.getInstance().sendPasswordResetEmail(act_forget_pass_email_edt.getText().toString().trim()).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if(task.isSuccessful()){
                            CustomSnackBar.showSnackBar(parentLayout,
                                    getResources().getString(R.string.recovery_mail), isDark);
                            act_forget_pass_email_edt.setEnabled(true);
                            act_forget_pass_email_edt.setText("");
                            act_forget_pass_sent_btn.setEnabled(true);
                        }else{
                            CustomSnackBar.showSnackBar(parentLayout,
                                    Objects.requireNonNull(task.getException()).getMessage(), isDark);

                            act_forget_pass_email_edt.setEnabled(true);
                            act_forget_pass_sent_btn.setEnabled(true);
                        }
                    }
                });

            }else{
                CustomSnackBar.showSnackBar(parentLayout, getResources().getString(R.string.email_not_valid), isDark);
            }

        });
    }
}
