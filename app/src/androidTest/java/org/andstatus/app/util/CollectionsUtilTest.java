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

package org.andstatus.app.util;

import org.andstatus.app.context.Travis;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.Assert.assertEquals;

@Travis
public class CollectionsUtilTest {

    @Test
    public void testSort() {
        List<String> list = new CopyOnWriteArrayList<>(new String[]{"one", "two", "three", "four"});
        CollectionsUtil.sort(list);
        assertEquals("four", list.get(0));
        assertEquals("two", list.get(3));
    }
}
