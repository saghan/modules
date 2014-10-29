package org.motechproject.messagecampaign.it.scheduler;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.motechproject.commons.date.model.Time;
import org.motechproject.commons.date.util.DateTimeSourceUtil;
import org.motechproject.commons.date.util.datetime.DateTimeSource;
import org.motechproject.messagecampaign.contract.CampaignRequest;
import org.quartz.SchedulerException;

import java.util.List;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static org.motechproject.commons.date.util.DateUtil.newDateTime;

public class RepeatCampaignSchedulingBundleIT extends BaseSchedulingIT {

    @Test
    public void shouldScheduleAllMessagesOfCampaignAtMessageStartTime() throws SchedulerException {
        CampaignRequest campaignRequest = new CampaignRequest("entity_1", "WeeklyCampaign", new LocalDate(2020, 7, 10), null);
        getMessageCampaignService().enroll(campaignRequest);
        List<DateTime> fireTimes = getFireTimes("org.motechproject.messagecampaign.fired-campaign-message-MessageJob.WeeklyCampaign.entity_1.message_key_1-repeat");
        assertEquals(asList(
                newDateTime(2020, 7, 10, 10, 30, 0),
                newDateTime(2020, 7, 17, 10, 30, 0),
                newDateTime(2020, 7, 24, 10, 30, 0)),
                fireTimes);

        fireTimes = getFireTimes("org.motechproject.messagecampaign.fired-campaign-message-MessageJob.WeeklyCampaign.entity_1.message_key_2-repeat");
        assertEquals(asList(
                newDateTime(2020, 7, 10, 8, 30, 0),
                newDateTime(2020, 7, 20, 8, 30, 0),
                newDateTime(2020, 7, 30, 8, 30, 0)),
                fireTimes);

        List<DateTime> endOfCampaignFireTimes = getFireTimes("org.motechproject.messagecampaign.campaign-completed-EndOfCampaignJob.WeeklyCampaign.entity_1-runonce");
        assertEquals(asList(newDateTime(2020, 7, 30, 8, 30, 0)), endOfCampaignFireTimes);
    }

    @Test
    public void shouldScheduleWeeklyMessagesAtUserSpecifiedTime() throws SchedulerException {
        CampaignRequest campaignRequest = new CampaignRequest("entity_1", "WeeklyCampaign", new LocalDate(2020, 7, 10), new Time(15, 20));
        getMessageCampaignService().enroll(campaignRequest);
        List<DateTime> fireTimes = getFireTimes("org.motechproject.messagecampaign.fired-campaign-message-MessageJob.WeeklyCampaign.entity_1.message_key_1-repeat");
        assertEquals(asList(
                newDateTime(2020, 7, 10, 15, 20, 0),
                newDateTime(2020, 7, 17, 15, 20, 0),
                newDateTime(2020, 7, 24, 15, 20, 0)),
                fireTimes);

        List<DateTime> endOfCampaignFireTimes = getFireTimes("org.motechproject.messagecampaign.campaign-completed-EndOfCampaignJob.WeeklyCampaign.entity_1-runonce");
        assertEquals(asList(newDateTime(2020, 7, 30, 15, 20, 0)), endOfCampaignFireTimes);
    }

    @Test
    public void shouldNotScheduleMessagesInPastForDelayedEnrollment() throws SchedulerException {
        try {
            DateTimeSourceUtil.setSourceInstance(new DateTimeSource() {
                private DateTime dateTime = new DateTime(2020, 7, 15, 0, 0);

                @Override
                public DateTimeZone timeZone() {
                    return dateTime.getZone();
                }

                @Override
                public DateTime now() {
                    return dateTime;
                }

                @Override
                public LocalDate today() {
                    return dateTime.toLocalDate();
                }
            });

            CampaignRequest campaignRequest = new CampaignRequest("entity_1", "WeeklyCampaign", new LocalDate(2020, 7, 10), null);
            getMessageCampaignService().enroll(campaignRequest);
            List<DateTime> fireTimes = getFireTimes("org.motechproject.messagecampaign.fired-campaign-message-MessageJob.WeeklyCampaign.entity_1.message_key_1-repeat");
            assertEquals(asList(
                    newDateTime(2020, 7, 17, 10, 30, 0),
                    newDateTime(2020, 7, 24, 10, 30, 0)),
                    fireTimes);


            List<DateTime> endOfCampaignFireTimes = getFireTimes("org.motechproject.messagecampaign.campaign-completed-EndOfCampaignJob.WeeklyCampaign.entity_1-runonce");
            assertEquals(asList(newDateTime(2020, 7, 30, 8, 30, 0)), endOfCampaignFireTimes);
        } finally {
            DateTimeSourceUtil.setSourceInstance(DATE_TIME_SOURCE);
        }
    }

    @Test
    public void shouldScheduleMessagesEvery12Hours() throws SchedulerException {
        CampaignRequest campaignRequest = new CampaignRequest("entity_1", "HourlyCampaign", new LocalDate(2020, 7, 10), new Time(4, 30));
        getMessageCampaignService().enroll(campaignRequest);
        List<DateTime> fireTimes = getFireTimes("org.motechproject.messagecampaign.fired-campaign-message-MessageJob.HourlyCampaign.entity_1.message_key_1-repeat");
        assertEquals(asList(
                newDateTime(2020, 7, 10, 4, 30, 0),
                newDateTime(2020, 7, 10, 16, 30, 0),
                newDateTime(2020, 7, 11, 4, 30, 0),
                newDateTime(2020, 7, 11, 16, 30, 0)),
                fireTimes);


        List<DateTime> endOfCampaignFireTimes = getFireTimes("org.motechproject.messagecampaign.campaign-completed-EndOfCampaignJob.HourlyCampaign.entity_1-runonce");
        assertEquals(asList(newDateTime(2020, 7, 11, 16, 30, 0)), endOfCampaignFireTimes);
    }
}
