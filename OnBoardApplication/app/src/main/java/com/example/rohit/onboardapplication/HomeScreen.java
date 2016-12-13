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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@TargetApi(23)
public class HomeScreen extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private int REQUEST_ENABLE_BT = 1;
    private boolean mScanning = false;
    private long SCAN_PERIOD = 5000;
    private BluetoothGatt mGatt;
    private TextToSpeech tts;

    private ListView listView;
    private Button scanButton;
    private ArrayAdapter<BluetoothDevice> adapter;
    private ArrayList<BluetoothDevice> BT_devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
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

        scanButton = (Button) findViewById(R.id.button);
        listView = (ListView) findViewById(R.id.list);
        adapter = new ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_1, BT_devices);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                BluetoothDevice  device    = (BluetoothDevice) listView.getItemAtPosition(position);
                connectToDevice(device);
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                BT_devices.clear();
                scanLeDevice(true);
            }
        });

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
                                if(BT_devices.get(i).getName()!=null){
                                    if (BT_devices.get(i).getName().equals(device.getName())) {
                                        push = false;
                                        break;
                                    }
                                }
                            }
                            if(push){
                                BT_devices.add(device);
                                adapter.notifyDataSetChanged();
                            }
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

}
