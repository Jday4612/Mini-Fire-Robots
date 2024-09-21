package com.example.fireapp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    private ImageView imageView1;
    private ImageView imageView2;
    private String serverIP = "10.10.141.50";  // 서버 IP
    private int serverPort = 5002;  // 서버 포트
    private static final String CHANNEL_ID = "fire_alert_channel"; // 알림 채널 ID
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1; // 알림 권한 요청 코드

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 알림 권한 확인
        checkNotificationPermission();

        // Notification 채널 생성 (Android 8.0 이상)
        createNotificationChannel();

        imageView1 = findViewById(R.id.imageView1);
        imageView2 = findViewById(R.id.imageView2);

        // ViewTreeObserver를 사용하여 ImageView의 크기 얻기
        imageView1.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                imageView1.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                new VideoStreamClient().execute(); // 비디오 스트림 클라이언트 시작
            }
        });
    }

    // 알림 권한 확인 및 요청
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 이상
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Notification permission granted");
            } else {
                Log.e("MainActivity", "Notification permission denied");
            }
        }
    }

    // 알림 채널 생성 (안드로이드 8.0 이상에서 필요)
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Fire Alert Channel";
            String description = "Channel for fire alerts";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // 화재 경고 알림 보내기
    private void sendFireAlertNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_fire_alert) // 알림에 사용할 작은 아이콘
                .setContentTitle("화재 경고")
                .setContentText("화재가 감지되었습니다!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true); // 클릭 시 알림이 자동으로 사라지도록 설정

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build()); // 알림 발송
    }

    private class VideoStreamClient extends AsyncTask<Void, Object[], Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Socket socket = new Socket(serverIP, serverPort);
                InputStream inputStream = socket.getInputStream();

                while (true) {
                    byte[] sizeBuffer = new byte[16];
                    int sizeRead = inputStream.read(sizeBuffer);
                    if (sizeRead == -1)
                        break;

                    String sizeString = new String(sizeBuffer).trim();
                    if (sizeString.isEmpty()) {
                        Log.e("VideoStreamClient", "Received empty size string");
                        continue;
                    }

                    Log.d("VideoStreamClient", "Received size string: " + sizeString);

                    // 카메라 ID와 크기 정보 분리
                    int cameraId = Character.getNumericValue(sizeString.charAt(0));
                    String sizeWithoutId = sizeString.substring(1).trim();
                    if (sizeWithoutId.isEmpty()) {
                        Log.e("VideoStreamClient", "Size without ID is empty");
                        continue;
                    }

                    int frameSize;
                    try {
                        frameSize = Integer.parseInt(sizeWithoutId);
                    } catch (NumberFormatException e) {
                        Log.e("VideoStreamClient", "Received invalid size: " + sizeWithoutId);
                        continue;
                    }

                    byte[] frameBuffer = new byte[frameSize];
                    int bytesRead = 0;
                    while (bytesRead < frameSize) {
                        int result = inputStream.read(frameBuffer, bytesRead, frameSize - bytesRead);
                        if (result == -1)
                            break;
                        bytesRead += result;
                    }

                    Bitmap bitmap = BitmapFactory.decodeByteArray(frameBuffer, 0, frameSize);
                    if (bitmap != null) {
                        publishProgress(new Object[]{bitmap, cameraId});
                    } else {
                        Log.e("VideoStreamClient", "Received null bitmap");
                    }
                }

                socket.close();
            } catch (IOException e) {
                Log.e("VideoStreamClient", "Error: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[]... params) {
            Bitmap bitmap = (Bitmap) params[0][0];
            int cameraId = (int) params[0][1];

            if (bitmap != null) {
                // 각 ImageView에 맞는 크기로 비트맵 리사이즈
                Bitmap resizedBitmap = resizeBitmap(bitmap, cameraId == 1 ? imageView1.getWidth() : imageView2.getWidth(),
                        cameraId == 1 ? imageView1.getHeight() : imageView2.getHeight());
                if (cameraId == 1) {
                    imageView1.setImageBitmap(resizedBitmap);
                } else if (cameraId == 2) {
                    imageView2.setImageBitmap(resizedBitmap);
                }

                // 화재 감지 시 알림 발송 (서버에서 화재 신호를 받는 경우 처리)
                if (cameraId == 1) { // 임의로 cameraId가 1일 때 화재 감지했다고 가정
                    // 알림을 비동기로 발송
                    sendFireAlertNotification(); // 화재 경고 알림 발송
                }
            }
        }
    }

    // Bitmap 리사이즈 메소드
    private Bitmap resizeBitmap(Bitmap bitmap, int targetWidth, int targetHeight) {
        if (bitmap == null)
            return null;

        // 비율 계산
        float aspectRatio = (float) bitmap.getWidth() / (float) bitmap.getHeight();
        int width, height;

        if (targetWidth / (float) targetHeight > aspectRatio) {
            // 세로가 더 좁은 경우
            width = (int) (targetHeight * aspectRatio);
            height = targetHeight;
        } else {
            // 가로가 더 좁은 경우
            width = targetWidth;
            height = (int) (targetWidth / aspectRatio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }
}