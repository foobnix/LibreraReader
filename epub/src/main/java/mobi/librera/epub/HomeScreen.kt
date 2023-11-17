package mobi.librera.epub

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template

class HomeScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {

        return MessageTemplate
            .Builder("Hello Android Auto :)")
            .setTitle("My first screen")
            .build()
    }
}