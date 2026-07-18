package com.example;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import com.google.android.material.button.MaterialButton;

@UnstableApi
public class PlayerActivity extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer player;
    private ProgressBar playerProgressBar;
    private LinearLayout errorLayout;
    private TextView txtErrorMessage;
    
    private String videoUrl;
    private String videoTitle;
    private String videoProtocol;

    private int currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    private boolean isLocked = false;
    private boolean playWhenReady = true;
    private int currentItem = 0;
    private long playbackPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Extract stream details from intent
        if (getIntent() != null) {
            videoUrl = getIntent().getStringExtra("video_url");
            videoTitle = getIntent().getStringExtra("video_title");
            videoProtocol = getIntent().getStringExtra("video_protocol");
        }

        if (videoTitle == null) videoTitle = "Stream Playback";
        if (videoProtocol == null) videoProtocol = "auto";

        // Bind standard views
        playerView = findViewById(R.id.playerView);
        playerProgressBar = findViewById(R.id.playerProgressBar);
        errorLayout = findViewById(R.id.errorLayout);
        txtErrorMessage = findViewById(R.id.txtErrorMessage);
        MaterialButton btnRetry = findViewById(R.id.btnRetry);

        btnRetry.setOnClickListener(v -> {
            errorLayout.setVisibility(View.GONE);
            initializePlayer();
        });

        // Initialize HUD / Custom controls action listeners
        setupCustomControls();
    }

    private void setupCustomControls() {
        // Back Button
        View btnBack = playerView.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Title and Subtitle Setup
        TextView txtVideoTitle = playerView.findViewById(R.id.txtVideoTitle);
        if (txtVideoTitle != null) {
            txtVideoTitle.setText(videoTitle);
        }

        TextView txtVideoSubtitle = playerView.findViewById(R.id.txtVideoSubtitle);
        if (txtVideoSubtitle != null) {
            txtVideoSubtitle.setText("Source: " + videoUrl);
        }

        TextView txtFormatBadge = playerView.findViewById(R.id.txtFormatBadge);
        if (txtFormatBadge != null) {
            txtFormatBadge.setText(videoProtocol.toUpperCase());
            if ("hls".equals(videoProtocol)) {
                txtFormatBadge.setTextColor(0xFF03A9F4);
            } else if ("dash".equals(videoProtocol)) {
                txtFormatBadge.setTextColor(0xFFFFB74D);
            } else {
                txtFormatBadge.setTextColor(0xFF81C784);
            }
        }

        // Lock Control Screen Setup
        View btnLock = playerView.findViewById(R.id.btnLock);
        View btnUnlockScreen = playerView.findViewById(R.id.btnUnlockScreen);
        View lockedOverlay = playerView.findViewById(R.id.lockedOverlay);

        if (btnLock != null && lockedOverlay != null) {
            btnLock.setOnClickListener(v -> {
                isLocked = true;
                lockedOverlay.setVisibility(View.VISIBLE);
                playerView.hideController();
            });
        }

        if (btnUnlockScreen != null && lockedOverlay != null) {
            btnUnlockScreen.setOnClickListener(v -> {
                isLocked = false;
                lockedOverlay.setVisibility(View.GONE);
                playerView.showController();
            });
        }

        // Aspect Ratio Selector Click listener
        View btnAspectRatio = playerView.findViewById(R.id.btnAspectRatio);
        TextView lblResizeMode = playerView.findViewById(R.id.lblResizeMode);

        if (btnAspectRatio != null) {
            btnAspectRatio.setOnClickListener(v -> {
                if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                    currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL;
                    if (lblResizeMode != null) lblResizeMode.setText("Resize: Fill");
                } else if (currentResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                    currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
                    if (lblResizeMode != null) lblResizeMode.setText("Resize: Zoom");
                } else {
                    currentResizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
                    if (lblResizeMode != null) lblResizeMode.setText("Resize: Fit");
                }
                playerView.setResizeMode(currentResizeMode);
            });
        }
    }

    private void initializePlayer() {
        if (videoUrl == null) return;

        playerProgressBar.setVisibility(View.VISIBLE);

        // Build player
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Map MIME types explicitly so ExoPlayer configures the exact decoders/parsers
        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(videoUrl);
        
        if ("hls".equals(videoProtocol)) {
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8);
        } else if ("dash".equals(videoProtocol)) {
            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD);
        } else if ("mp4".equals(videoProtocol)) {
            mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP4);
        }
        
        MediaItem mediaItem = mediaItemBuilder.build();
        player.setMediaItem(mediaItem);
        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentItem, playbackPosition);
        
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_BUFFERING) {
                    playerProgressBar.setVisibility(View.VISIBLE);
                } else {
                    playerProgressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                playerProgressBar.setVisibility(View.GONE);
                errorLayout.setVisibility(View.VISIBLE);
                txtErrorMessage.setText(error.getLocalizedMessage() != null ? 
                        error.getLocalizedMessage() : "Unable to establish secure stream playback.");
            }
        });

        player.prepare();
    }

    private void releasePlayer() {
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            playbackPosition = player.getCurrentPosition();
            currentItem = player.getCurrentMediaItemIndex();
            player.release();
            player = null;
        }
    }

    private void hideSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT > 23) {
            initializePlayer();
            if (playerView != null) playerView.onResume();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        if (Build.VERSION.SDK_INT <= 23 || player == null) {
            initializePlayer();
            if (playerView != null) playerView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT <= 23) {
            if (playerView != null) playerView.onPause();
            releasePlayer();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT > 23) {
            if (playerView != null) playerView.onPause();
            releasePlayer();
        }
    }

    @Override
    public void onBackPressed() {
        // Handle physical back button while screen locked
        if (isLocked) {
            // Do nothing or remind user to unlock first
            return;
        }
        super.onBackPressed();
    }
}
