package com.example.bluetoothhc05;

// Android/general
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
// Java
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
// Bluetooth
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
// Visual/interface
import android.view.View;
import android.app.ProgressDialog;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;



public class MainActivity extends AppCompatActivity {

    // Variable declarations
    private SeekBar rSeek;
    private SeekBar gSeek;
    private SeekBar bSeek;
    private Switch powerSwitch;
    private BluetoothAdapter myBluetooth = null;
    private BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    Set<BluetoothDevice> pairedDevices;
    ArrayList<String> deviceList;
    private ListView deviceListView;
    private Button refreshButton;
    String address = null;
    private ProgressDialog progress;
    final UUID SERIAL_PROFILE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Main setup function called when the main activity is opened
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Default and required onCreate functions
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register buttons by finding
        powerSwitch = findViewById(R.id.powerSwitch);
        rSeek = findViewById(R.id.rSeek);
        gSeek = findViewById(R.id.gSeek);
        bSeek = findViewById(R.id.bSeek);
        deviceListView = findViewById(R.id.ListView1);
        refreshButton = findViewById(R.id.refreshButton);

        // Register callbacks
        // Set the power switch call back
        powerSwitch.setOnCheckedChangeListener( new Switch.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                sendBluetoothPacket();
            }
        });
        // Set the seekbar callbacks. You have to declare all three stubs even if they aren't used
        // because they are virtual members
        // All of these functions just call "sendBluetoothPacket()" whenever anything changes.
        rSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sendBluetoothPacket();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sendBluetoothPacket();
            }
        });
        gSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sendBluetoothPacket();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sendBluetoothPacket();
            }
        });
        bSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sendBluetoothPacket();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sendBluetoothPacket();
            }
        });
        // Bluetooth refresh callback -- this rechecks the bluetooth situation and updates the device list
        refreshButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               checkBluetooth();
               pairedDevicesList();
            }
        });

        // Setup and check if bluetooth is on
        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        deviceList = new ArrayList<>();
        checkBluetooth();
        pairedDevicesList();
    }

    // This function will try to send a packet through he bluetooth connection to update the RGB values
    // If successful the arduino will change the RGB colors
    public void sendBluetoothPacket () {

        boolean checked = powerSwitch.isChecked();
        int power = checked ? 1 : 0;
        int r = power*rSeek.getProgress();
        int g = power*gSeek.getProgress();
        int b = power*bSeek.getProgress();

        String message = "rgb " +  Integer.toString(r) + " " + Integer.toString(g) + " " + Integer.toString(b) + "\n";

        // Only send a message if BT is connected.
        if( isBtConnected ) {
            try {
                Log.println(Log.INFO, "sending packet", message);
                // Write the message bytes!!!
                btSocket.getOutputStream().write(message.getBytes());
            }
            catch (IOException e){
                // If the message failed, let the user know.
                Toast.makeText(getApplicationContext(), "Message was not sent. Try again.", Toast.LENGTH_LONG).show();
            }
        }
        else{
            Log.println(Log.INFO, "packet not sent:", message);
        }
    }

    // This function does routine checks to determine if you've allowed bluetooth on your device
    // If not, it will ask you to allow it
    public void checkBluetooth() {
        if (myBluetooth == null) {
            // If bluetooth is not available, display a message
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
        }
        if (myBluetooth != null) {
            // Check if bluetooth is enabled
            if (!myBluetooth.isEnabled()) {
                // If not, ask to the user turn the bluetooth on
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon, 1);
            }
        }
    }

    // pairedDevicesList: populates a list of bluetooth devices paired with your device so that you
    // can select the HC-05 module. Once selected, the app will attempt to connect to it.
    private void pairedDevicesList()
    {
        // Get information about bluetooth devices
        pairedDevices = myBluetooth.getBondedDevices();
        deviceList.clear();
        if (pairedDevices.size()>0){
            for(BluetoothDevice bt : pairedDevices){
                deviceList.add(bt.getName() + "\n" + bt.getAddress());
            }
        }
        else{
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        // Populate a list with the names/MAC of identified devices
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1, deviceList);
        deviceListView.setAdapter(adapter);
        // Set a listener for list view so that it connects the bluetooth
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView av, View view, int position, long id){
                // The selected view is passed to connectBluetooth so it can extract the info
                connectBluetooth( view );
            }
        });

    }

    // connectBluetooth: connects to the bluetooth device.
    // This is where you are likely to encounter the most issues (connecting two devices can be
    // tricky!). This function often requires some patience and troubleshooting.
    private void connectBluetooth( View view ) {

        // Get the device MAC address: the last 17 chars in the View
        String info = ((TextView) view).getText().toString();
        address = info.substring(info.length() - 17);
        // Let the user know that the device was chosen. Nice to get feedback after an action.
        Toast.makeText(getApplicationContext(), "SET TO DEVICE " + info, Toast.LENGTH_LONG).show();
        // A progress dialog in case your phone is a dinosaur.
        progress = ProgressDialog.show(MainActivity.this, "Connecting...", "Please wait!!!");
        try {
            if (btSocket == null || !isBtConnected) {
                // Get your local bluetooth adapter so you can make a connection
                myBluetooth = BluetoothAdapter.getDefaultAdapter();
                // Connect to the MAC address if its available.
                BluetoothDevice localDevice = myBluetooth.getRemoteDevice(address);
                // Create a communication channel, requires to send messages
                btSocket = localDevice.createInsecureRfcommSocketToServiceRecord(SERIAL_PROFILE_UUID);
                // Stop discovering devices
                BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                // Make the connection
                btSocket.connect();
                // Now you're connected...
                isBtConnected = true;
            }
        } catch (IOException e) {
            //... unless the connection failed and an error was thrown.
            isBtConnected = false;
            // Display the error message in the LOG so that you can debug it.
            Log.println(Log.INFO, "connection failure:", e.getMessage());
            Log.println(Log.INFO, "address:", "_" + address + "_");
        }
        // Send a bluetooth packet immediately (will send whatever the UI is currently set to)
        sendBluetoothPacket();
        // We're done, so close out the progress bar.
        progress.dismiss();
    }



}