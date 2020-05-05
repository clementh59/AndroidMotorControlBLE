# Steps

First, I created a new basic android project

Then, I integrated BLE with these steps : 

## Integrating Bluetooth Low Energy (BLE)

I followed the official documentation of Android to do this.(https://developer.android.com/guide/topics/connectivity/bluetooth-le) 

### Adding the right permissions to the app manifest

I added the 3 following lines

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.moundapp.esp32_ble">
    
    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>

</manifest>
```
