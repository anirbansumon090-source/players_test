package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText urlInputEditText;
    private TextInputLayout urlInputLayout;
    private RadioGroup protocolRadioGroup;
    private LinearLayout streamsContainer;

    // A simple Model class to represent pre-configured streams
    private static class StreamItem {
        String title;
        String subtitle;
        String url;
        String protocol; // "hls", "dash", "mp4", "auto"
        String icon;

        StreamItem(String title, String subtitle, String url, String protocol, String icon) {
            this.title = title;
            this.subtitle = subtitle;
            this.url = url;
            this.protocol = protocol;
            this.icon = icon;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find views
        urlInputEditText = findViewById(R.id.urlInputEditText);
        urlInputLayout = findViewById(R.id.urlInputLayout);
        protocolRadioGroup = findViewById(R.id.protocolRadioGroup);
        streamsContainer = findViewById(R.id.streamsContainer);
        MaterialButton btnPlayCustom = findViewById(R.id.btnPlayCustom);

        // Pre-fill a default stream to make it easy to start
        urlInputEditText.setText("https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8");

        // Set up standard toolbar if present
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Universal Stream Player");
        }

        // Play Custom URL Listener
        btnPlayCustom.setOnClickListener(v -> {
            String url = urlInputEditText.getText() != null ? urlInputEditText.getText().toString().trim() : "";
            if (TextUtils.isEmpty(url)) {
                urlInputLayout.setError("Stream URL is required");
                return;
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                urlInputLayout.setError("Must start with http:// or https://");
                return;
            }
            urlInputLayout.setError(null);

            // Determine protocol from selection
            String protocol = "auto";
            int selectedId = protocolRadioGroup.getCheckedRadioButtonId();
            if (selectedId == R.id.radioHls) {
                protocol = "hls";
            } else if (selectedId == R.id.radioDash) {
                protocol = "dash";
            } else if (selectedId == R.id.radioMp4) {
                protocol = "mp4";
            } else {
                // Auto guess based on URL extension
                protocol = autoDetectProtocol(url);
            }

            playVideo(url, "Custom Stream", protocol);
        });

        // Initialize and display pre-configured channels
        setupPreconfiguredStreams();
    }

    private String autoDetectProtocol(String url) {
        String lowercaseUrl = url.toLowerCase();
        if (lowercaseUrl.contains(".m3u8")) {
            return "hls";
        } else if (lowercaseUrl.contains(".mpd")) {
            return "dash";
        } else {
            return "mp4"; // Default progressive
        }
    }

    private void setupPreconfiguredStreams() {
        List<StreamItem> streams = new ArrayList<>();
        
        streams.add(new StreamItem(
                "Sintel (HLS Multi-Bitrate)",
                "Official high-quality adaptive HLS video streaming sample",
                "https://bitmovin-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
                "hls",
                "🎬"
        ));

        streams.add(new StreamItem(
                "Tears of Steel (DASH / MPD)",
                "Blender open movie Sci-Fi stream in standard DASH container",
                "https://sec.ch9.ms/ch9/53a8/12586b3a-59fb-4f27-bc9d-b40b8fec53a8/TearsOfSteelDASH_high.ism/manifest(format=mpd-time-csf)",
                "dash",
                "🚀"
        ));

        streams.add(new StreamItem(
                "Big Buck Bunny (DASH / MPD)",
                "Envivio multi-bitrate test stream in MPEG-DASH protocol",
                "https://dash.akamaized.net/envivio/EnvivioDash3/manifest.mpd",
                "dash",
                "🐰"
        ));

        streams.add(new StreamItem(
                "Big Buck Bunny (HLS Standard)",
                "Classic animated bunny film stream over HLS protocol",
                "https://multiplatform-f.akamaihd.net/i/multi/will/bunny/big_buck_bunny_,640x360_400,640x360_700,640x360_1000,640x360_1500,.f4v.csmil/master.m3u8",
                "hls",
                "🌳"
        ));

        streams.add(new StreamItem(
                "Big Buck Bunny (Progressive MP4)",
                "Standard high-compatibility progressive HTTP MP4 file",
                "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                "mp4",
                "📱"
        ));

        streams.add(new StreamItem(
                "Tears of Steel (Progressive MP4)",
                "Alternative standard 4K sci-fi test clip progressive stream",
                "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                "mp4",
                "🛠️"
        ));

        LayoutInflater inflater = LayoutInflater.from(this);
        for (StreamItem stream : streams) {
            View itemView = inflater.inflate(R.layout.item_stream, streamsContainer, false);
            
            TextView iconText = itemView.findViewById(R.id.iconText);
            TextView streamTitle = itemView.findViewById(R.id.streamTitle);
            TextView streamSubtitle = itemView.findViewById(R.id.streamSubtitle);
            TextView streamTypeBadge = itemView.findViewById(R.id.streamTypeBadge);

            iconText.setText(stream.icon);
            streamTitle.setText(stream.title);
            streamSubtitle.setText(stream.subtitle);
            streamTypeBadge.setText(stream.protocol.toUpperCase());

            // Set badge color based on type
            if ("hls".equals(stream.protocol)) {
                streamTypeBadge.setTextColor(0xFF03A9F4); // light blue
            } else if ("dash".equals(stream.protocol)) {
                streamTypeBadge.setTextColor(0xFFFFB74D); // orange
            } else {
                streamTypeBadge.setTextColor(0xFF81C784); // green
            }

            itemView.setOnClickListener(v -> playVideo(stream.url, stream.title, stream.protocol));

            streamsContainer.addView(itemView);
        }
    }

    private void playVideo(String url, String title, String protocol) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("video_url", url);
        intent.putExtra("video_title", title);
        intent.putExtra("video_protocol", protocol);
        startActivity(intent);
    }
}
