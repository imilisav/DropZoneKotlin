package com.example.dropzonekotlin

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.Intent.EXTRA_REFERRER_NAME
import android.os.IBinder
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.util.Log
import java.io.File
import java.io.IOException
import java.util.*

class BluetoothConnectionService : Service() {

    companion object {
        val uuid: UUID = UUID.fromString("8989063a-c9af-463a-b3f1-f21d9b2b827b")
        var fileURI: String = ""
    }

    /**
     * I'm Testing this out right now, to see what causes the IOException.
     * */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

//        if (intent!!.getStringExtra(EXTRA_MESSAGE) == "START_SERVER") {
//            BluetoothServerController().start()
//        } else if (intent.getStringExtra(EXTRA_MESSAGE) == "START_CLIENT") {
//            fileURI = intent.getStringExtra(EXTRA_REFERRER_NAME)
//            BluetoothClient(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)).start()
//        }
        Log.i("BLUETOOTH SERVICE", intent!!.getStringExtra(EXTRA_MESSAGE))

        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("BLUETOOTH SERVICE", "Destroyed")
    }

    class BluetoothClient(device: BluetoothDevice): Thread() {
        private val socket = device.createRfcommSocketToServiceRecord(uuid)

         override fun run() {
            Log.i("client", "Connecting")
            this.socket.connect()

            Log.i("client", "Sending")
            val outputStream = this.socket.outputStream
            val inputStream = this.socket.inputStream
            try {
                val buff = ByteArray(1024)
                File(fileURI).inputStream().buffered().use { input ->
                    while(true) {
                        val sz = input.read(buff)
                        if (sz <= 0) break

                        ///at that point we have a sz bytes in the buff to process
                        outputStream.write(sz)
                    }
                }
                outputStream.flush()
                Log.i("client", "Sent")
            } catch(e: Exception) {
                Log.e("client", "Cannot send", e)
            } finally {
                outputStream.close()
                inputStream.close()
                this.socket.close()
            }
        }
    }

    class BluetoothServerController : Thread() {
        private var cancelled: Boolean
        private val serverSocket: BluetoothServerSocket?

        init {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            if (btAdapter != null) {
                this.serverSocket = btAdapter.listenUsingRfcommWithServiceRecord("BluetoothConnectionService", uuid)
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
            try {
                val available = inputStream.available()
                val bytes = ByteArray(available)
                Log.i("server", "Reading")
                inputStream.read(bytes, 0, available)
                val text = String(bytes)
                Log.i("server", "Message received")
                Log.i("server", text)
                //activity.appendText(text)
            } catch (e: Exception) {
                Log.e("client", "Cannot read data", e)
            } finally {
                inputStream.close()
                outputStream.close()
                socket.close()
            }
        }
    }
}
