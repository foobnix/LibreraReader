package org.spreadme.pdfgadgets.config

data class AppVersion constructor(
    val major: Int,
    val minor: Int,
    val revision: Int
) : Comparable<AppVersion> {

    companion object {
        fun toVersion(version: String): AppVersion {
            val numbers = version.split(".")
            if (numbers.size != 3) {
                throw IllegalArgumentException("illegal app version")
            }
            val major = numbers[0].toInt()
            val minor = numbers[1].toInt()
            val revision = numbers[2].toInt()
            return AppVersion(major, minor, revision)
        }
    }

    override fun compareTo(other: AppVersion): Int {
        val major = this.major.compareTo(other.major)
        if (major == 0) {
            val minor = this.minor.compareTo(other.minor)
            if (minor == 0) {
                return this.revision.compareTo(other.revision)
            }
            return minor
        }
        return major
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppVersion

        if (major != other.major) return false
        if (minor != other.minor) return false
        if (revision != other.revision) return false

        return true
    }

    override fun hashCode(): Int {
        var result = major
        result = 31 * result + minor
        result = 31 * result + revision
        return result
    }

}
