package com.example.brickbreaker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class GameActivity extends AppCompatActivity implements SensorEventListener {

    private GameSurface gameSurface;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private float accelerometerX = 0;
    private float gyroscopeY = 0;
    private int deflectionCount = 0;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep the screen on while playing the game
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Set the activity to fullscreen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        gameSurface = new GameSurface(this);
        setContentView(gameSurface);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        // Initialize and start background music
        mediaPlayer = MediaPlayer.create(this, R.raw.background_music);
        mediaPlayer.setLooping(true); // Loop the music
        mediaPlayer.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerometerX = event.values[0];
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroscopeY = event.values[1];
        }

        float combinedMovement = (accelerometerX * -5f) + (gyroscopeY * 5f);
        gameSurface.updatePaddlePosition(combinedMovement);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    private class GameSurface extends SurfaceView implements SurfaceHolder.Callback {

        private GameThread gameThread;
        private Paint paint;
        private int paddleWidth = 180;
        private int paddleHeight = 30;
        private int ballRadius = 25;
        private float paddleX;
        private float paddleY;
        private float ballX;
        private float ballY;
        private float ballVelocityX;
        private float ballVelocityY;

        private Brick[][] bricks;
        private int numRows = 5;
        private int numColumns = 10;
        private int brickSpacing = 5;
        private int topOffset = 150;
        private int bottomOffset = 300;
        private float brickHeight = 60;

        private int score = 0;
        private int bricksBroken = 0;
        private Paint scorePaint;

        private boolean gameOver = false;
        private boolean ballPaused = false;
        private boolean animatingNewRow = false;
        private float newRowY = -brickHeight;

        private Random random = new Random();

        private Bitmap backgroundBitmap;
        private Bitmap floorBitmap;
        private Rect backgroundRect;
        private Rect floorRect;

        public GameSurface(Context context) {
            super(context);
            getHolder().addCallback(this);
            paint = new Paint();
            scorePaint = new Paint();
            scorePaint.setColor(Color.WHITE);
            scorePaint.setTextSize(60);
            scorePaint.setTypeface(Typeface.create("serif", Typeface.BOLD));
            scorePaint.setTextAlign(Paint.Align.CENTER);

            backgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.background);
            floorBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.floor);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            backgroundRect = new Rect(0, 0, getWidth(), getHeight());
            floorRect = new Rect(0, getHeight() - bottomOffset, getWidth(), getHeight());

            paddleX = getWidth() / 2 - paddleWidth / 2;
            paddleY = getHeight() - bottomOffset - paddleHeight - 20;

            ballX = random.nextBoolean() ? ballRadius : getWidth() - ballRadius;
            ballY = topOffset + numRows * (brickHeight + brickSpacing) + ballRadius;
            ballVelocityX = ballX == ballRadius ? 10 : -10;
            ballVelocityY = 10;

            initializeBricks();

            gameThread = new GameThread(holder);
            gameThread.setRunning(true);
            gameThread.start();
        }

        private void initializeBricks() {
            float brickWidth = (getWidth() - (brickSpacing * (numColumns - 1))) / numColumns;
            bricks = new Brick[numRows][numColumns];

            for (int i = 0; i < numRows; i++) {
                for (int j = 0; j < numColumns; j++) {
                    float x = j * (brickWidth + brickSpacing);
                    float y = i * (brickHeight + brickSpacing) + topOffset;
                    bricks[i][j] = new Brick(x, y, brickWidth, brickHeight);
                }
            }
        }

        private void startNewRowAnimation() {
            animatingNewRow = true;
            newRowY = -brickHeight;
        }

        private void addNewRow() {
            float brickWidth = (getWidth() - (brickSpacing * (numColumns - 1))) / numColumns;

            for (int i = numRows - 1; i > 0; i--) {
                for (int j = 0; j < numColumns; j++) {
                    bricks[i][j] = bricks[i - 1][j];
                    if (bricks[i][j] != null) {
                        bricks[i][j].moveDown(brickHeight + brickSpacing);
                    }
                }
            }

            for (int j = 0; j < numColumns; j++) {
                float x = j * (brickWidth + brickSpacing);
                bricks[0][j] = new Brick(x, topOffset, brickWidth, brickHeight);
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // Not used
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            boolean retry = true;
            gameThread.setRunning(false);
            while (retry) {
                try {
                    gameThread.join();
                    retry = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void updatePaddlePosition(float deltaX) {
            paddleX += deltaX;

            if (paddleX < 0) paddleX = 0;
            if (paddleX + paddleWidth > getWidth()) paddleX = getWidth() - paddleWidth;
        }

        public void setGameOver(boolean gameOver) {
            this.gameOver = gameOver;
        }

        private class GameThread extends Thread {
            private SurfaceHolder surfaceHolder;
            private boolean running;

            public GameThread(SurfaceHolder surfaceHolder) {
                this.surfaceHolder = surfaceHolder;
            }

            public void setRunning(boolean running) {
                this.running = running;
            }

            @Override
            public void run() {
                while (running) {
                    Canvas canvas = null;
                    try {
                        canvas = surfaceHolder.lockCanvas();
                        synchronized (surfaceHolder) {
                            updateGame();
                            drawGame(canvas);
                        }
                    } finally {
                        if (canvas != null) {
                            surfaceHolder.unlockCanvasAndPost(canvas);
                        }
                    }
                }
            }

            private void updateGame() {
                if (gameOver || ballPaused) {
                    return;
                }

                ballX += ballVelocityX;
                ballY += ballVelocityY;

                if (ballX < 0 || ballX > getWidth() - ballRadius) {
                    ballVelocityX = -ballVelocityX;
                }
                if (ballY < 0) {
                    ballVelocityY = -ballVelocityY;
                }

                if (ballY + ballRadius >= paddleY && ballY + ballRadius <= paddleY + ballVelocityY && ballX + ballRadius > paddleX && ballX < paddleX + paddleWidth) {
                    ballVelocityY = -ballVelocityY;
                    deflectionCount++;

                    if (deflectionCount % 5 == 0) {
                        ballVelocityX *= 1.1;
                        ballVelocityY *= 1.1;
                    }
                }

                for (int i = 0; i < numRows; i++) {
                    for (int j = 0; j < numColumns; j++) {
                        Brick brick = bricks[i][j];
                        if (brick.isVisible()) {
                            if (RectF.intersects(brick.getRect(), new RectF(ballX - ballRadius, ballY - ballRadius, ballX + ballRadius, ballY + ballRadius))) {
                                brick.setVisible(false);
                                ballVelocityY = -ballVelocityY;
                                score += 5;
                                bricksBroken++;

                                if (bricksBroken % 10 == 0) {
                                    startNewRowAnimation();
                                }

                                break;
                            }
                        }
                    }
                }

                if (ballY >= getHeight() - bottomOffset / 2) {
                    running = false;
                    showGameOverMessage();
                }

                if (animatingNewRow) {
                    newRowY += 10;
                    if (newRowY >= topOffset) {
                        animatingNewRow = false;
                        addNewRow();
                    }
                }
            }

            private void showGameOverMessage() {
                final int finalScore = score;

                ballVelocityX = 0;
                ballVelocityY = 0;
                ballPaused = true;

                // Stop the background music
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }

                Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    vibrator.vibrate(500);
                }

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    TextView gameOverTextView = new TextView(getContext());
                    gameOverTextView.setText("GAME OVER! 😢");
                    gameOverTextView.setTextSize(24);
                    gameOverTextView.setTypeface(Typeface.DEFAULT_BOLD);
                    gameOverTextView.setTextColor(Color.BLACK);
                    gameOverTextView.setPadding(50, 50, 50, 50);
                    builder.setView(gameOverTextView);
                    builder.setCancelable(false);
                    AlertDialog alert = builder.create();
                    alert.show();

                    handler.postDelayed(() -> {
                        alert.dismiss();
                        Intent intent = new Intent(getContext(), ScoreActivity.class);
                        intent.putExtra("score", finalScore);
                        getContext().startActivity(intent);
                        ((GameActivity) getContext()).finish();
                    }, 3000);
                });
            }

            private void drawGame(Canvas canvas) {
                if (canvas == null) return;

                if (backgroundBitmap != null) {
                    canvas.drawBitmap(backgroundBitmap, null, backgroundRect, null);
                }

                if (floorBitmap != null) {
                    canvas.drawBitmap(floorBitmap, null, floorRect, null);
                }

                paint.setColor(Color.WHITE);
                canvas.drawRect(paddleX, paddleY, paddleX + paddleWidth, paddleY + paddleHeight, paint);

                paint.setColor(Color.GREEN);  // Change the ball color to green
                canvas.drawCircle(ballX, ballY, ballRadius, paint);

                for (int i = 0; i < numRows; i++) {
                    for (int j = 0; j < numColumns; j++) {
                        bricks[i][j].draw(canvas);
                    }
                }

                if (animatingNewRow) {
                    float brickWidth = (getWidth() - (brickSpacing * (numColumns - 1))) / numColumns;
                    for (int j = 0; j < numColumns; j++) {
                        float x = j * (brickWidth + brickSpacing);
                        RectF rect = new RectF(x, newRowY, x + brickWidth, newRowY + brickHeight);
                        paint.setColor(Color.RED);
                        canvas.drawRect(rect, paint);
                    }
                }

                canvas.drawText("Score: " + score, getWidth() / 2, 100, scorePaint);
            }
        }
    }

    // Brick class integrated into GameActivity
    public class Brick {
        private float x, y, width, height;
        private boolean visible;

        public Brick(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.visible = true;
        }

        public RectF getRect() {
            return new RectF(x, y, x + width, y + height);
        }

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public void moveDown(float distance) {
            this.y += distance;
        }

        public void draw(Canvas canvas) {
            if (visible) {
                Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.FILL_AND_STROKE);
                paint.setStrokeWidth(6);
                canvas.drawRect(getRect(), paint);

                paint.setColor(Color.DKGRAY);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(6);
                canvas.drawRect(getRect(), paint);
            }
        }
    }
}
