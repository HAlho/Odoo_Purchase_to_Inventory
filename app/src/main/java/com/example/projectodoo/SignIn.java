package com.example.projectodoo;

import static android.content.ContentValues.TAG;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;


public class SignIn extends AppCompatActivity {
    int authResult = 0;
    Thread thread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_in);

        //initialize buttons
        Button button = (Button) findViewById(R.id.signInButton);


        //set a listener for a button click
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                //link interface
                EditText databaseName = (EditText) findViewById(R.id.databaseName);
                EditText emailAddress = (EditText) findViewById(R.id.emailAddress);
                EditText password2 = (EditText) findViewById(R.id.password);
                final TextView signInResult = (TextView) findViewById(R.id.signInResult);

                //convert user input to string
                final String db = databaseName.getText().toString(),
                username = emailAddress.getText().toString(),
                password = password2.getText().toString();

                //
                if(db.equals("")){
                    //request database name
                    signInResult.setText("Please specify database name");
                }else {

                    xmlAuthenticate(db, username, password);


                    while (thread.isAlive()) {
                        signInResult.setText("Checking Credentials");
                    }

                    if (authResult > 0) {
                        signInResult.setText("Success");

                        Credentials credentials =new Credentials(db,username,password, authResult);

                        // Move to main page in response to button click
                        Intent intent = new Intent(v.getContext(), HomeAll.class);

                        //move user info to next page
                        intent.putExtra("Credentials", (Parcelable) credentials);

                        startActivity(intent);
                    } else
                        signInResult.setText("Failed to Login");
                }


            }
        });

    }


    public void xmlAuthenticate(String Db, String Username, String Password){
        thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {

                    final XmlRpcClient client = new XmlRpcClient();
                    authResult=0;

                    try {


                        final XmlRpcClientConfigImpl common_config = new XmlRpcClientConfigImpl();
                        common_config.setServerURL(
                                new URL("http://192.168.0.123:8069/xmlrpc/2/common"));
                        int uid = (int)client.execute(
                                common_config, "authenticate", Arrays.asList(
                                        Db, Username, Password, emptyMap()));

                        authResult=uid;

                        Log.e(TAG, String.valueOf(uid));


                    } catch (XmlRpcException e){
                        Log.e(TAG, e.getMessage());
                    }

                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        });
        thread.start();
    }






}