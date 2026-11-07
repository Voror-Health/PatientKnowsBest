package org.endeavourhealth.patientfhirextractor.resources;

import org.endeavourhealth.patientfhirextractor.constants.ResourceConstants;
import org.endeavourhealth.patientfhirextractor.models.AppointmentFull;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.Coding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;


public class Appointment {

    /**
     *
     * @param appointmentResult
     * @return
     * @throws Exception
     */
    public static org.hl7.fhir.dstu3.model.Appointment getAppointmentResource(AppointmentFull appointmentResult) throws Exception {
        org.hl7.fhir.dstu3.model.Appointment appointment = new org.hl7.fhir.dstu3.model.Appointment();
        appointment.setId(String.valueOf(UUID.randomUUID()));
        appointment.getMeta().addProfile("https://fhir.hl7.org.uk/STU3/StructureDefinition/CareConnect-Appointment-1");
        appointment.addIdentifier()
                .setValue(String.valueOf(appointmentResult.getId()))
                .setSystem(ResourceConstants.SYSTEM_ID);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/London"));
        Date startDate = dateFormat.parse(appointmentResult.getStartDate());

        SimpleDateFormat dateFormatZone = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss "+ 'Z');
        String startUTC = dateFormatZone.format(startDate);

        SimpleDateFormat dateFormatUTC = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss "+ 'Z');
        Date  startDt = dateFormatUTC.parse(startUTC);

        appointment.setStart(startDt);
        if(appointmentResult.getActualDuration()!=0) {
            appointment.setEnd(addMinutesToDate(appointmentResult.getActualDuration(),startDt));
        } else {
            appointment.setEnd(null);
            appointment.getEndElement().addExtension().setUrl("http://hl7.org/fhir/StructureDefinition/data-absent-reason")
                    .setValue(new CodeType("unknown"));
        }

        if(appointmentResult.getStatus()!=null)
            appointment.setStatus(new org.hl7.fhir.dstu3.model.Appointment
                    .AppointmentStatusEnumFactory()
                    .fromCode(appointmentResult.getStatus()
                            .toLowerCase().replaceAll(" ", "")));

        Coding coding = new Coding();
        coding.setSystem("http://hl7.org/fhir/STU3/v2/0276/index.html");
        coding.setDisplay(replaceNull(appointmentResult.getType()));
        appointment.getAppointmentType().addCoding(coding);

        return appointment;
    }

    static final long ONE_MINUTE_IN_MILLIS = 60000;
    public static Date addMinutesToDate(int minutes, Date beforeTime) {

        long curTimeInMs = beforeTime.getTime();
        Date afterAddingMins = new Date(curTimeInMs
                + (minutes * ONE_MINUTE_IN_MILLIS));
        return afterAddingMins;
    }

    /**
     *
     * @param input
     * @return
     */
    public static String replaceNull(String input) {
        return input == null ? "" : input;
    }

}
