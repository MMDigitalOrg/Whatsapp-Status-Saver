package com.Udaicoders.wawbstatussaver;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.Udaicoders.wawbstatussaver.util.Utils;

import java.io.File;


public class VideoPlayerActivity extends AppCompatActivity {

    VideoView displayVV;
    ImageView backIV;
    LinearLayout downloadIV, shareIV, deleteIV;
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
        deleteIV = findViewById(R.id.deleteIV);

        // Hide save button if this is already a downloaded file; show delete instead
        if (isDownloaded) {
            downloadIV.setVisibility(View.GONE);
            deleteIV.setVisibility(View.VISIBLE);
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

        deleteIV.setOnClickListener(v -> {
            if (videoUriString != null) {
                new AlertDialog.Builder(VideoPlayerActivity.this)
                        .setTitle(R.string.confirm)
                        .setMessage(R.string.del_status)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            dialog.dismiss();
                            try {
                                File file = new File(videoUriString);
                                // Validate path is within app's download directory
                                String canonicalPath = file.getCanonicalPath();
                                File downloadDir = new File(Environment.getExternalStorageDirectory(),
                                        "Download" + File.separator + getResources().getString(R.string.app_name));
                                String allowedPath = downloadDir.getCanonicalPath();
                                if (!canonicalPath.startsWith(allowedPath)) {
                                    Toast.makeText(VideoPlayerActivity.this, getResources().getString(R.string.delete_error), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                if (file.exists() && file.delete()) {
                                    Toast.makeText(VideoPlayerActivity.this, getResources().getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent();
                                    setResult(10, intent);
                                    finish();
                                } else {
                                    Toast.makeText(VideoPlayerActivity.this, getResources().getString(R.string.delete_error), Toast.LENGTH_SHORT).show();
                                }
                            } catch (Exception e) {
                                Toast.makeText(VideoPlayerActivity.this, getResources().getString(R.string.delete_error), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
                        .show();
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
