package com.example.bluetoothchatapp

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuProvider
import com.example.bluetoothchatapp.databinding.ActivityDeviceListBinding

class DeviceListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceListBinding
    private lateinit var adapterPairedDevices: ArrayAdapter<String>
    private lateinit var adapterAvailableDevices: ArrayAdapter<String>
    private var availabledevices = arrayListOf<BluetoothDevice>()


    private val bluetoothDeviceListener = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission", "SuspiciousIndentation")
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if (BluetoothDevice.ACTION_FOUND == action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                if (device != null && device.bondState != BluetoothDevice.BOND_BONDED) {
                    adapterAvailableDevices.add(device.name + "\n" + device.address)
                    availabledevices.add(device)
                }
//                else if (device != null && device.bondState != BluetoothDevice.BOND_NONE){
//                   device.createBond()
//                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                showProgressBar(false)
                if (adapterAvailableDevices.count == 0) {
                    Toast.makeText(
                        this@DeviceListActivity,
                        "No New Devices Found",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@DeviceListActivity,
                        "Click On the device & Start the Chat",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapterPairedDevices = ArrayAdapter(this, R.layout.devices_list_items)
        adapterAvailableDevices = ArrayAdapter(this, R.layout.devices_list_items,)

        binding.listPairedDevices.adapter = adapterPairedDevices
        binding.listAvailableDevices.adapter = adapterAvailableDevices

        binding.listPairedDevices.setOnItemClickListener { adapterView, view, int, long ->
            val info = (view as TextView).text.toString()
            val address = info.substring(info.length - 17)

            val intent = Intent()
            intent.putExtra(DEVICE_ADDRESS, address)
            setResult(RESULT_OK, intent)
            finish()
        }
        binding.listAvailableDevices.setOnItemClickListener{adapterView, view, int, long ->
           availabledevices[int].createBond()
            
        }

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager.adapter

        val pairedDevices = bluetoothAdapter.bondedDevices

        if (pairedDevices != null && pairedDevices.size > 0) {
            for (device in pairedDevices) {
                if (device.bluetoothClass.deviceClass == BluetoothClass.Device.PHONE_SMART) {
                    adapterPairedDevices.add(device.name + "\n" + device.address)
                }
            }
        }

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_device_list, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_scan_devices -> {
                        Toast.makeText(this@DeviceListActivity, "Scanning... ", Toast.LENGTH_SHORT)
                            .show()
                        scanDevices(bluetoothAdapter)

                        true
                    }
                    else -> true
                }
            }
        })
    }

    override fun onStart() {
        super.onStart()
        registerReceiver()
    }

    private fun registerReceiver() {
        val intentFilter1 = IntentFilter(BluetoothDevice.EXTRA_DEVICE)
        registerReceiver(bluetoothDeviceListener, intentFilter1)
        val intentFilter2 = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(bluetoothDeviceListener, intentFilter2)
        val intentFilter3 = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothDeviceListener, intentFilter3)
    }

    fun scanDevices(bluetoothAdapter: BluetoothAdapter) {
        showProgressBar(true)
        adapterAvailableDevices.clear()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Please Grant the BT Scan Permission First", Toast.LENGTH_SHORT)
                .show()

            return
        } else {
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter.startDiscovery()
        }
    }

    private fun showProgressBar(isVisible: Boolean) {
        if (isVisible) {
            binding.btScanProgressBar.visibility = View.VISIBLE
        } else {
            binding.btScanProgressBar.visibility = View.GONE
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(bluetoothDeviceListener)
    }
}