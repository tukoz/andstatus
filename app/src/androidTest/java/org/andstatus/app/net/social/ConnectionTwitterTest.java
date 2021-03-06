/* 
 * Copyright (c) 2013 yvolk (Yuri Volkov), http://yurivolkov.com
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

package org.andstatus.app.net.social;

import android.support.test.InstrumentationRegistry;

import org.andstatus.app.account.AccountDataReaderEmpty;
import org.andstatus.app.account.AccountName;
import org.andstatus.app.account.MyAccount;
import org.andstatus.app.context.DemoData;
import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.context.TestSuite;
import org.andstatus.app.data.DataUpdater;
import org.andstatus.app.data.DownloadStatus;
import org.andstatus.app.data.MyContentType;
import org.andstatus.app.net.http.HttpConnectionMock;
import org.andstatus.app.net.http.OAuthClientKeys;
import org.andstatus.app.net.social.Connection.ApiRoutineEnum;
import org.andstatus.app.origin.Origin;
import org.andstatus.app.origin.OriginConnectionData;
import org.andstatus.app.service.CommandData;
import org.andstatus.app.service.CommandEnum;
import org.andstatus.app.service.CommandExecutionContext;
import org.andstatus.app.util.MyHtml;
import org.andstatus.app.util.RawResourceUtils;
import org.andstatus.app.util.TriState;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class ConnectionTwitterTest {
    private Connection connection;
    private HttpConnectionMock httpConnection;
    private OriginConnectionData connectionData;
    
    @Before
    public void setUp() throws Exception {
        TestSuite.initializeWithData(this);

        TestSuite.setHttpConnectionMockClass(HttpConnectionMock.class);
        Origin origin = MyContextHolder.get().persistentOrigins().fromName(DemoData.TWITTER_TEST_ORIGIN_NAME);

        connectionData = OriginConnectionData.fromAccountName(
                AccountName.fromOriginAndUserName(origin, DemoData.TWITTER_TEST_ACCOUNT_USERNAME),
                TriState.UNKNOWN);
        connectionData.setAccountUserOid(DemoData.TWITTER_TEST_ACCOUNT_USER_OID);
        connectionData.setDataReader(new AccountDataReaderEmpty());
        connection = connectionData.newConnection();
        httpConnection = (HttpConnectionMock) connection.http;

        httpConnection.data.originUrl = origin.getUrl();
        httpConnection.data.oauthClientKeys = OAuthClientKeys.fromConnectionData(httpConnection.data);

        if (!httpConnection.data.oauthClientKeys.areKeysPresent()) {
            httpConnection.data.oauthClientKeys.setConsumerKeyAndSecret("keyForGetTimelineForTw", "thisIsASecret341232");
        }
        TestSuite.setHttpConnectionMockClass(null);
    }

    @Test
    public void testGetTimeline() throws IOException {
        String jso = RawResourceUtils.getString(InstrumentationRegistry.getInstrumentation().getContext(),
                org.andstatus.app.tests.R.raw.twitter_home_timeline);
        httpConnection.setResponse(jso);
        
        List<MbActivity> timeline = connection.getTimeline(ApiRoutineEnum.HOME_TIMELINE,
                new TimelinePosition("380925803053449216") , TimelinePosition.EMPTY, 20, connectionData.getAccountUserOid());
        assertNotNull("timeline returned", timeline);
        int size = 4;
        assertEquals("Number of items in the Timeline", size, timeline.size());

        int ind = 0;
        MbActivity activity = timeline.get(ind);
        String hostName = DemoData.getTestOriginHost(DemoData.TWITTER_TEST_ORIGIN_NAME).replace("api.", "");
        assertEquals("Posting message", MbObjectType.MESSAGE, activity.getObjectType());
        MbMessage message = timeline.get(ind).getMessage();
        assertEquals("MyAccount", connectionData.getAccountUserOid(), activity.accountUser.oid);
        assertEquals("Favorited by me", TriState.TRUE, message.getFavoritedByMe());
        MbUser author = message.getAuthor();
        assertEquals("Oid", "221452291", author.oid);
        assertEquals("Username", "Know", author.getUserName());
        assertEquals("WebFinger ID", "Know@" + hostName, author.getWebFingerId());
        assertEquals("Display name", "Just so you Know", author.getRealName());
        assertEquals("Description", "Unimportant facts you'll never need to know. Legally responsible publisher: @FUN", author.getDescription());
        assertEquals("Location", "Library of Congress", author.location);
        assertEquals("Profile URL", "https://" + hostName + "/Know", author.getProfileUrl());
        assertEquals("Homepage", "http://t.co/4TzphfU9qt", author.getHomepage());
        assertEquals("Avatar URL", "https://si0.twimg.com/profile_images/378800000411110038/a8b7eced4dc43374e7ae21112ff749b6_normal.jpeg", author.avatarUrl);
        assertEquals("Banner URL", "https://pbs.twimg.com/profile_banners/221452291/1377270845", author.bannerUrl);
        assertEquals("Messages count", 1592, author.msgCount);
        assertEquals("Favorites count", 163, author.favoritesCount);
        assertEquals("Following (friends) count", 151, author.followingCount);
        assertEquals("Followers count", 1878136, author.followersCount);
        assertEquals("Created at", connection.parseDate("Tue Nov 30 18:17:25 +0000 2010"), author.getCreatedDate());
        assertEquals("Updated at", 0, author.getUpdatedDate());
        assertEquals("Actor is author", author.oid, activity.getActor().oid);
        assertEquals("Favorited by actor", TriState.UNKNOWN, message.getFavorited());

        ind++;
        message = timeline.get(ind).getMessage();
        assertTrue("Message is loaded", message.getStatus() == DownloadStatus.LOADED);
        assertTrue("Does not have a recipient", message.getRecipient().isEmpty());
        assertTrue("Is a reblog", !message.isReblogged());
        assertTrue("Is a reply", message.getInReplyTo().nonEmpty());
        assertEquals("Reply to the message id", "17176774678", message.getInReplyTo().oid);
        assertEquals("Reply to the message by userOid", DemoData.TWITTER_TEST_ACCOUNT_USER_OID, message.getInReplyTo().getAuthor().oid);
        assertTrue("Reply status is unknown", message.getInReplyTo().getStatus() == DownloadStatus.UNKNOWN);
        assertTrue("Is not Favorited", !message.getFavoritedByMe().toBoolean(true));
        String startsWith = "@t131t";
        assertEquals("Body of this message starts with", startsWith, message.getBody().substring(0, startsWith.length()));

        ind++;
        message = timeline.get(ind).getMessage();
        assertTrue("Does not have a recipient", message.getRecipient().isEmpty());
        assertTrue("Is not a reblog", message.isReblogged());
        assertTrue("Is not a reply", message.getInReplyTo().isEmpty());
        assertEquals("Reblog of the message id", "315088751183409153", message.oid);
        assertEquals("Reblog of the message by userOid", "442756884", message.getAuthor().oid);
        assertTrue("Is not Favorited", !message.getFavoritedByMe().toBoolean(true));
        startsWith = "This AndStatus application";
        assertEquals("Body of reblogged message starts with", startsWith, message.getBody().substring(0, startsWith.length()));
        Date date = TestSuite.utcTime(2013, Calendar.SEPTEMBER, 26, 18, 23, 5);
        assertEquals("Reblogged at Thu Sep 26 18:23:05 +0000 2013 (" + date + ")", date,
                TestSuite.utcTime(message.sentDate));
        date = TestSuite.utcTime(2013, Calendar.MARCH, 22, 13, 13, 7);
        assertEquals("Reblogged message created at Fri Mar 22 13:13:07 +0000 2013 (" + date + ")", date,
                TestSuite.utcTime(message.getUpdatedDate()));

        ind++;
        message = timeline.get(ind).getMessage();
        assertTrue("Does not have a recipient", message.getRecipient().isEmpty());
        assertTrue("Is a reblog", !message.isReblogged());
        assertTrue("Is not a reply", message.getInReplyTo().isEmpty());
        assertTrue("Is not Favorited", !message.getFavoritedByMe().toBoolean(true));
        assertEquals("Author's oid is user oid of this account", connectionData.getAccountUserOid(), message.getAuthor().oid);
        startsWith = "And this is";
        assertEquals("Body of this message starts with", startsWith, message.getBody().substring(0, startsWith.length()));
    }

    @Test
    public void testGetMessageWithAttachment() throws IOException {
        String jso = RawResourceUtils.getString(InstrumentationRegistry.getInstrumentation().getContext(),
                org.andstatus.app.tests.R.raw.twitter_message_with_media);
        httpConnection.setResponse(jso);

        MbMessage message = connection.getMessage("503799441900314624").getMessage();
        assertNotNull("message returned", message);
        assertEquals("has attachment", message.attachments.size(), 1);
        MbAttachment attachment = MbAttachment.fromUrlAndContentType(new URL(
                "https://pbs.twimg.com/media/Bv3a7EsCAAIgigY.jpg"), MyContentType.IMAGE);
        assertEquals("attachment", attachment, message.attachments.get(0));
        attachment.setUrl(new URL("https://pbs.twimg.com/media/Bv4a7EsCAAIgigY.jpg"));
        assertNotSame("attachment", attachment, message.attachments.get(0));
    }

    @Test
    public void testGetMessageWithEscapedHtmlTag() throws IOException {
        String jso = RawResourceUtils.getString(InstrumentationRegistry.getInstrumentation().getContext(),
                org.andstatus.app.tests.R.raw.twitter_message_with_escaped_html_tag);
        httpConnection.setResponse(jso);

        String body = "Update: Streckensperrung zw. Berliner Tor &lt;&gt; Bergedorf. Ersatzverkehr mit Bussen und Taxis " +
                "Störungsdauer bis ca. 10 Uhr. #hvv #sbahnhh";
        MbMessage message = connection.getMessage("834306097003581440").getMessage();
        assertNotNull("message returned", message);
        assertEquals("Body of this message", MyHtml.unescapeHtml(body), message.getBody());
        assertEquals("Body of this message", ",update,streckensperrung,zw,berliner,tor,bergedorf,ersatzverkehr,mit,bussen," +
                "und,taxis,störungsdauer,bis,ca,10,uhr,hvv,#hvv,sbahnhh,#sbahnhh,", message.getBodyToSearch());

        MyAccount ma = DemoData.getMyAccount(connectionData.getAccountName().toString());
        CommandExecutionContext executionContext = new CommandExecutionContext(
                CommandData.newAccountCommand(CommandEnum.GET_STATUS, ma));
        DataUpdater di = new DataUpdater(executionContext);
        long messageId = di.onActivity(message.update(ma.toPartialUser()));
        assertTrue("Message added", messageId != 0);
    }

}
