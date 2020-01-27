/*
 * MIT License
 *
 * Copyright (c) 2020 Manuel Gundlach <manuel.gundlach@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dowscr.manuel.prass;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Vibrator;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Random;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

class Print extends AsyncTask<String, Void, Void> implements PrinterConstants {
    private final Random Rand = new Random();
    private final String TS;
    private final InputStream FileStream;
    private final String FileName;
    private final String commandoPrint;
    private final int ID;
    private final Activity activity;
    private final int NotificationID = Rand.nextInt();
    @SuppressWarnings("UnusedAssignment")
    private int PrinterID = 0;
    private String JobID = null;

    Print(Activity a,
          String FN,
          InputStream F,
          int PI,
          String cp,
          String ts,
          int id) {
        activity = a;
        FileName = FN;
        FileStream = F;
        PrinterID = PI;
        commandoPrint = cp;
        TS = ts;
        ID = id;
    }

    private String JobInQueue(String printer, String JobID) throws Exception {
        String[] s = ConnectionHandler.executeRemoteCommand("lpq -P" + printer).split("\\s+");
        for (int i = 0; i < s.length; i++) {
            if (s[i].equals(JobID))
                return s[i - 2];
        }
        return null;
    }

    private void NotificatePrinting(String title, String text, boolean bar, int progress, int NotiID) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(activity)
                        .setContentTitle(title)
                        .setSmallIcon(R.drawable.ic_white)
                        .setContentText(text)
                        .setPriority(1);

        if (bar)
            mBuilder.setProgress(4, progress, false);
        else
            mBuilder.setAutoCancel(true);

        Intent resultIntent = new Intent(activity, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(activity);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        if (bar) {
            Intent intent = new Intent(activity, ModifyJob.class).setAction("CancelClick").putExtra("ID", ID);
            PendingIntent PendingIntentCancel = PendingIntent.getService(activity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder.addAction(R.drawable.ic_cancel_white_24dp, "Cancel", PendingIntentCancel);
        }

        NotificationManager mNotificationManager =
                (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        Objects.requireNonNull(mNotificationManager).notify(NotiID, mBuilder.build());
    }

    private void vibrate() {
        if (!MainActivity.getVibrate())
            return;
        Vibrator v = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
        long[] p = {0, 500, 400, 400, 400, 500};
        Objects.requireNonNull(v).vibrate(p, -1);
    }

    private void cancelJob() {
        try {
            ConnectionHandler.executeRemoteCommand("cancel " + JobID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteRemoteFile(String f) {
        try {
            ConnectionHandler.executeRemoteCommand("rm " + "\"" + f + "\"");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCancelled() {
        if (JobID != null) {
            cancelJob();
        }
        NotificatePrinting("Cancelled Printing " + FileName, "Printing Cancelled.", false, 0, NotificationID);
        activity.finish();
    }

    @Override
    protected void onPostExecute(Void result) {
        activity.finish();
    }

    @Override
    protected Void doInBackground(String... s) {
        NotificatePrinting("Printing " + FileName, "Copying file…", true, 1, NotificationID);
        try {
            if (isCancelled()) return null;
            ConnectionHandler.copyFiletoHost(FileName, FileStream, TS);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            NotificatePrinting("Printing " + FileName + " failed", "File does not exist.", false, 0, NotificationID);
            vibrate();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            NotificatePrinting("Printing " + FileName + " failed", "Error when copying file.", false, 0, NotificationID);
            vibrate();
            deleteRemoteFile(TS + FileName);
            return null;
        }
        if (isCancelled()) return null;
        NotificatePrinting("Printing " + FileName, "Sending print commando…", true, 2, NotificationID);
        try {
            Thread.sleep(100);
            String[] JobResult = ConnectionHandler.executeRemoteCommand(commandoPrint)/*"request id is Canon_MX850_series-11 (1 file(s))"*/.split("\\s+");

            if (isCancelled()) return null;

            if (JobResult[0].equals("request") && JobResult[1].equals("id") && JobResult[2].equals("is"))
                JobID = JobResult[3].split("-")[1];
            else {
                NotificatePrinting("Printing " + FileName + " failed", "Printer " + PrinterNames[PrinterID] + " rejected job.", false, 0, NotificationID);
                vibrate();
                deleteRemoteFile(TS + FileName);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            NotificatePrinting("Printing " + FileName + " failed", "Error when sending print commando on " + PrinterNames[PrinterID] + ".", false, 0, NotificationID);
            vibrate();
            deleteRemoteFile(TS + FileName);
            return null;
        }
        if (isCancelled()) return null;
        NotificatePrinting("Printing " + FileName, "Printing…", true, 3, NotificationID);
        String lastpos = null;
        while (true) {
            try {
                if (isCancelled()) return null;
                String pos = JobInQueue(PrinterIDS[PrinterID], JobID);
                if (pos == null)
                    break;
                if (!pos.equals(lastpos))
                    NotificatePrinting("Printing " + FileName, "Printing… (Rank " + pos + ")", true, 4, NotificationID);
                lastpos = pos;
                Thread.sleep(200);
            } catch (Exception e) {
                if (isCancelled()) return null;
                NotificatePrinting("Status of printing " + FileName + " unknown.", "Status of print job unknown due to connection problems.", false, 0, NotificationID);
                e.printStackTrace();
            }
        }
        if (isCancelled()) return null;
        NotificatePrinting("Printed " + FileName, "Done. Collect at room" + PrinterNames[PrinterID] + ".", false, 0, NotificationID);
        vibrate();
        deleteRemoteFile(TS + FileName);
        return null;
    }
}