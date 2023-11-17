package mobi.librera.epub

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver

class MyCarAppSession : Session(), DefaultLifecycleObserver {

    override fun onCreateScreen(intent: Intent): Screen {
        return CarScreesn(carContext)
    }
}