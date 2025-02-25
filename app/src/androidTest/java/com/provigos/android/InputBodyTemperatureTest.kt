/* MIT License
 *
 * Copyright 2024 Provigos

 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.provigos.android

import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.DatePicker
import android.widget.NumberPicker
import android.widget.TimePicker
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.PickerActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.provigos.android.presentation.view.activities.Input2Activity
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InputBodyTemperatureTest {
    private var intent: Intent = Intent(ApplicationProvider.getApplicationContext(), Input2Activity::class.java)

    @get:Rule
    val activityScenario = ActivityScenarioRule<Input2Activity>(intent.putExtra("key", "body_temperature"))

    @Test
    fun inputBodyTemperatureTest() {
        setDate(R.id.weight_date, 2020, 1, 1)
        setTime(R.id.weight_time, 12, 0)
        setNumberPickerValue(onView(withId(R.id.weight_number_picker)), 33)
        setNumberPickerValue(onView(withId(R.id.weight_number_picker2)), 0)
        onView(withId(R.id.save_text)).perform(click())
    }

    private fun setDate(datePickerLaunchViewId: Int, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        onView(withId(datePickerLaunchViewId)).perform(click())
        onView(withClassName(Matchers.equalTo<String>(DatePicker::class.java.name))).perform(
            PickerActions.setDate(year, monthOfYear, dayOfMonth)
        )
        onView(withId(android.R.id.button1)).perform(click())
    }

    private fun setTime(timePickerLaunchViewId: Int, hour: Int, minutes: Int) {
        onView(withId(timePickerLaunchViewId)).perform(click())
        onView(withClassName(Matchers.equalTo<String>(TimePicker::class.java.name))).perform(
            PickerActions.setTime(hour, minutes)
        )
        onView(withId(android.R.id.button1)).perform(click())
    }

    private fun setValue(value: Int): ViewAction {
        return object: ViewAction {
            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isAssignableFrom(NumberPicker::class.java)
            }

            override fun getDescription(): String {
                return "set the value of a " + NumberPicker::class.java.name
            }

            override fun perform(uiController: UiController?, view: View?) {
                val numberPicker = view as NumberPicker

                numberPicker.value = value

                /*val setValueMethod = NumberPicker::class.java.getDeclaredMethod(
                    "setValueInternal",
                    Int::class.java,
                    Boolean::class.java
                )
                setValueMethod.isAccessible = true

                setValueMethod.invoke(numberPicker, value, true)*/
            }
        }
    }

    private fun setNumberPickerValue(viewInteraction: ViewInteraction, value: Int) {
        viewInteraction.perform(setValue(value))

        viewInteraction.perform(GeneralSwipeAction(Swipe.SLOW, GeneralLocation.TOP_CENTER, GeneralLocation.BOTTOM_CENTER, Press.FINGER))
        SystemClock.sleep(50)
        viewInteraction.perform(GeneralSwipeAction(Swipe.SLOW, GeneralLocation.BOTTOM_CENTER, GeneralLocation.TOP_CENTER, Press.FINGER))
        SystemClock.sleep(50)

    }
}