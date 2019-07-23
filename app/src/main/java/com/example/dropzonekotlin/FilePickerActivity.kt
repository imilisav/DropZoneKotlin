package com.example.dropzonekotlin

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE
import kotlinx.android.synthetic.main.activity_file_selector.*
import org.jetbrains.anko.toast

class FilePickerActivity : AppCompatActivity() {
    private var device : BluetoothDevice? = null
    private var fileURI : String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_selector)

        // Update the user textview
        device = intent.getParcelableExtra(EXTRA_DEVICE)
        if (device!!.name == null) {
            user_info_name_value.text = device!!.address
        } else {
            user_info_name_value.text = device!!.name
        }

        // Lambda function to open file picker
        file_select_button.setOnClickListener{ filePicker() }

        file_selector_send.setOnClickListener{ send() }
    }

    private fun filePicker() {
        val mimeTypes : Array<String> = arrayOf("image/*", "video/*", "application/pdf", "audio/*", "text/*")
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)

        startActivityForResult(Intent.createChooser(intent, "Choose a file"), 111)
    }

    private fun send() {
        if (fileURI == "") {
            toast("Please choose a file first")
        } else {
            val intent = Intent(this, SendActivity::class.java)
            intent.putExtra(EXTRA_DEVICE, device!!)
            intent.putExtra(EXTRA_MESSAGE, fileURI)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111) {
            if (resultCode == RESULT_OK) {
                val selectedFile = data?.data //The uri with the location of the file
                file_info_name_value.text = selectedFile?.path
                fileURI = selectedFile?.path
            } else if (resultCode == RESULT_CANCELED) {
                toast("File choosing cancelled")
            }
        }
    }
}
