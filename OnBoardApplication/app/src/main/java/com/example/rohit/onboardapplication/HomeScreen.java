package com.example.rohit.onboardapplication;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@TargetApi(23)
public class HomeScreen extends AppCompatActivity implements SimpleGestureFilter.SimpleGestureListener {

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private int REQUEST_ENABLE_BT = 1;
    private boolean mScanning = false;
    private long SCAN_PERIOD = 5000;
    private ArrayList<BluetoothDevice> BT_devices;
    private BluetoothGatt mGatt;
    private TextToSpeech tts;
    private SimpleGestureFilter detector;
    private int listIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        // Detect touched area
        detector = new SimpleGestureFilter(this,this);

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.UK);
                }
            }
        });


        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        BT_devices = new ArrayList<BluetoothDevice>();
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler();
        tts.speak("Application ready to use", TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);

    }

    private void scanLeDevice(final boolean enable) {
        if (enable && !mScanning) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            boolean push = true;
                            for(int i=0;i<BT_devices.size();i++){
                                if(BT_devices.get(i).getName().equals(device.getName())){
                                    push = false;
                                    break;
                                }
                            }
                            if(push) BT_devices.add(device);
                        }
                    });
                }
            };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            // scanLeDevice(false); // will stop after first device detection
        }
    }

    public void disconnectDevice(){
       if(mGatt==null){
           return;
       }
        mGatt.disconnect();
        mGatt.close();
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));
            System.out.print("Number of services: "+services.size()+"\n");
            System.out.print("Service: "+ services.get(2).getUuid() + " char: "+services.get(2).getCharacteristics().get(0).getUuid()+"\n");
            writeCharacteristic("Rohit");
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
            //gatt.disconnect();
        }
    };

        public boolean writeCharacteristic(String str) {

        //check mBluetoothGatt is available
        if (mGatt == null) {
            return false;
        }

        BluetoothGattService Service = mGatt.getService(mGatt.getServices().get(2).getUuid());

        if (Service == null) {
            return false;
        }

        BluetoothGattCharacteristic charac = Service.getCharacteristic(mGatt.getServices().get(2).getCharacteristics().get(0).getUuid());
        if (charac == null) {
            return false;
        }

        byte[] value = new byte[20];
        value = str.getBytes();
        charac.setValue(value);
        boolean status = mGatt.writeCharacteristic(charac);
        return status;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent me){
        // Call onTouchEvent of SimpleGestureFilter class
        this.detector.onTouchEvent(me);
        return super.dispatchTouchEvent(me);
    }
    @Override
    public void onSwipe(int direction) {
        String str = "";

        switch (direction) {
            case SimpleGestureFilter.SWIPE_RIGHT :
                // NEXT BUS
                if(BT_devices.size()==0){
                    Toast.makeText(HomeScreen.this, "no bus found", Toast.LENGTH_SHORT).show();
                    tts.speak("no bus found", TextToSpeech.QUEUE_FLUSH, null);
                } else{
                    listIndex = (listIndex+1)%BT_devices.size();
                    String bus_name = BT_devices.get(listIndex).getName();
                    Toast.makeText(HomeScreen.this, bus_name, Toast.LENGTH_SHORT).show();
                    tts.speak(bus_name, TextToSpeech.QUEUE_FLUSH, null);
                }
                break;

            case SimpleGestureFilter.SWIPE_LEFT :

                // PREV BUS
                if(BT_devices.size()==0){
                    Toast.makeText(HomeScreen.this, "no bus found", Toast.LENGTH_SHORT).show();
                    tts.speak("no bus found", TextToSpeech.QUEUE_FLUSH, null);
                } else{
                    if(listIndex==0){
                        listIndex = BT_devices.size()-1;
                    }else{
                        listIndex -= 1;
                    }
                    String bus_name = BT_devices.get(listIndex).getName();
                    Toast.makeText(HomeScreen.this, bus_name, Toast.LENGTH_SHORT).show();
                    tts.speak(bus_name, TextToSpeech.QUEUE_FLUSH, null);
                }
                break;

            case SimpleGestureFilter.SWIPE_DOWN :

                // SELECT BUS
                {
                    if(BT_devices.size()==0){
                        Toast.makeText(HomeScreen.this, "no bus to connect", Toast.LENGTH_SHORT).show();
                        tts.speak("no bus to connect", TextToSpeech.QUEUE_FLUSH, null);
                    }

                    if(listIndex==-1){
                        Toast.makeText(HomeScreen.this, "no bus selected", Toast.LENGTH_SHORT).show();
                        tts.speak("no bus selected", TextToSpeech.QUEUE_FLUSH, null);
                    }else{
                        Toast.makeText(HomeScreen.this, "connecting to  "+ BT_devices.get(listIndex).getName(), Toast.LENGTH_SHORT).show();
                        tts.speak("connecting to  "+ BT_devices.get(listIndex).getName(), TextToSpeech.QUEUE_FLUSH, null);
                        connectToDevice(BT_devices.get(listIndex));
                        while(tts.isSpeaking());
                        Toast.makeText(HomeScreen.this, "connection successfull ", Toast.LENGTH_SHORT).show();
                        tts.speak("connection successfull ", TextToSpeech.QUEUE_FLUSH, null);
                        /*disconnectDevice();
                        while (tts.isSpeaking());
                        Toast.makeText(HomeScreen.this, "diconneceted successfully ", Toast.LENGTH_SHORT).show();
                        tts.speak("disconnected successfully ", TextToSpeech.QUEUE_FLUSH, null);
                    */}
                    break;
                }

            case SimpleGestureFilter.SWIPE_UP :

                // SCAN BUS
                {
                    if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }

                    Toast.makeText(HomeScreen.this, "please wait searching buses", Toast.LENGTH_SHORT).show();
                    while (tts.isSpeaking());
                    tts.speak("please wait searching buses", TextToSpeech.QUEUE_FLUSH, null);

                    BT_devices = new ArrayList<BluetoothDevice>();
                    listIndex = -1;
                    scanLeDevice(true);

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (BT_devices.isEmpty()) {
                                Toast.makeText(HomeScreen.this, "no bus found", Toast.LENGTH_SHORT).show();
                                tts.speak("no bus found", TextToSpeech.QUEUE_FLUSH, null);
                            } else {
                                String toSpeak = Integer.toString(BT_devices.size());
                                Toast.makeText(HomeScreen.this, toSpeak + " bus found", Toast.LENGTH_SHORT).show();
                                tts.speak(toSpeak + " bus found", TextToSpeech.QUEUE_FLUSH, null);
                                while(tts.isSpeaking());
                            }
                        }
                    }, SCAN_PERIOD);
                    break;
                }
        }
    }

    @Override
    public void onDoubleTap() {
        Toast.makeText(this, "Double Tap", Toast.LENGTH_SHORT).show();
    }
}
