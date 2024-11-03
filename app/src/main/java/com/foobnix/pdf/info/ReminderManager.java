package com.foobnix.pdf.info;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class ReminderManager {

    private static ReminderManager instance;
    private Context context;
    private AlarmManager alarmManager;

    private ReminderManager(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new ReminderManager(context);
        }
    }

    public static synchronized ReminderManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ReminderManager is not initialized, call init() method first.");
        }
        return instance;
    }

    public void setReminder(String reminderId, long time) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("reminderId", reminderId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, reminderId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent);
        Prefs.get().putReminder(reminderId, time);
    }

    public void cancelReminder(String reminderId) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, reminderId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
        Prefs.get().putReminder(reminderId, 0);
    }

    public long getReminderTime(String reminderId) {
        return Prefs.get().getReminder(reminderId);
    }
}
