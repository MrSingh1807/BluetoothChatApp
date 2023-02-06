package com.example.bluetoothchatapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
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
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuProvider
import com.example.bluetoothchatapp.databinding.ActivityDeviceListBinding

class DeviceListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceListBinding
    private lateinit var adapterPairedDevices: ArrayAdapter<String>
    private lateinit var adapterAvailableDevices: ArrayAdapter<String>

    private val bluetoothDeviceListener = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action

            if (BluetoothDevice.ACTION_FOUND == action){
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                if (ActivityCompat.checkSelfPermission(
                        this@DeviceListActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                } else {
                    if (device != null && device.bondState != BluetoothDevice.BOND_BONDED) {
                        adapterAvailableDevices.add(device.name + "\n" + device.address)
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action){
                showProgressBar(false)
                if (adapterAvailableDevices.count == 0){
                    Toast.makeText(this@DeviceListActivity, "No New Devices Found",Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@DeviceListActivity, "Click On the device & Start the Chat",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapterPairedDevices = ArrayAdapter(this, R.layout.devices_list_items)
        adapterAvailableDevices = ArrayAdapter(this, R.layout.devices_list_items)

        binding.listPairedDevices.adapter = adapterPairedDevices
        binding.listAvailableDevices.adapter = adapterAvailableDevices

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager.adapter

        val pairedDevices = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        } else {
            bluetoothAdapter.bondedDevices
        }

        if (pairedDevices != null && pairedDevices.size > 0) {
            for (device in pairedDevices) {
                adapterPairedDevices.add(device.name + "\n" + device.address)
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
    private fun registerReceiver(){
        val intentFilter1 = IntentFilter(BluetoothDevice.EXTRA_DEVICE)
        registerReceiver(bluetoothDeviceListener, intentFilter1)
        val intentFilter2 = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(bluetoothDeviceListener,intentFilter2)
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