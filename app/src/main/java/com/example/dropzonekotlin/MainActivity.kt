package com.example.dropzonekotlin

import android.Manifest
import android.app.Activity
import android.support.v7.app.AppCompatActivity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import kotlin.collections.ArrayList

/**
 * MainActivity is the main page of the application. This is where you can query all devices available for receiving
 * files and receive files yourself.
 *
 * Some things to be aware of before reading further:
 * In The Zone - Refers to the process of becoming visible to other bluetooth devices and open to connect
 *                  to other bluetooth devices to allow data transfer.
 *
 * Out The Zone - Refers to not being visible to other bluetooth devices anymore and no bluetooth devices can connect
 *                 to this device.
 *
 * How the application works, is the user starts off "outside of the zone". The reason for this, is to allow the user to
 * have control when other devices can connect and send files to them. Entering the zone is the only way they can send
 * files to others to prevent accidental sending of files. Ensuring both parties are connected to bluetooth and can see
 * one another allows for easier connection and communication.
 *
 * The contents of this activity User Interface contains a list that is updated via a "Refresh List" button that the
 * user presses. The other button is the one that puts you "In the Zone" and "Out the Zone" based on whether or not
 * you are in the zone already. Each item in the list represents a remote device that you could send a file to. Clicking
 * on a name in this list , will start the FilePickerActivity, which will be explained in more depth in that class file.
 *
 * @author DropZone Team
 * @version 1.0
 */
class MainActivity : AppCompatActivity() {

    /**
     * m_bluetoothAdapter: The Bluetooth Adapter of this phone.
     */
    private var m_bluetoothAdapter : BluetoothAdapter? = null

    /**
     * m_pairedDevices: set of all devices already paired to this phone.
     */
    private lateinit var m_pairedDevices : Set<BluetoothDevice>

    /**
     * m_discoveredDevices: set of devices that the BluetoothAdapter finds via discovery.
     */
    private lateinit var m_discoveredDevices : ArrayList<BluetoothDevice>

    /**
     * isInZone: boolean representing if the user is in the zone or not.
     */
    private var isInZone : Boolean = false

    /**
     * REQUEST_ENABLE_BLUETOOTH: constant for requesting bluetooth be turned on.
     */
    private val REQUEST_ENABLE_BLUETOOTH = 1

    /**
     * MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: variable representing whether the user has permission to access
     * the user's location.
     */
    private var MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 0

    /**
     * MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: variable representing whether the user has permission to read the
     * user's external file storage.
     */
    private var MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 0

    /**
     * MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: variable representing whether the user has permission to write
     * to the user's external file storage.
     */
    private var MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0

    /**
     * onCreate is a AppCompactActivity method that initializes the activity and displays it to the phone screen.
     * Must be used for the MainActivity to display correctly.
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Getting Bluetooth Adapter
        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Checking if bluetooth is supported, letting user know if it isn't and closing application
        if (m_bluetoothAdapter == null) {
            toast("This device does not support bluetooth")
            this.finish()
            return
        }

        // Request to enable bluetooth for user if not enabled, this ensures the user's bluetooth is on for the rest
        // of the process
        if (!m_bluetoothAdapter!!.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH)
        }

        // Check for location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION)
            }
        }

        // Check for read file permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
            }
        }

        // Check for write file permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
            }
        }

        m_discoveredDevices = ArrayList()

        // Register for broadcasts when a device is discovered.
        val deviceFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, deviceFilter)

        // Register for broadcasts when the device name is updated
        val deviceNameFilter = IntentFilter(BluetoothDevice.ACTION_NAME_CHANGED)
        registerReceiver(nameReceiver, deviceNameFilter)

        // Lambda function to enable discovery and become discoverable, and update the discovered devices list
        main_enter_zone.setOnClickListener { enterTheZone() }

        // Lambda function to refresh the list with new devices, appended at the end of list
        main_refresh_user_list.setOnClickListener{ refreshList() }
    }

    /**
     * Create a BroadcastReceiver for the ACTION_FOUND intent. This constantly listens for new phones that are visible
     * via bluetooth. When the device information is received, we update this device info in the m_discoveredDevices
     * ArrayList if the device exists already, or add it if it doesn't.
     */
    private val receiver = object : BroadcastReceiver() {

        /**
         * Updates the m_discoveredDevices ArrayList when the BroadcastReceiver discovers a new device.
         *
         * @param context
         * @param intent
         * @return void
         */
        override fun onReceive(context: Context, intent: Intent) {
            val action : String? = intent.action
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice = intent.getParcelableExtra(EXTRA_DEVICE)
                    val index = m_discoveredDevices.indexOf(device)
                    if (index == -1) {
                        m_discoveredDevices.add(device)
                    } else {
                        m_discoveredDevices[index] = device
                    }
                }
            }
        }
    }

    /**
     * Create a BroadcastReceiver for the ACTION_NAME_CHANGED intent. This constantly listens to the discovered devices
     * until they notify this phone of their friendly name. When the name is received, we update the device in the
     * m_discoveredDevices ArrayList if the device exists already, or add it if it doesn't.
     */
    private val nameReceiver = object : BroadcastReceiver() {

        /**
         * Updates the m_discoveredDevices ArrayList when the BroadcastReceiver receives a name update from a discovered
         * device.
         *
         * @param context
         * @param intent
         * @return void
         */
        override fun onReceive(context: Context, intent: Intent) {
            val action : String? = intent.action
            when(action) {
                BluetoothDevice.ACTION_NAME_CHANGED -> {
                    // The name in discovery has changed, update the list
                    val device: BluetoothDevice = intent.getParcelableExtra(EXTRA_DEVICE)
                    val index = m_discoveredDevices.indexOf(device)
                    if (index == -1) {
                        m_discoveredDevices.add(device)
                    } else {
                        m_discoveredDevices[index] = device
                    }
                }
            }
        }
    }

    /**
     * Refreshes the list of available paired devices and devices that have been discovered (also close to this phone)
     * by updating the lists containing these devices first and then updating the ListView adapter with these new lists.
     *
     * @return void
     */
    private fun refreshList() {
        if (!isInZone) {
            toast("Please enter the zone first")
            return
        }

        // Get the bonded devices from the bluetooth adapter
        // Bonded devices are also known as paired devices
        m_pairedDevices = m_bluetoothAdapter!!.bondedDevices

        // Establishing a list of devices, since they contain more info about the device needed for the next Activity
        // This list will contain all devices, bonded or discovered
        val list : ArrayList<BluetoothDevice> = ArrayList()

        // List of device names, this will be used to display on UI
        // This list will contain all devices, bonded or discovered
        val listDeviceNames : ArrayList<String> = ArrayList()

        // Add all paired devices to both Lists
        if (m_pairedDevices.isNotEmpty()) {
            for (device: BluetoothDevice in m_pairedDevices) {
                list.add(device)
                listDeviceNames.add(device.name) // Just need the name for this list
            }
        } else {
            toast("No paired bluetooth devices found")
        }

        // Add all discovered devices (at that point in time refreshList was called) to both lists
        if (m_discoveredDevices.isNotEmpty()) {
            for (device: BluetoothDevice in m_discoveredDevices) {
                list.add(device)

                // Checking if the discovered device has only an address or a friendly name.
                // device's names are updated with the ACTION_NAME_CHANGED receiver, check nameReceiver for more info
                if (device.name == null) {
                    listDeviceNames.add(device.address)
                } else {
                    listDeviceNames.add(device.name)
                }
            }
        } else {
            toast("No new bluetooth devices found")
        }

        // Updating the ListView Adapter with new lists, the ListView adapter handles the organization and display of
        // all UI components pertaining to the ListView, it requires a list as input
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listDeviceNames)
        main_select_user_list.adapter = adapter

        // With every item on the ListView that gets added, we attach a onClickListener that will start the next
        // Activity (FilePickerActivity) with the proper device from the device list attached.
        main_select_user_list.onItemClickListener = AdapterView.OnItemClickListener{_, _, position, _ ->
            val device: BluetoothDevice = list[position]

            val intent = Intent(this, FilePickerActivity::class.java)
            intent.putExtra(EXTRA_DEVICE, device)
            startActivity(intent)
        }
    }

    /**
     * This method is called when either bluetooth discovery is enabled or when bluetooth is enabled, the methods
     * that call this method send a request code as a parameter that can be referred to in this method.
     *
     * This method is used to notify the user the bluetooth status on their phone.
     *
     * There are two cases seen in this method:
     *  RESULT_OK - Activity ran correctly, it has proper data to be read by the method.
     *
     *  RESULT_CANCELLED - The user cancelled before bluetooth could be set up.
     *  This is usually done by denying the requests that pop up when "Entering the Zone"
     *
     *  @param requestCode - code for the action that was undertaken.
     *  @param resultCode - result of that action
     *  @param data - data only exists, if resultCode = RESULT_OK
     *  @return void
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Check which action has occurred
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {

            // Check status and notify respectively
            if (resultCode == Activity.RESULT_OK) {
                if (m_bluetoothAdapter!!.isEnabled) {
                    toast("Bluetooth has been enabled")
                } else {
                    toast("Bluetooth has been disabled")
                }
            } else if (resultCode == Activity.RESULT_CANCELED){
                toast("Bluetooth has been cancelled")
            }
        } else if (requestCode.equals(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)) {
            if (resultCode == Activity.RESULT_CANCELED) {
                toast("Bluetooth has been cancelled")
            }
        }
    }

    /**
     * Changes the Status to "In Zone" and changes the text on the "Enter the Zone" button to "Exit the Zone"
     *
     * @param statusTextView the text above the "Enter/Exit the Zone button".
     * @return void
     */
    private fun changeTextToConnected(statusTextView : TextView) {
        statusTextView.setText(R.string.connected)
        statusTextView.setTextColor(Color.GREEN)

        main_enter_zone.setText(R.string.exitZone)
    }

    /**
     * Changes the Status to "Not In Zone" and changes the text on the "Exit the Zone" button to "Enter the Zone"
     *
     * @param statusTextView the text above the "Enter/Exit the Zone button".
     * @return void
     */
    private fun changeTextToDisconnected(statusTextView : TextView) {
        statusTextView.setText(R.string.disconnected)
        statusTextView.setTextColor(Color.RED)

        main_enter_zone.setText(R.string.enterZone)
    }

    /**
     * Starts Bluetooth, Requests the user to be discoverable to other devices, and starts a bluetooth server to listen
     * to incoming connection requests through BluetoothConnectionService.
     *
     * @see BluetoothConnectionService.startServer
     * @return void
     */
    private fun enterTheZone() {
        // If user already in zone
        if (isInZone) { exitTheZone() }
        else {
            // Start bluetooth just in case user turned off bluetooth at any point of app operation
            if (!m_bluetoothAdapter!!.isEnabled) {
                m_bluetoothAdapter!!.enable()
            }

            // Create Intent to become discoverable to others via bluetooth
            val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                // Discoverable 300 seconds => 5 minutes
            }
            startActivity(discoverableIntent) // Start the discovery process

            BluetoothConnectionService().startServer()

            // Update the UI
            changeTextToConnected(status_title)
            isInZone = true
            refreshList()
        }
    }

    /**
     * Stops the bluetooth discovery of the device, stops this phone from being visible to other bluetooth devices
     * anymore and stops any bluetooth devices to connect to this device. Clears the list in the UI. This method is
     * called when the user presses the "Exit the Zone" button.
     *
     * @return void
     */
    private fun exitTheZone() {
        BluetoothConnectionService.BluetoothServerController().cancel()
        m_bluetoothAdapter!!.cancelDiscovery()

        main_select_user_list.adapter = null
        toast("Discoverability off")

        changeTextToDisconnected(status_title)
        isInZone = false
    }

    /**
     * onDestroy is a AppCompactActivity method that destroys the activity and stops the receivers so they stop
     * listening for new bluetooth devices beyond the app's lifecycle. Called when user closes the app.
     *
     * @return void
     */
    override fun onDestroy() {
        super.onDestroy()

        // unregister the ACTION_FOUND receiver
        unregisterReceiver(receiver)

        // unregister the ACTION_NAME_CHANGED receiver
        unregisterReceiver(nameReceiver)
    }
}
