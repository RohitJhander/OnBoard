package com.example.rohit.onboardapplication;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@TargetApi(23)
public class HomeScreen extends AppCompatActivity {


    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private int REQUEST_ENABLE_BT = 1;
    private boolean mScanning = false;
    private BluetoothGatt mGatt;
    private ListView listView;
    private Button scanButton;
    private Button restrictedButton;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> BT_devices;
    private HashMap<String,BluetoothDevice> map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if(ContextCompat.checkSelfPermission(HomeScreen.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("BleActivity", "Location access not granted!");
            ActivityCompat.requestPermissions(HomeScreen.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 42);
        }

        BT_devices = new ArrayList<String>();
        map = new HashMap<String, BluetoothDevice>();
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler();

        scanButton = (Button) findViewById(R.id.scan);
        listView = (ListView) findViewById(R.id.list);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, BT_devices);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                BluetoothDevice  device   = (BluetoothDevice) map.get(listView.getItemAtPosition(position));
                connectToDevice(device);
                Toast.makeText(HomeScreen.this, device.getName()+" selected", Toast.LENGTH_LONG).show();
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        disconnectDevice();
                    }
                }, Constants.connectionTime);

            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                BT_devices.clear();
                map.clear();
                adapter.clear();
                adapter.notifyDataSetChanged();
                scanLeDevice(true);
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(BT_devices.size()==0){
                            Toast.makeText(HomeScreen.this, Constants.noBusFoundMsg, Toast.LENGTH_LONG).show();
                        }else{
                            Toast.makeText(HomeScreen.this, BT_devices.size() +" devices found", Toast.LENGTH_LONG).show();
                        }
                    }
                }, Constants.SCAN_PERIOD);

            }
        });

        restrictedButton = (Button) findViewById(R.id.restricted);
        restrictedButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(HomeScreen.this,RestrictedActivity.class);
                startActivity(intent);
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
            }, Constants.SCAN_PERIOD);
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
                                if(device.getName()!=null){
                                    if (BT_devices.get(i).equals(device.getName())) {
                                        push = false;
                                        break;
                                    }
                                }
                            }
                            if(push){
                                if(device!=null && device.getName()!=null){
                                    BT_devices.add(device.getName());
                                    map.put(device.getName(),device);
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }
                    });
                }
            };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            // scanLeDevice(false);
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
            writeCharacteristic(Constants.sendData);
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

        BluetoothGattService Service = mGatt.getService(mGatt.getServices().get(Constants.writeToServiceIndex).getUuid());

        if (Service == null) {
            return false;
        }

        BluetoothGattCharacteristic charac = Service.getCharacteristic(mGatt.getServices().get(Constants.writeToServiceIndex).getCharacteristics().get(Constants.writeToCharacterisiticIndex).getUuid());
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
