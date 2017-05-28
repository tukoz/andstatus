/*
 * Copyright (c) 2016 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.andstatus.app.timeline;

import org.andstatus.app.context.ActivityTest;
import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.context.TestSuite;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author yvolk@yurivolkov.com
 */
public class TimelineListTest extends ActivityTest<TimelineList> {

    @Override
    protected Class<TimelineList> getActivityClass() {
        return TimelineList.class;
    }

    @Before
    public void setUp() {
        super.setUp();
        TestSuite.initializeWithData(this);
    }

    @Test
    public void testActivityOpened() throws InterruptedException {
        int expectedCount = MyContextHolder.get().persistentTimelines().values().size();
        TestSuite.waitForListLoaded(getActivity(), expectedCount);
        assertTrue("Timelines shown: " + getActivity().getListAdapter().getCount(),
                getActivity().getListAdapter().getCount() ==  expectedCount);
    }
}
