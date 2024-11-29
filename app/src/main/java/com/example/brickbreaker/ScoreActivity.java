package com.example.brickbreaker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ScoreActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "BrickBreakerPrefs";
    private static final String KEY_HIGH_SCORE = "highScore";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the status bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_score);

        // Get the current score passed from GameActivity
        Intent intent = getIntent();
        int currentScore = intent.getIntExtra("score", 0);

        // Load the high score from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int highScore = sharedPreferences.getInt(KEY_HIGH_SCORE, 0);

        // Reference the TextView for score display
        TextView scoreTextView = findViewById(R.id.scoreTextView);

        // Text to display in the TextView
        StringBuilder scoreMessage = new StringBuilder();

        // Check if the current score is a new high score
        if (currentScore > highScore) {
            highScore = currentScore;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(KEY_HIGH_SCORE, highScore);
            editor.apply(); // Save the new high score

            // Add "New High Score!" message on its own line
            scoreMessage.append("New High Score!\n"); // One line break
            scoreTextView.setTypeface(Typeface.create("serif", Typeface.BOLD));  // Times New Roman and bold
            scoreTextView.setTextSize(30);  // Larger text for emphasis
        } else {
            // Use Times New Roman and bold for normal score messages
            scoreTextView.setTypeface(Typeface.create("serif", Typeface.BOLD));
            scoreTextView.setTextSize(40);  // Slightly smaller size
        }

        // Add "Your Score" and "High Score" on their own lines
        scoreMessage.append("Your Score: ").append(currentScore).append("\n");
        scoreMessage.append("High Score: ").append(highScore);

        // Set the shadow for better visibility
        scoreTextView.setShadowLayer(4, 2, 2, Color.BLACK);

        // Display the score message
        scoreTextView.setText(scoreMessage.toString());

        // Handle the Play Again button click
        Button playAgainButton = findViewById(R.id.playAgainButton);
        playAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the GameActivity again
                Intent intent = new Intent(ScoreActivity.this, GameActivity.class);
                startActivity(intent);
                finish();  // Finish ScoreActivity so the user can't go back to it with the back button
            }
        });
    }
}
