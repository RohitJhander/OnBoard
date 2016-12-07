package com.example.rohit.onboardapplication;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

@TargetApi(23)
public class HomeScreen extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private int REQUEST_ENABLE_BT = 1;
    private boolean mScanning = false;
    private long SCAN_PERIOD = 60000;
    private ArrayList<BluetoothDevice> BT_devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        BT_devices =  new ArrayList<BluetoothDevice>();
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler();

        Button button = (Button) findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    scanLeDevice(true);
                    final ProgressDialog dialog = new ProgressDialog(HomeScreen.this);
                    dialog.setTitle("Finding Bluetooth Devices");
                    dialog.setMessage("Please wait ...");
                    dialog.setIndeterminate(true);
                    dialog.setCancelable(false);
                    dialog.show();
                    long delayInMillis = 60000;
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            dialog.cancel();
                        }
                    }, delayInMillis);
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                             if(BT_devices.isEmpty()){
                                 for (int i=0; i < 10; i++)
                                 {
                                     Toast.makeText(HomeScreen.this,"No Device Found", Toast.LENGTH_LONG).show();
                                 }
                            }else{
                                 for (int i=0; i < 10; i++)
                                 {
                                     Toast.makeText(HomeScreen.this,"Device Found: "+ BT_devices.get(0).getName(), Toast.LENGTH_LONG).show();
                                 }
                            }
                        }
                    }, 60000);
                }
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
                            BT_devices.add(device);
                        }
                    });
                }
            };
}
