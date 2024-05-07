package uk.gov.hmcts.juror.pnc.check.rules;

import lombok.Getter;

import java.util.Calendar;

public enum DateUnit {
    //Days are always the maximum value
    YEARS(Calendar.YEAR, 366),
    MONTHS(Calendar.MONTH, 31),
    DAYS(Calendar.DAY_OF_MONTH, 1);

    @Getter
    private final int field;
    private final int days;

    DateUnit(int field, int days) {
        this.field = field;
        this.days = days;
    }

    public long toDays(long disposalDuration) {
        return disposalDuration * this.days;
    }
}
