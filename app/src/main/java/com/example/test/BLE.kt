package com.example.test

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.lang.Long.parseLong
import java.util.*


private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2

private val tempCharacteristicUUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
private val tempServiceUUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")

class BLE : AppCompatActivity() {
    private lateinit var goBack : Button
    private lateinit var checkBLE :Button
    private lateinit var textState: TextView
    private lateinit var bleGatt: BluetoothGatt
    private lateinit var tempVal: TextView
    private lateinit var alertText: TextView

    private val bleAdapter : BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val bleScanner by lazy {
        bleAdapter.bluetoothLeScanner
    }



    private val isLocationPermissionGranted
        get() = hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)

    private var isScanning: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble)

        goBack = findViewById(R.id.backToMain)
        checkBLE = findViewById(R.id.toggleBluetooth)
        textState = findViewById(R.id.bleState)
        tempVal = findViewById(R.id.currTempVal)
        alertText = findViewById(R.id.alertText)

        goBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        checkBLE.setOnClickListener{
            if (isScanning) {
                stopBleScan()
            } else {
                startBleScan()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!bleAdapter.isEnabled) {
            promptEnableBluetooth()
        } else {
            val toast = Toast.makeText(this, "bluetooth already running", Toast.LENGTH_SHORT)
            toast.show()

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("BLE Permission: ", "granted")
                } else {

                }
            }

            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("Location Permission: ", "granted")
                }
            }

        }
    }




    private fun promptEnableBluetooth() {
        if (!bleAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) return
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    private fun startBleScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationPermissionGranted) {
            requestLocationPermission()
        } else {
            Log.i("BLE Scan: ", "can be started")
            bleScanner.startScan(null, scanSettings, scanCallback)
            isScanning = true
            textState.text = "scanning..."
            checkBLE.text = "turn off"
        }
    }

    private fun stopBleScan() {
        bleScanner.stopScan(scanCallback)
        isScanning = false
        textState.text = "off"
        checkBLE.text = "turn on"
    }

    private fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) == PackageManager.PERMISSION_GRANTED
    }


    // SCAN METHODS ----------------------------------------------------------------------------------------------
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result != null) {
                with(result.device)  {
                    Log.i("ScanCallback", "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                    if (name == "IR recv"){
                        Log.w("ScanCallback", "found the right one!")
                        stopBleScan()
                        this.connectGatt(this@BLE, false, gattCallback)

                    }

                }
            }
        }
    }

//    private val scanFilter = ScanFilter.Builder()
//        .setDeviceAddress(bleMac)
//        .build()

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private var gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val deviceAddress = gatt?.device?.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("Bluetooth GattCallback", "Successfully connected to $deviceAddress")
                    if (gatt != null) {
                        bleGatt = gatt
                    }
                    Handler(Looper.getMainLooper()).post {
                        textState.setText("connected")
                        gatt?.discoverServices()
                    }


                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("Bluetooth GattCallback", "Successfully disconnected to $deviceAddress")
                }
            } else {
                Log.w("Bluetooth GattCallback", "Error $status encountered for $deviceAddress! Disconnecting...")
                gatt?.close()
            }

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            with(gatt) {
                Log.w("Discovery Services: ", "Discovered ${this?.services?.size} from ${this?.device?.address}")
                if (gatt != null) {
                    for (bleService : BluetoothGattService in gatt.services ) {
                        Log.w("service: ", bleService.toString())
                    }
                    readTempLevel()
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            with (characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        Log.i("BluetoothGattCallback", "Read characteristic ${this?.uuid}:\n${this?.value?.toHexString()}")
                        this?.value?.toHexString()?.let { changeTempVal(it) }
                    }
                    BluetoothGatt.GATT_READ_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Read not permitted for ${this?.uuid}")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic read failed for ${this?.uuid}, error: $status")
                    }
                }

            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            with (characteristic) {
                Log.i("BluetoothGattCallback", "Read characteristic ${this?.uuid}:\n${this?.value?.toHexString()}")
                this?.value?.toHexString()?.let { changeTempVal(it) }

            }
        }
    }

    fun changeTempVal(temp: String) {
        var intTemp = parseLong(temp, 16)
        if (intTemp > 85) {

            runOnUiThread {
                // Stuff that updates the UI
                alertText.visibility = View.VISIBLE
                tempVal.setTextColor(Color.parseColor("#DC143C"))
                tempVal.text = intTemp.toString()
            }
//            alertText.text = "Alert!\nTemperature threshold broken!"
        } else {
            tempVal.setTextColor(Color.BLACK)
            alertText.visibility = View.INVISIBLE
            tempVal.text = intTemp.toString()
        }

    }

    private fun addNotification() {
        var notif = NotificationCompat.Builder(this, "0")
            .setContentTitle("Temperature alert!")
            .setContentText("The temperature threshold was broken!!")
    }

    private fun readTempLevel() {
        val tempLevelService = bleGatt.getService(tempServiceUUID)
        val tempLevelChar = tempLevelService.getCharacteristic(tempCharacteristicUUID)

        val descriptors: MutableList<BluetoothGattDescriptor>? = tempLevelChar.descriptors
        val descriptorUUID = descriptors?.get(0)?.uuid
        val tempLevelDescriptor = tempLevelChar.getDescriptor(descriptorUUID)

        Log.w("BluetoothGattCallback", "Discovered ${descriptors?.size} descriptors for $tempLevelChar")
        for (desc: BluetoothGattDescriptor in descriptors!!) {
            Log.w("descriptor: ", desc.toString())
        }

        if (tempLevelChar?.isReadable() == true) {
            tempLevelDescriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            bleGatt.writeDescriptor(tempLevelDescriptor)
            bleGatt.readCharacteristic(tempLevelChar)
            setCharacterNotification(tempLevelChar, true)
        }
    }

    fun setCharacterNotification (
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) {
        bleGatt?.let { gatt ->
            gatt.setCharacteristicNotification(characteristic, enabled)

        } ?: run {
            Log.w("BluetoothGattCallback", "BluetoothGatt not initialized")
        }
    }

    fun BluetoothGattCharacteristic.isReadable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)


    private fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return properties and property != 0
    }

    fun ByteArray.toHexString(): String =
        joinToString ( separator = "", prefix= "" ) { String.format("%02X", it)}

}