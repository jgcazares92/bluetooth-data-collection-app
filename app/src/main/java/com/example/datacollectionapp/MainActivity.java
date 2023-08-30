package com.example.datacollectionapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * This is the main Activity that displays the current chat session.
 */
@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity {
    private Context context;
    public static Context otherContext;
    private BluetoothAdapter bluetoothAdapter;
    private ChatUtils chatUtils;

    // Layout Views
    private ListView listMainChat;
    public Button btnCollectData;
    private Button next_activity_btn;
    private ArrayAdapter<String> adapterMainChat;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGED = 0;
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;
    public static final int MESSAGE_TOAST = 4;


    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "deviceName";
    public static final String TOAST = "toast";

    // Sensors
    private SensorManager sensorManager;
    private Sensor mLinearAcceleration;
    private Sensor mAccelerometer;
    double ax, ay, az; // acceleration in x,y, and z axis
    private Sensor mGyroscope;
    double gx, gy, gz; // gyroscope x, y, and z components

    //FusedLocationProviderClient
    FusedLocationProviderClient mFusedLocationClient;
    public static LocationManager locationManager;
    public static LocationListener locationListener;

    public static String mLat; //= "-9999";
    public static String mLong; //= "-9999";
    public static String mSpeed; //= "-9999";
    public static String mAltitude;
    public static String timestamp;

    // File Writer
    public OutputStream outputStreamA;
    public OutputStreamWriter outputStreamWriterA;

    public OutputStream outputStreamB;
    public OutputStreamWriter outputStreamWriterB;

    //private File filePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "ABC.txt");
    //File currentDir = Environment.getExternalStorageDirectory();
    File currentDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath());
    //File currentDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
    private final int CREATE_FILE = 1;

    public File root;
    public File txt;
    private List<String> fileList = new ArrayList<String>();

    private boolean running = false;
    private Handler collectionHandler;

    private static final String FILE_NAME = "example.txt";
    private String mMimeType;
    public File outputFileA;
    public File outputFileB;

    private GnssStatus.Callback gnssCallback;
    public String satellitesAvailable;
    public String satellitesUsed;

    private static final String TAG = LocationServices.class.getSimpleName();
    private PowerManager.WakeLock mWakeLock = null;
    public int LOCATION_NOTIFICATION_ID = 1001;
    public String NOTIFICATION_CHANNEL_ID = "CollectionApp";


    private void setState(CharSequence subTitle) {
        getSupportActionBar().setSubtitle(subTitle);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnCollectData = findViewById(R.id.btn_collect_data);
        Log.d("Build Version: ", " " + Integer.valueOf(android.os.Build.VERSION.SDK));


        root = currentDir;
        txt = new File(root, "TXT");
        //ListDir(txt);

        checkWritePermissions();

        context = this;
        otherContext = context;
        ChatUtils.mainActivity = this;

        init();

        String folder_example = "APP_COLLECTED_DATA";
        currentDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(), folder_example);
        if (!currentDir.exists()){currentDir.mkdirs();}

        next_activity_btn = (Button) findViewById(R.id.next_activity);
        next_activity_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivity2();
            }
        });




        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(accelGyroListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        mGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(accelGyroListener, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        Log.d("Version :", "API LVL = " + Integer.valueOf(Build.VERSION.SDK));


        if (savedInstanceState != null) {
            running = savedInstanceState.getBoolean("running");
        }

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProviderEnabled(String provider) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProviderDisabled(String provider) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onLocationChanged(Location location) {
                String result = "";
                // TODO Auto-generated method stub
                mLat = String.valueOf(location.getLatitude());
                mLong = String.valueOf(location.getLongitude());
                mSpeed = String.valueOf(location.getSpeed());
                mAltitude = String.valueOf(location.getAltitude());
                result = "Location (success): " +
                        mLat +
                        mLong;
                Log.d("LOCATION TASK: ", "(0) getCurrentLocation() result: " + result);
            }
        };
        Log.d("location: ", "locationlistener = " + locationListener);
        Log.d("location: ", "locationManager = " + locationManager);
        getUpdatedLocation();
//        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//        Intent intent = new Intent(this, GPSTracker.class);
//        startService(intent);

        initCallbacks();

        {
            try {
                //File outputFile = new File(directory, "output.txt");
                int readYear = Calendar.getInstance().get(Calendar.YEAR);
                int readDate = Calendar.getInstance().get(Calendar.DATE);
                int readMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
                int readHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                int readMinute = Calendar.getInstance().get(Calendar.MINUTE);

                outputFileA = new File(currentDir, "GPS-" + readDate + "-"
                        + readMonth + "-" + readYear + "-" + readHour + readMinute + ".txt");
                outputFileB = new File(currentDir, "AccelGyro-" + readDate + "-"
                        + readMonth + "-" + readYear + "-" + readHour + readMinute + ".txt");
                Log.d("- - OUTPUT STREAM - -: ", "Created new outputFile . . . ");
                Log.d("- - OUTPUT STREAM - -: ", "File located at: " + String.valueOf(currentDir));
                outputStreamA = new FileOutputStream(outputFileA);
                outputStreamWriterA = new OutputStreamWriter(outputStreamA);

                outputStreamB = new FileOutputStream(outputFileB);
                outputStreamWriterB = new OutputStreamWriter(outputStreamB);

                writeToFile("H;M;S;MS;Latitude;Longitude;Altitude;SatellitesAvailable;SatellitesUsed");
                writeToFileB("H;M;S;MS;ax;ay;az;gx;gy;gz");

            } catch (FileNotFoundException e) {
                Log.e("EXCEPTION: ", "Failed to create output file: " + e.toString());
            }
        }

    }

    public void openActivity2() {
        Intent intent = new Intent(this, OpenExternal.class);
        startActivity(intent);
        Log.d("ACTIVITY: ", "trying to go to next activity...");
    }

    //@RequiresApi(api = Build.VERSION_CODES.P)
    public void getUpdatedLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        // Min update time is 45 s by default due to issue w/ Network Provider
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        Log.d("Checker : ", "About to get current location . . .");

    }


    private SensorEventListener accelGyroListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            String sensorName = sensorEvent.sensor.getName();
            Log.d(sensorName, "X:" + ax + "; Y:" + ay + "; Z: " + az);
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                ax = sensorEvent.values[0];
                ay = sensorEvent.values[1];
                az = sensorEvent.values[2];

            } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                gx = sensorEvent.values[0];
                gy = sensorEvent.values[1];
                gz = sensorEvent.values[2];
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };


    public void startCollectionTimer(View view) {
        running = true;
//        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CollectionApp:WakeTag");
//        mWakeLock.acquire();

        //addServiceNotification(locationManager);

        // Create handler
        collectionHandler = new Handler();

        // Call the post() method,
        // passing in a new Runnable.
        // The post() method processes
        // code without a delay,
        // so the code in the Runnable
        // will run almost immediately.
        collectionHandler.post(new Runnable() {
            @Override
            public void run() {
                if (running == true) {
                    Log.d("COLLECTION TASK: ", "Latitude = " + mLat);
                    //t[0]++;
                    int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                    int currentMinute = Calendar.getInstance().get(Calendar.MINUTE);
                    int currentSecond = Calendar.getInstance().get(Calendar.SECOND);
                    int currentMilli = Calendar.getInstance().get(Calendar.MILLISECOND);
                    timestamp = currentHour + ";" + currentMinute + ";" + currentSecond + ";" + currentMilli;

                    mLinearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);


                    adapterMainChat.add("Me (" + currentHour + ":" + currentMinute + ":" + currentSecond + "): " +
                            mLat + ", " + mLong + ", " + mAltitude);
                    writeToFile(timestamp + ";" + mLat + ";" + mLong + ";" + mAltitude + ";" + satellitesAvailable + ";" + satellitesUsed);
                    writeToFileB(timestamp + ";" + ax + ";" + ay + ";" + az + ";" + gx + ";" + gy + ";" + gz);

                    // Post the code again
                    // with a delay of half a second.
                    collectionHandler.postDelayed(this, 500);
                }
            }
        });
    }


    public void addServiceNotification(Service service){
        NotificationManagerCompat mgr = NotificationManagerCompat.from(service);
        String title = "Caption";
        String text = "Location";

        if (Build.VERSION.SDK_INT >= 26){
//            NotificationManager notificationManager = (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);
//            if (notificationManager != null){
//                NotificationChannel mChannel = notificationManager.getNotificationChannel(NotificationManager.EXTRA_NOTIFICATION_CHANNEL_ID);
//                if (mChannel == null){
//                    mChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "CollectionApp", NotificationManager.IMPORTANCE_LOW);
//                    notificationManager.createNotificationChannel(mChannel);
//                }
//            }
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(service, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(R.drawable.app_icon)
                    .setTicker(title)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(title)
                    .setContentText(text)
                    .setOnlyAlertOnce(true)
                    .setOngoing(true)
                    .build();
            service.startForeground(LOCATION_NOTIFICATION_ID, notification);
            mgr.cancel(LOCATION_NOTIFICATION_ID);
            mgr.notify(LOCATION_NOTIFICATION_ID, notification);
        }
    }

    public void stopCollectionTimer(View view) {
        Log.d("COLLECTION TASK: ", "Stopping collection...");
        running = false;
        String text = "Output file test text";
        FileOutputStream fos = null;

        try {
            outputStreamWriterA.close();
            outputStreamA.close();
            outputStreamWriterB.close();
            outputStreamB.close();

//            fos = new FileOutputStream(new File(currentDir, FILE_NAME));
//            //fos = openFileOutput(FILE_NAME, MODE_PRIVATE);
//            fos.write(text.getBytes());
            Toast.makeText(this, "Saved to " + currentDir,
                    Toast.LENGTH_LONG).show();
            // Run scanFile to update the output files
            // in the external directory.
            // FILES WILL NOT SHOW UP WITHOUT DOING THIS!
            scanFile(this, outputFileA, mMimeType);
            scanFile(this, outputFileB, mMimeType);

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

    public void initCallbacks(){
        Log.d("MyServiceTag: ", "About to try to get satellite count......");
        gnssCallback = new GnssStatus.Callback() {
            @Override
            public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                final int satelliteCount = status.getSatelliteCount();
                int usedCount = 0;
                for (int i = 0; i < satelliteCount; ++i) {
                    if (status.usedInFix(i)) {
                        ++usedCount;
                    }
                }
                satellitesAvailable = String.valueOf(satelliteCount);
                satellitesUsed = String.valueOf(usedCount);

                Log.d("MyServiceTag: ", "Satellites count = " + satelliteCount + "; Used = " + usedCount);
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.registerGnssStatusCallback(gnssCallback,
                new Handler(Looper.myLooper()));
    }

    private void init() {
        listMainChat = findViewById(R.id.list_conversation);
        adapterMainChat = new ArrayAdapter<String>(context, R.layout.message_layout);
        listMainChat.setAdapter(adapterMainChat);

    }

    public void definitCallbacks(){
        locationManager.unregisterGnssStatusCallback(gnssCallback);
    }

    public void writeToFile(String data){
        try {
            Log.d("- - - - STREAM WRITER: ", "Trying to write to file . . . ");
            Log.d("- - - - STREAM WRITER: ", "message = " + data);
            if (outputStreamWriterA == null){
                Log.e("OutputStreamError: ", "OutputStreamWriter is NULL!");
            }
            outputStreamWriterA.write( data + "\n");
        }
        catch (IOException e){
            Log.e("EXCEPTION: ", "File write failed: " + e.toString());
        }
    }

    public void writeToFileB(String data){
        try {
            if (outputStreamWriterB == null){
                Log.e("OutputStreamError: ", "OutputStreamWriter is NULL!");
            }
            outputStreamWriterB.write( data + "\n");
        }
        catch (IOException e){
            Log.e("EXCEPTION: ", "File write failed: " + e.toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void checkWritePermissions() {
        if(Build.VERSION.SDK_INT>=23){
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    public void scanFile(Context context, File f, String mimeType){
        MediaScannerConnection.scanFile(context, new String[] {f.getAbsolutePath()},
                new String[] {mimeType}, null);
    }

    public File commonDocumentDirPath(String FolderName){
        File dir = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + FolderName);
            dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath());
        } else {
            //dir = new File(Environment.getExternalStorageDirectory() + "/"+FolderName);
            dir = new File(Environment.getExternalStorageDirectory() + "/"+FolderName);
        }
        Toast.makeText(this, "Creating new directory: " + dir,
                Toast.LENGTH_LONG).show();
        Log.d("DIR: ", "Created new directory at: " + dir);
        return dir;
    }

    @Override
    protected void onDestroy() {
        Log.d("DESTROYER: ", "DESTROYING THE APPLICATION");
        try {
            Log.d("DESTROYER: ", "CLOSING OUTPUTSTREAM WRITER!");
            outputStreamWriterA.close();
            outputStreamA.close();
            outputStreamWriterB.close();
            outputStreamB.close();
            definitCallbacks();
            if (mWakeLock != null && mWakeLock.isHeld()) {
                mWakeLock.release();
            }
        } catch (IOException e) {
            Log.e("DESTOYER: ", "ERROR CLOSING OUTPUTSTREAM WRITER! " + e);
        }
        super.onDestroy();
        if (chatUtils != null) {
            chatUtils.stop();
        }

    }
}
