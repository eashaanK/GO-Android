package com.heathtracker.fire.firehealthtracker;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

public class WelcomeActivity extends Activity{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set content view AFTER ABOVE sequence (to avoid crash)
        this.setContentView(R.layout.welcome);

        Button button = (Button) findViewById(R.id.gameOnButton);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startGameOn();
            }
        });
    }

    //TODO: Due to time contrainsts, app is limited to WebView only
    private void startGameOn(){
        /*String url = getIntent().getDataString();
        Bundle extras = new Bundle();*/
        //extras.putString(URL_PARAM, url);

        Intent i = new Intent(WelcomeActivity.this, GameOnActivity.class);
        /*i.putExtras(extras);*/
        startActivity(i);

    }
}