package com.example.datacollectionapp;

/**
 * Created by Jerry C on Sept/15/2021.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class ChatUtils extends AppCompatActivity {
    // Member fields
    private Context context;
    private final Handler handler;
    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;
    private int state;
    private int writeInt;
    public static MainActivity mainActivity;

    private final UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final String APP_NAME = "BluetoothChatApp";

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    public String mLat = "-9999";
    public String mLong = "-9999";
    public String mSpeed = "-9999";

    public static String readLat;
    public static String readLong;
    public static String readSpeed;
    //private LocationManager locationManager;
    FusedLocationProviderClient mFusedLocationClient;
    int PERMISSION_ID = 44;
    private Context mContext;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public ChatUtils(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;
        mContext = context;

        state = STATE_NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Return the current connection state. */
    public int getState() {
        return state;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGED, state, -1).sendToTarget();
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    private synchronized void start() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (acceptThread == null) {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_LISTEN);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (acceptThread != null) {
            acceptThread.cancel();
            acceptThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public void connect(BluetoothDevice device) {
        if (state == STATE_CONNECTING) {
            connectThread.cancel();
            connectThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_CONNECTING);
    }





    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * //@param buffer The bytes to write
     * //@see ConnectedThread#write(byte[])
     */
    public void write() {
        // Create temporary object
        ConnectedThread connThread;
        //context = getApplicationContext();
        //this.context = context;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (state != STATE_CONNECTED) {
                return;
            }
            connThread = connectedThread;
        }

        //LocManagement();

        Log.d("WRITER FUNC: ", "About to create Newsletter Task . . . ");

        class NewsletterTask extends TimerTask{
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                if (state == STATE_CONNECTED) {
                    Log.d("NEWSLETTER: ", "Running the Timer Task");
                    Log.d("STATE: ", String.valueOf(state));

                    //getLastLocation();
                    Log.d("L O C A T I O N: ", "- - Get Last Location - - ");
                    //Log.d("L O C A T I O N: ", "Othercontext = " + MainActivity.otherContext);
                    //mainActivity.UpdateLocation(MainActivity.otherContext);
                    //mainActivity.getLastLocation();
                    //mainActivity.getUpdatedLocation();
                    Log.d("L O C A T I O N: ", "mLat = " + MainActivity.mLat);
                    Log.d("L O C A T I O N: ", "mLong = "+ MainActivity.mLong);
                    mLat = MainActivity.mLat;
                    mLong = MainActivity.mLong;
                    mSpeed = MainActivity.mSpeed;

                    connThread.write(mLat, mLong, mSpeed);
                } else {
                    cancel();
                }
            }
        }

        Timer newTimer = new Timer();

        if (state != STATE_NONE) {

            newTimer.schedule(new NewsletterTask(), 0, 3000);
            //new Timer().schedule(new NewsletterTask(), 0, 1000);
        } else {
            Log.d("Cancel Timer: ", "Cancelling TIMER!");
            newTimer.cancel();

        }

        int sentMessages = 0;
        /*while(sentMessages < 5){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            sentMessages += 1;
        }*/



        Log.d("WRITER: ", "AT THE END");

        //connThread.write(buffer);
    }


    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            // Create a new listening server socket
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID);
            } catch (IOException e) {
                Log.e("Accept->Constructor", e.toString());
            }
            serverSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                socket = serverSocket.accept();
            } catch (IOException e) {
                Log.e("Accept->Run", e.toString());
                try {
                    serverSocket.close();
                } catch (IOException e1) {
                    Log.e("Accept->Close", e1.toString());
                }
            }

            // If a connection was accepted
            if (socket != null) {
                switch (state) {
                    case STATE_LISTEN:
                    case STATE_CONNECTING:
                        // Situation normal. Start the connected thread.
                        connected(socket, socket.getRemoteDevice());
                        break;
                    case STATE_NONE:
                    case STATE_CONNECTED:
                        // Either not ready or already connected. Terminate new socket.
                        try {
                            Log.d("DEBUG", "CLOSING SOCKET IN CHATUTTILS");
                            socket.close();
                        } catch (IOException e) {
                            Log.e("Accept->CloseSocket", e.toString());
                        }
                        break;
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e("Accept->CloseServer", e.toString());
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket tmp = null;
            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e("Connect->Constructor", e.toString());
            }
            socket = tmp;
        }

        public void run() {
            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                socket.connect();
            } catch (IOException e) {
                Log.e("Connect->Run", e.toString());
                try {
                    socket.close();
                } catch (IOException e1) {
                    Log.e("Connect->CloseSocket", e1.toString());
                }
                // Start the service over to restart listening mode
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (ChatUtils.this) {
                connectThread = null;
            }

            // Start the connected thread
            connected(socket, device);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("Connect->Cancel", e.toString());
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            Log.d("READING:", "RUNNING READER > > >");
            byte[] buffer = new byte[1024];
            int bytes;
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = inputStream.read(buffer);
                    // Send the obtained bytes to the UI Activity
                    handler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e("CONNECTION ERROR", "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void read() {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Log.d("READING:", "RUNNING READER > > >");
                    byte[] buffer = new byte[1024];
                    int bytes;
                    // Keep listening to the InputStream while connected
                    while (true) {
                        try {
                            // Read from the InputStream
                            bytes = inputStream.read(buffer);
                            // Send the obtained bytes to the UI Activity
                            handler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                        } catch (IOException e) {
                            Log.e("CONNECTION ERROR", "disconnected", e);
                            connectionLost();
                            break;
                        }
                    }
                }
            }, 0, 300);
            timer.cancel();
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(String mLat, String mLong, String mSpeed) {
            /*int messagesSent = 0;
            while(messagesSent < 2) {
                try {
                    Log.d("WRITER MESSAGE: ", "TRYING TO WRITE");
                    String outputString = "Hello World!";
                    buffer = outputString.getBytes();
                    outputStream.write(buffer);
                    // Share the sent message back to the UI Activity
                    handler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();

                } catch (IOException e) {
                    Log.e("WRITING ERROR", "Exception during write", e);
                }

                messagesSent = messagesSent + 1;
            }*/
            try {
                //Log.d("WRITER MESSAGE: ", "TRYING TO WRITE");
                // Get sensor data

                /*String lat = MainActivity.latitude;
                String longit = MainActivity.longitude;
                String speed = MainActivity.speed;*/
                String lat = mLat;
                String longit = mLong;
                String speed = mSpeed;
                readLat = lat;
                readLong = longit;
                readSpeed = speed;

                //String outputString = "(Lat, Long):  (" + lat + ", " + longit + ") ;" + "Speed = " + speed;
                String outputString = lat + ";" + longit + ";"+speed;
                byte[] buffer = outputString.getBytes();
                outputStream.write(buffer);
                // Share the sent message back to the UI Activity
                handler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();

            } catch (IOException e) {
                Log.e("WRITING ERROR", "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("ClOSING ERROR", "close() of connect socket failed", e);
            }
        }
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Connection Lost");
        message.setData(bundle);
        handler.sendMessage(message);

        ChatUtils.this.start();
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private synchronized void connectionFailed() {
        // Send a failure message back to the Activity
        Message message = handler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Cant connect to the device");
        message.setData(bundle);
        handler.sendMessage(message);

        ChatUtils.this.start();
    }

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        //Log.d("TEST", "INSIDE OF connected() ");
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        Message message = handler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setState(STATE_CONNECTED);
    }
}
