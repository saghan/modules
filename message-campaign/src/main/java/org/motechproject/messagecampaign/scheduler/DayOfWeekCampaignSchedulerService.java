package org.motechproject.messagecampaign.scheduler;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.motechproject.event.MotechEvent;
import org.motechproject.messagecampaign.EventKeys;
import org.motechproject.messagecampaign.dao.CampaignRecordService;
import org.motechproject.messagecampaign.domain.campaign.CampaignEnrollment;
import org.motechproject.messagecampaign.domain.campaign.DayOfWeekCampaign;
import org.motechproject.messagecampaign.domain.message.DayOfWeek;
import org.motechproject.messagecampaign.domain.message.DayOfWeekCampaignMessage;
import org.motechproject.scheduler.contract.DayOfWeekSchedulableJob;
import org.motechproject.scheduler.service.MotechSchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class DayOfWeekCampaignSchedulerService extends CampaignSchedulerService<DayOfWeekCampaignMessage, DayOfWeekCampaign> {

    @Autowired
    public DayOfWeekCampaignSchedulerService(MotechSchedulerService schedulerService, CampaignRecordService campaignRecordService) {
        super(schedulerService, campaignRecordService);
    }

    @Override
    protected void scheduleMessageJob(CampaignEnrollment enrollment, DayOfWeekCampaign campaign, DayOfWeekCampaignMessage message) {
        MotechEvent motechEvent = new MotechEvent(EventKeys.SEND_MESSAGE, jobParams(message.getMessageKey(), enrollment));
        LocalDate start = enrollment.getReferenceDate();
        LocalDate end = start.plus(campaign.maxDuration());

        List<DayOfWeek> daysOfWeek = message.getDaysOfWeek();
        getSchedulerService().scheduleDayOfWeekJob(new DayOfWeekSchedulableJob(motechEvent, start, end,
                castDaysOfWeekList(daysOfWeek), deliverTimeFor(enrollment, message), true));
    }

    private List<org.motechproject.commons.date.model.DayOfWeek> castDaysOfWeekList(List<DayOfWeek> daysOfWeek) {
        if (daysOfWeek == null) {
            return null;
        }

        List<org.motechproject.commons.date.model.DayOfWeek> result = new ArrayList<>();

        for (DayOfWeek dayOfWeekCampaignMessageDayOfWeek : daysOfWeek) {
            result.add(org.motechproject.commons.date.model.DayOfWeek.getDayOfWeek(dayOfWeekCampaignMessageDayOfWeek.getValue()));
        }
        return result;
    }

    @Override
    public void unscheduleMessageJobs(CampaignEnrollment enrollment) {
        DayOfWeekCampaign campaign = (DayOfWeekCampaign) getCampaignRecordService().findByName(enrollment.getCampaignName()).build();
        for (DayOfWeekCampaignMessage message : campaign.getMessages()) {
            getSchedulerService().safeUnscheduleJob(EventKeys.SEND_MESSAGE, messageJobIdFor(message.getMessageKey(), enrollment.getExternalId(), enrollment.getCampaignName()));
        }
    }

    @Override
    protected DateTime campaignEndDate(DayOfWeekCampaign campaign, CampaignEnrollment enrollment) {
        Period maxDuration = campaign.maxDuration();
        LocalDate endDate = enrollment.getReferenceDate().plus(maxDuration);
        DateTime endDt = endDate.toDateMidnight().toDateTime();
        DateTime startDt = enrollment.getReferenceDate().toDateTimeAtStartOfDay();

        Map<String, List<DateTime>> timings = getCampaignTimings(startDt, endDt, enrollment, campaign);

        return findLastDateTime(timings);
    }
}
