package com.example.rohit.onboardapplication;

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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RestrictedActivity extends AppCompatActivity {

    private Set<String> restrictedSet;
    private ArrayList<String> restrictedList;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mGatt;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private boolean mScanning = false;

    private Button addBus;
    private Button clearList;
    private ListView busList;
    private ArrayAdapter<String> adapter;

    private Handler taskHandler;

    private boolean isBusFound=  false;
    private BluetoothDevice busFound;

    private TextToSpeech tts;

    private SharedPreferences setPref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restricted);

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.UK);
                }
            }
        });

        taskHandler = new Handler();

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler();

        setPref = getApplicationContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        Set<String> test = setPref.getStringSet("set",null);
        if(test!=null){
            restrictedSet = test;
            restrictedList = convertSetToArray(test);
        }else{
            restrictedSet = new HashSet<String>();
            restrictedList = new ArrayList<String>();
            SharedPreferences.Editor edit = setPref.edit();
            edit.putStringSet("set",restrictedSet);
            edit.commit();
        }

        addBus = (Button) findViewById(R.id.add);
        clearList = (Button) findViewById(R.id.clear);
        busList = (ListView) findViewById(R.id.list);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, restrictedList);
        busList.setAdapter(adapter);

        addBus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopRepeatTask();
                AlertDialog.Builder alert = new AlertDialog.Builder(RestrictedActivity.this);
                final EditText edittext = new EditText(RestrictedActivity.this);
                alert.setMessage("Enter Bus");
                alert.setTitle("Add Bus to Restricted Set");
                alert.setView(edittext);
                alert.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String busName = edittext.getText().toString();
                        if(restrictedSet!=null){
                            restrictedSet.add(busName);
                        }else{
                            restrictedSet = new HashSet<String>();
                            restrictedSet.add(busName);
                        }
                        SharedPreferences.Editor prefEditor = setPref.edit();
                        prefEditor.putStringSet("set", restrictedSet);
                        prefEditor.commit();
                        restrictedList.add(busName);
                        adapter.notifyDataSetChanged();
                        System.out.println(setPref.getStringSet("set",null).toString());
                        //repeatTask();
                    }
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                       dialog.cancel();
                    }
                });
                alert.show();
            }
        });

        clearList.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopRepeatTask();
                AlertDialog.Builder alert = new AlertDialog.Builder(RestrictedActivity.this);
                alert.setMessage("Do you want to continue?");
                alert.setTitle("Clear List");
                alert.setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        restrictedSet = new HashSet<String>();
                        SharedPreferences.Editor prefEditor = setPref.edit();
                        prefEditor.putStringSet("set", restrictedSet);
                        prefEditor.commit();
                        restrictedList.clear();
                        adapter.notifyDataSetChanged();
                       // repeatTask();
                    }
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });
                alert.show();
            }
        });

       repeatTask();
    }

    Runnable busScanner = new Runnable() {
        @Override
        public void run() {
            try {
                scanLeDevice(true);
                Handler scanWaitHandler = new Handler();
                scanWaitHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(isBusFound==true){
                            connectToDevice(busFound);
                            //writeCharacteristic("Rohit");
                            tts.speak(busFound.getName(),TextToSpeech.QUEUE_FLUSH, null);
                            while (tts.isSpeaking());
                            Handler connectionWaitHandler = new Handler();
                            connectionWaitHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    disconnectDevice();
                                    isBusFound =  false;
                                }
                            }, 1000);
                        }
                    }
                }, 1000);
            } finally {
                taskHandler.postDelayed(busScanner, 3000);
            }
        }
    };

    void repeatTask(){
        busScanner.run();
    }

    void stopRepeatTask(){
       // scanLeDevice(false);
        //taskHandler.removeCallbacks(busScanner);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
        //stopRepeatTask();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       // stopRepeatTask();
    }

    @Override
    public void onResume() {
        super.onResume();
       // busScanner.run();
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
            }, 1000);
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
                            // On Scan What to Do
                            boolean found =  false;
                            for (Iterator<String> it = restrictedSet.iterator(); it.hasNext(); ) {
                                String busName  = it.next();
                                if (busName.equals(device.getName())){
                                    isBusFound = true;
                                    busFound = device;
                                    break;
                                }
                            }
                        }
                    });
                }
            };

    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);
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
        }
    };

    public boolean writeCharacteristic(String str) {

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

    private ArrayList<String> convertSetToArray(Set<String> s){
        ArrayList<String > l  = new ArrayList<String>();
        for (Iterator<String> it = s.iterator(); it.hasNext(); ) {
            String busName  = it.next();
            l.add(busName);
        }
        return l;
    }
}