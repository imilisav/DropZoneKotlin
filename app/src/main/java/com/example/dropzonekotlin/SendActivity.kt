package com.example.dropzonekotlin

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.widget.TextView

/**
 * SendActivity
 *
 * Displays an User Interface that notifies the user whether the file was sent successfully
 * or if there was an error associated with the sending of the file.
 *
 * SendActivity does this, by creating a BluetoothConnectionService object and
 * telling it to execute a method startClient() that will start the sending process.
 * This is covered in more detail in the BluetoothConnectionService documentation.
 *
 * The activity waits for the sending process to complete and updates a TextView with the respective message.
 *
 * @see BluetoothConnectionService
 * @author DropZone Team
 * @version 1.0
 */
class SendActivity : AppCompatActivity() {

    /**
     * onCreate is a AppCompactActivity method that initializes the activity and displays it to the phone screen.
     * Must be used for the SendActivity to display correctly.
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        // Gather the required data
        val device: BluetoothDevice = intent.getParcelableExtra(EXTRA_DEVICE)
        val fileURI: String = intent.getStringExtra(EXTRA_MESSAGE)

        // Start the bluetooth client to send file
        val sendingResult = BluetoothConnectionService().startClient(device, fileURI)

        //Update the TextView with the sending result
        val sendLoading: TextView = findViewById(R.id.send_loading)
        if (sendingResult) {
            sendLoading.setText(R.string.success)
        } else {
            sendLoading.setText(R.string.failure)
        }
    }
}
