package com.example.dropzonekotlin

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.dropzonekotlin.BluetoothConnectionHelper.Companion.getPublicStorageDir
import com.example.dropzonekotlin.BluetoothConnectionHelper.Companion.isExternalStorageWritable
import java.io.*
import java.util.*
import java.nio.ByteBuffer

/**
 * BluetoothConnectionService is a class containing a client, server and server controller threads. The way a bluetooth
 * connection works in this application, is that one phone is has a server role and the other phone has a client role.
 *
 * Client is the remote device sending the phone a file. It writes 4 things to the outputStream.
 * 1 - an integer representing the number of bytes of the string containing the file name.
 * 2 - the actual string of the file name converted to a ByteArray
 * 3 - an integer representing the number of bytes of file.
 * 4 - the actual file coverted to a ByteArray
 *
 * A server controller functions to listen to incoming connection requests, and when a request is made, a connection is
 * attempted between the two phones. If it is successful, then the server is called to read the file that was sent over
 * via the client.
 *
 * The server is the phone receiving the file. It reads the 4 pieces of data from the client
 * 1 - an integer n representing the number of bytes of the string containing the file name.
 * 2 - it uses n to create a buffer of n size which will be used to convert that file name back
 *     to a string.
 * 3 - an integer m representing the number of bytes of file.
 * 4 - it uses m to break down how many Kilobyte buffers are needed to read the entire file. Each buffer gets added to
 *     a file output stream, which is the file on the recipient's phone.
 */
class BluetoothConnectionService {

    /**
     * Declaring variables or methods in companion objects, declares them as static methods/variables.
     */
    companion object {
        /**
         * uuid: The UUID special to this application. The connected phone will exchange this uuid to verify both phones
         * are using the DropZone application.
         */
        val uuid: UUID = UUID.fromString("8989063a-c9af-463a-b3f1-f21d9b2b827b")

        /**
         * fileURI: the absolute path of the file on this phone.
         */
        var fileURI: String = ""

        /**
         * success: result of sending file over to the remote device.
         */
        var success: Boolean = false
    }

    /**
     * public method to start up the server controller.
     *
     * @return void
     */
    fun startServer() {
        BluetoothServerController().start()
    }

    /**
     * public method to start up the client to begin sending a file.
     *
     * @param device
     * @param uri
     * @return Boolean, representing if the file sent or not.
     */
    fun startClient(device: BluetoothDevice, uri: String) : Boolean {
        fileURI = uri

        // start the client and then wait for it to finish to update the activity with the result of the sending
        val btClient = BluetoothClient(device)
        btClient.start()
        try {
            btClient.join()
        } catch (e: InterruptedException) {
        }

        return success
    }

    /**
     * The BluetoothClient Class. This extends a thread, so that the client and server and controller code can run
     * separately.
     *
     * @param device the remote device to send a file to.
     */
    class BluetoothClient(device: BluetoothDevice): Thread() {
        /**
         * socket: the bluetooth socket of the remote device, listening for the uuid specified.
         */
        private val socket = device.createRfcommSocketToServiceRecord(uuid)

        /**
         * Sends the file to the remote device as explained at the top of this file. This is a thread method that is
         * needed for proper operation.
         *
         * @return void
         */
         override fun run() {

            // Attempt the connection, if it fails, throw an exception and return, success is already false, meaning the
            // sending failed.
             try {
                this.socket.connect()
             } catch (e: IOException) {
                 return
             }

            // Get this phones input and output streams
             val outputStream = this.socket.outputStream
             val inputStream = this.socket.inputStream

            // Create a File object containing the file contents, and convert into byte array
             val file = File(fileURI)
             val fileBytes: ByteArray
             try {
                 fileBytes = ByteArray(file.length().toInt())
                 val bis = BufferedInputStream(FileInputStream(file))
                 bis.read(fileBytes, 0, fileBytes.size)
                 bis.close()
             } catch (e: IOException) {
                 return
             }

            // Int -> 4 bytes required to represent it
             val fileNameSize = ByteBuffer.allocate(4)
             fileNameSize.putInt(file.name.toByteArray().size) // Get number of bytes and put that number in
                                                               // fileNameSize, a ByteArray[4]

            // Same thing for the size of the actual file, put the size in an int, represented in 4 bytes
             val fileSize = ByteBuffer.allocate(4)
             fileSize.putInt(fileBytes.size)

             outputStream.write(fileNameSize.array())    // The int containing number of bytes in the file name string
             outputStream.write(file.name.toByteArray()) // The file name string turned to bytes
             outputStream.write(fileSize.array())        // The int containing number of bytes in the actual file
             outputStream.write(fileBytes)               // The actual file in bytes

             success = true                              // If no exception has been thrown, then successful sending

             sleep(5000)                           // Add delay of 5 seconds before closing the socket, this
                                                         // allows for the remote device to open its sockets correctly
             outputStream.close()
             inputStream.close()
             this.socket.close()
         }
    }

    /**
     * The BluetoothServerController Class. This extends a thread, so that the client and server and controller code
     * can run separately.
     */
    class BluetoothServerController : Thread() {
        /**
         * cancelled: contains the result if the server controller has been cancelled by the user.
         */
        private var cancelled: Boolean

        /**
         * serverSocket: this is the bluetooth socket of the phone who has instantiated this class.
         */
        private val serverSocket: BluetoothServerSocket?

        /**
         * Constructor of the Bluetooth Server Controller. It starts to listen for two things in a connection request.
         * The name of the application, and the uuid that was declared as a global variable above. Every DropZone user
         * has this name & uuid combination. This verifies that the remote device will be a DropZone user, otherwise
         * the connection request will be denied.
         *
         * @return BluetoothServerController object ready to listen to incoming connection requests.
         */
        init {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()

            // Checks if the bluetooth Adapter exists when this is called
            if (btAdapter != null) {
                // Start the listening process here, requests with the proper
                this.serverSocket = btAdapter.listenUsingRfcommWithServiceRecord("DropZoneKotlin", uuid)
                this.cancelled = false
            } else {
                // cancel the listening process if the adapter doesn't exist
                this.serverSocket = null
                this.cancelled = true
            }

        }

        /**
         * Constantly waits for connection requests by looping infinitely and attempts to make a connection when
         * a request is made. These connection requests are filtered by the listenUsingRfcommWithServiceRecord method in
         * the constructor so we can make the connection right away since the method has already
         * done the vital security checks for us.
         *
         * Since we extend the thread class, this method needs to be called for proper thread operation.
         *
         * @return void
         */
        override fun run() {
            var socket: BluetoothSocket

            while(true) {
                // If user has cancelled listening at any point in time, we break
                if (this.cancelled) {
                    break
                }

                // attempt a connection if there exists a request, if connection does not work, break
                try {
                    socket = serverSocket!!.accept()
                } catch(e: IOException) {
                    break
                }

                // If connection was successful, start the bluetooth server, to read the incoming file
                if (!this.cancelled && socket != null) {
                    BluetoothServer(socket).start()
                }
            }
        }

        /**
         * This stops the BluetoothServerController. called by the user to stop listening for incoming requests.
         *
         * @return void
         */
        fun cancel() {
            this.cancelled = true
            this.serverSocket!!.close()
        }
    }

    /**
     * This is the BluetoothServer Class, this is called only when a connection with a remote device has been properly
     * made and the file sending has commenced from the client side. This server class will read the contents and
     * create a file with the file contents, and store it in the appropriate directory in external storage.
     *
     * Extends the Thread class.
     *
     * @param socket the socket of the phone who will act as a server.
     */
    class BluetoothServer(private val socket: BluetoothSocket): Thread() {
        /**
         * inputStream: the data stream that will contain the file contents, also known as the remote device's output
         */
        private val inputStream = this.socket.inputStream

        /**
         * outputStream: the data stream that this phone can communicate to the remote device.
         */
        private val outputStream = this.socket.outputStream

        /**
         * In this method, we are extracting the 4 components of the file to be able to get the file properly saved on
         * the device that is acting as a Server.
         *
         * Since we extend the thread class, this method needs to be called for proper thread operation.
         *
         * @return void
         */
        override fun run() {
            if (isExternalStorageWritable()) {
                val totalFileNameSizeInBytes: Int
                val totalFileSizeInBytes: Int

                // File name string size
                val fileNameSizebuffer = ByteArray(4) // Only 4 bytes needed for this operation, int => 4 bytes
                inputStream!!.read(fileNameSizebuffer, 0, 4)
                var fileSizeBuffer = ByteBuffer.wrap(fileNameSizebuffer)
                totalFileNameSizeInBytes = fileSizeBuffer.int

                // String of file name
                val fileNamebuffer = ByteArray(1024)
                inputStream.read(fileNamebuffer, 0, totalFileNameSizeInBytes)
                val fileName = String(fileNamebuffer, 0, totalFileNameSizeInBytes)

                // File size integer bytes
                val fileSizebuffer = ByteArray(4) // int => 4 bytes
                inputStream.read(fileSizebuffer, 0, 4)
                fileSizeBuffer = ByteBuffer.wrap(fileSizebuffer)
                totalFileSizeInBytes = fileSizeBuffer.int

                // The actual file bytes
                val baos = ByteArrayOutputStream()
                val buffer = ByteArray(1024)
                var read: Int
                var totalBytesRead = 0
                read = inputStream.read(buffer, 0, buffer.size)
                while (read != -1) {
                    baos.write(buffer, 0, read)
                    totalBytesRead += read
                    if (totalBytesRead == totalFileSizeInBytes) {
                        break
                    }
                    read = inputStream.read(buffer, 0, buffer.size)
                }
                baos.flush()

                val saveFile = getPublicStorageDir(fileName)
                if (saveFile.exists()) {
                    saveFile.delete()
                }
                val fos = FileOutputStream(saveFile.path)
                fos.write(baos.toByteArray())
                fos.close()
            }
            sleep(5000)
            inputStream.close()
            outputStream.close()
            socket.close()
        }
    }
}
