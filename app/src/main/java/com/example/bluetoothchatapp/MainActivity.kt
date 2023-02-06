package com.example.bluetoothchatapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuProvider
import com.example.bluetoothchatapp.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isBtEnabled: Boolean = false

    private var activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
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

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("test006", "${it.key} = ${it.value}")
            }

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main_activity, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_search_devices -> {
                        checkLocationPermission()
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

    fun enableBluetooth() {
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_SHORT).show()
        }

        if (bluetoothAdapter?.isEnabled == false) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
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
                activityResultLauncher.launch(enableBTIntent)
            }
        } else {
            // Disable the bluetooth
            bluetoothAdapter.disable()
            isBtEnabled = false
            Toast.makeText(this, "BT disabled", Toast.LENGTH_SHORT).show()

        }
    }

    fun checkLocationPermission(){
    /*  **** Reduce Code ****
    if BT is enabled; that's mean user already granted all permissions

      if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ){
            requestPermissions()
            return
        } else {
     */
            if (isBtEnabled){
                val intent = Intent(this, DeviceListActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Switch On Your BT first", Toast.LENGTH_SHORT).show()

            }
        }

    private fun requestPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

}