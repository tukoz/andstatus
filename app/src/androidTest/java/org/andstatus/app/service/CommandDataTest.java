/*
 * Copyright (C) 2014 yvolk (Yuri Volkov), http://yurivolkov.com
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

package org.andstatus.app.service;

import org.andstatus.app.SearchObjects;
import org.andstatus.app.account.MyAccount;
import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.context.TestSuite;
import org.andstatus.app.data.DbUtils;
import org.andstatus.app.data.MyQuery;
import org.andstatus.app.data.OidEnum;
import org.andstatus.app.context.DemoData;
import org.andstatus.app.util.MyLog;
import org.junit.Before;
import org.junit.Test;

import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommandDataTest {

    @Before
    public void setUp() throws Exception {
        TestSuite.initializeWithData(this);
    }

    @Test
    public void testQueue() throws InterruptedException {
        long time0 = System.currentTimeMillis(); 
        CommandData commandData = CommandData.newUpdateStatus(DemoData.getConversationMyAccount(), 1);
        testQueueOneCommandData(commandData, time0);

        long msgId = MyQuery.oidToId(OidEnum.MSG_OID, MyContextHolder.get().persistentOrigins()
                .fromName(DemoData.CONVERSATION_ORIGIN_NAME).getId(),
                DemoData.CONVERSATION_ENTRY_MESSAGE_OID);
        long downloadDataRowId = 23;
        commandData = CommandData.newFetchAttachment(msgId, downloadDataRowId);
        testQueueOneCommandData(commandData, time0);
    }

    private void testQueueOneCommandData(CommandData commandData, long time0)
            throws InterruptedException {
        final String method = "testQueueOneCommandData";
        assertEquals(0, commandData.getResult().getExecutionCount());
        assertEquals(0, commandData.getResult().getLastExecutedDate());
        assertEquals(CommandResult.INITIAL_NUMBER_OF_RETRIES, commandData.getResult().getRetriesLeft());
        commandData.getResult().prepareForLaunch();
        boolean hasSoftError = true;
        commandData.getResult().incrementNumIoExceptions();
        commandData.getResult().setMessage("Error in " + commandData.hashCode());
        commandData.getResult().afterExecutionEnded();
        DbUtils.waitMs(method, 50);
        long time1 = System.currentTimeMillis();
        assertTrue(commandData.getResult().getLastExecutedDate() >= time0);
        assertTrue(commandData.getResult().getLastExecutedDate() < time1);
        assertEquals(1, commandData.getResult().getExecutionCount());
        assertEquals(CommandResult.INITIAL_NUMBER_OF_RETRIES - 1, commandData.getResult().getRetriesLeft());
        assertEquals(hasSoftError, commandData.getResult().hasSoftError());
        assertFalse(commandData.getResult().hasHardError());
        
        CommandQueue queues = new CommandQueue();
        queues.clear();
        queues.get(QueueType.TEST).add(commandData);
        assertEquals(1, queues.save(QueueType.TEST));
        assertEquals(1, queues.load(QueueType.TEST));

        CommandData commandData2 = queues.get(QueueType.TEST).poll();

        assertEquals(commandData, commandData2);
        // Below fields are not included in equals
        assertEquals(commandData.getCommandId(), commandData2.getCommandId());
        assertEquals(commandData.getCreatedDate(), commandData2.getCreatedDate());

        assertEquals(commandData.getResult().getLastExecutedDate(), commandData2.getResult().getLastExecutedDate());
        assertEquals(commandData.getResult().getExecutionCount(), commandData2.getResult().getExecutionCount());
        assertEquals(commandData.getResult().getRetriesLeft(), commandData2.getResult().getRetriesLeft());
        assertEquals(commandData.getResult().hasError(), commandData2.getResult().hasError());
        assertEquals(commandData.getResult().hasSoftError(), commandData2.getResult().hasSoftError());
        assertEquals(commandData.getResult().getMessage(), commandData2.getResult().getMessage());
    }

    @Test
    public void testEquals() {
        CommandData data1 = CommandData.newSearch(SearchObjects.MESSAGES, MyContextHolder.get(), null, "andstatus");
        CommandData data2 = CommandData.newSearch(SearchObjects.MESSAGES, MyContextHolder.get(), null, "mustard");
        assertTrue("Hashcodes: " + data1.hashCode() + " and " + data2.hashCode(), data1.hashCode() != data2.hashCode());
        assertFalse(data1.equals(data2));
        
        data1.getResult().prepareForLaunch();
        data1.getResult().incrementNumIoExceptions();
        data1.getResult().afterExecutionEnded();
        assertFalse(data1.getResult().shouldWeRetry());
        CommandData data3 = CommandData.newSearch(SearchObjects.MESSAGES, MyContextHolder.get(), null, "andstatus");
        assertTrue(data1.equals(data3));
        assertTrue(data1.hashCode() == data3.hashCode());
        assertEquals(data1, data3);
    }

    @Test
    public void testPriority() {
        Queue<CommandData> queue = new PriorityBlockingQueue<>(100);
        queue.add(CommandData.newSearch(SearchObjects.MESSAGES,
                MyContextHolder.get(), DemoData.getMyAccount(DemoData.GNUSOCIAL_TEST_ACCOUNT_NAME).getOrigin(), "q1"));
        queue.add(CommandData.newUpdateStatus(null, 2));
        queue.add(CommandData.newCommand(CommandEnum.GET_TIMELINE));
        queue.add(CommandData.newUpdateStatus(null, 3));
        queue.add(CommandData.newCommand(CommandEnum.GET_STATUS));
        
        assertEquals(CommandEnum.UPDATE_STATUS, queue.poll().getCommand());
        assertEquals(CommandEnum.UPDATE_STATUS, queue.poll().getCommand());
        assertEquals(CommandEnum.GET_STATUS, queue.poll().getCommand());
        assertEquals(CommandEnum.GET_TIMELINE, queue.poll().getCommand());
        assertEquals(CommandEnum.GET_TIMELINE, queue.poll().getCommand());
    }

    @Test
    public void testSummary() {
        followUnfollowSummary(CommandEnum.FOLLOW_USER);
        followUnfollowSummary(CommandEnum.STOP_FOLLOWING_USER);
    }

    private void followUnfollowSummary(CommandEnum command) {
        MyAccount ma = DemoData.getMyAccount(DemoData.CONVERSATION_ACCOUNT_NAME);
        assertTrue(ma.isValid());
        long userId = MyQuery.oidToId(OidEnum.USER_OID, ma.getOrigin().getId(),
                DemoData.CONVERSATION_MEMBER_USER_OID);
        CommandData data = CommandData.newUserCommand(
                command, null, DemoData.getConversationMyAccount().getOrigin(), userId, "");
        String summary = data.toCommandSummary(MyContextHolder.get());
        String msgLog = command.name() + "; Summary:'" + summary + "'";
        MyLog.v(this, msgLog);
        assertTrue(msgLog, summary.contains(command.getTitle(MyContextHolder.get(),
                ma.getAccountName()) + " " + MyQuery.userIdToWebfingerId(userId)));
    }

}
