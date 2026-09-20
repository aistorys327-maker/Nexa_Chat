package com.nexachat.app.calls;

import android.content.Context;
import android.media.AudioManager;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.nexachat.app.firebase.FirebaseManager;
import com.nexachat.app.models.CallSession;

/**
 * Production-ready Firebase-signaled call engine.
 * Serves as the signaling layer and audio routing controller, ready for WebRTC peer streams.
 */
public class FirebaseCallEngine implements CallEngine {

    private final Context context;
    private CallEventListener eventListener;
    private DatabaseReference activeCallRef;
    private ValueEventListener callListener;

    private boolean isMuted = false;
    private boolean isSpeakerOn = false;
    private boolean isVideoEnabled = true;

    private final AudioManager audioManager;

    public FirebaseCallEngine(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public void setEventListener(CallEventListener listener) {
        this.eventListener = listener;
    }

    @Override
    public void startCall(CallSession session) {
        if (session == null || session.getCallId() == null) return;

        activeCallRef = FirebaseManager.getInstance().getDatabase()
                .getReference("calls")
                .child(session.getCallId());

        session.setStatus(CallSession.STATUS_CALLING);
        activeCallRef.setValue(session);

        listenToCallSession(session);

        if (eventListener != null) {
            eventListener.onCallConnecting(session);
        }
    }

    @Override
    public void answerCall(CallSession session) {
        if (session == null || session.getCallId() == null) return;

        activeCallRef = FirebaseManager.getInstance().getDatabase()
                .getReference("calls")
                .child(session.getCallId());

        activeCallRef.child("status").setValue(CallSession.STATUS_ACCEPTED);
        session.setStatus(CallSession.STATUS_ACCEPTED);

        listenToCallSession(session);

        if (eventListener != null) {
            eventListener.onCallConnected(session);
        }
    }

    @Override
    public void endCall(CallSession session) {
        if (activeCallRef != null) {
            activeCallRef.child("status").setValue(CallSession.STATUS_ENDED);
            if (callListener != null) {
                activeCallRef.removeEventListener(callListener);
                callListener = null;
            }
        }

        resetAudio();

        if (eventListener != null) {
            eventListener.onCallEnded(session, "Call ended by user");
        }
    }

    private void listenToCallSession(CallSession session) {
        if (callListener != null && activeCallRef != null) {
            activeCallRef.removeEventListener(callListener);
        }

        callListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                CallSession remoteSession = snapshot.getValue(CallSession.class);
                if (remoteSession == null) return;

                if (CallSession.STATUS_ACCEPTED.equals(remoteSession.getStatus())) {
                    if (eventListener != null) {
                        eventListener.onCallConnected(remoteSession);
                    }
                } else if (CallSession.STATUS_ENDED.equals(remoteSession.getStatus())
                        || CallSession.STATUS_REJECTED.equals(remoteSession.getStatus())) {
                    resetAudio();
                    if (eventListener != null) {
                        eventListener.onCallEnded(remoteSession, "Call terminated");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (eventListener != null) {
                    eventListener.onError(error.getMessage());
                }
            }
        };

        activeCallRef.addValueEventListener(callListener);
    }

    @Override
    public void setMuted(boolean muted) {
        this.isMuted = muted;
        if (audioManager != null) {
            audioManager.setMicrophoneMute(muted);
        }
    }

    @Override
    public void setSpeakerOn(boolean speakerOn) {
        this.isSpeakerOn = speakerOn;
        if (audioManager != null) {
            audioManager.setSpeakerphoneOn(speakerOn);
        }
    }

    @Override
    public void setVideoEnabled(boolean videoEnabled) {
        this.isVideoEnabled = videoEnabled;
    }

    @Override
    public boolean isMuted() {
        return isMuted;
    }

    @Override
    public boolean isSpeakerOn() {
        return isSpeakerOn;
    }

    @Override
    public boolean isVideoEnabled() {
        return isVideoEnabled;
    }

    private void resetAudio() {
        if (audioManager != null) {
            try {
                audioManager.setMicrophoneMute(false);
                audioManager.setSpeakerphoneOn(false);
                audioManager.setMode(AudioManager.MODE_NORMAL);
            } catch (Exception ignored) {
            }
        }
    }
}
