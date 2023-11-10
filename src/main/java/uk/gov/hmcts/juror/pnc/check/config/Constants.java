package uk.gov.hmcts.juror.pnc.check.config;

import java.util.Locale;


public final class Constants {

    public static final String DEFAULT_PERSON_DETAILS_LOCATION = "MOJ";
    public static final String NO_RECORDS_FOUND_ERROR_CODE = "JUR001 - No Records Found:";
    public static final Locale LOCALE = Locale.UK;
    public static final int MAX_BULK_CHECKS = 500;

    private Constants() {
    }
}
