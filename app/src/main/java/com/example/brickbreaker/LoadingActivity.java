package com.example.brickbreaker;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LoadingActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView loadingText;
    private int progressStatus = 0;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        progressBar = findViewById(R.id.progressBar);
        loadingText = findViewById(R.id.loadingText); // Ensure this matches the ID in XML

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (progressStatus < 100) {
                    progressStatus += 1;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(progressStatus);
                            loadingText.setText(progressStatus + "%");
                        }
                    });
                    try {
                        Thread.sleep(40);  // This simulates the loading time
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (progressStatus >= 100) {
                    Intent intent = new Intent(LoadingActivity.this, GameActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        }).start();
    }
}
