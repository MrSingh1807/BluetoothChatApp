package com.example.bluetoothchatapp

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.bluetoothchatapp.MainActivity.Companion.MESSAGE_DEVICE_NAME
import com.example.bluetoothchatapp.MainActivity.Companion.MESSAGE_STATE_CHANGED
import com.example.bluetoothchatapp.MainActivity.Companion.MESSAGE_TOAST
import com.example.bluetoothchatapp.MainActivity.Companion.MESSAGE_WRITE
import java.io.IOException
import java.util.UUID


class ChatUtils(val context: Context, val handler: Handler) {

    private val APP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var connectThread: ConnectThread? = null
    private var acceptThread: AcceptThread? = null
    private var connectedThread: ConnectedThread? = null

    private var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    companion object {
        /***********   Chat Utils have some states    **********/
        const val STATE_NONE = 0
        const val STATE_LISTEN = 1
        const val STATE_CONNECTING = 2
        const val STATE_CONNECTED = 3
    }

    var btState = STATE_NONE

    private fun setState(state: Int) {
        handler.obtainMessage(MESSAGE_STATE_CHANGED, state, -1).sendToTarget()
    }

    fun start() {
        if (connectThread != null) {
            connectThread?.cancel()
            connectThread = null
        }
        if (acceptThread != null) {
            acceptThread = AcceptThread()
            acceptThread?.start()
        }

        if (connectedThread != null) {
            connectedThread?.cancel()
            connectedThread = null
        }
        setState(STATE_LISTEN)
    }

    fun stop() {
        if (connectThread != null) {
            connectThread?.cancel()
            connectThread = null
        }
        if (acceptThread != null) {
            acceptThread?.cancel()
            acceptThread?.start()
        }
        if (connectedThread != null) {
            connectedThread?.cancel()
            connectedThread = null
        }
        setState(STATE_NONE)
    }

    fun connect(device: BluetoothDevice) {
        if (btState == STATE_CONNECTING) {
            connectThread?.cancel()
            connectThread = null
        }
        connectThread = ConnectThread(device)
        connectThread!!.start()

        if (connectedThread != null) {
            connectedThread?.cancel()
            connectedThread = null
        }

        setState(STATE_CONNECTED)
    }
    fun write(buffer: ByteArray){
        var connThread : ConnectedThread
        synchronized(this){
            if (btState != STATE_CONNECTED){
                return
            }
            connThread = connectedThread!!
        }
        connThread.write(buffer)
    }

    inner class AcceptThread : Thread() {

        @SuppressLint("MissingPermission")
        private val btServerSocket: BluetoothServerSocket? =
            bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                APP_NAME, APP_UUID
            )

        override fun run() {
            var socket: BluetoothSocket? = null
            try {
                socket = btServerSocket!!.accept()
            } catch (e: IOException) {
                Log.d("Accept Thread: Run", e.message.toString())
                try {
                    btServerSocket?.close()
                } catch (e1: IOException) {
                    Log.d("Accept Thread: Close", e1.message.toString())
                }
            }

            if (socket != null) {
                when (btState) {
                    STATE_LISTEN -> {}
                    STATE_CONNECTING -> {
                        connect(socket.remoteDevice)
                    }
                    STATE_NONE -> {}
                    STATE_CONNECTED -> {
                        try {
                            socket.close()
                        } catch (e: IOException) {
                            Log.d("AcceptThread -> Run", e.message.toString())
                        }
                    }
                    else -> {}
                }
            }
        }

        fun cancel() {
            try {
                btServerSocket?.close()
            } catch (e: IOException) {
                Log.d("ConnectThread -> Close Server", e.message.toString())
            }
        }

    }
    inner class ConnectThread(private val device: BluetoothDevice) : Thread() {

        @SuppressLint("MissingPermission")
        private val socket: BluetoothSocket =
            device.createInsecureRfcommSocketToServiceRecord(APP_UUID)

        override fun run() {
            try {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                socket.connect()
            } catch (e: IOException) {
                Log.d("ConnectThread -> Run", e.message.toString())
                try {
                    socket.close()
                } catch (e1: IOException) {
                    Log.d("ConnectThread -> CloseSocket", e1.message.toString())
                    connectionFailed()
                }
            }

            synchronized(this@ChatUtils) {
                connectThread = null
            }

            connected(device)
        }

        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) {
                Log.d("ConnectThread -> CloseSocket", e.message.toString())

            }
        }

        private fun connectionFailed() {
            val messageToInformBTNotConnect = handler.obtainMessage(MESSAGE_TOAST)
            val bundle = Bundle()
            bundle.putString(TOAST, "Can't connect to the service")
            messageToInformBTNotConnect.data = bundle
            handler.sendMessage(messageToInformBTNotConnect)

            this@ChatUtils.start()
        }
        @SuppressLint("MissingPermission")
        private fun connected(device: BluetoothDevice) {
            if (connectThread != null) {
                connectThread?.cancel()
                connectThread = null
            }
            if (connectedThread != null) {
                connectedThread?.cancel()
                connectedThread = null
            }

            connectedThread = ConnectedThread(socket)
            connectedThread?.start()

            val messageToMainActivity = handler.obtainMessage(MESSAGE_DEVICE_NAME)
            val bundle = Bundle()
            bundle.putString(DEVICE_NAME, device.name)
            messageToMainActivity.data = bundle
            handler.sendMessage(messageToMainActivity)

            setState(STATE_CONNECTED)
        }
    }
    inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {

        val inputStream = socket.inputStream
        val outPutStream = socket.outputStream

        override fun run() {
            val buffer = ByteArray(1024)
            val bytes = inputStream.read(buffer)

            try {
                handler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget()
            }catch (e: IOException){
               connectionLost()
            }
        }
        fun write(buffer: ByteArray){
            try {
                outPutStream.write(buffer)
                handler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)
            } catch (e: IOException){
                connectionLost()
            }
        }
        fun cancel(){
            try {
                socket.close()
            } catch (e: IOException){
                Log.d("ConnectedThread: Run ", e.message.toString())
            }
        }
    }

    private fun connectionLost() {
        val message = handler.obtainMessage(MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(TOAST, "Connection Lost....")
        message.data = bundle

        handler.sendMessage(message)

        this@ChatUtils.start()
    }

}
