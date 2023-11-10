package uk.gov.hmcts.juror.pnc.check.rules;

import java.util.Calendar;

public enum DateUnit {
    //If more date units are added SentenceLength Rule will need to be updated to account for new lengths
    YEARS(Calendar.YEAR);

    private final int field;

    DateUnit(int field) {
        this.field = field;
    }

    public int getField() {
        return this.field;
    }
}
