package uk.gov.hmcts.juror.pnc.check.model;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PoliceNationalComputerCheckResultTest {

    @ParameterizedTest
    @EnumSource(PoliceNationalComputerCheckResult.Status.class)
    void fullConstructorTest(PoliceNationalComputerCheckResult.Status status) {
        String message = RandomStringUtils.random(25);
        PoliceNationalComputerCheckResult result = new PoliceNationalComputerCheckResult(status, message);

        assertEquals(status, result.getStatus(), "Status should match");
        assertEquals(message, result.getMessage(), "Message should match");
    }


    @ParameterizedTest
    @EnumSource(PoliceNationalComputerCheckResult.Status.class)
    void onlyStatusConstructorTest(PoliceNationalComputerCheckResult.Status status) {
        PoliceNationalComputerCheckResult result = new PoliceNationalComputerCheckResult(status);

        assertEquals(status, result.getStatus(), "Status should match");
        assertNull(result.getMessage(), "Message should not be present");
    }

    @Test
    void passedStaticGenerator() {
        PoliceNationalComputerCheckResult result = PoliceNationalComputerCheckResult.passed();

        assertEquals(PoliceNationalComputerCheckResult.Status.ELIGIBLE, result.getStatus(), "Status should be  passed");
        assertNull(result.getMessage(), "Message should not be present");
    }
}
