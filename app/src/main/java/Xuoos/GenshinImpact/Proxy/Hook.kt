package Xuoos.GenshinImpact.Proxy

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.XModuleResources
//
import android.os.Handler
import android.os.Looper

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.PixelFormat
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.text.style.StyleSpan
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
//
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
//
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.util.*

import android.webkit.SslErrorHandler
import android.widget.*
import com.github.kyuubiran.ezxhelper.init.EzXHelperInit
import com.github.kyuubiran.ezxhelper.utils.*
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
/*
import org.json.JSONObject
import android.util.Base64
*/
import Xuoos.GenshinImpact.Proxy.Utils.dp2px
//import GenshinProxy.Xuoos.Utils.isInit
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
///
import java.io.File
import java.math.BigInteger
/*
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
*/
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.regex.Pattern
import javax.net.ssl.*
import kotlin.system.exitProcess
import java.io.IOException

class Hook {

    private var SizeError = false

    private lateinit var server: String
    private lateinit var SaveIP: String
    private lateinit var modulePath: String
    private lateinit var dialog: LinearLayout
    private lateinit var sp: SharedPreferences
    private lateinit var moduleRes: XModuleResources
    private lateinit var windowManager: WindowManager

    private val regex = Pattern.compile("http(s|)://.*?\\.(hoyoverse|mihoyo|yuanshen|mob)\\.com")

    private val more_domain =
            arrayListOf(
             "overseauspider.yuanshen.com:8888",
             "uspider.yuanshen.com:8888"
            )
    
    private val activityList: ArrayList<Activity> = arrayListOf()
    private var activity: Activity
        get() {
            for (mActivity in activityList) {
                if (mActivity.isFinishing) {
                    activityList.remove(mActivity)
                } else {
                    return mActivity
                }
            }
            throw Throwable("Activity not found.")
        }
        set(value) {
            activityList.add(value)
        }

    private fun getDefaultSSLSocketFactory(): SSLSocketFactory {
        return SSLContext.getInstance("TLS").apply {
            init(arrayOf<KeyManager>(), arrayOf<TrustManager>(DefaultTrustManager()), SecureRandom())
        }.socketFactory
    }

    private fun getDefaultHostnameVerifier(): HostnameVerifier {
        return DefaultHostnameVerifier()
    }

    class DefaultHostnameVerifier : HostnameVerifier {
        @SuppressLint("BadHostnameVerifier")
        override fun verify(p0: String?, p1: SSLSession?): Boolean {
            return true
        }

    }

    @SuppressLint("CustomX509TrustManager")
    private class DefaultTrustManager : X509TrustManager {

        @SuppressLint("TrustAllX509TrustManager")
        override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {
        }

        @SuppressLint("TrustAllX509TrustManager")
        override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }

    fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        modulePath = startupParam.modulePath
        moduleRes = XModuleResources.createInstance(modulePath, null)
        TrustMeAlready().initZygote()
    }

    @SuppressLint("WrongConstant", "ClickableViewAccessibility")
    fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {

        if (lpparam.packageName != "com.miHoYo.YuanShenPS") return
        EzXHelperInit.initHandleLoadPackage(lpparam)

        fun getFolderSize(folderPath: String): Long {
           val folder = File(folderPath)
           var totalSize: Long = 0

           if (folder.exists() && folder.isDirectory) {
               folder.listFiles()?.forEach { file ->
                   totalSize += if (file.isFile) file.length() else getFolderSize(file.absolutePath)
               }
           }
           return totalSize
        }

        val folderPath = "/storage/emulated/0/Android/data/com.miHoYo.YuanShenPS"
        val folderSize = getFolderSize(folderPath)
        var size = "无法获取"
        var unit = ""

        //检测数据大小，20gb
        if (folderSize < 20L * 1024 * 1024 * 1024) {

           if (folderSize < 1024 * 1024) {
               size = (folderSize / 1024).toString()
               unit = "KB"
           } else if (folderSize < 1024 * 1024 * 1024) {
               size = String.format("%.2f", folderSize.toFloat() / (1024 * 1024))
               unit = "MB"
           } else {
               size = String.format("%.2f", folderSize.toFloat() / (1024 * 1024 * 1024))
               unit = "GB"
           }
         SizeError = true
        }

        findMethod("com.combosdk.openapi.ComboApplication") { name == "attachBaseContext" }.hookBefore {
            val context = it.args[0] as Context
            sp = context.getSharedPreferences("ProxyConfig", 0)
            if (!sp.contains("serverip")) {
              sp.edit().putString("serverip", "https://127.0.0.1:54321").apply()
            }
            server = sp.getString("serverip", "") ?: ""
            if (!sp.contains("ResCheck")) {
              sp.edit().putBoolean("ResCheck", true).apply()
            }
              if (sp.getBoolean("ResCheck", true)) {
                 if (SizeError == true) {
                    server = "https://sdk.mihoyu.cn"
                 }
              }
        }

        findMethod(Activity::class.java, true) { name == "onCreate" }.hookBefore { param ->
            activity = param.thisObject as Activity
        }

        SSLHook()
        HttpHook()
 
        //hook 活动
        findMethod("com.miHoYo.GetMobileInfo.MainActivity") { name == "onCreate" }.hookBefore { param ->
          activity = param.thisObject as Activity

            Permission_test()
            if (sp.getBoolean("AutoDelCache", false)) AutoDelCache()
            if (sp.getBoolean("AtDelLl2cppFolder", false)) AtDelLl2cppFolder()

              if (sp.getBoolean("ResCheck", true)) {
                 if (SizeError == true) {
                 val sb_size = StringBuilder()
                 sb_size.append("客户端无数据/数据不完整，大小:\t")
                 sb_size.append(size)
                 sb_size.append(unit)
                 val sizewaring = sb_size.toString()
                 Toast.makeText(activity, sizewaring, Toast.LENGTH_SHORT).show()
                 Toast.makeText(activity, "自动进入资源下载服务器...", Toast.LENGTH_SHORT).show()
                 }
              }

          activity.windowManager.addView(LinearLayout(activity).apply {
             dialog = this
             visibility = View.GONE
             //文本位置
             setGravity(Gravity.CENTER)
             background = ShapeDrawable().apply {
                 shape = RoundRectShape(floatArrayOf(18f, 18f, 18f, 18f, 18f, 18f, 18f, 18f), null, null)
                 //背景颜色设置
                 paint.color = Color.argb((255 * 0.355).toInt(), 0x80, 0x8E, 0xEA)
             }

            addView(TextView(activity).apply {
                //外层
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).also {
                 it.gravity = Gravity.CENTER
                }
                //setPadding(10, 10, 10, 10) //距离
                setTextColor(Color.RED) //文字(标题)颜色
                setGravity(Gravity.CENTER) //文本与文本之间位置
                  setOnClickListener {
                    showDialog()
                  }
            })
          }, WindowManager.LayoutParams(dp2px(activity, 200f), dp2px(activity, 90f), WindowManager.LayoutParams.TYPE_APPLICATION, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply {
            //垂直居中
            gravity = Gravity.CENTER_VERTICAL
            //浮窗位置
            x = 0
            y = 0
          })

    fun runOnMainThread(action: () -> Unit) {
        Handler(Looper.getMainLooper()).post(action)
    }

     // 随机颜色
     fun getRainbowColor(): Int {
        val rainbowColors = intArrayOf(
            Color.parseColor("#FF8C00"), // 橙色
            Color.parseColor("#FFFF00"), // 黄色
            Color.parseColor("#008000"), // 绿色
            Color.parseColor("#0000FF"), // 蓝色
            Color.parseColor("#4B0082"), // 靛蓝色
            Color.parseColor("#EE82EE"), // 紫罗兰色
            Color.parseColor("#800000"), // 栗色
            Color.parseColor("#808000"), // 橄榄色
            Color.parseColor("#00FFFF")  // 青色
        )
     val random = Random()
     return rainbowColors[random.nextInt(rainbowColors.size)]
    }

          var ShowIP = server
          if (ShowIP == "") {
             ShowIP = "未设置地址(连接至官方服务器)"
          }

          val sb = StringBuilder()
          sb.append("点我打开代理设置\n")
          sb.append("目标服务器:\n")
          val startIndex = sb.length // 记录 ip 开始的位置
          sb.append(ShowIP)
          val originalString = sb.toString()

          Thread {
            runOnMainThread {
               val textView = dialog.getChildAt(0) as? TextView
               textView?.let {
                it.text = originalString
                val span = SpannableString(originalString)
                span.setSpan(ForegroundColorSpan(Color.GREEN), 0, 9, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                span.setSpan(ForegroundColorSpan(getRainbowColor()), startIndex, startIndex + ShowIP.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                span.setSpan(UnderlineSpan(), startIndex, startIndex + ShowIP.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                span.setSpan(StyleSpan(Typeface.BOLD), startIndex, startIndex + ShowIP.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) // 加粗
                it.text = span
                dialog.visibility = View.VISIBLE
               }
            }
            Thread.sleep(8000) //显示时长
             runOnMainThread {
                dialog.visibility = View.GONE
                activity.windowManager.removeView(dialog)
             }
          }.start()
        }
    }

    private fun AutoDelCache() {
       try {
         val cachePath = File("/sdcard/Android/data/com.miHoYo.YuanShenPS/cache")
         val cache1Path = File("/sdcard/Android/data/com.miHoYo.YuanShenPS/files/gee_logger_cache")
         val cache2Path = File("/data/data/com.miHoYo.YuanShenPS/code_cache")
         val cache3Path = File("/data/data/com.miHoYo.YuanShenPS/cache/WebView")
         val cache4Path = File("/data/data/com.miHoYo.YuanShenPS/app_webview/Default/GPUCache")
            if (cachePath.exists()) {
               cachePath.deleteRecursively()
            }
            if (cache1Path.exists()) {
               cache1Path.deleteRecursively()
            }
            if (cache2Path.exists()) {
               cache2Path.deleteRecursively()
            }
            if (cache3Path.exists()) {
               cache3Path.deleteRecursively()
            }
            if (cache4Path.exists()) {
               cache4Path.deleteRecursively()
            }
       } catch (e: IOException) {
              Toast.makeText(activity, "删除缓存时发生错误", Toast.LENGTH_LONG).show()
       }
    }


    private fun IPDialog() {
         val subject = arrayOf(
         "自定义服务器",
         "游戏数据下载",
         "本地服务器",
         "YuukiPS",
         "TomyJan",
         "天理尝蛆",
         )
         var selectedSubject = subject[0]
         AlertDialog.Builder(activity).apply {
             setTitle("请选择服务器:")
             setCancelable(false)
             setSingleChoiceItems(subject, 0) { _, which ->
                 selectedSubject = subject[which]
             }
             setPositiveButton("确定") { _, _ ->
                 when (selectedSubject) {
                     "自定义服务器" -> {
                     CustomIPDialog()
                     }
                     "游戏数据下载" -> {
                        sp.edit().run {
                         putString("serverip", "https://sdk.mihoyu.cn")
                         apply()
                         Toast.makeText(activity, "已保存地址设置，请重新打开客户端~", Toast.LENGTH_SHORT).show()
                         //不加sleep大概率保存无效
                         Thread.sleep(500)
                         exitProcess(0)
                        }
                     }
                     "本地服务器" -> {
                        sp.edit().run {
                         putString("serverip", "https://127.0.0.1:54321")
                         apply()
                         Toast.makeText(activity, "已保存地址设置，请重新打开客户端~", Toast.LENGTH_SHORT).show()
                         Thread.sleep(500)
                         exitProcess(0)
                        }
                     }
                     "YuukiPS" -> {
                        sp.edit().run {
                         putString("serverip", "https://login.yuuki.me")
                         apply()
                         Toast.makeText(activity, "已保存地址设置，请重新打开客户端~", Toast.LENGTH_SHORT).show()
                         Thread.sleep(500)
                         exitProcess(0)
                        }
                     }
                     "TomyJan" -> {
                        sp.edit().run {
                         putString("serverip", "https://tomyjan.com")
                         apply()
                         Toast.makeText(activity, "已保存地址设置，请重新打开客户端~", Toast.LENGTH_SHORT).show()
                         Thread.sleep(500)
                         exitProcess(0)
                        }
                     }
                     "天理尝蛆" -> {
                        sp.edit().run {
                         putString("serverip", "https://login.tianliserver.com")
                         apply()
                         Toast.makeText(activity, "已保存地址设置，请重新打开客户端~", Toast.LENGTH_SHORT).show()
                         Thread.sleep(500)
                         exitProcess(0)
                        }
                     }
                 }
             }
             setNeutralButton("取消") { _, _ ->
                showDialog()
             }
         }.show()
    }

    private fun showDialog() {
         AlertDialog.Builder(activity).apply {
              setTitle("代理设置")
              setMessage("倒卖🐶骨灰都给你妈扬咯")
              setCancelable(false)
              setView(ScrollView(context).apply {
                  setPadding(25, 0, 25, 0)
                  addView(LinearLayout(activity).apply {
                      orientation = LinearLayout.VERTICAL
                      addView(Switch(activity).apply {
                          text = "游戏数据检测 (需重启)"
                          isChecked = sp.getBoolean("ResCheck", false)
                          setOnCheckedChangeListener { _, b ->
                              sp.edit().run {
                                  putBoolean("ResCheck", b)
                                  apply()
                              }
                          }
                      })
                      addView(Switch(activity).apply {
                          text = "自动删除客户端缓存 (需重启)"
                          isChecked = sp.getBoolean("AutoDelCache", false)
                          setOnCheckedChangeListener { _, b ->
                              sp.edit().run {
                                  putBoolean("AutoDelCache", b)
                                  apply()
                              }
                          }
                      })
                      addView(Switch(activity).apply {
                          text = "自动删除“il2cpp”文件夹 (需重启)"
                          isChecked = sp.getBoolean("AtDelLl2cppFolder", false)
                          setOnCheckedChangeListener { _, b ->
                              sp.edit().run {
                                  putBoolean("AtDelLl2cppFolder", b)
                                  apply()
                              }
                          }
                      })
                  })
              })
              setPositiveButton("关闭") { _, _ ->
                  // none
              }
              setNegativeButton("修改服务器地址") { _, _ ->
                  IPDialog()
              }
              setNeutralButton("🚫退出游戏") { _, _ ->
                  exitProcess(0)
              }
          }.show()
    }

/*
setNeutralButton 左
setPositiveButton 右
setNegativeButton 右中
*/

    private fun CustomIPDialog() {
       AlertDialog.Builder(activity).apply {
               setTitle("请输入服务器地址:")
               setCancelable(false)
               setView(ScrollView(context).apply {
                   setPadding(25, 0, 25, 0)
                   addView(LinearLayout(activity).apply {
                       orientation = LinearLayout.VERTICAL
                       addView(EditText(activity).apply {
                           //输入框提示
                           hint = "http(s)://server.com:1234"
                           //输入框显示内容
                           val str = ""
                           setText(str.toCharArray(), 0, str.length)
                           addTextChangedListener(object : TextWatcher {
                               override fun beforeTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {}
                               override fun onTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {}

                               @SuppressLint("CommitPrefEdits")
                               override fun afterTextChanged(p0: Editable) {
                                 val import_ip = p0.toString()
                                 val sb = StringBuilder()
                                 sb.append("https://")
                                 sb.append(import_ip)
                                 val originalString = sb.toString()
                                  sp.edit().run {
                                    if (import_ip == "") {
                                      SaveIP = ""
                                    } else if (import_ip == "localhost" || import_ip == "127.0.0.1") {
                                      SaveIP = "https://127.0.0.1:54321"
                                    } else if (!import_ip.startsWith("https://") && !import_ip.startsWith("http://")) {
                                      SaveIP = originalString
                                    } else {
                                      SaveIP = import_ip
                                    }
                                      //实时保存
                                      //apply()
                                  }
                               }
                           })
                       })
                   })
               })
               setNeutralButton("返回") { _, _ ->
                   showDialog()
               }
               setPositiveButton("保存地址") { _, _ ->
                 if (SaveIP == "") {
                   Toast.makeText(activity, "错误: 输入框未填写任何内容", Toast.LENGTH_SHORT).show()
                   CustomIPDialog()
                 } else if (SaveIP.endsWith("/")) {
                   Toast.makeText(activity, "错误: 地址结尾不能有“/”！", Toast.LENGTH_SHORT).show()
                   CustomIPDialog()
                 } else {
                     sp.edit().run {
                          putString("serverip", SaveIP)
                          apply()
                          val ser_ip = sp.getString("serverip", "") ?: ""  
                          val sb = StringBuilder()
                          sb.append("已保存地址:\t")
                          sb.append(ser_ip)
                          val originalString = sb.toString()
                          Toast.makeText(activity, originalString, Toast.LENGTH_SHORT).show()
                          Toast.makeText(activity, "请重新打开客户端~", Toast.LENGTH_SHORT).show()
                     Thread.sleep(500)
                     exitProcess(0)
                     }
                 }
               }
        }.show()
    }

    //自动删除il2cpp
    private fun AutoDelLl2cppFolder() {
       try {
        val il2cppPath = File("/sdcard/Android/data/com.miHoYo.YuanShenPS/files/il2cpp")
          if (startAtDelLl2cppFolder) {
              if (il2cppPath.exists()) {
                 il2cppPath.deleteRecursively()
              }
          }
       } catch (e: IOException) {
              Toast.makeText(activity, "删除il2cpp文件夹时发生错误", Toast.LENGTH_LONG).show()
       }
    }

    private fun SSLHook() {
        // OkHttp3 Hook
        findMethodOrNull("com.combosdk.lib.third.okhttp3.OkHttpClient\$Builder") { name == "build" }
                ?.hookBefore {
                    it.thisObject.invokeMethod(
                            "sslSocketFactory",
                            args(getDefaultSSLSocketFactory()),
                            argTypes(SSLSocketFactory::class.java)
                    )
                    it.thisObject.invokeMethod(
                            "hostnameVerifier",
                            args(getDefaultHostnameVerifier()),
                            argTypes(HostnameVerifier::class.java)
                    )
                }
        findMethodOrNull("okhttp3.OkHttpClient\$Builder") { name == "build" }?.hookBefore {
            it.thisObject.invokeMethod(
                    "sslSocketFactory",
                    args(getDefaultSSLSocketFactory(), DefaultTrustManager()),
                    argTypes(SSLSocketFactory::class.java, X509TrustManager::class.java)
            )
            it.thisObject.invokeMethod(
                    "hostnameVerifier",
                    args(getDefaultHostnameVerifier()),
                    argTypes(HostnameVerifier::class.java)
            )
        }
        // WebView Hook
        arrayListOf(
                        "android.webkit.WebViewClient",
                        //"cn.sharesdk.framework.g",
                        //"com.facebook.internal.WebDialog\$DialogWebViewClient",
                        "com.geetest.sdk.dialog.views.GtWebView\$c",
                        "com.miHoYo.sdk.webview.common.view.ContentWebView\$6"
                )
                .forEach {
                    findMethodOrNull(it) {
                        name == "onReceivedSslError" &&
                                parameterTypes[1] == SslErrorHandler::class.java
                    }
                            ?.hookBefore { param -> (param.args[1] as SslErrorHandler).proceed() }
                }
        // Android HttpsURLConnection Hook
        findMethodOrNull("javax.net.ssl.HttpsURLConnection") {
            name == "getDefaultSSLSocketFactory"
        }
                ?.hookBefore { it.result = getDefaultSSLSocketFactory() }
        findMethodOrNull("javax.net.ssl.HttpsURLConnection") { name == "setSSLSocketFactory" }
                ?.hookBefore { it.result = null }
        findMethodOrNull("javax.net.ssl.HttpsURLConnection") {
            name == "setDefaultSSLSocketFactory"
        }
                ?.hookBefore { it.result = null }
        findMethodOrNull("javax.net.ssl.HttpsURLConnection") { name == "setHostnameVerifier" }
                ?.hookBefore { it.result = null }
        findMethodOrNull("javax.net.ssl.HttpsURLConnection") {
            name == "setDefaultHostnameVerifier"
        }
                ?.hookBefore { it.result = null }
        findMethodOrNull("javax.net.ssl.HttpsURLConnection") {
            name == "getDefaultHostnameVerifier"
        }
                ?.hookBefore { it.result = getDefaultHostnameVerifier() }
    }

    // Bypass HTTP
    private fun HttpHook() {
        findMethod("com.miHoYo.sdk.webview.MiHoYoWebview") {
            name == "load" &&
                    parameterTypes[0] == String::class.java &&
                    parameterTypes[1] == String::class.java
        }
                .hookBefore { replaceUrl(it, 1) }
        findAllMethods("android.webkit.WebView") { name == "loadUrl" }.hookBefore {
            replaceUrl(it, 0)
        }
        findAllMethods("android.webkit.WebView") { name == "postUrl" }.hookBefore {
            replaceUrl(it, 0)
        }

        findMethod("okhttp3.HttpUrl") { name == "parse" && parameterTypes[0] == String::class.java }
                .hookBefore { replaceUrl(it, 0) }
        findMethod("com.combosdk.lib.third.okhttp3.HttpUrl") {
            name == "parse" && parameterTypes[0] == String::class.java
        }
                .hookBefore { replaceUrl(it, 0) }

        findMethod("com.google.gson.Gson") {
            name == "fromJson" &&
                    parameterTypes[0] == String::class.java &&
                    parameterTypes[1] == java.lang.reflect.Type::class.java
        }
                .hookBefore { replaceUrl(it, 0) }
        findConstructor("java.net.URL") { parameterTypes[0] == String::class.java }.hookBefore {
            replaceUrl(it, 0)
        }
        findMethod("com.combosdk.lib.third.okhttp3.Request\$Builder") {
            name == "url" && parameterTypes[0] == String::class.java
        }
                .hookBefore { replaceUrl(it, 0) }
        findMethod("okhttp3.Request\$Builder") {
            name == "url" && parameterTypes[0] == String::class.java
        }
                .hookBefore { replaceUrl(it, 0) }
    }

    //检测数据是否可读
    private fun Permission_test() {
       try {
          val file = File("/sdcard/Android/data/com.miHoYo.YuanShenPS/files/AssetBundles/blocks/test.txt")
          val folder = File("/sdcard/Android/data/com.miHoYo.YuanShenPS/files/AssetBundles")
          val folder1 = File("/sdcard/Android/data/com.miHoYo.YuanShenPS/files/AssetBundles/blocks")
              if (!folder.exists() && !folder.isDirectory()) {
                  folder.mkdirs()
              }
              if (!folder1.exists() && !folder1.isDirectory()) {
                  folder1.mkdirs()
              }
              if (!file.exists()) {
                  file.createNewFile()
                  file.appendText("测试客户端读取/写入\n--Xuoos")
              } else {
                  file.delete()
                  file.createNewFile()
                  file.appendText("测试客户端读取/写入\n--Xuoos")
              }
       } catch (e: IOException) {
              Toast.makeText(activity, "内存不足/无法读取游戏数据！", Toast.LENGTH_LONG).show()
       }
    }

    private fun replaceUrl(method: XC_MethodHook.MethodHookParam, args: Int) {
        var Xuoos = method.args[args].toString()
        val m = regex.matcher(Xuoos)

        if (server == "") return
        if (Xuoos == "") return
        if (method.args[args] == null) return
        if (Xuoos.startsWith("autopatchhk.yuanshen.com")) return
        if (Xuoos.startsWith("autopatchcn.yuanshen.com")) return
        // 跳过配置设置 (不可用)skip config areal (BAD 3.5)
        //if (Xuoos.startsWith("[{\"area\":")) return

        for (list in more_domain) {
            for (head in arrayListOf("http://", "https://")) {
               method.args[args] = method.args[args].toString().replace(head + list, server)
            }
        }

        if (m.find()) {
            method.args[args] = m.replaceAll(server)
        }
    }
}