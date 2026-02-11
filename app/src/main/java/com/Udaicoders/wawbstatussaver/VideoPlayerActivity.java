package com.Udaicoders.wawbstatussaver;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.Udaicoders.wawbstatussaver.util.Utils;


public class VideoPlayerActivity extends AppCompatActivity {

    VideoView displayVV;
    ImageView backIV;
    LinearLayout downloadIV, shareIV;
    ProgressBar videoLoader;
    Uri videoUri;
    String videoUriString;
    boolean isDownloaded;
    boolean isPrepared = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        backIV = findViewById(R.id.backIV);
        backIV.setOnClickListener(view -> onBackPressed());

        videoLoader = findViewById(R.id.videoLoader);

        // Accept URI from intent, fallback to legacy static path
        videoUriString = getIntent().getStringExtra("videoUri");
        isDownloaded = getIntent().getBooleanExtra("isDownloaded", false);
        if (videoUriString != null) {
            videoUri = Uri.parse(videoUriString);
        } else if (Utils.mPath != null) {
            videoUriString = Utils.mPath;
            if (Utils.mPath.startsWith("content")) {
                videoUri = Uri.parse(Utils.mPath);
            } else {
                videoUri = Uri.parse("file://" + Utils.mPath);
            }
        }

        displayVV = findViewById(R.id.displayVV);

        // Show loader until video is ready
        videoLoader.setVisibility(View.VISIBLE);

        displayVV.setOnPreparedListener(mp -> {
            isPrepared = true;
            videoLoader.setVisibility(View.GONE);
            mp.setLooping(false);
            mp.start();
        });

        displayVV.setOnErrorListener((mp, what, extra) -> {
            videoLoader.setVisibility(View.GONE);
            Toast.makeText(VideoPlayerActivity.this, "Error playing video", Toast.LENGTH_SHORT).show();
            return true;
        });

        displayVV.setVideoURI(videoUri);

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(displayVV);
        displayVV.setMediaController(mediaController);

        // Action buttons
        downloadIV = findViewById(R.id.downloadIV);
        shareIV = findViewById(R.id.shareIV);

        // Hide save button if this is already a downloaded file
        if (isDownloaded) {
            downloadIV.setVisibility(View.GONE);
        }

        downloadIV.setOnClickListener(v -> {
            if (videoUriString != null) {
                try {
                    Utils.download(VideoPlayerActivity.this, videoUriString);
                    Toast.makeText(VideoPlayerActivity.this, getResources().getString(R.string.saved_success), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(VideoPlayerActivity.this, "Couldn't save video", Toast.LENGTH_SHORT).show();
                }
            }
        });

        shareIV.setOnClickListener(v -> {
            if (videoUriString != null) {
                Utils.shareFile(VideoPlayerActivity.this, true, videoUriString);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only resume playback if it was already prepared (don't double-load)
        if (isPrepared && !displayVV.isPlaying()) {
            displayVV.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (displayVV.isPlaying()) {
            displayVV.pause();
        }
    }
}
