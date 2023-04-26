package com.example.bluetoothchatapp

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuProvider
import com.example.bluetoothchatapp.ChatUtils.Companion.STATE_CONNECTED
import com.example.bluetoothchatapp.ChatUtils.Companion.STATE_CONNECTING
import com.example.bluetoothchatapp.ChatUtils.Companion.STATE_LISTEN
import com.example.bluetoothchatapp.ChatUtils.Companion.STATE_NONE
import com.example.bluetoothchatapp.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isBtEnabled = false
    private var chatUtils: ChatUtils? = null
    private lateinit var adapterMainChat: ArrayAdapter<String>

    private var connectedDeviceName: String? = null
    private var bluetoothAdapter: BluetoothAdapter? = null

    /***********   Launchers   **********/
    private var enableOrDisableBTLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            // There are no request codes
            val data = result.data
            Log.e("Activity result", "OK -> $data")
            isBtEnabled = true
            Toast.makeText(this, "BT enabled", Toast.LENGTH_SHORT).show()

        } else if (result.resultCode == RESULT_CANCELED) {
            Log.e("Activity result", "Not Ok")
            isBtEnabled = false
            Toast.makeText(this, "BT not opened", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            Log.d("test006", "${it.key} = ${it.value}")
        }

    }

    private val deviceListActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val address = result.data?.getStringExtra(DEVICE_ADDRESS)
            chatUtils?.connect(device = bluetoothAdapter!!.getRemoteDevice(address))
            Toast.makeText(this, "Address -> $address", Toast.LENGTH_SHORT).show()

        }
    }


    /***********   Broadcast Receiver   **********/
    private val br = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
                when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    BluetoothAdapter.STATE_ON -> {
                        isBtEnabled = true
                        bluetoothAdapter?.startDiscovery()
                    }
                    BluetoothAdapter.STATE_OFF -> {
                        isBtEnabled = false
                        bluetoothAdapter?.cancelDiscovery()
                    }
                }
            }
        }
    }


    /***********   Handler   **********/
    companion object {
        /***********   Messages For Handlers   **********/
        const val MESSAGE_STATE_CHANGED = 0
        const val MESSAGE_READ = 1
        const val MESSAGE_WRITE = 2
        const val MESSAGE_DEVICE_NAME = 3
        const val MESSAGE_TOAST = 4
    }

    val handler = Handler { message ->
        when (message.what) {
            MESSAGE_STATE_CHANGED -> {
                when (message.arg1) {
                    STATE_NONE -> {
                        setState("Not Connected")
                    }
                    STATE_LISTEN -> {
                        setState("Not Connected")
                    }
                    STATE_CONNECTING -> {
                        setState("Connected.....")
                    }
                    STATE_CONNECTED -> {
                        setState("Connected: $connectedDeviceName")
                    }
                }
            }
            MESSAGE_READ -> {
                val buffer = message.obj as ByteArray
                val inputBuffer = String(buffer, 0, message.arg1)
                adapterMainChat.add("$connectedDeviceName: $inputBuffer")
            }
            MESSAGE_WRITE -> {
                val buffer = message.obj as ByteArray
                val outPutStream = String(buffer)

                adapterMainChat.add("Me: $outPutStream")
            }
            MESSAGE_DEVICE_NAME -> {
                connectedDeviceName = message.data.getString(DEVICE_NAME)
                Toast.makeText(
                    this@MainActivity,
                    "$connectedDeviceName is Connected",
                    Toast.LENGTH_SHORT
                ).show()
            }
            MESSAGE_TOAST -> {
                Toast.makeText(this@MainActivity, message.data.getString(TOAST), Toast.LENGTH_SHORT)
                    .show()
            }
        }
        true
    }

    private fun setState(subTitle: CharSequence) {
        supportActionBar?.subtitle = subTitle
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        chatUtils = ChatUtils(this, handler = handler)

        // Set Adapter For Chatting Messages
        adapterMainChat = ArrayAdapter(this, R.layout.devices_list_items)
        binding.listConversation.adapter = adapterMainChat

        binding.sendBTN.setOnClickListener {
            val message = binding.enterMessageET.text.toString()

            if (message.isNotEmpty()) {
                binding.enterMessageET.setText("")
                chatUtils!!.write(message.toByteArray())
            }
        }

        // Bluetooth Adapter for Enable & Disable Bluetooth
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter!!.isEnabled) {
            isBtEnabled = true
        }

        // Menu item Click Handle
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main_activity, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_search_devices -> {
                        detailsListActivityLaunchIntent()
                        true
                    }
                    R.id.menu_enable_bluetooth -> {
                        enableBluetooth()
                        true
                    }
                    else -> {
                        true
                    }
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(br, filter)
    }

    fun enableBluetooth() {
        // Manage BT state here, whether it is enable or disable

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show()
        }

        if (!bluetoothAdapter!!.isEnabled) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions()
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            } else {

                val enableBTIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableOrDisableBTLauncher.launch(enableBTIntent)


                if (bluetoothAdapter?.scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    val discoveryIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                    discoveryIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                    startActivity(discoveryIntent)
                }
            }
        } else {
            // Disable  Bluetooth
            bluetoothAdapter?.disable()
            Toast.makeText(this, "BT disabled", Toast.LENGTH_SHORT).show()

        }
    }

    fun detailsListActivityLaunchIntent() {
        // Move to Device Activity to Know about Paired Or Available Devices when BT is enabled
        if (isBtEnabled) {
            val launchDeviceActivityIntent = Intent(this, DeviceListActivity::class.java)
            deviceListActivityResultLauncher.launch(launchDeviceActivityIntent)
        } else {
            Toast.makeText(this, "Switch On Your BT first", Toast.LENGTH_SHORT).show()

        }
    }

    private fun requestPermissions() {
        // All required Permission Here
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                )
            )
        }
    }

    override fun onStop() {
        unregisterReceiver(br)
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (chatUtils != null) {
            chatUtils!!.stop()
        }
    }

}