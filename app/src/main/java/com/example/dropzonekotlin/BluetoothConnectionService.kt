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

class BluetoothConnectionService {

    companion object {
        val uuid: UUID = UUID.fromString("8989063a-c9af-463a-b3f1-f21d9b2b827b")
        var fileURI: String = ""
        const val TAG: String = "BLUETOOTH CONNECTION"
        var success: Boolean = false
    }

    fun startServer() {
        Log.i("Server", "Starting")
        BluetoothServerController().start()
    }

    fun startClient(device: BluetoothDevice, uri: String) : Boolean {
        fileURI = uri

        val btClient = BluetoothClient(device)
        btClient.start()
        try {
            btClient.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, "File sending was interrupted")
        }

        return success
    }

    class BluetoothClient(device: BluetoothDevice): Thread() {
        private val socket = device.createRfcommSocketToServiceRecord(uuid)

         override fun run() {
             Log.i("client", "Connecting")

             this.socket.connect()

             val outputStream = this.socket.outputStream
             val inputStream = this.socket.inputStream

             val file = File(fileURI)
             val fileBytes: ByteArray
             try {
                 fileBytes = ByteArray(file.length().toInt())
                 val bis = BufferedInputStream(FileInputStream(file))
                 bis.read(fileBytes, 0, fileBytes.size)
                 bis.close()
             } catch (e: IOException) {
                 Log.e(TAG, "Error occurred when converting File to byte[]", e)
                 return
             }

             val fileNameSize = ByteBuffer.allocate(4)
             fileNameSize.putInt(file.name.toByteArray().size)

             val fileSize = ByteBuffer.allocate(4)
             fileSize.putInt(fileBytes.size)

             outputStream.write(fileNameSize.array())
             outputStream.write(file.name.toByteArray())
             outputStream.write(fileSize.array())
             outputStream.write(fileBytes)

             success = true

             sleep(5000)
             outputStream.close()
             inputStream.close()
             this.socket.close()
         }
    }

    class BluetoothServerController : Thread() {
        private var cancelled: Boolean
        private val serverSocket: BluetoothServerSocket?

        init {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            if (btAdapter != null) {
                this.serverSocket = btAdapter.listenUsingRfcommWithServiceRecord("DropZoneKotlin", uuid)
                this.cancelled = false
            } else {
                this.serverSocket = null
                this.cancelled = true
            }

        }

        override fun run() {
            var socket: BluetoothSocket

            while(true) {
                if (this.cancelled) {
                    break
                }

                try {
                    socket = serverSocket!!.accept()
                } catch(e: IOException) {
                    break
                }

                if (!this.cancelled && socket != null) {
                    Log.i("server", "Connecting")
                    BluetoothServer(socket).start()
                }
            }
        }

        fun cancel() {
            this.cancelled = true
            this.serverSocket!!.close()
        }
    }

    class BluetoothServer(private val socket: BluetoothSocket): Thread() {
        private val inputStream = this.socket.inputStream
        private val outputStream = this.socket.outputStream

        override fun run() {
            if (isExternalStorageWritable()) {
                // TODO: put try-catch blocks for each read
                Log.i(TAG, "START Bluetooth Server")
                val totalFileNameSizeInBytes: Int
                val totalFileSizeInBytes: Int

                // File name string size
                val fileNameSizebuffer = ByteArray(4)
                inputStream!!.read(fileNameSizebuffer, 0, 4)
                var fileSizeBuffer = ByteBuffer.wrap(fileNameSizebuffer)
                totalFileNameSizeInBytes = fileSizeBuffer.int

                // String of file name
                val fileNamebuffer = ByteArray(1024)
                inputStream.read(fileNamebuffer, 0, totalFileNameSizeInBytes)
                val fileName = String(fileNamebuffer, 0, totalFileNameSizeInBytes)

                // File size
                val fileSizebuffer = ByteArray(4)
                inputStream.read(fileSizebuffer, 0, 4)
                fileSizeBuffer = ByteBuffer.wrap(fileSizebuffer)
                totalFileSizeInBytes = fileSizeBuffer.int

                // The actual file
                Log.i(TAG, "Receiving file, please wait")
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
                Log.i(TAG, "File Received")
            }
            sleep(5000)
            inputStream.close()
            outputStream.close()
            socket.close()
        }
    }
}
