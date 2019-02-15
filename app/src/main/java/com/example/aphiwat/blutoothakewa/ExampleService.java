package com.example.aphiwat.blutoothakewa;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import java.util.UUID;

import static com.example.aphiwat.blutoothakewa.MainActivity.gql;


public class ExampleService extends Service {
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int MESSAGE_READ = 0;
    public static final int MESSAGE_WRITE = 1;
    public static final int CONNECTING = 2;
    public static final int CONNECTED = 3;
    public static final int NO_SOCKET_FOUND = 4;
    public static final int STATE_MESSAGE_RECEIVED = 5;
    private final static String FOREGROUND_CHANNEL_ID = "foreground_channel_id";
    private static final String FILE_NAME = "example.txt";
    private static int stateService = Constants.STATE_SERVICE.NOT_CONNECTED;
    BluetoothAdapter bluetoothAdapter;
    String bluetooth_message = "00";
    String tx = "";
    String prevTempMsg = "";
    boolean bluTT = false;
    int count = 0;
    SendReceive sendReceive;
    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg_type) {
            super.handleMessage(msg_type);
            switch (msg_type.what) {
                case MESSAGE_WRITE:

                    if (msg_type.obj != null) {
                        ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket) msg_type.obj);
                        connectedThread.write(bluetooth_message.getBytes());

                    }
                    break;

                case CONNECTED:
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                    bluTT = true;
                    break;

                case CONNECTING:
                    Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
                    break;

                case STATE_MESSAGE_RECEIVED:

                    byte[] readbuffer = (byte[]) msg_type.obj;
                    String tempMsg = new String(readbuffer, 0, msg_type.arg1);
                    //IO.saveFile(tempMsg);

                    if (tempMsg.contains(prevTempMsg)) {
                        tx = tx + tempMsg.replace(prevTempMsg, "");
                    } else {
                        tx = tx + tempMsg;
                    }
                    prevTempMsg = tempMsg;

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
                            if (len > 10 && i.substring(0, 4).equals("push") && i.substring(len - 1).equals(")")) {
                                j += "[" + i + "], ";
                                gql.addQueue(i);
                            }
                        }

                        Log.d("GraphQL", "size:" + gql.getQueue().size() + ", isSend:" + gql.isSend());
                        Log.d("GraphQL", "li:" + j);
                    }

                    gql.send();

                    break;
            }
        }
    };
    private NotificationManager mNotificationManager;

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
                bluetoothAdapter = MainActivity.bluetoothAdapter;
                sendReceive = null;
                Log.d("GGez", "Received user starts foreground intent");
                startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification(input));
                tx = BlueData.tx;

                startConnectBlue();

                //connect();
                break;
            case Constants.ACTION.STOP_ACTION:
                sendReceive.stop();
                stopForeground(true);
                stopSelf();
                break;
            case Constants.ACTION.RE_ACTION:
                startConnectBlue();
                Log.d("Receiveev", "Re");
                startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification("ReConect"));
                break;
            default:
                sendReceive.stop();
                stopForeground(true);
                stopSelf();
        }
        //do heavy work on a background thread
        //stopSelf();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        sendReceive = null;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startConnectBlue() {
        ConnectThread connectThread = new ConnectThread(BlueData.device);
        connectThread.start();
    }

    private void connect() {
        // after 10 seconds its connected
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        Log.d("GGez", "Bluetooth Low Energy device is connected!!");
                        Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_SHORT).show();
                        stateService = Constants.STATE_SERVICE.CONNECTED;
                        startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification("AKE_Wa"));
                    }
                }, 10000);
    }

    /////////////////////////////

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
        //
        Intent reIntent = new Intent(this, ExampleService.class);
        reIntent.setAction(Constants.ACTION.RE_ACTION);
        PendingIntent pendingReIntent = PendingIntent.getService(this, 0, reIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.popup);
        remoteViews.setOnClickPendingIntent(R.id.btn_stop, pendingStopIntent);
        remoteViews.setOnClickPendingIntent(R.id.re, pendingReIntent);

        // if it is connected
        switch (stateService) {
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
            while (true) {
                try {
                    byte[] buffer = new byte[100000];
                    int bytes;
                    bytes = inStream.read(buffer);
                    Log.d("Receiveev", "yoyo");
                    mHandler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d("Receiveev", "WTF");
                    startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification("Disconect"));
//                    try {
//                        Thread.sleep(10000);
//                    } catch (InterruptedException e1) {
//                        e1.printStackTrace();
//                    }
                    return;
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

    //////////////////////////////////////////////////////
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mHandler.obtainMessage(CONNECTING).sendToTarget();

                mmSocket.connect();
                bluTT = true;
                //msg_box.setText(String.valueOf(mmSocket));
                sendReceive = new SendReceive(mmSocket);
                sendReceive.start();
                Log.d("Connn", "yoyo");
                startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification("Connect"));

            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
            //bluetooth_message = "Initial message";
            //mHandler.obtainMessage(MESSAGE_WRITE,mmSocket).sendToTarget();
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }


    ///////////////////////////////////////////////////////

    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[2];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();

                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
}
