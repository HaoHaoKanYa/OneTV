package top.cywin.onetv.tv

import android.app.Application
import top.cywin.onetv.core.data.AppData

class MyTVApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        AppData.init(applicationContext)
        UnsafeTrustManager.enableUnsafeTrustManager()
    }
}
