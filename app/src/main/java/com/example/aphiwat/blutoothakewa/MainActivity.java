package com.example.aphiwat.blutoothakewa;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_ENABLE_BT = 1;
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int MESSAGE_READ = 0;
    public static final int MESSAGE_WRITE = 1;
    public static final int CONNECTING = 2;
    public static final int CONNECTED = 3;
    public static final int NO_SOCKET_FOUND = 4;
    public static final int STATE_MESSAGE_RECEIVED = 5;
    private static final String FILE_NAME = "example.txt";
    private static final String FILE_LOCAL = "Blue.dat";
    ListView lv_paired_devices;
    TextView msg_box;
    EditText writeNa;
    boolean bluTT = false;
    Button send, readFile, remove, readGPS;
    Set<BluetoothDevice> set_pairedDevices;
    ArrayAdapter adapter_paired_devices;
    BluetoothAdapter bluetoothAdapter;
    //SendReceive sendReceive;
    String tx = "";
    String prevTempMsg = "Maira";
    ArrayList<String> local = new ArrayList<>();
    String bluetooth_message = "00";

    private GraphQL gql;
    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg_type) {
            super.handleMessage(msg_type);
            switch (msg_type.what) {
                case MESSAGE_READ:

                    byte[] readbuf = (byte[]) msg_type.obj;
                    String string_recieved = new String(readbuf);
                    //do some task based on recieved string
                    break;
                case MESSAGE_WRITE:

                    if (msg_type.obj != null) {
                        ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket) msg_type.obj);
                        connectedThread.write(bluetooth_message.getBytes());

                    }
                    break;

                case CONNECTED:
                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                    bluTT = true;
                    msg_box.setText("Hell+");
                    break;

                case CONNECTING:
                    Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
                    break;

                case NO_SOCKET_FOUND:
                    Toast.makeText(getApplicationContext(), "No socket found", Toast.LENGTH_SHORT).show();
                    break;

            }
        }
    };
    private String AUTH_USER = "Maira";

//    private String AUTH_USER = "volunteer3";
//    private String AUTH_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6InZvbHVudGVlcjMiLCJleHAiOjE1NDA3OTczMTcsIm9yaWdfaWF0IjoxNTQwNzk3MDE3fQ.LqiKgrYxtxLyAzYfV_I3_EHNu6HiEPVdmE1-aJFNsNU";

//    private String AUTH_USER = "orachat.ch";
//    private String AUTH_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6Im9yYWNoYXQuY2giLCJleHAiOjE1NDU2NDg3ODksIm9yaWdJYXQiOjE1NDU2NDg0ODl9.a1NXZtqFa04vOlK2jIT34CCjKk4iERzqzzFPKfl2vx0";

//    private String AUTH_USER = "test_smartwatch";
//    private String AUTH_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6InRlc3Rfc21hcnR3YXRjaCIsImV4cCI6MTU0NzQ1NTczMCwib3JpZ0lhdCI6MTU0NzQ1NTQzMH0.phovlfEAp4L4XbNvk_2IuzUpHXVviVmRVhH4lyUpkiU";

//    private String AUTH_USER = "robot1";
//    private String AUTH_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6InJvYm90MSIsImV4cCI6MTU0Nzc5NDUzOCwib3JpZ0lhdCI6MTU0Nzc5NDIzOH0.sQExDIT97pm7UeJCLeWrWWhkFfZfDFbWZhyExWlBY48";

    //    private String AUTH_USER = "robot2";
//    private String AUTH_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6InJvYm90MiIsImV4cCI6MTU0Nzc5NDQ3MSwib3JpZ0lhdCI6MTU0Nzc5NDE3MX0.e7xVjPlLwS44XBs3EpK
    private String AUTH_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VybmFtZSI6Ik1haXJhS3V5YW1hIiwiZXhwIjoxNTQ4NDI3MzI4LCJvcmlnSWF0IjoxNTQ4NDI3MDI4fQ.V-hlVNVH0Yk5FYtiNdqY7xJ-6AoazRsLsKoEFMxDbc8";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tx = load();
        setContentView(R.layout.activity_main);
        initialize_layout();
        initialize_bluetooth();
        start_accepting_connection();
        initialize_clicks();
        msg_box.setText(String.valueOf(loadB()));

        gql = new GraphQL(AUTH_TOKEN);
        gql.setSizePerRound(100);
        gql.setMaxTimes(3);
    }

    public void start_accepting_connection() {
        //call this on button click as suited by you

        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start();
        Toast.makeText(getApplicationContext(), "accepting", Toast.LENGTH_SHORT).show();
    }

    public void initialize_clicks() {
        lv_paired_devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object[] objects = set_pairedDevices.toArray();
                BluetoothDevice device = (BluetoothDevice) objects[position];
                saveB(device);
                msg_box.setText(String.valueOf(loadB()));
                ConnectThread connectThread = new ConnectThread(device);
                connectThread.start();

                Toast.makeText(getApplicationContext(), "device choosen " + device.getName(), Toast.LENGTH_SHORT).show();
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String string = String.valueOf(writeNa.getText());
                if (bluTT) {
                    //sendReceive.write(string.getBytes());
                }
            }
        });
        readFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                msg_box.setText(tx);
            }
        });
        readGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] name = tx.split(";");
                if (tx != "") {
                    String a = "pushGps";
                    for (int i = 0; i < name.length; i++) {
                        if (name[i].toLowerCase().indexOf(a.toLowerCase()) != -1)
                            local.add(name[i]);
                    }
                    msg_box.setText(local.get(0));
                    for (int i = 1; i < local.size(); i++) {
                        msg_box.append(local.get(i));
                    }
                }
                //msg_box.setText(tx);
            }
        });
        remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tx = "";
                save(tx);
            }
        });
    }

    public void initialize_layout() {
        lv_paired_devices = (ListView) findViewById(R.id.lv_paired_devices);
        msg_box = (TextView) findViewById(R.id.msgbox);
        send = (Button) findViewById(R.id.sendd);
        remove = (Button) findViewById(R.id.clear);
        readGPS = (Button) findViewById(R.id.readGps);
        readFile = (Button) findViewById(R.id.readFile);
        writeNa = (EditText) findViewById(R.id.nameNa);
        adapter_paired_devices = new ArrayAdapter(getApplicationContext(), R.layout.support_simple_spinner_dropdown_item);
        lv_paired_devices.setAdapter(adapter_paired_devices);
    }

    public void initialize_bluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Toast.makeText(getApplicationContext(), "Your Device doesn't support bluetooth. you can play as Single player", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            set_pairedDevices = bluetoothAdapter.getBondedDevices();

            if (set_pairedDevices.size() > 0) {

                for (BluetoothDevice device : set_pairedDevices) {
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address

                    adapter_paired_devices.add(device.getName() + "\n" + device.getAddress());
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        save(tx);
        super.onDestroy();
    }

    ////////////////////////////////////////////////////
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

    public String load() {
        FileInputStream fis = null;
        String a = "WTF";
        try {
            fis = openFileInput(FILE_NAME);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String text;

            while ((text = br.readLine()) != null) {
                sb.append(text).append("\n");
            }

            return sb.toString();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return a;
    }

    //////////////////////////////////////////////////
    public void saveB(BluetoothDevice a) {
        BluetoothDevice text = a;
        FileOutputStream fos = null;
        try {
            fos = openFileOutput(FILE_LOCAL, MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(text);

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

    public BluetoothDevice loadB() {
        FileInputStream fis = null;
        BluetoothDevice sb = null;
        String a = "WTF";
        try {
            fis = openFileInput(FILE_LOCAL);
            ObjectInputStream is = new ObjectInputStream(fis);
            sb = (BluetoothDevice) is.readObject();
            return sb;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb;
    }

    public class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("NAME", MY_UUID);
            } catch (IOException e) {
            }
            serverSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    // Do work to manage the connection (in a separate thread)
                    mHandler.obtainMessage(CONNECTED).sendToTarget();
                    BlueData.socket=socket;

                    String input = "mm : "+String.valueOf(BlueData.socket);
                    Intent serviceIntent = new Intent(MainActivity.this, ExampleService.class);
                    serviceIntent.putExtra("inputExtra", input);
                    serviceIntent.setAction(Constants.ACTION.START_ACTION);
                    startService(serviceIntent);

                }
            }
        }
    }

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
                BlueData.socket=mmSocket;
                String input = "mm : "+String.valueOf(BlueData.socket);
                Intent serviceIntent = new Intent(MainActivity.this, ExampleService.class);
                serviceIntent.putExtra("inputExtra", input);
                serviceIntent.setAction(Constants.ACTION.START_ACTION);
                startService(serviceIntent);


            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }

            // Do work to manage the connection (in a separate thread)
//            bluetooth_message = "Initial message"
//            mHandler.obtainMessage(MESSAGE_WRITE,mmSocket).sendToTarget();
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

    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
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