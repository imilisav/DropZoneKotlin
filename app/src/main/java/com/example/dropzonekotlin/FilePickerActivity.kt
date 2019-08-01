package com.example.dropzonekotlin

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE
import kotlinx.android.synthetic.main.activity_file_selector.*
import org.jetbrains.anko.toast
import android.support.v7.app.AlertDialog
import java.io.File

/**
 * FilePickerActivity
 *
 * Displays User Interface with the name of the remote bluetooth device and the file path of the file that will be sent
 *
 * This Activity allows the user to confirm which user they wish to send to, but also to choose which file they want to
 * send. To allow for choosing of a file, this application will use the built-in file chooser that comes with the
 * Android OS that the phone is currently using. This is done by evoking a intent through the filePicker()
 * method.
 *
 * This Activity ensures that the user chooses a file less than 5MB, and that the file be a PDF, VIDEO, IMAGE,
 * or AUDIO file type.
 *
 * Finally the activity starts the SendActivity to initiate the sending process.
 *
 * @see FilePickerHelper
 * @author DropZone Team
 * @version 1.0
 */
class FilePickerActivity : AppCompatActivity() {

    /**
     * device: The Bluetooth Device that will receive the file contents.
     */
    private var device : BluetoothDevice? = null

    /**
     * fileURI: The URI for the file of interest on the current phone.
     */
    private var fileURI : String = ""

    /**
     * onCreate is a AppCompactActivity method that initializes the activity and displays it to the phone screen.
     * Must be used for the FilePickerActivity to display correctly.
     *
     * @param savedInstanceState
     * @return void
     */
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

    /**
     * This method creates an intent to use the built-in file picker of the device's Operating System. The mime types
     * dictate which types of files will show up as selectable in the picker. This application will allow image files,
     * video files, pdf files, and audio files.
     *
     * The method will initialize the file picker in another Activity and make that newly opened activity, when
     * completed, return a result which will be handled in onActivityResult()
     *
     * @return void
     */
    private fun filePicker() {
        val mimeTypes : Array<String> = arrayOf("image/*", "video/*", "application/pdf", "audio/*")

        // create the intent to open file picker, add the desired file types to our picker and select the option that
        // the files be openable on the phone
        val intent = Intent()
            .setType("*/*")
            .setAction(Intent.ACTION_GET_CONTENT) // ACTION_GET_CONTENT refers to the built-in file picker
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)

        // Open file picker, and add a requestCode to refer to correct Activity result in startActivityForResult()
        startActivityForResult(Intent.createChooser(intent, "Choose a file"), 111)
    }

    /**
     * Opens a dialog to confirm with the user that they wish to send the file. This helps the user double check what
     * they have selected before sending. This dialog also serves as a preventive measure against accidental sending.
     *
     * @return void
     */
    private fun send() {
        if (fileURI == "") {
            toast("Please choose a file first")
        } else {
            // Double check with user
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle("Sending Confirmation")
            alertDialogBuilder.setMessage("Are you sure you wish to send this file?")
            alertDialogBuilder.setPositiveButton(R.string.send) { _, _ -> checkLessThan5MB(fileURI) }
            alertDialogBuilder.setNegativeButton(R.string.cancel) { _, _ -> toast("File sending cancelled") }
            alertDialogBuilder.show()
        }
    }

    /**
     * Before starting SendActivity, this method checks the file size to be sure that it is less than 5MB. It is
     * calculated by getting the number of bytes in a Kilobyte (1024 B / 1 KB) * (1024 KB / 1 MB) to convert to 1 MB
     * and finally multiply by 5 to get the number of bytes in 5MB.
     *
     * This value is then compared with the number of bytes in the file of interest, if it is less than 5MB, it is okay
     * to send. If it is greater than 5MB a dialog is displayed and sending is cancelled.
     *
     * @return void
     */
    private fun checkLessThan5MB(fileURI : String) {
        // Create File object to convert to bytes
        val file = File(fileURI)

        // Check with 5 MB Limit
        if (file.readBytes().size > (1024 * 1024 * 5)) {

            // Dialog the user and send back to file picker activity
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle("File too large")
            alertDialogBuilder.setMessage("This file is larger than the 5MB Limit")
            alertDialogBuilder.setPositiveButton("OK") { _, _ ->
                toast("File sending failed")
            }
            alertDialogBuilder.show()
        } else {

            // Create the intent to move forward to SendActivity, along with attaching device and fileURI
            // information for the SendActivity to read.
            val intent = Intent(this, SendActivity::class.java)
            intent.putExtra(EXTRA_DEVICE, device!!)
            intent.putExtra(EXTRA_MESSAGE, fileURI)
            startActivity(intent)
        }
    }

    /**
     * This method is called to open the built-in file picker. When a user chooses a file, the file picker activity
     * returns a request code that we set up in filePicker() method. After confirming that the right requestCode is
     * returned, then the result of this activity can be viewed.
     *
     * There are three cases:
     *  RESULT_OK - Activity ran correctly, it has proper data to be read by the method.
     *
     *  RESULT_CANCELLED - The user cancelled the file picker. This is usually done via pressing the back button.
     *
     *  ANY OTHER RESULT - Something went wrong, let user know.
     *
     * @return void
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 111) {
            if (resultCode == RESULT_OK) {
                val selectedFile = data?.data //The uri with a location of the file

                // Helper method, refer to FilePickerHelper getPath() method
                val selectedFilePath = FilePickerHelper.getPath(this, selectedFile!!)

                // Update the TextView to contain the file path of chosen file
                file_info_name_value.text = selectedFilePath

                // Update the global variable of the fileURI with the actual URI
                fileURI = selectedFilePath!!

            } else if (resultCode == RESULT_CANCELED) {
                toast("File choosing cancelled")
            } else {
                toast("Error with choosing this file")
            }
        }
    }
}
