package com.pax.connectbase

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.PatternMatcher
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pax.baselink.api.BaseLinkApi
import com.pax.baselink.api.BaseResp
import com.pax.baselink.listener.IBluetoothDevice
import com.pax.baselink.listener.IBluetoothSearchListener
import com.pax.connectbase.dropdspin.TestDdsActivity
import com.pax.connectbase.scanner.zxing.ZxingScanner
import com.pax.connectbase.util.BuletoothUtil
import com.pax.connectbase.util.LogUtils

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var tvTest: TextView
    private lateinit var tvBlueName: TextView
    private lateinit var tvBlueConn: TextView
    private lateinit var tvWifiName: TextView
    private lateinit var tvWifiConn: TextView
    private lateinit var tvDnsTest: TextView
    private var progressDialog: ProgressDialog? = null
    private lateinit var baseLinkApi: BaseLinkApi
    private var bluetoothDevice: IBluetoothDevice? = null
    private var baseBlueIdStr: String = ""
    private var isFoundBlue: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        actionBar?.title = "Connect Base"
        //check application permission
        checkPermission()
        initView()
    }

    private fun initView() {
        tvTest = findViewById(R.id.tv_test)
        tvTest.setOnClickListener {
            //第一步：通过扫条码方式，获取到底座的SN
            //scanQrByZxing()
            //------test dns
            /*val testDnsList = BuletoothUtil.get4gDnsServers(baseContext)
            Log.w(TAG, "testDnsList=" + testDnsList.joinToString())
            tvDnsTest.text = testDnsList.joinToString()*/
            //------test dropdown spinner
            startActivity(Intent(this, TestDdsActivity::class.java))
        }
        tvBlueName = findViewById(R.id.tv_bluetooth_name_v)
        tvBlueConn = findViewById(R.id.tv_bluetooth_pair_v)
        tvWifiName = findViewById(R.id.tv_wifi_name_v)
        tvWifiConn = findViewById(R.id.tv_wifi_conn_v)
        tvDnsTest = findViewById(R.id.tv_dns_test)
    }

    @SuppressLint("MissingPermission")
    private fun doDiscovery() {
        var tempDevice: BluetoothDevice? = BuletoothUtil.findPairedDevice(baseBlueIdStr)
        showProcessDialog()
        if (tempDevice == null) {
            baseLinkApi.stopBluetoothSearch()
            SystemClock.sleep(500)
            isFoundBlue = false
            baseLinkApi.startBluetoothSearch(object : IBluetoothSearchListener {
                override fun onFinished() {
                    Log.i(TAG, "bluetooth search finish")
                    if (!isFoundBlue) {
                        progressDialog?.dismiss()
                        tvTest.text = "No Bluetooth Base with sn $baseBlueIdStr was found"
                    }
                }

                override fun onDiscovered(p0: IBluetoothDevice?) {
                    val nameBlue: String = p0?.name ?: ""
                    val identifierBlue: String = p0?.identifier ?: ""
                    Log.i(TAG, "onDiscovered 蓝牙名=$nameBlue")
                    if (nameBlue.contains(baseBlueIdStr)) {
                        Log.i(TAG, "找到:蓝牙名SN=$baseBlueIdStr")
                        isFoundBlue = true
                        bluetoothDevice = p0
                        baseLinkApi.stopBluetoothSearch()
                        APP.runInBackground {
                            //第四部：通过蓝牙通道，连接上底座，进行通信
                            val isSuccess: Boolean = baseLinkApi.btConnect(bluetoothDevice, 20)
                            if (isSuccess) {
                                Log.i(TAG, "btConnect success")
                                runOnUiThread {
                                    //progressDialog?.dismiss()
                                    tvBlueName.text = bluetoothDevice?.name
                                    tvBlueConn.text = "Connect Success"
                                }
                                //第五部：获取底座WiFi信息SSID、密码，(或者也可以设置WiFi的SSID和密码)
                                handleWifiConn()
                            } else {
                                Log.i(TAG, "btConnect fail")
                                runOnUiThread {
                                    progressDialog?.dismiss()
                                    tvBlueName.text = bluetoothDevice?.name
                                    tvBlueConn.text = "Connect Fail"
                                }
                            }
                        }
                    }
                }
            }, 20)
        } else {
            APP.runInBackground {
                //第四部：通过蓝牙通道，连接上底座，进行通信
                val isSuccess: Boolean = baseLinkApi.btConnect(tempDevice.address, 20)
                if (isSuccess) {
                    Log.i(TAG, "btConnect success")
                    runOnUiThread {
                        //progressDialog?.dismiss()
                        tvBlueName.text = tempDevice.name
                        tvBlueConn.text = "Connect Success"
                    }
                    //第五部：获取底座WiFi信息SSID、密码，(或者也可以设置WiFi的SSID和密码)
                    handleWifiConn()
                } else {
                    Log.i(TAG, "btConnect fail")
                    runOnUiThread {
                        progressDialog?.dismiss()
                        tvBlueName.text = tempDevice.name
                        tvBlueConn.text = "Connect Fail"
                    }
                }
            }
        }
    }

    private fun handleWifiConn() {
        //这里是可以设置WiFi的SSID、密码的例子
        /*val retChangeWifi = BuletoothUtil.setBaseWifiPwd(baseLinkApi)
        if (retChangeWifi.respCode == BaseResp.SUCCESS) {
            LogUtils.i(TAG, "setBaseWifiPwd success")
        } else {
            runOnUiThread {
                progressDialog?.dismiss()
                tvTest.text = "errCode:${retChangeWifi.respCode} errDesc:${retChangeWifi.respMsg}"
            }
            return
        }*/
        val devInfo = baseLinkApi.deviceInfo
        if (devInfo.respCode == BaseResp.SUCCESS) {
            runOnUiThread {
                LogUtils.i(TAG, "devInfo.respData.wifiSsid=" + devInfo.respData.wifiSsid)
                tvWifiName.text = devInfo.respData.wifiSsid
                //progressDialog?.dismiss()
            }
            //第六部：通过WiFi的SSID、密码，使A930RTX连上底座WIFI
            //此处根据实际需求看
            //可以添加判断，如果A930RTX已经连上了此底座WiFi；不需要再连了
            if (BuletoothUtil.isConnectSsid(this@MainActivity, devInfo.respData.wifiSsid)) {
                LogUtils.i(TAG, "devInfo.respData.wifiSsid=" + "already connect")
                runOnUiThread {
                    tvWifiConn.text = "already connect"
                    progressDialog?.dismiss()
                }
            } else {
                LogUtils.i(TAG, "devInfo.respData.wifiSsid=" + "request connect")

                val isSuccessWifi = BuletoothUtil.connectToWifiWithSSIDAndPwd(
                    this@MainActivity,
                    devInfo.respData.wifiSsid, devInfo.respData.wifiPasswd
                )
                runOnUiThread {
                    progressDialog?.dismiss()
                    if (isSuccessWifi) {
                        tvWifiConn.text = "Connect Success"
                    } else {
                        tvWifiConn.text = "Connect Fail"
                    }
                }
            }
        } else {
            progressDialog?.dismiss()
            //tvTest.text = "Error Obtaining Base Information"
            tvTest.text = "errCode:${devInfo.respCode} errDesc:${devInfo.respMsg}"
        }
    }

    private fun showProcessDialog() {
        progressDialog = ProgressDialog(this)
        progressDialog?.setTitle("Please Wait........")
        progressDialog?.setCancelable(false)
        progressDialog?.create()
        progressDialog?.show()
    }

    private fun checkPermission() {
        Log.d(TAG, "checkPermission")
        val currentapiVersion = Build.VERSION.SDK_INT
        Log.d(TAG, "currentapiVersion=$currentapiVersion")
        if (currentapiVersion >= Build.VERSION_CODES.M) { //API LEVEL 18
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_DENIED) {
                Log.d(TAG, "checkPermission - requestPermissions")
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            } else {
                Log.d(TAG, "checkPermission - no need requestPermissions")
                if (BuletoothUtil.hasBluetoothM() && !BuletoothUtil.isBluetoothEnable()) BuletoothUtil.openBluetooth()
                baseLinkApi = BaseLinkApi.getInstance(this)
            }
        } else {
            Log.d(TAG, "checkPermission - SDK Vesion < 23")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1001 -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "requestPermissions success")
                if (BuletoothUtil.hasBluetoothM() && !BuletoothUtil.isBluetoothEnable()) BuletoothUtil.openBluetooth()
                baseLinkApi = BaseLinkApi.getInstance(this)
            } else {
                Log.d(TAG, "requestPermissions fail")
                finish()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun scanQrByZxing() {
        //BuletoothUtil.openBluetooth()
        if (!BuletoothUtil.isLocationServiceEnabled(this)) {
            LogUtils.d(TAG, "请在状态栏快捷设置里打开定位功能")
            tvTest.text = "Turn on the location feature in the status bar shortcut Settings"
            return
        }
        tvTest.text = "Scan"
        APP.runInBackground {
            val zxingScanner = ZxingScanner(this@MainActivity)
            zxingScanner.open()
            zxingScanner.start(object : ZxingScanner.ZxingPortListener {
                override fun onReadSuccess(result: String?) {
                    LogUtils.d(TAG, "onReadSuccess")
                    zxingScanner.close()
                    if (!TextUtils.isEmpty(result)) {
                        //第二步：这里成功获取到底座SN
                        baseBlueIdStr = result.toString()
                    }
                    if (BuletoothUtil.isNonEmptyNumeric(baseBlueIdStr)) {
                        runOnUiThread {
                            //第三步：搜索周围蓝牙设备(或者蓝牙已经配对，就能够方便直接获取到)，通过SN找到哪一个是底座的蓝牙标识，例如名称
                            doDiscovery()
                        }
                    }  else {
                        tvTest.text = "The scan result is not base information"
                    }
                }

                override fun onReadError() {
                    LogUtils.d(TAG, "onReadError")
                    zxingScanner.close()
                }

                override fun onCancel() {
                    LogUtils.d(TAG, "onCancel")
                    zxingScanner.close()
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //第七部：断开程序的蓝牙连接通道
        baseLinkApi?.btDisconnect()
        if (broadcastReceiver != null) unregisterReceiver(broadcastReceiver)
    }

    @RequiresApi(VERSION_CODES.Q)
    private fun testConnectWifi2(ssid: String, pwd: String) {
        LogUtils.d(TAG, "testConnectWifi2")
        val specifier = WifiNetworkSpecifier.Builder()
            .setSsidPattern(PatternMatcher(ssid, PatternMatcher.PATTERN_PREFIX))
            .setWpa2Passphrase(pwd)
            .build()
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                LogUtils.d(TAG, "networkCallback onAvailable")
                // 处理成功逻辑
                runOnUiThread {
                    tvWifiConn.text = "Connect Success"
                }
            }

            override fun onUnavailable() {
                LogUtils.d(TAG, "networkCallback onUnavailable")
                // 处理失败逻辑
                runOnUiThread {
                    tvWifiConn.text = "Connect Fail"
                }
            }
        }
        connectivityManager.requestNetwork(request, networkCallback)
    }

    private var broadcastReceiver: BroadcastReceiver? = null
    @RequiresApi(VERSION_CODES.Q)
    private fun testConnectWifi(ssid: String, pwd: String) {
        LogUtils.d(TAG, "testConnectWifi")
        val suggestion2 = WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(pwd)
                .setIsAppInteractionRequired(true) // Optional (Needs location permission)
                .build()
        val suggestionsList = listOf(suggestion2)
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val status = wifiManager.addNetworkSuggestions(suggestionsList)
        if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            LogUtils.d(TAG, "WifiManager.STATUS_NETWORK_SUGGESTIONS error")
            // do error handling here
        } else {
            LogUtils.d(TAG, "WifiManager.STATUS_NETWORK_SUGGESTIONS")
        }
        // Optional (Wait for post connection broadcast to one of your suggestions)
        val intentFilter = IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!intent.action.equals(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                    LogUtils.d(TAG, "WIFI_NETWORK_SUGGESTION connect Fail")
                    return
                }
                LogUtils.d(TAG, "WIFI_NETWORK_SUGGESTION connect Success")
                // do post connect processing here
            }
        }
        registerReceiver(broadcastReceiver, intentFilter)
    }
}