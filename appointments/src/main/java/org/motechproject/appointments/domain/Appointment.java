package org.motechproject.appointments.domain;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Field;

import javax.jdo.annotations.Unique;
import java.util.UUID;

/**
 * Appointment class to track the appointments of a person identified by externalId
 */
@Entity
public class Appointment {

    /**
     * External id of the person for whom this appointment tracks. Note that the interpretation
     * of the id is left to the implementation/user.
     */
    @Field (required = true)
    private String externalId;

    /**
     * Appointment id generated by the system.
     */
    @Field
    @Unique
    private String apptId;

    /**
     * Due date for the appointment.
     */
    @Field
    private DateTime appointmentDate;

    /**
     * Date of the fulfilled visit.
     */
    @Field
    private DateTime visitedDate;

    /**
     * Status flag to indicate if the appointment was missed.
     */
    @Field
    private AppointmentStatus status;

    /**
     * Flag to activate or deactivate sending reminders
    */
    @Field
    private boolean sendReminders;

    /**
     * The interval between reminders - a period of time.
     */
    @Field
    private Period reminderInterval;

    /**
     * Time to start firing the reminder events.
     */
    @Field
    private DateTime reminderStartTime;

    /**
     * Constructs an empty appointment object with its status set to - {@link org.motechproject.appointments.domain.AppointmentStatus#NONE}.
     */
    public Appointment() { this("", null, null, AppointmentStatus.NONE, false, null, null); }

    /**
     * Constructs an appointment.
     * @param externalId external id of the person for whom this appointment tracks
     * @param appointmentDate due date for the appointment
     * @param visitedDate date of the fulfilled visit
     * @param status status flag to indicate if the appointment was missed
     * @param sendReminders flag to activate or deactivate sending reminders
     * @param reminderInterval the interval between reminders - a period of time
     * @param reminderStartTime time to start firing the reminder events
     */
    public Appointment(String externalId, DateTime appointmentDate, DateTime visitedDate, AppointmentStatus status, boolean sendReminders, Period reminderInterval, DateTime reminderStartTime) {
        this.externalId = externalId;
        this.apptId = UUID.randomUUID().toString();
        this.appointmentDate = appointmentDate;
        this.visitedDate = visitedDate;
        this.status = status;
        this.sendReminders = sendReminders;
        this.reminderInterval = reminderInterval;
        this.reminderStartTime = reminderStartTime;
    }

    // Getters & Setters

    /**
     * @return external id of the person for whom this appointment tracks
     */
    public String getExternalId() { return this.externalId; }

    /**
     * @param externalId external id of the person for whom this appointment tracks
     */
    public void setExternalId(String externalId) { this.externalId = externalId; }

    /**
     * @return appointment id generated by the system
     */
    public String getApptId() { return this.apptId; }

    /**
     * @return due date for the appointment
     */
    public DateTime getAppointmentDate() { return this.appointmentDate; }

    /**
     * @param appointmentDate due date for the appointment
     */
    public void setAppointmentDate(DateTime appointmentDate) { this.appointmentDate = appointmentDate; }

    /**
     * @return date of the fulfilled visit
     */
    public DateTime getVisitedDate() { return this.visitedDate; }

    /**
     * @param visitedDate date of the fulfilled visit
     */
    public void setVisitedDate(DateTime visitedDate) { this.visitedDate = visitedDate; }

    /**
     * @return status flag to indicate if the appointment was missed
     */
    public AppointmentStatus getStatus() { return this.status; }

    /**
     * @param status status flag to indicate if the appointment was missed
     */
    public void setStatus(AppointmentStatus status) { this.status = status; }

    /**
     * @return flag to activate or deactivate sending reminders
     */
    public boolean getSendReminders() { return this.sendReminders; }

    /**
     * @param sendReminders flag to activate or deactivate sending reminders
     */
    public void setSendReminders(boolean sendReminders) { this.sendReminders = sendReminders; }

    /**
     * @return the interval between reminders - a period of time
     */
    public Period getReminderInterval() { return this.reminderInterval; }

    /**
     * @param interval the interval between reminders - a period of time
     */
    public void setReminderInterval(Period interval) { this.reminderInterval = interval; }

    /**
     * @return time to start firing the reminder events
     */
    public DateTime getReminderStartTime() { return this.reminderStartTime; }

    /**
     * @param startTime time to start firing the reminder events
     */
    public void setReminderStartTime(DateTime startTime) { this.reminderStartTime = startTime; }
}
