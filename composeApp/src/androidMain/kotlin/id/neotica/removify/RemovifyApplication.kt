package id.neotica.removify

import android.app.Application
import id.neotica.removify.di.appModules
import id.neotica.removify.platform.AndroidAppContext
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class RemovifyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppContext.appContext = applicationContext
        startKoin {
            androidContext(applicationContext)
            modules(*appModules)
        }
    }
}
