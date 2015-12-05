package com.harnk.whereru;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

/**
 * Created by scottnull on 11/22/15.
 */
public class LoginActivity extends AppCompatActivity {

    EditText nicknameTextField, secretCodeTextField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();

        nicknameTextField = (EditText)findViewById(R.id.nickName);
        nicknameTextField.setText(deviceSingleton.getNickname());
        secretCodeTextField = (EditText)findViewById(R.id.mapGroup);
        secretCodeTextField.setText(deviceSingleton.getSecretCode());

    }
    private void showErrorAlert(String text) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(text);
//            alertDialog.setMessage("Fill in additional message");
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do something dummy
            }
        });
        alertDialog.show();
    }

    public void doStart(View view){
        nicknameTextField = (EditText)findViewById(R.id.nickName);
        secretCodeTextField = (EditText)findViewById(R.id.mapGroup);
        //if either field is zero length throw up alert
        if (nicknameTextField.length()==0){
            showErrorAlert("Fill in your nickname");
        }
        if (secretCodeTextField.length()==0){
            showErrorAlert("Fill in a map group name");
        }

        //set values to singleton and sharedpreferences
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        deviceSingleton.setNickname(nicknameTextField.getText().toString());
        deviceSingleton.setSecretCode(secretCodeTextField.getText().toString());
        Log.d("SCXTT", "doStart clicked in LoginActivity: " + deviceSingleton.getNickname() + " " + deviceSingleton.getSecretCode());


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("nickname", deviceSingleton.getNickname());
        editor.putString("secretCode", deviceSingleton.getSecretCode());
        editor.commit();

        String savedMapGroup = prefs.getString("secretCode", "");
        Log.d("SCXTT", "SAVED prefs secretCode: " + savedMapGroup + " nickname: " + deviceSingleton.getNickname() + " SCXTT WIP check what is saved for savedLocStr");

        // do postJoinRequest

    }

}
