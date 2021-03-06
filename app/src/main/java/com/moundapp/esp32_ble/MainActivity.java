package com.moundapp.esp32_ble;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    /*The REQUEST_ENABLE_BT constant passed to startActivityForResult(android.content.Intent, int)
    is a locally-defined integer (which must be greater than 0) that the system passes back to you in your
    onActivityResult(int, int, android.content.Intent) implementation as the requestCode parameter.*/
    private static int REQUEST_ENABLE_BT = 15;

    /*The BluetoothAdapter is required for any and all Bluetooth activity.
    The BluetoothAdapter represents the device's own Bluetooth adapter (the Bluetooth radio).
    There's one Bluetooth adapter for the entire system, and your application can interact with
    it using this object*/
    private BluetoothAdapter bluetoothAdapter;

    //A list that contains the list of bluetooth Devices that I found
    private ArrayList<BluetoothDevice> bluetoothDevicesScanned;

    private boolean mScanning = false;//variable to know if I am currently scanning or not

    private BluetoothLeService bluetoothLeService;


    /***        UI variables        ****/

    private EditText edit_text_name_target;//this is a reference to my UI editText
    private Button button_connect;//this is a reference to my UI button

    //callback that will be called when I find a device
    private BluetoothAdapter.LeScanCallback leScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //When I find a device, I add it to my bluetoothScannedList
                            if (!bluetoothDevicesScanned.contains(device)) {
                                bluetoothDevicesScanned.add(device);
                                Log.i("New device scanned", "" + device.getName());
                            }
                        }
                    });
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        quitteSiLapplicationNeSupportePasLeBLE();

        initialiseLeBluetooth();

        initComponents();
    }

    private void quitteSiLapplicationNeSupportePasLeBLE(){

        //If the phone doesn't support BLE
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            //Then I show a Toast that indicate the user that it doesn't support BLE
            Toast.makeText(this, "Le BLE n'est pas supporté sur cet appareil!", Toast.LENGTH_SHORT).show();

            //After, that, I wait 1s to call the finish function that terminate the app
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    finish();
                }
            }, 1000);
        }

    }

    private void initialiseLeBluetooth(){
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //I start a new Activity that is a dialog that ask the user to activate the bluetooth
            //When this activity finishes, the function onActivityResult will be called with the parameter REQUEST_ENABLE_BT as requestCode
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }else{
            //we start the bluetooth scan!
            startBluetoothScan();
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //If the request
        if (requestCode == REQUEST_ENABLE_BT){//if the result correspond to the dialog "Turn your bluetooth on"
            if (resultCode == RESULT_OK){
                //The user turn the bluetooth On
                //everything is ok
                startBluetoothScan();
            }else{
                //The user hasn't turn the bluetooth on
                finish();//I finish the app if he doesn't want to turn bluetooth on
            }
        }

    }

    private void startBluetoothScan(){
        bluetoothDevicesScanned = new ArrayList<>();//initialisation of my list
        if (askPermissionsOfUser()){
            Log.i("device","start the scan");
            bluetoothAdapter.startLeScan(leScanCallback);//I launch the scan
        }
    }

    /**
     *
     * @return true if permissions are already granted, otherwise else
     */
    private boolean askPermissionsOfUser(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access fro BLE");
                builder.setMessage("Please grant location access so this app can detect devices.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener(){
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
                return false;
            }
        }

        return true;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //everything is OK
                    bluetoothAdapter.startLeScan(leScanCallback);
                } else {
                    finish();//Else i finish the app
                }
                return;
            }
        }
    }


    private void connectToDevice(BluetoothDevice device){

        goToCommandView();
        TextView tv = findViewById(R.id.txt_viw);
        bluetoothLeService = new BluetoothLeService(tv, this);
        bluetoothLeService.connectToDevice(getApplicationContext(),device);
    }

    /***********            UI              **********/

    private void initComponents(){
        edit_text_name_target = findViewById(R.id.edit_text_name_target);
        button_connect = findViewById(R.id.btn_connect);
        button_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //When i click on the button, this function is called

                String deviceName = edit_text_name_target.getText().toString();//I get the text of the editText
                BluetoothDevice deviceSearched = null;
                Log.i("DEVICE", "I m searching a device with name equals " + deviceName);
                for (BluetoothDevice device:bluetoothDevicesScanned) {
                    if (deviceName.equals(device.getName())){
                        deviceSearched = device;
                    }
                }

                if (deviceSearched == null){
                    Toast.makeText(view.getContext(), "Aucun serveur BLE avec ce nom trouvé", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(view.getContext(), "Un appareil a été trouvé", Toast.LENGTH_SHORT).show();
                    connectToDevice(deviceSearched);//I connect to this bluetooth device
                }

            }
        });
    }

    private void goToCommandView(){
        setContentView(R.layout.communicate_page);
        findViewById(R.id.btn_avancer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothLeService.envoyerCommande((byte)18);
            }
        });
        findViewById(R.id.btn_reculer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothLeService.envoyerCommande((byte)26);
            }
        });
    }


}