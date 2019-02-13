package com.example.aphiwat.blutoothakewa;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class ExampleService extends Service {
    private final static String FOREGROUND_CHANNEL_ID = "foreground_channel_id";
    private NotificationManager mNotificationManager;
    private static int stateService = Constants.STATE_SERVICE.NOT_CONNECTED;

    private static final String FILE_NAME = "example.txt";
    public static final int STATE_MESSAGE_RECEIVED = 5;
    String tx="";
    int count = 0;
    SendReceive sendReceive;

    private String AUTH_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6Ik1haXJhS3V5YW1hIiwiZXhwIjoxNTQ4NDI3MzI4LCJvcmlnSWF0IjoxNTQ4NDI3MDI4fQ.V-hlVNVH0Yk5FYtiNdqY7xJ-6AoazRsLsKoEFMxDbc8";
    private GraphQL gql;
    String prevTempMsg = "Maira";

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        stateService = Constants.STATE_SERVICE.NOT_CONNECTED;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");
        switch (intent.getAction()) {
            case Constants.ACTION.START_ACTION:
                Log.d("GGez", "Received user starts foreground intent");
                startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification(input));
                tx=BlueData.tx;
                if(BlueData.socket!=null) {
                    sendReceive = new SendReceive(BlueData.socket);
                    sendReceive.start();
                }
                connect();
                break;
            case Constants.ACTION.STOP_ACTION:
                stopForeground(true);
                stopSelf();
                sendReceive=null;
                break;
            default:
                stopForeground(true);
                sendReceive=null;
                stopSelf();
        }
        //do heavy work on a background thread
        //stopSelf();
        return START_NOT_STICKY;
    }
    @Override
    public void onDestroy() {
        sendReceive=null;
        super.onDestroy();
    }

    @Nullable

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void connect() {
        // after 10 seconds its connected
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        Log.d("GGez", "Bluetooth Low Energy device is connected!!");
                        Toast.makeText(getApplicationContext(),"Connected!",Toast.LENGTH_SHORT).show();
                        stateService = Constants.STATE_SERVICE.CONNECTED;
                        startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification("AKE_Wa"));
                    }
                }, 10000);
    }
    private Notification prepareNotification(String input) {
        // handle build version above android oreo
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                mNotificationManager.getNotificationChannel(FOREGROUND_CHANNEL_ID) == null) {
            CharSequence name = getString(R.string.text_name_notification);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(FOREGROUND_CHANNEL_ID, name, importance);
            channel.enableVibration(false);
            mNotificationManager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);



        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // make a stop intent
        Intent stopIntent = new Intent(this, ExampleService.class);
        stopIntent.setAction(Constants.ACTION.STOP_ACTION);
        PendingIntent pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.popup);
        remoteViews.setOnClickPendingIntent(R.id.btn_stop, pendingStopIntent);

        // if it is connected
        switch(stateService) {
            case Constants.STATE_SERVICE.NOT_CONNECTED:
                remoteViews.setTextViewText(R.id.tv_state, input);
                break;
            case Constants.STATE_SERVICE.CONNECTED:
                remoteViews.setTextViewText(R.id.tv_state, input);
                break;
        }

        // notification builder
        NotificationCompat.Builder notificationBuilder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationBuilder = new NotificationCompat.Builder(this, FOREGROUND_CHANNEL_ID);
        } else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }
        notificationBuilder
                .setContent(remoteViews)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
        }
        return notificationBuilder.build();
    }

    ///////////////////////////// BLUETOOTH///////////////////////////////////

    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg_type) {
            super.handleMessage(msg_type);
            switch (msg_type.what) {

                case STATE_MESSAGE_RECEIVED:
                    byte[] readbuffer = (byte[]) msg_type.obj;
                    String tempMsg = new String(readbuffer, 0, msg_type.arg1);
                    //IO.saveFile(tempMsg);
//                    Log.d("GraphQL", "ms: " + tempMsg);

                    if (tempMsg.contains(prevTempMsg)) {
                        tx = tx + tempMsg.replace(prevTempMsg, "");
//                        Log.d("GraphQL", "en: " + tempMsg);
                    } else {
                        tx = tx + tempMsg;
                    }
                    prevTempMsg = tempMsg;
//                    Log.d("GraphQL", "tx: " + tx);

                    String[] name = tx.split(";");

                    int n = 0;
                    if (name.length != 0 && name[name.length - 1].length() != 0) {
                        if (name[name.length - 1].substring(name[name.length - 1].length() - 1).equals(")")) {
                            n = name.length;
                            tx = "";
                            save(tx);
                        } else {
                            n = name.length - 1;
                            tx = name[n];
                        }
                    }

                    if (!gql.isSend()) {
                        String j = "";
                        for (String i : name) {
                            int len = i.length();
//                            Log.d("GraphQL", len + " : " + i);
                            if (len > 10 && i.substring(0, 4).equals("push") && i.substring(len - 1).equals(")")) {
                                j += "[" + i + "], ";
                                gql.addQueue(i);
                            }// else {
//                                Log.d("GraphQL", "tx:" + tx + ", temp:" + tempMsg + ", i:" + i);
//                            }

                        }

//                        Log.d("GraphQL", "size:" + gql.getQueue().size() + ", isSend:" + gql.isSend());
//                        Log.d("GraphQL", "li:" + j);
                    }

                    gql.send();

                    break;
            }
        }
    };



    private class SendReceive extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inStream;
        private final OutputStream outStream;

        public SendReceive(BluetoothSocket socket) {
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;
            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inStream = tempIn;
            outStream = tempOut;
        }

        public void run() {
            byte[] buffer = new byte[100000];
            int bytes;
            while (true) {
                try {
                    bytes = inStream.read(buffer);
                    mHandler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public void save(String a) {
        String text = a;
        FileOutputStream fos = null;

        try {
            fos = openFileOutput(FILE_NAME, MODE_PRIVATE);
            fos.write(text.getBytes());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
