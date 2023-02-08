package com.example.bluetoothchatapp

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class DevicesAdapter(
    private val context: Context,
    private val resource: Int,
    private val devices: List<BluetoothDevice>
) :
    ArrayAdapter<BluetoothDevice>(context, resource, devices) {
    private val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    @SuppressLint("ViewHolder", "MissingPermission", "SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val device = devices[position]
        val view = layoutInflater.inflate(resource, parent, false)

        view.findViewById<TextView>(R.id.deviceInfo).text = (device.name + "\n" + device.address)

        return view
    }

}