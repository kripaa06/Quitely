package com.example.quitely.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.quitely.MainActivity;
import com.example.quitely.R;

public class NotificationHelper {

    public static final String CHANNEL_AUTOMATION = "quitely_automation_events";
    public static final String CHANNEL_STATUS = "quitely_status_channel";

    private static final int NOTIF_ID_ZONE_ENTER = 1001;
    private static final int NOTIF_ID_ZONE_EXIT = 1002;
    private static final int NOTIF_ID_SCHEDULE_START = 2001;
    private static final int NOTIF_ID_SCHEDULE_END = 2002;
    private static final int NOTIF_ID_PERMISSION = 3001;

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                // Automation Events Channel (High importance)
                NotificationChannel automationChannel = new NotificationChannel(
                        CHANNEL_AUTOMATION,
                        "Sound Automation Events",
                        NotificationManager.IMPORTANCE_HIGH
                );
                automationChannel.setDescription("Alerts when sound mode is automatically changed or restored");
                automationChannel.enableVibration(false);

                // Status Channel (Default importance)
                NotificationChannel statusChannel = new NotificationChannel(
                        CHANNEL_STATUS,
                        "Quitely Service Status",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                statusChannel.setDescription("General updates and permission status");

                manager.createNotificationChannel(automationChannel);
                manager.createNotificationChannel(statusChannel);
            }
        }
    }

    public static void showZoneEnteredNotification(Context context, String zoneName, String soundMode) {
        String title = "Silent Zone Entered";
        String message = zoneName + " detected. Phone switched to " + soundMode + ".";
        sendNotification(context, CHANNEL_AUTOMATION, NOTIF_ID_ZONE_ENTER, title, message);
    }

    public static void showZoneExitedNotification(Context context, String zoneName, String restoredMode) {
        String title = "Silent Zone Exited";
        String message = "Left " + zoneName + ". Restored to " + restoredMode + ".";
        sendNotification(context, CHANNEL_AUTOMATION, NOTIF_ID_ZONE_EXIT, title, message);
    }

    public static void showScheduleStartedNotification(Context context, String scheduleName, String soundMode) {
        String title = "Scheduled Quiet Mode";
        String message = scheduleName + " started. Sound mode set to " + soundMode + ".";
        sendNotification(context, CHANNEL_AUTOMATION, NOTIF_ID_SCHEDULE_START, title, message);
    }

    public static void showScheduleEndedNotification(Context context, String scheduleName, String restoredMode) {
        String title = "Schedule Completed";
        String message = scheduleName + " finished. Restored to " + restoredMode + ".";
        sendNotification(context, CHANNEL_AUTOMATION, NOTIF_ID_SCHEDULE_END, title, message);
    }

    public static void showPermissionRequiredNotification(Context context, String permissionTitle, String message) {
        sendNotification(context, CHANNEL_STATUS, NOTIF_ID_PERMISSION, permissionTitle, message);
    }

    private static void sendNotification(Context context, String channelId, int notificationId, String title, String message) {
        // Android 13+ check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_quitely)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
        }
    }
}
