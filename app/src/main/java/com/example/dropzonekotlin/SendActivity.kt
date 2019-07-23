package com.example.dropzonekotlin

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
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
        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        fileURI = intent.getStringExtra(EXTRA_MESSAGE)
        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Start the bluetooth client intent to send file
        val intent = Intent(this, BluetoothConnectionService::class.java)
        intent.putExtra(EXTRA_MESSAGE, "START_CLIENT")
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device)
        intent.putExtra(Intent.EXTRA_REFERRER_NAME, fileURI)
        startService(intent)
    }

}
