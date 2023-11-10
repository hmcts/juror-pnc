package uk.gov.hmcts.juror.pnc.check.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class JurorCheckDetailsTest {
    private JurorCheckDetails jurorCheckDetails;

    @BeforeEach
    void beforeEach() {
        jurorCheckDetails = new JurorCheckDetails();
    }

    @Test
    void positiveSetResultTypical() {
        JurorCheckBatch jurorCheckBatch = mock(JurorCheckBatch.class);
        jurorCheckDetails.setBatch(jurorCheckBatch);

        PoliceNationalComputerCheckResult result = PoliceNationalComputerCheckResult.passed();
        jurorCheckDetails.setResult(result);

        assertEquals(result, jurorCheckDetails.getResult(), "Results should match");
        verify(jurorCheckBatch, times(1)).incrementResultsCounter();
        verifyNoMoreInteractions(jurorCheckBatch);
    }

    @Test
    void positiveSetResultAlreadySet() {
        JurorCheckBatch jurorCheckBatch = mock(JurorCheckBatch.class);
        jurorCheckDetails.setBatch(jurorCheckBatch);

        PoliceNationalComputerCheckResult result = PoliceNationalComputerCheckResult.passed();
        jurorCheckDetails.setResult(result);

        PoliceNationalComputerCheckResult secondResult = new PoliceNationalComputerCheckResult(
            PoliceNationalComputerCheckResult.Status.INELIGIBLE);
        jurorCheckDetails.setResult(secondResult);

        assertEquals(secondResult, jurorCheckDetails.getResult(), "Results should match");
        verify(jurorCheckBatch, times(1)).incrementResultsCounter();//Verify this is not called twice
        verifyNoMoreInteractions(jurorCheckBatch);
    }

    @Test
    void positiveSetResultWithoutBatch() {
        PoliceNationalComputerCheckResult result = PoliceNationalComputerCheckResult.passed();
        jurorCheckDetails.setResult(result);
        assertEquals(result, jurorCheckDetails.getResult(), "Results should match");
    }

    @Test
    void positiveGetAndIncrementRetryCount() {
        String errorMessage = "Should increment value by 1 each call";
        assertEquals(1, jurorCheckDetails.getAndIncrementRetryCount(), errorMessage);
        assertEquals(2, jurorCheckDetails.getAndIncrementRetryCount(), errorMessage);
        assertEquals(3, jurorCheckDetails.getAndIncrementRetryCount(), errorMessage);
    }
}
