package uk.gov.hmcts.juror.pnc.check.rules;


import org.junit.jupiter.api.Test;

import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("PMD.LawOfDemeter")
class DateUnitTest {

    @Test
    void positiveYearShouldReturnCorrectField() {
        assertEquals(Calendar.YEAR, DateUnit.YEARS.getField(), "Field should be aligned to date unit");
    }
}
