package com.nexachat.app.calls;

import android.content.Context;

import com.nexachat.app.models.CallSession;

public class CallManager {

    private static volatile CallManager instance;
    private CallEngine callEngine;
    private CallSession currentSession;

    private CallManager(Context context) {
        this.callEngine = new FirebaseCallEngine(context);
    }

    public static CallManager getInstance(Context context) {
        if (instance == null) {
            synchronized (CallManager.class) {
                if (instance == null) {
                    instance = new CallManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * Plug in a custom third-party engine (e.g. WebRTC, Agora, Twilio).
     */
    public void setCustomCallEngine(CallEngine engine) {
        if (engine != null) {
            this.callEngine = engine;
        }
    }

    public CallEngine getCallEngine() {
        return callEngine;
    }

    public CallSession getCurrentSession() {
        return currentSession;
    }

    public void setCurrentSession(CallSession session) {
        this.currentSession = session;
    }
}
