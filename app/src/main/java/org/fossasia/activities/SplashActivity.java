package org.fossasia.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import org.fossasia.R;
import org.fossasia.db.JsonToDatabase;


public class SplashActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Thread(new Runnable() {
            @Override
            public void run() {
                JsonToDatabase dataDownload = new JsonToDatabase(getApplicationContext());
                dataDownload.setOnJsonToDatabaseCallback(new JsonToDatabase.JsonToDatabaseCallback() {
                    @Override
                    public void onDataLoaded() {
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        finish();
                    }

                });
                dataDownload.startDataDownload();
            }
        }).start();
    }

}
