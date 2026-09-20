package com.nexachat.app.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {

    public static String formatMessageTime(long timestamp) {
        if (timestamp <= 0) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static String formatConversationTime(long timestamp) {
        if (timestamp <= 0) return "";
        Calendar now = Calendar.getInstance();
        Calendar messageTime = Calendar.getInstance();
        messageTime.setTimeInMillis(timestamp);

        if (now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR)) {
            if (now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR)) {
                SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            } else if (now.get(Calendar.DAY_OF_YEAR) - messageTime.get(Calendar.DAY_OF_YEAR) == 1) {
                return "Yesterday";
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
                return sdf.format(new Date(timestamp));
            }
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }

    public static String formatLastSeen(long timestamp, boolean isOnline) {
        if (isOnline) {
            return "Online";
        }
        if (timestamp <= 0) {
            return "Offline";
        }
        Calendar now = Calendar.getInstance();
        Calendar lastSeenCal = Calendar.getInstance();
        lastSeenCal.setTimeInMillis(timestamp);

        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String timeStr = timeFormat.format(new Date(timestamp));

        if (now.get(Calendar.YEAR) == lastSeenCal.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == lastSeenCal.get(Calendar.DAY_OF_YEAR)) {
            return "Last seen today at " + timeStr;
        } else if (now.get(Calendar.YEAR) == lastSeenCal.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) - lastSeenCal.get(Calendar.DAY_OF_YEAR) == 1) {
            return "Last seen yesterday at " + timeStr;
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            return "Last seen " + dateFormat.format(new Date(timestamp));
        }
    }

    public static String formatDuration(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }
}
