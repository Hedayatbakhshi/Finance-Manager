package com.financemanager.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.financemanager.app.ui.MainActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies the app starts and the main navigation is on screen. */
@RunWith(AndroidJUnit4.class)
public class SmokeTest {

    @Test
    public void launchesDashboard() {
        try (ActivityScenario<MainActivity> ignored =
                     ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()));
        }
    }
}
