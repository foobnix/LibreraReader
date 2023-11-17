package mobi.librera.epub

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template

class CarScreesn(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {

        return MessageTemplate
            .Builder("Hello Android Auto :)")
            .setTitle("My first screen")
            .build()
    }
}