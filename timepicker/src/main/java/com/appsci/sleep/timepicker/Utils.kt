package com.appsci.sleep.timepicker

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Utils {
    companion object {
        fun to_0_360(angle: Double): Double {
            var result = angle % 360
            if (result < 0) result += 360
            return result
        }

        fun to_0_720(angle: Double): Double {
            var result = angle % 720
            if (result < 0) result += 720
            return result
        }

        fun minutesToAngle(mins: Int): Double {
            return to_0_720(90 - (mins / (12 * 60.0)) * 360.0)
        }

        fun angleToMins(angle: Double): Int {
            return (((to_0_720(90 - angle)) / 360) * 12 * 60).toInt()
        }

        /**
        @param angle1 - first angle in radians
        @param angle2 - second angle in radians
        @return angle between vectors in radians
         **/
        fun angleBetweenVectors(angle1: Double, angle2: Double): Double {
            val x1 = cos(angle1)
            val y1 = sin(angle1)
            val x2 = cos(angle2)
            val y2 = sin(angle2)
            return vectorsAngleRad(x1, y1, x2, y2)
        }

        private fun cross(x1: Double, y1: Double, x2: Double, y2: Double): Double {
            return (x1 * y2 - y1 * x2)
        }

        private fun dot(x1: Double, y1: Double, x2: Double, y2: Double): Double {
            return (x1 * x2 + y1 * y2)
        }

        fun vectorsAngleRad(x1: Double, y1: Double, x2: Double, y2: Double): Double {
            return atan2(cross(x1, y1, x2, y2), dot(x1, y1, x2, y2))
        }

        fun snapMinutes(minutes: Int, step: Int): Int {
            return (minutes / step) * step + (2 * (minutes % step) / step) * step
        }
    }
}