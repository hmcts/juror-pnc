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
        assertEquals(getExpectedToString(status, message), result.toString(),
            "ToString should be describe the result");
    }


    @ParameterizedTest
    @EnumSource(PoliceNationalComputerCheckResult.Status.class)
    void onlyStatusConstructorTest(PoliceNationalComputerCheckResult.Status status) {
        PoliceNationalComputerCheckResult result = new PoliceNationalComputerCheckResult(status);

        assertEquals(status, result.getStatus(), "Status should match");
        assertNull(result.getMessage(), "Message should not be present");
        assertEquals(getExpectedToString(status, null), result.toString(),
            "ToString should be describe the result");
    }

    @Test
    void passedStaticGenerator() {
        PoliceNationalComputerCheckResult result = PoliceNationalComputerCheckResult.passed();

        assertEquals(PoliceNationalComputerCheckResult.Status.ELIGIBLE, result.getStatus(), "Status should be  passed");
        assertNull(result.getMessage(), "Message should not be present");
        assertEquals(getExpectedToString(PoliceNationalComputerCheckResult.Status.ELIGIBLE, null), result.toString(),
            "ToString should be describe the result");
    }


    private String getExpectedToString(PoliceNationalComputerCheckResult.Status status, String message) {
        return "Result: " + status.name() + " Message: " + message;
    }
}
