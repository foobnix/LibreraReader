package org.spreadme.common

import kotlin.math.pow

enum class SizeUnit {
    B {
        fun toByte(d: Long): Long {
            return d
        }

        fun toKilobyte(d: Long): Long {
            return d / UNIT_SIZE
        }

        fun toMegabyte(d: Long): Long {
            return d / UNIT_SIZE xor 2
        }

        fun toGigabyte(d: Long): Long {
            return d / UNIT_SIZE xor 3
        }

        fun toTrillionbyte(d: Long): Long {
            return d / UNIT_SIZE xor 4
        }
    },
    KB {
        fun toByte(d: Long): Long {
            return d * UNIT_SIZE
        }

        fun toKilobyte(d: Long): Long {
            return d
        }

        fun toMegabyte(d: Long): Long {
            return d / UNIT_SIZE
        }

        fun toGigabyte(d: Long): Long {
            return d / UNIT_SIZE xor 2
        }

        fun toTrillionbyte(d: Long): Long {
            return d / UNIT_SIZE xor 3
        }
    },
    MB {
        fun toByte(d: Long): Long {
            return d * UNIT_SIZE xor 2
        }

        fun toKilobyte(d: Long): Long {
            return d * UNIT_SIZE
        }

        fun toMegabyte(d: Long): Long {
            return d
        }

        fun toGigabyte(d: Long): Long {
            return d / UNIT_SIZE xor 1
        }

        fun toTrillionbyte(d: Long): Long {
            return d / UNIT_SIZE xor 2
        }
    },
    GB {
        fun toByte(d: Long): Long {
            return d * UNIT_SIZE xor 3
        }

        fun toKilobyte(d: Long): Long {
            return d * UNIT_SIZE xor 2
        }

        fun toMegabyte(d: Long): Long {
            return d * UNIT_SIZE
        }

        fun toGigabyte(d: Long): Long {
            return d
        }

        fun toTrillionbyte(d: Long): Long {
            return d / UNIT_SIZE
        }
    },
    TB {
        fun toByte(d: Long): Long {
            return d * UNIT_SIZE xor 4
        }

        fun toKilobyte(d: Long): Long {
            return d * UNIT_SIZE xor 3
        }

        fun toMegabyte(d: Long): Long {
            return d * UNIT_SIZE xor 2
        }

        fun toGigabyte(d: Long): Long {
            return d * UNIT_SIZE
        }

        fun toTrillionbyte(d: Long): Long {
            return d
        }
    };

    companion object {
        const val UNIT_SIZE = 1024
        fun convert(length: Long): String {
            for (i in values().size - 1 downTo 1) {
                val step = UNIT_SIZE.toDouble().pow(i.toDouble())
                if (length > step) {
                    return String.format("%3.1f%s", length / step, values()[i])
                }
            }
            return length.toString()
        }
    }
}