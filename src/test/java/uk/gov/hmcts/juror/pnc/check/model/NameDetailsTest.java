package uk.gov.hmcts.juror.pnc.check.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NameDetailsTest {


    @Test
    void positiveGetCombinedNameWithMiddleName() {
        NameDetails nameDetails = NameDetails.builder()
            .firstName("Ben")
            .middleName("middlename")
            .lastName("Edwards").build();
        assertEquals("EDWARDS/BEN/MIDDLENAME", nameDetails.getCombinedName(),
            "Combined name should combine all three provided segments");
    }

    @Test
    void positiveGetCombinedNameWithoutMiddleName() {
        NameDetails nameDetails = NameDetails.builder()
            .firstName("Ben")
            .lastName("Edwards").build();
        assertEquals("EDWARDS/BEN", nameDetails.getCombinedName(),
            "Combined name should not include middle name if it is not provided");
    }

    @Test
    void positiveGetCombinedNameWithMmultipleMiddleName() {
        NameDetails nameDetails = NameDetails.builder()
            .firstName("Ben")
            .middleName("middlename with multiple segments")
            .lastName("Edwards").build();
        assertEquals("EDWARDS/BEN/MIDDLENAME/WITH/MULTIPLE/SEGMENTS", nameDetails.getCombinedName(),
            "Combined name should combine all three provided segments and segment middle name by space");
    }
}
