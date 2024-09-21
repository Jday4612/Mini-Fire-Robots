package com.example.fireapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView1;
    private ImageView imageView2;
    private String serverIP = "서버 IP";  // 서버의 IP 주소 입력
    private int serverPort = 5002;  // 서버 포트

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    private class VideoStreamClient extends AsyncTask<Void, Bitmap[], Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Socket socket = new Socket(serverIP, serverPort);
                InputStream inputStream = socket.getInputStream();

                while (true) {
                    // 1. 먼저 16바이트의 크기 정보를 읽음
                    byte[] sizeBuffer = new byte[16];
                    int sizeRead = inputStream.read(sizeBuffer);
                    if (sizeRead == -1) break;  // 연결이 종료되었을 때

                    String sizeString = new String(sizeBuffer).trim();
                    int frameSize;

                    try {
                        frameSize = Integer.parseInt(sizeString);
                    } catch (NumberFormatException e) {
                        Log.e("VideoStreamClient", "Received invalid size: " + sizeString);
                        continue;  // 숫자가 아닌 경우 무시하고 다음 데이터 처리
                    }

                    // 2. 프레임 데이터를 받을 배열 준비
                    byte[] frameBuffer = new byte[frameSize];
                    int bytesRead = 0;
                    while (bytesRead < frameSize) {
                        int result = inputStream.read(frameBuffer, bytesRead, frameSize - bytesRead);
                        if (result == -1) break;
                        bytesRead += result;
                    }

                    // 3. 프레임 데이터로 Bitmap 생성
                    Bitmap bitmap = BitmapFactory.decodeByteArray(frameBuffer, 0, frameSize);
                    if (bitmap != null) {
                        publishProgress(new Bitmap[]{bitmap}); // UI 업데이트
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
        protected void onProgressUpdate(Bitmap[]... bitmaps) {
            if (bitmaps[0][0] != null) {
                // 각 ImageView에 맞는 크기로 비트맵 리사이즈
                Bitmap resizedBitmap1 = resizeBitmap(bitmaps[0][0], imageView1.getWidth(), imageView1.getHeight());
                imageView1.setImageBitmap(resizedBitmap1);

                // 두 번째 ImageView를 위한 비트맵 추가
//                Bitmap resizedBitmap2 = resizeBitmap(bitmaps[0][0], imageView2.getWidth(), imageView2.getHeight());
//                imageView2.setImageBitmap(resizedBitmap2);
            }
        }
    }

    // Bitmap 리사이즈 메소드
    private Bitmap resizeBitmap(Bitmap bitmap, int targetWidth, int targetHeight) {
        if (bitmap == null) return null;

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

