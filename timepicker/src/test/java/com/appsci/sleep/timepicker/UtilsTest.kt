package com.appsci.sleep.timepicker

import com.tngtech.java.junit.dataprovider.DataProvider
import com.tngtech.java.junit.dataprovider.DataProviderRunner
import com.tngtech.java.junit.dataprovider.UseDataProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@RunWith(DataProviderRunner::class)
class UtilsTest {

    @JvmField
    @Rule
    val rule: MockitoRule = MockitoJUnit.rule()

    companion object {

        @JvmStatic
        @DataProvider()
        fun angleToTime(): List<List<Any>> {
            val result = mutableListOf<List<Any>>()
            val angles = listOf(0, 60, 90, 150, 180, 270, 330, 390, 450, 510, 570, 660, 690).map { it.toDouble() }
            val mins = listOf(3, 1, 0, 22, 21, 18, 16, 14, 12, 10, 8, 5, 4).map { it * 60 }
            angles.forEachIndexed { index, _ ->
                result.add(listOf(angles[index], mins[index]))
            }
            return result
        }

        @JvmStatic
        @DataProvider()
        fun snapMinutes(): List<List<Any>> {
            val minutes = listOf(0, 10, 15, 18, 28, 35)
            val snaps = listOf(0, 15, 15, 15, 30, 30)
            val result = mutableListOf<List<Any>>()
            minutes.forEachIndexed { index, _ ->
                result.add(listOf(minutes[index], snaps[index]))
            }
            return result
        }

        @JvmStatic
        @DataProvider()
        fun angleBetween(): List<List<Any>> {
            val firstAngles = listOf(0, 45, 90, 135, 190, 350).map { Math.toRadians(it.toDouble()) }
            val secondAngles = listOf(45, 90, 100, 145, 170, 10).map { Math.toRadians(it.toDouble()) }
            val expected = listOf(45, 45, 10, 10, -20, 20).map { Math.toRadians(it.toDouble()) }
            val result = mutableListOf<List<Any>>()
            firstAngles.forEachIndexed { index, _ ->
                result.add(listOf(firstAngles[index], secondAngles[index], expected[index]))
            }
            return result
        }
    }

    @Test
    @UseDataProvider("angleToTime")
    fun angleToTimeAndViceVersa(angle: Double, mins: Int) {
        assertThat(Utils.angleToMins(angle)).isEqualTo(mins)
        assertThat(Utils.minutesToAngle(mins)).isEqualTo(angle, Offset.offset(0.0001))
    }

    @Test
    @UseDataProvider("snapMinutes")
    fun testSnap(minutes: Int, expected: Int) {
        assertThat(Utils.snapMinutes(minutes, 15)).isEqualTo(expected)

    }

    @Test
    @UseDataProvider("angleBetween")
    fun testAngelBetweenVectors(first: Double, second: Double, expected: Double) {
        val result = Utils.angleBetweenVectors(first, second)
        assertThat(result).isEqualTo(expected, Offset.offset(Math.toRadians(0.001)))
    }
}