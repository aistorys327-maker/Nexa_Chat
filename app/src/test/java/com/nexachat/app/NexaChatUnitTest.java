package com.nexachat.app;

import com.nexachat.app.firebase.FirebaseManager;
import com.nexachat.app.models.CallSession;
import com.nexachat.app.models.Conversation;
import com.nexachat.app.models.Message;
import com.nexachat.app.security.SecurityHelper;
import com.nexachat.app.utils.DateTimeUtils;

import org.junit.Test;
import static org.junit.Assert.*;

public class NexaChatUnitTest {

    @Test
    public void conversationId_generation_isConsistent() {
        String uid1 = "user_abc";
        String uid2 = "user_xyz";

        String id1 = FirebaseManager.getOneToOneConversationId(uid1, uid2);
        String id2 = FirebaseManager.getOneToOneConversationId(uid2, uid1);

        assertEquals(id1, id2);
        assertEquals("user_abc_user_xyz", id1);
    }

    @Test
    public void pinHashing_isConsistentAndSecure() {
        String hash1 = SecurityHelper.hashPin("1234");
        String hash2 = SecurityHelper.hashPin("1234");
        String hash3 = SecurityHelper.hashPin("5678");

        assertNotNull(hash1);
        assertEquals(hash1, hash2);
        assertNotEquals(hash1, hash3);
        assertEquals(64, hash1.length()); // SHA-256 hex string length
    }

    @Test
    public void durationFormatting_returnsExpectedMinutesAndSeconds() {
        assertEquals("00:00", DateTimeUtils.formatDuration(0));
        assertEquals("00:45", DateTimeUtils.formatDuration(45));
        assertEquals("01:15", DateTimeUtils.formatDuration(75));
        assertEquals("10:05", DateTimeUtils.formatDuration(605));
    }

    @Test
    public void callSession_lifecycle_andDefaults() {
        CallSession session = new CallSession("call_1", "u1", "Alice", "avatar1", "u2", "Bob", CallSession.TYPE_AUDIO);

        assertEquals("call_1", session.getCallId());
        assertEquals("u1", session.getCallerId());
        assertEquals("Bob", session.getReceiverName());
        assertEquals(CallSession.TYPE_AUDIO, session.getCallType());
        assertEquals(CallSession.STATUS_CALLING, session.getStatus());

        session.setStatus(CallSession.STATUS_ACCEPTED);
        session.setDurationSeconds(120);

        assertEquals(CallSession.STATUS_ACCEPTED, session.getStatus());
        assertEquals(120, session.getDurationSeconds());
    }

    @Test
    public void messageModel_types_andStatus() {
        Message textMsg = new Message("m1", "conv1", "u1", "Alice", "Hello World");
        assertEquals(Message.TYPE_TEXT, textMsg.getMessageType());
        assertEquals(Message.STATUS_SENT, textMsg.getStatus());
        assertEquals("Hello World", textMsg.getText());

        textMsg.setStatus(Message.STATUS_READ);
        assertEquals(Message.STATUS_READ, textMsg.getStatus());
    }

    @Test
    public void statusItem_24HourExpiry_behavesCorrectly() {
        long now = System.currentTimeMillis();
        com.nexachat.app.models.StatusItem freshStatus = new com.nexachat.app.models.StatusItem(
                "s1", "u1", "Alice", "", now - (1000 * 60 * 60 * 2),
                com.nexachat.app.models.StatusItem.TYPE_IMAGE, "", "photo_url", "#0284C7", "Fresh status"
        );
        assertFalse(freshStatus.isExpired());

        com.nexachat.app.models.StatusItem expiredStatus = new com.nexachat.app.models.StatusItem(
                "s2", "u1", "Alice", "", now - (1000 * 60 * 60 * 25),
                com.nexachat.app.models.StatusItem.TYPE_IMAGE, "", "photo_url", "#0284C7", "Old status"
        );
        assertTrue(expiredStatus.isExpired());
    }

    @Test
    public void userStatusGroup_collectsAndFindsLatest() {
        com.nexachat.app.models.UserStatusGroup group = new com.nexachat.app.models.UserStatusGroup("u1", "Alice", "avatar_url");
        long now = System.currentTimeMillis();
        com.nexachat.app.models.StatusItem s1 = new com.nexachat.app.models.StatusItem(
                "s1", "u1", "Alice", "", now - 5000,
                com.nexachat.app.models.StatusItem.TYPE_TEXT, "Msg 1", "", "#0284C7", ""
        );
        com.nexachat.app.models.StatusItem s2 = new com.nexachat.app.models.StatusItem(
                "s2", "u1", "Alice", "", now - 1000,
                com.nexachat.app.models.StatusItem.TYPE_TEXT, "Msg 2", "", "#00B0FF", ""
        );
        group.addStatus(s1);
        group.addStatus(s2);

        assertEquals(2, group.getStatusList().size());
        assertEquals(s2.getTimestamp(), group.getLatestTimestamp());
        assertEquals(s2, group.getLatestStatus());
    }

    @Test
    public void firebaseManager_cleanPhoneNumber_handlesNullAndFormats() {
        assertEquals("", FirebaseManager.cleanPhoneNumber(null));
        assertEquals("", FirebaseManager.cleanPhoneNumber(""));
        assertEquals("919876543210", FirebaseManager.cleanPhoneNumber("+91 98765-43210"));
        assertEquals("123456", FirebaseManager.cleanPhoneNumber("123-456"));
    }

    @Test
    public void firebaseManager_phoneToEmail_generatesValidSyntheticEmail() {
        assertEquals("919876543210@nexachat.app", FirebaseManager.phoneToEmail("+91 98765 43210"));
        assertEquals("@nexachat.app", FirebaseManager.phoneToEmail(null));
    }

    @Test
    public void firebaseManager_normalizePasswordOrPin_enforcesLength() {
        assertEquals("1234_nexa", FirebaseManager.normalizePasswordOrPin("1234"));
        assertEquals("password123", FirebaseManager.normalizePasswordOrPin("password123"));
    }

    @Test
    public void hiddenChatManager_hashing_isSecureAndDeterministic() {
        String h1 = com.nexachat.app.security.HiddenChatManager.hashString("secretPass123");
        String h2 = com.nexachat.app.security.HiddenChatManager.hashString("secretPass123");
        String h3 = com.nexachat.app.security.HiddenChatManager.hashString("otherPass");

        assertNotNull(h1);
        assertEquals(h1, h2);
        assertNotEquals(h1, h3);
        assertEquals(64, h1.length()); // SHA-256 is 64 hex characters
    }
}

