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

import org.andstatus.app.ViewItem;
import org.andstatus.app.account.MyAccount;
import org.andstatus.app.context.MyContext;

/**
 * @author yvolk@yurivolkov.com
 */
public class TimelineListViewItem implements ViewItem {
    final Timeline timeline;
    final TimelineTitle timelineTitle;

    public TimelineListViewItem(MyContext myContext, Timeline timeline) {
        this.timeline = timeline;
        this.timelineTitle = TimelineTitle.load(myContext, timeline, MyAccount.EMPTY);
    }

    @Override
    public long getId() {
        return timeline == null ? 0 : timeline.getId();
    }
}
