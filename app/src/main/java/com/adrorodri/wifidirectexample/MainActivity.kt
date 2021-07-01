package com.adrorodri.wifidirectexample

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.IntentFilter
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.*
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private val intentFilter = IntentFilter()
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private lateinit var receiver: WifiDirectBroadcastReceiver

    private lateinit var buttonGroup: AppCompatButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecyclerAdapter
    private val listOfDevices = mutableListOf<WifiP2pDevice>()

    var isWifiP2pEnabled = false
    var isGroupRunning = false
    var isConnected = false

    private val peersUpdateListener = WifiP2pManager.PeerListListener { listResponse -> updateList(listResponse.deviceList) }
    private val connectionListener = WifiP2pManager.ConnectionInfoListener { info ->
            val groupOwnerAddress: String = info.groupOwnerAddress.hostAddress
            if (info.groupFormed && info.isGroupOwner) {
                isConnected = true
                supportActionBar?.title = "Connected as Server"
                supportActionBar?.subtitle = "My IP: $groupOwnerAddress"
                // Aqui abres un medio de comunicacion, como ser un socket para que el cliente se conecte y mande datos o mensajes
            } else if (info.groupFormed) {
                isConnected = true
                supportActionBar?.title = "Connected as Client"
                supportActionBar?.subtitle = "Server IP: $groupOwnerAddress"
                // Aqui te conectas al servidor, por ejemplo a traves de un socket para intercomunicarse datos
            } else {
                isConnected = false
                supportActionBar?.title = "Ready!"
                supportActionBar?.subtitle = null
            }
        }

    companion object {
        private const val REQ_CODE_PERMISSION = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()

        if (!hasPermission(ACCESS_FINE_LOCATION)) {
            requestPermissions()
            return
        } else {
            findPeers()
        }
    }

    @SuppressLint("MissingPermission")
    private fun findPeers() {
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)

        receiver = WifiDirectBroadcastReceiver(this, manager, channel,
            peersUpdateListener,
            connectionListener)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        registerReceiver(receiver, intentFilter)

        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d("P2P Discover Peers", "Started!")
            }

            override fun onFailure(reason: Int) {
                Log.d("P2P Discover Peers", "Error ${reason}!")
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.p2pmenu, menu);
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.disconnect -> {
                if (isConnected) {
                    manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                        override fun onSuccess() {
                            Log.d("P2P Stopped Correctly", "Started!")
                        }

                        override fun onFailure(reason: Int) {
                            Log.d("P2P Stop Error", "Error ${reason}!")
                        }
                    })
                }
            }
        }
        return true
    }

    private fun initViews() {
        buttonGroup = findViewById(R.id.buttonGroup)
        recyclerView = findViewById(R.id.recyclerView)

        adapter = RecyclerAdapter(this, listOfDevices)
        adapter.setOnItemClickListener {
            if(!isGroupRunning) {
                connect(it)
            } else {
                Toast.makeText(this, "You are host of a group!", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        buttonGroup.setOnClickListener {
            if (!isGroupRunning) {
                createGroup()
                buttonGroup.text = "Stop Group"
                isGroupRunning = true
            } else {
                stopGroup()
                buttonGroup.text = "Create Group"
                isGroupRunning = false
            }
        }
    }

    fun updateList(deviceNameList: MutableCollection<WifiP2pDevice>) {
        listOfDevices.clear()
        listOfDevices.addAll(deviceNameList.toList())
        adapter.notifyDataSetChanged()
    }

    fun onDisconnected() {
        isConnected = false
        supportActionBar?.title = "Ready!"
        supportActionBar?.subtitle = null
        findPeers()
    }

    @SuppressLint("MissingPermission")
    fun connect(device: WifiP2pDevice) {
        if (isConnected) {
            return
        }
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // WiFiDirectBroadcastReceiver notifies us. Ignore for now.
            }
            override fun onFailure(reason: Int) {
                Toast.makeText(
                    this@MainActivity,
                    "Connect failed. Retry.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun createGroup() {
        manager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Device is ready to accept incoming connections from peers.
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(
                    this@MainActivity,
                    "P2P group creation failed. Retry.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    fun stopGroup() {
        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // Device stopped to accept incoming connections from peers.
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(
                    this@MainActivity,
                    "P2P group removal failed. Retry.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun requestPermissions() {
        EasyPermissions.requestPermissions(
            PermissionRequest.Builder(this, REQ_CODE_PERMISSION, ACCESS_FINE_LOCATION).build()
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        findPeers()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        Toast.makeText(this, "Permissions are required!", Toast.LENGTH_SHORT).show()
        requestPermissions()
    }

    fun updateThisDevice(thisDevice: WifiP2pDevice?) {
        Log.d("P2P Update This Device", thisDevice.toString())
    }
}