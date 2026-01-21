package com.pax.connectbase.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.PatternMatcher
import android.util.Log
import android.util.Patterns
import com.pax.baselink.api.BWifiManageParam
import com.pax.baselink.api.BaseLinkApi
import com.pax.baselink.api.BaseResp
import com.pax.baselink.api.EWifiManageType
import java.util.regex.Pattern

/**
 * Created by caizhiwei on 2023/11/2
 */
class BuletoothUtil {
    companion object {
        @SuppressLint("MissingPermission")
        @JvmStatic fun findPairedDevice(sn: String): BluetoothDevice? {
            val pairedDevices: Set<BluetoothDevice> =
                BluetoothAdapter.getDefaultAdapter().bondedDevices
            Log.i("BuletoothUtil", "已配对bondedDevices=${pairedDevices.size}")
            pairedDevices.forEach { device ->
                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
                Log.i("BuletoothUtil", "已配对蓝牙名=$deviceName")

            }
            return pairedDevices.find { it.name.contains(sn) }
        }

        @JvmStatic fun isNonEmptyNumeric(input: String): Boolean {
            return input.isNotBlank() && input.matches(Regex("\\d+"))
        }

        @JvmStatic fun hasBluetoothM(): Boolean {
            return BluetoothAdapter.getDefaultAdapter() != null
        }
        @JvmStatic fun isBluetoothEnable(): Boolean {
            return BluetoothAdapter.getDefaultAdapter().isEnabled
        }

        @SuppressLint("MissingPermission")
        @JvmStatic fun openBluetooth(): Boolean {
            return BluetoothAdapter.getDefaultAdapter().enable()
        }

        @JvmStatic fun connectToWifiWithSSIDAndPwd(context: Context, ssid: String, password: String): Boolean {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            // 检查 WiFi 是否已启用
            Log.i("BuletoothUtil", "wifi enable=" + wifiManager.isWifiEnabled)
            if (!wifiManager.isWifiEnabled) {
                wifiManager.isWifiEnabled = true
            }
            // 创建 WiFi 配置
            val wifiConfig = WifiConfiguration()
            wifiConfig.SSID = "\"$ssid\"" // 注意 SSID 需要用双引号括起来
            wifiConfig.preSharedKey = "\"$password\"" // 注意密码需要用双引号括起来
            // 添加并连接到 WiFi 网络
            val netId = wifiManager.addNetwork(wifiConfig)
            wifiManager.disconnect()
            wifiManager.enableNetwork(netId, true)
            return wifiManager.reconnect()
        }

        @JvmStatic fun setBaseWifiPwd(baseLinkApi: BaseLinkApi): BaseResp<*> {
            val param = BWifiManageParam()
            param.manageType = EWifiManageType.WIFI_MODE_AP_AUTO_START // 作为热点 自启动
            //------------------------NAT mode-------------------------------//
            param.ssid = "B910S_RTX_2960000508"
            param.passwd = "pax@base" //密码不能少于 8 位
            return baseLinkApi.wifiManage(param, 15)
        }

        @JvmStatic fun isLocationServiceEnabled(context: Context): Boolean {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
            return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }

        @JvmStatic fun isConnectSsid(context: Context, ssid: String): Boolean {
            Log.i("BuletoothUtil", "ssid = $ssid isConnectSsid()?")
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo: WifiInfo = wifiManager.connectionInfo
            val ssidPattern = Pattern.compile("\"$ssid\"")
            return wifiManager.isWifiEnabled && ssidPattern.matcher(wifiInfo.ssid).matches()
        }

        /**
         * 获取设备4G网络的DNS服务器列表
         * @param context 上下文对象
         * @return 包含DNS服务器地址的字符串列表
         */
        @JvmStatic fun get4gDnsServers(context: Context): List<String> {
            // 1. 获取ConnectivityManager服务
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

            // 如果服务不可用，返回空列表
            if (connectivityManager == null) {
                Log.w("NetworkUtils", "ConnectivityManager not available")
                return emptyList()
            }

            // 2. 遍历所有网络
            return connectivityManager.allNetworks
                .asSequence()  // 使用序列提高效率
                .mapNotNull { network ->
                    // 3. 获取网络能力和链路属性
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    val linkProperties = connectivityManager.getLinkProperties(network)

                    // 4. 检查是否为移动网络
                    if (capabilities != null && linkProperties != null &&
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        linkProperties.dnsServers
                    } else {
                        null
                    }
                }
                .flatten()  // 展平所有DNS服务器列表
                .map { it.hostAddress }  // 转换为IP地址字符串
                .filterNotNull()  // 过滤掉可能的null值
                .toList()  // 转换为最终列表
        }
    }
}