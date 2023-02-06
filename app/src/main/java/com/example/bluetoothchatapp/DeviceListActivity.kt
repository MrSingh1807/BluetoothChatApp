package com.example.bluetoothchatapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat
import com.example.bluetoothchatapp.databinding.ActivityDeviceListBinding

class DeviceListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeviceListBinding
    private lateinit var adapterPairedDevices: ArrayAdapter<String>
    private lateinit var adapterAvailableDevices: ArrayAdapter<String>


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

        if (pairedDevices != null && pairedDevices.size > 0){
            for (device in pairedDevices){
                adapterPairedDevices.add(device.name + "\n" + device.address)
            }
        }

    }
}