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
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    private var m_bluetoothAdapter : BluetoothAdapter? = null
    private lateinit var m_pairedDevices : Set<BluetoothDevice>
    private lateinit var m_discoveredDevices : ArrayList<BluetoothDevice>
    private val REQUEST_ENABLE_BLUETOOTH = 1
    private var isInZone : Boolean = false
    private var MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Getting Bluetooth Adapter
        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (m_bluetoothAdapter == null) {
            toast("This device does not support bluetooth")
            return
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

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action : String? = intent.action
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice = intent.getParcelableExtra(EXTRA_DEVICE)
                    Log.i("discoveredDevice", "" + device.address)
                    m_discoveredDevices.add(device)
                }
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_NAME_CHANGED.
    private val nameReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action : String? = intent.action
            when(action) {
                BluetoothDevice.ACTION_NAME_CHANGED -> {
                    // The name in discovery has changed, update the list
                    val device: BluetoothDevice = intent.getParcelableExtra(EXTRA_DEVICE)
                    Log.i("updatedName", "" + device.name)
                    var index = m_discoveredDevices.indexOf(device)
                    if (index == -1) {
                        m_discoveredDevices.add(device)
                    } else {
                        m_discoveredDevices[index] = device
                    }
                }
            }
        }
    }

    private fun refreshList() {
        if (!isInZone) {
            toast("Please enter the zone first")
            return
        }
        
        m_pairedDevices = m_bluetoothAdapter!!.bondedDevices
        val list : ArrayList<BluetoothDevice> = ArrayList()
        val listDeviceNames : ArrayList<String> = ArrayList()

        if (!m_pairedDevices.isEmpty()) {
            for (device: BluetoothDevice in m_pairedDevices) {
                list.add(device)
                listDeviceNames.add(device.name)
                Log.i("device", "" + device)
            }
        } else {
            toast("No paired bluetooth devices found")
        }

        if (!m_discoveredDevices.isEmpty()) {
            for (device: BluetoothDevice in m_discoveredDevices) {
                list.add(device)
                if (device.name == null) {
                    listDeviceNames.add(device.address)
                } else {
                    listDeviceNames.add(device.name)
                }
                Log.i("discoveredDevice", "" + device)
            }
        } else {
            toast("No new bluetooth devices found")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listDeviceNames)
        main_select_user_list.adapter = adapter

        main_select_user_list.onItemClickListener = AdapterView.OnItemClickListener{_, _, position, _ ->
            val device: BluetoothDevice = list[position]

            val intent = Intent(this, FilePickerActivity::class.java)
            intent.putExtra(EXTRA_DEVICE, device)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
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

    private fun changeTextToConnected(statusTextView : TextView) {
        statusTextView.setText(R.string.connected)
        statusTextView.setTextColor(Color.GREEN)

        main_enter_zone.setText(R.string.exitZone)
    }

    private fun changeTextToDisconnected(statusTextView : TextView) {
        statusTextView.setText(R.string.disconnected)
        statusTextView.setTextColor(Color.RED)

        main_enter_zone.setText(R.string.enterZone)
    }

    private fun enterTheZone() {
        // If user already in zone
        if (isInZone) { exitTheZone() }
        else {
            // Create Intent to become discoverable to others via bluetooth
            val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            startActivity(discoverableIntent)

            Log.i("discovery", "" + m_bluetoothAdapter!!.startDiscovery())

            val intent = Intent(this, BluetoothConnectionService::class.java)
            intent.putExtra(EXTRA_MESSAGE, "START_SERVER")
            startService(intent)

            changeTextToConnected(status_title)
            isInZone = true
        }
    }

    private fun exitTheZone() {
        val intent = Intent(this, BluetoothConnectionService::class.java)
        stopService(intent)

        m_bluetoothAdapter!!.cancelDiscovery()

        main_select_user_list.adapter = null
        toast("Discoverability off")

        changeTextToDisconnected(status_title)
        isInZone = false
    }

    override fun onDestroy() {
        super.onDestroy()

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver)
        unregisterReceiver(nameReceiver)
    }
}
