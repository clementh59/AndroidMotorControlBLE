package com.moundapp.esp32_ble;

import android.app.Activity;
import android.app.Service;
import android.arch.core.util.Function;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import java.util.UUID;

// A service that interacts with the BLE device via the Android BLE API.
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothGatt bluetoothGatt;
    private int connectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static UUID UUID_SERVICE = UUID.fromString("839e2af8-4b00-469b-9d62-1493215ee80d");
    public final static UUID UUID_CHARACTERISTIC = UUID.fromString("78f4c237-6c83-4313-9b3e-7c56868997e5");

    private static BluetoothGattCharacteristic commandCharacteristic;

    private Activity activity;
    private TextView receivedValues;

    public BluetoothLeService(TextView tv, Activity a) {
        this.receivedValues = tv;
        activity = a;
    }

    // Various callback methods defined by the BLE API.
    public final BluetoothGattCallback gattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        connectionState = STATE_CONNECTED;
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                bluetoothGatt.discoverServices());

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        connectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.i(TAG, "service discovered");
                        BluetoothGattCharacteristic characteristic = gatt.getService(UUID_SERVICE).getCharacteristic(UUID_CHARACTERISTIC);
                        //I authorize notifications
                        gatt.setCharacteristicNotification(characteristic, true);
                        // 0x2902 org.bluetooth.descriptor.gatt.client_characteristic_configuration.xml
                        UUID uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(uuid);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                        gatt.writeCharacteristic(characteristic);
                        commandCharacteristic = characteristic;
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.i(TAG, "characteristic read success " + characteristic.getStringValue(0));
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    final BluetoothGattCharacteristic characteristic1 = characteristic;
                    Log.i(TAG,"Characteristic changed : "+characteristic.getUuid() + " " + characteristic.getStringValue(0));
                    final String text = "La valeur lue est ";
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            receivedValues.setText(text+String.valueOf(characteristic1.getValue()));
                        }
                    });

                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    Log.i(TAG,"Characteristic write : "+characteristic.getUuid() + " " + characteristic.getStringValue(0));
                }

                @Override
                public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    Log.i(TAG,"descriptor read : "+descriptor.getUuid() + " " + descriptor.getValue().toString());
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                    Log.i(TAG,"descriptor write : "+descriptor.getUuid() + " " + descriptor.getValue().toString());
                }
            };

    public void envoyerCommande(byte cmd){
        commandCharacteristic.setValue(new byte[]{cmd});
        bluetoothGatt.writeCharacteristic(commandCharacteristic);
        Log.i(TAG, "send value " + cmd);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void connectToDevice(Context context, BluetoothDevice device){
        bluetoothGatt = device.connectGatt(context,false, gattCallback);
    }

}
