package com.nexachat.app.calls;

import com.nexachat.app.models.CallSession;

/**
 * Extensible interface for audio/video calling engines (WebRTC, Agora, Jitsi, etc.)
 */
public interface CallEngine {

    interface CallEventListener {
        void onCallConnecting(CallSession session);
        void onCallConnected(CallSession session);
        void onCallEnded(CallSession session, String reason);
        void onError(String error);
    }

    void setEventListener(CallEventListener listener);

    void startCall(CallSession session);

    void answerCall(CallSession session);

    void endCall(CallSession session);

    void setMuted(boolean muted);

    void setSpeakerOn(boolean speakerOn);

    void setVideoEnabled(boolean videoEnabled);

    boolean isMuted();

    boolean isSpeakerOn();

    boolean isVideoEnabled();
}
