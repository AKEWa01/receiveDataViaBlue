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
import java.io.InputStreamReader;
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
    private static final String FILE_NAME = "example.txt";
    public static BluetoothAdapter bluetoothAdapter;
    public static GraphQL gql;
    ListView lv_paired_devices;
    TextView msg_box;
    EditText writeNa;
    boolean bluTT = false;
    Button send, readFile, remove, readGPS;
    Set<BluetoothDevice> set_pairedDevices;
    ArrayAdapter adapter_paired_devices;
    //SendReceive sendReceive;
    String tx = "";
    ArrayList<String> local = new ArrayList<>();
    //public static final int STATE_MESSAGE_RECEIVED = 5;
    String bluetooth_message = "00";
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
//                case MESSAGE_WRITE:
//
//                    if (msg_type.obj != null) {
//                        ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket) msg_type.obj);
//                        connectedThread.write(bluetooth_message.getBytes());
//
//                    }
//                    break;
//
//                case CONNECTED:
//                    Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
//                    bluTT = true;
//                    msg_box.setText("Hell+");
//                    break;
//
//                case CONNECTING:
//                    Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
//                    break;

                case NO_SOCKET_FOUND:
                    Toast.makeText(getApplicationContext(), "No socket found", Toast.LENGTH_SHORT).show();
                    break;

//                case STATE_MESSAGE_RECEIVED:
//                    byte[] readbuffer = (byte[]) msg_type.obj;
//                    String tempMsg = new String(readbuffer,0, msg_type.arg1);
//                    //IO.saveFile(tempMsg);
//                    tx=tx + tempMsg;
//                    break;
            }
        }
    };
    private String AUTH_USER = "Maira";
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

        gql = new GraphQL(AUTH_TOKEN);
        gql.setSizePerRound(500);
        gql.setMaxTimes(3);
        BlueData.tx = tx;
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
                BlueData.device = device;

                Intent serviceIntent = new Intent(MainActivity.this, ExampleService.class);
                serviceIntent.putExtra("inputExtra", "Click Dev");
                serviceIntent.setAction(Constants.ACTION.START_ACTION);
                startService(serviceIntent);

                Toast.makeText(getApplicationContext(), "device choosen " + device.getName(), Toast.LENGTH_SHORT).show();
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                msg_box.setText(String.valueOf(BlueData.socket));
                String input = "AKEWaNaja";
                Intent serviceIntent = new Intent(MainActivity.this, ExampleService.class);
                serviceIntent.putExtra("inputExtra", input);
                serviceIntent.setAction(Constants.ACTION.START_ACTION);
                startService(serviceIntent);
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
        Intent serviceIntent = new Intent(MainActivity.this, ExampleService.class);
        serviceIntent.setAction(Constants.ACTION.STOP_ACTION);
        super.onDestroy();
    }

    @Override
    protected void onStop() {

        super.onStop();
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


//    private class ConnectThread extends Thread {
//        private final BluetoothSocket mmSocket ;
//        private final BluetoothDevice mmDevice;
//
//        public ConnectThread(BluetoothDevice device) {
//            // Use a temporary object that is later assigned to mmSocket,
//            // because mmSocket is final
//            BluetoothSocket tmp = null;
//            mmDevice = device;
//
//            // Get a BluetoothSocket to connect with the given BluetoothDevice
//            try {
//                // MY_UUID is the app's UUID string, also used by the server code
//                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
//            } catch (IOException e) {
//            }
//            mmSocket = tmp;
//        }
//
//        public void run() {
//            // Cancel discovery because it will slow down the connection
//            bluetoothAdapter.cancelDiscovery();
//
//            try {
//                // Connect the device through the socket. This will block
//                // until it succeeds or throws an exception
//                mHandler.obtainMessage(CONNECTING).sendToTarget();
//
//                mmSocket.connect();
//                bluTT = true;
//                //msg_box.setText(String.valueOf(mmSocket));
//                //sendReceive = new SendReceive(mmSocket);
//
//                BlueData.socket=mmSocket;
//                String input = "mm : "+String.valueOf(BlueData.socket);
//                Intent serviceIntent = new Intent(MainActivity.this, ExampleService.class);
//                serviceIntent.putExtra("inputExtra", input);
//                serviceIntent.setAction(Constants.ACTION.START_ACTION);
//                startService(serviceIntent);
//
//                //sendReceive.start();
//
//            } catch (IOException connectException) {
//                // Unable to connect; close the socket and get out
//                try {
//                    mmSocket.close();
//                } catch (IOException closeException) {
//                }
//                return;
//            }
//
//            // Do work to manage the connection (in a separate thread)
////            bluetooth_message = "Initial message";
////            mHandler.obtainMessage(MESSAGE_WRITE,mmSocket).sendToTarget();
//        }
//
//        /**
//         * Will cancel an in-progress connection, and close the socket
//         */
//        public void cancel() {
//            try {
//                mmSocket.close();
//            } catch (IOException e) {
//            }
//        }
//    }
//
//    private class ConnectedThread extends Thread {
//
//        private final BluetoothSocket mmSocket;
//        private final InputStream mmInStream;
//        private final OutputStream mmOutStream;
//
//        public ConnectedThread(BluetoothSocket socket) {
//            mmSocket = socket;
//            InputStream tmpIn = null;
//            OutputStream tmpOut = null;
//
//            // Get the input and output streams, using temp objects because
//            // member streams are final
//            try {
//                tmpIn = socket.getInputStream();
//                tmpOut = socket.getOutputStream();
//            } catch (IOException e) {
//            }
//
//            mmInStream = tmpIn;
//            mmOutStream = tmpOut;
//        }
//
//        public void run() {
//            byte[] buffer = new byte[2];  // buffer store for the stream
//            int bytes; // bytes returned from read()
//
//            // Keep listening to the InputStream until an exception occurs
//            while (true) {
//                try {
//                    // Read from the InputStream
//                    bytes = mmInStream.read(buffer);
//                    // Send the obtained bytes to the UI activity
//                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
//
//                } catch (IOException e) {
//                    break;
//                }
//            }
//        }
//
//        /* Call this from the main activity to send data to the remote device */
//        public void write(byte[] bytes) {
//            try {
//                mmOutStream.write(bytes);
//            } catch (IOException e) {
//            }
//        }
//
//        /* Call this from the main activity to shutdown the connection */
//        public void cancel() {
//            try {
//                mmSocket.close();
//            } catch (IOException e) {
//            }
//        }
//    }
//    private class SendReceive extends Thread{
//        private final BluetoothSocket bluetoothSocket;
//        private final InputStream inStream;
//        private final OutputStream outStream;
//        public SendReceive(BluetoothSocket socket){
//            bluetoothSocket=socket;
//            InputStream tempIn=null;
//            OutputStream tempOut=null;
//            try {
//                tempIn=bluetoothSocket.getInputStream();
//                tempOut=bluetoothSocket.getOutputStream();
//            }catch (IOException e){
//                e.printStackTrace();
//            }
//            inStream=tempIn;
//            outStream=tempOut;
//        }
//        public void run(){
//            byte[] buffer = new byte[10000];
//            int bytes;
//            while (true){
//                try {
//                    bytes=inStream.read(buffer);
//                    mHandler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
//                }catch (IOException e){
//                    e.printStackTrace();
//                }
//            }
//        }
//        public void write(byte[] bytes){
//                try {
//                    outStream.write(bytes);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//        }
//    }

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
                    //sendReceive=new SendReceive(socket);
                    BlueData.socket = socket;

                    String input = "mm : " + String.valueOf(BlueData.socket);
                    Intent serviceIntent = new Intent(MainActivity.this, ExampleService.class);
                    serviceIntent.putExtra("inputExtra", input);
                    serviceIntent.setAction(Constants.ACTION.START_ACTION);
                    startService(serviceIntent);

                    //sendReceive.start();
                }
            }
        }
    }
}