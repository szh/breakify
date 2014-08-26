/**
 * Copyright (C) 2014  Shlomo Zalman Heigh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.heightechllc.breakify.tests;

import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

import com.heightechllc.breakify.AlarmNotifications;
import com.heightechllc.breakify.RingingActivity;

/**
 * Tests RingingActivity
 */
public class RingingActivityTest extends ActivityInstrumentationTestCase2<RingingActivity> {
    private RingingActivity mActivity;

    public RingingActivityTest() {
        super(RingingActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mActivity = getActivity();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        // Hide the notification
        AlarmNotifications.hideNotification(getInstrumentation().getTargetContext());
    }

    public void test_ringsWhenNoUserActionIsSet() {

        restartActivityWithNoUserAction();

        // Make sure FLAG_ACTIVITY_NO_USER_ACTION is set
        assertTrue((mActivity.getIntent().getFlags() & Intent.FLAG_ACTIVITY_NO_USER_ACTION) != 0);

        // TODO: Make sure AlarmRinger is running
    }

    public void test_doesNotRingWhenNoUserActionIsNotSet() {
        // Make sure FLAG_ACTIVITY_NO_USER_ACTION is not set
        assertTrue((mActivity.getIntent().getFlags() & Intent.FLAG_ACTIVITY_NO_USER_ACTION) == 0);

        // TODO: Make sure AlarmRinger is running
    }

    //
    // Helpers
    //

    private void restartActivityWithNoUserAction() {
        // Create a new intent with FLAG_ACTIVITY_NO_USER_ACTION set
        Intent intent = new Intent(getInstrumentation().getTargetContext(), RingingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        setActivityIntent(intent);

        // Restart the activity
        mActivity.finish();
        setActivity(null);
        mActivity = getActivity();
    }
}
