package com.adrorodri.wifidirectexample

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecyclerAdapter(val context: Context, val listOfDevices: List<WifiP2pDevice>): RecyclerView.Adapter<DeviceViewHolder>() {
    private var onItemClickListener: ((device: WifiP2pDevice) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val inflater = LayoutInflater.from(context)
        return DeviceViewHolder(inflater.inflate(R.layout.device_item, parent, false))
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(listOfDevices[position])
        holder.itemView.setOnClickListener { onItemClickListener?.invoke(listOfDevices[position]) }
    }

    override fun getItemCount() = listOfDevices.size

    fun setOnItemClickListener(listner: (device: WifiP2pDevice) -> Unit) {
        onItemClickListener = listner
    }
}

class DeviceViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
    private val textView = itemView.findViewById<TextView>(R.id.tvName)
    fun bind(device: WifiP2pDevice) {
        textView.text = device.deviceName
    }
}