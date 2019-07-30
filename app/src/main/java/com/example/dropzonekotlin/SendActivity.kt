package com.example.dropzonekotlin

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.widget.TextView

class SendActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        // Gather the required data
        val device: BluetoothDevice = intent.getParcelableExtra(EXTRA_DEVICE)
        val fileURI: String = intent.getStringExtra(EXTRA_MESSAGE)

        // Start the bluetooth client to send file
        val sendingResult = BluetoothConnectionService().startClient(device, fileURI)

        val sendLoading: TextView = findViewById(R.id.send_loading)
        if (sendingResult) {
            sendLoading.setText(R.string.success)
        } else {
            sendLoading.setText(R.string.failure)
        }
    }
}
