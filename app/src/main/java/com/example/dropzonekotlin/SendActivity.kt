package com.example.dropzonekotlin

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE

class SendActivity : AppCompatActivity() {

    companion object {
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        private lateinit var device : BluetoothDevice
        private lateinit var fileURI : String
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        // Gather the required data for the send intent
        device = intent.getParcelableExtra(EXTRA_DEVICE)
        fileURI = intent.getStringExtra(EXTRA_MESSAGE)
        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Start the bluetooth client to send file
        BluetoothConnectionService().start("START_CLIENT", device, fileURI)
    }
}
