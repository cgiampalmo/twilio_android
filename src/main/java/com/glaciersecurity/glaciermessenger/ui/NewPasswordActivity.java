/*
 * Copyright 2013-2017 Amazon.com,
 * Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License").
 * You may not use this file except in compliance with the
 * License. A copy of the License is located at
 *
 *      http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, express or implied. See the License
 * for the specific language governing permissions and
 * limitations under the License.
 */

package com.glaciersecurity.glaciermessenger.ui;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.glaciersecurity.glaciermessenger.R;
import com.glaciersecurity.glaciermessenger.cognito.AppHelper;

public class NewPasswordActivity extends AppCompatActivity {
    private String TAG = "NewPassword";
    private EditText newPassword;
    private TextInputLayout newPasswordLayout;

    private Button continueSignIn;
    private AlertDialog userDialog;
    private ProgressDialog waitDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_password);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayShowHomeEnabled(false);
            ab.setDisplayHomeAsUpEnabled(false);

            //HONEYBADGER AM-125 remove "Messenger" Title bar
            //ab.setTitle(R.string.app_name); //ALF changed from action_add_account, maybe part of AM-173
        }

        /*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_NewPassword);
        if (toolbar != null) {
            toolbar.setTitle("");
            setSupportActionBar(toolbar);
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        TextView main_title = (TextView) findViewById(R.id.newpassword_toolbar_title);
        main_title.setText("Welcome");

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit(false);
            }
        });*/

        init();
    }

    @Override
    public void onBackPressed() {
        exit(false);
    }

    private void init() {
        newPassword = (EditText) findViewById(R.id.editTextNewPassPass);

        this.newPasswordLayout = (TextInputLayout) findViewById(R.id.new_password_layout);
        this.newPasswordLayout.setError(null);

        continueSignIn = (Button) findViewById(R.id.buttonNewPass);
        continueSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newUserPassword = newPassword.getText().toString();
                if (newUserPassword != null) {
                    AppHelper.setPasswordForFirstTimeLogin(newUserPassword);
                    if (checkAttributes()) {
                        newPasswordLayout.setError(null);
                        exit(true);
                    } else {
                        newPasswordLayout.setError("Error");
                        showDialogMessage("Error", "Enter all required attributed", false);
                    }
                } else {
                    showDialogMessage("Error", "Enter all required attributed", false);
                }
            }
        });
    }

    private boolean checkAttributes() {
        // Check if all required attributes have values
        return true;
    }

    private void showDialogMessage(String title, String body, final boolean exit) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setMessage(body).setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    userDialog.dismiss();
                    if (exit) {
                        exit(false);
                    }
                } catch (Exception e) {
                    exit(false);
                }
            }
        });
        userDialog = builder.create();
        userDialog.show();
    }

    private void exit(Boolean continueWithSignIn) {
        Intent intent = new Intent();
        intent.putExtra("continueSignIn", continueWithSignIn);
        setResult(RESULT_OK, intent);
        finish();
    }
}
