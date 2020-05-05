# Steps To Reproduce the App

First, I created a new basic android project

Then, I integrated BLE with these steps : 

## Integrating Bluetooth Low Energy (BLE)

I followed the official documentation of Android to do this.(https://developer.android.com/guide/topics/connectivity/bluetooth-le) 

### Adding the right permissions to the app manifest

I add the 3 following lines

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.moundapp.esp32_ble">
    
    <uses-permission android:name="android.permission.BLUETOOTH"/>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>

</manifest>
```

### Check if the phone supports BLE

Then, I implement a function that quit the app if the phone doesn't
support BLE. I call her in the onCreate of my MainActivity (This is the
first function called when the application is created)

```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Le BLE n'est pas supporté sur cet appareil!", Toast.LENGTH_SHORT).show();
            finish();
        }

    }
}
```

