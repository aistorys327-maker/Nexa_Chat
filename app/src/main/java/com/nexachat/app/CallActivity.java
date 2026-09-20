package com.nexachat.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.nexachat.app.calls.CallEngine;
import com.nexachat.app.calls.CallManager;
import com.nexachat.app.databinding.ActivityCallBinding;
import com.nexachat.app.models.CallSession;
import com.nexachat.app.utils.DateTimeUtils;

public class CallActivity extends AppCompatActivity {

    public static final String EXTRA_CALL_SESSION = "extra_call_session";
    public static final String EXTRA_IS_INCOMING = "extra_is_incoming";

    private ActivityCallBinding binding;
    private CallManager callManager;
    private CallSession currentSession;
    private boolean isIncoming = false;

    private final Handler durationHandler = new Handler(Looper.getMainLooper());
    private int callDurationSeconds = 0;
    private boolean isCallActive = false;

    private final Runnable durationRunnable = new Runnable() {
        @Override
        public void run() {
            if (isCallActive) {
                callDurationSeconds++;
                binding.tvCallStatus.setText(DateTimeUtils.formatDuration(callDurationSeconds));
                durationHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        callManager = CallManager.getInstance(this);
        currentSession = (CallSession) getIntent().getSerializableExtra(EXTRA_CALL_SESSION);
        isIncoming = getIntent().getBooleanExtra(EXTRA_IS_INCOMING, false);

        if (currentSession == null) {
            Toast.makeText(this, "Unable to initialize call", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        callManager.setCurrentSession(currentSession);

        setupUI();
        setupCallEngine();
    }

    private void setupUI() {
        String displayName = isIncoming ? currentSession.getCallerName() : currentSession.getReceiverName();
        String avatarUrl = isIncoming ? currentSession.getCallerAvatarUrl() : null;

        binding.tvCallParticipantName.setText(displayName != null ? displayName : "Contact");
        binding.tvCallTypeBadge.setText(CallSession.TYPE_VIDEO.equalsIgnoreCase(currentSession.getCallType())
                ? "NexaChat Video Call" : "NexaChat Audio Call");

        if (!TextUtils.isEmpty(avatarUrl)) {
            Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.circle_avatar_placeholder)
                    .circleCrop()
                    .into(binding.ivCallAvatar);
        }

        binding.tvCallStatus.setText(isIncoming ? "Incoming call..." : "Calling...");

        // End Call
        binding.btnCallEnd.setOnClickListener(v -> endCall());

        // Mute toggle
        binding.btnCallMute.setOnClickListener(v -> {
            CallEngine engine = callManager.getCallEngine();
            boolean newMuted = !engine.isMuted();
            engine.setMuted(newMuted);
            binding.ivCallMuteIcon.setImageResource(newMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic);
            binding.ivCallMuteIcon.setColorFilter(getColor(newMuted ? R.color.cyan_accent : R.color.text_white));
        });

        // Speaker toggle
        binding.btnCallSpeaker.setOnClickListener(v -> {
            CallEngine engine = callManager.getCallEngine();
            boolean newSpeaker = !engine.isSpeakerOn();
            engine.setSpeakerOn(newSpeaker);
            binding.ivCallSpeakerIcon.setColorFilter(getColor(newSpeaker ? R.color.cyan_accent : R.color.text_white));
        });

        // Video toggle
        binding.btnCallVideo.setOnClickListener(v -> {
            CallEngine engine = callManager.getCallEngine();
            boolean newVideo = !engine.isVideoEnabled();
            engine.setVideoEnabled(newVideo);
            binding.ivCallVideoIcon.setColorFilter(getColor(newVideo ? R.color.cyan_accent : R.color.text_white));
            Toast.makeText(this, newVideo ? "Camera enabled" : "Camera muted", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupCallEngine() {
        CallEngine engine = callManager.getCallEngine();
        engine.setEventListener(new CallEngine.CallEventListener() {
            @Override
            public void onCallConnecting(CallSession session) {
                runOnUiThread(() -> binding.tvCallStatus.setText("Ringing..."));
            }

            @Override
            public void onCallConnected(CallSession session) {
                runOnUiThread(() -> {
                    isCallActive = true;
                    callDurationSeconds = 0;
                    binding.tvCallStatus.setText("00:00");
                    durationHandler.postDelayed(durationRunnable, 1000);
                });
            }

            @Override
            public void onCallEnded(CallSession session, String reason) {
                runOnUiThread(() -> {
                    isCallActive = false;
                    durationHandler.removeCallbacks(durationRunnable);
                    binding.tvCallStatus.setText("Call ended");
                    durationHandler.postDelayed(() -> finish(), 1200);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(CallActivity.this, "Call error: " + error, Toast.LENGTH_SHORT).show();
                    endCall();
                });
            }
        });

        if (isIncoming) {
            engine.answerCall(currentSession);
        } else {
            engine.startCall(currentSession);
        }
    }

    private void endCall() {
        isCallActive = false;
        durationHandler.removeCallbacks(durationRunnable);
        callManager.getCallEngine().endCall(currentSession);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isCallActive = false;
        durationHandler.removeCallbacks(durationRunnable);
    }
}
