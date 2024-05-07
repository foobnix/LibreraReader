package org.spreadme.common

import java.text.SimpleDateFormat
import java.util.*

/**
 * generate uuid
 */
fun uuid() = UUID.randomUUID().toString()

/**
 * boolean chooser
 * @param trueValue
 * @param falseValue
 */
fun <T> Boolean.choose(trueValue: T, falseValue: T): T {
    return if (this) {
        trueValue
    } else {
        falseValue
    }
}


/**
 * format date
 * @param format
 */
fun Date.format(format: String): String {
    val dateFormat = SimpleDateFormat(format)
    return dateFormat.format(this)
}

/**
 * defualt date format
 */
fun Date.format(): String {
    return this.format("yyyy-MM-dd HH:mm:ss")
}
