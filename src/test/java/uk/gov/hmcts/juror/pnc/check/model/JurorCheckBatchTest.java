package uk.gov.hmcts.juror.pnc.check.model;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.pnc.check.testsupport.TestUtil;

import java.util.Collection;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class JurorCheckBatchTest {


    @Test
    void constructorWithNullValues() {
        JurorCheckBatch jurorCheckBatch =
            assertDoesNotThrow(() -> new JurorCheckBatch(null, null));

        assertEquals(0, jurorCheckBatch.getJurorCheckDetails().size(),
            "Juror Details list should be setup as an empty list of null data is provided");
    }

    @Test
    void constructorWithValidValues() {
        JurorCheckBatch.MetaData metaData = mock(JurorCheckBatch.MetaData.class);
        Collection<JurorCheckDetails> checkDetails =
            Set.of(mock(JurorCheckDetails.class), mock(JurorCheckDetails.class));
        JurorCheckBatch jurorCheckBatch = new JurorCheckBatch(metaData, checkDetails);

        assertThat("Juror check details should match",jurorCheckBatch.getJurorCheckDetails(),
            hasItems(checkDetails.toArray(new JurorCheckDetails[0])));
        TestUtil.isUnmodifiable(jurorCheckBatch.getJurorCheckDetails());
        assertEquals(metaData, jurorCheckBatch.getMetaData(), "Meta data should match");
    }

    @Test
    void positiveIncrementResultsCounter() {
        JurorCheckBatch jurorCheckBatch = new JurorCheckBatch(null, Set.of(mock(JurorCheckDetails.class),
            mock(JurorCheckDetails.class), mock(JurorCheckDetails.class)));

        assertEquals(0, jurorCheckBatch.getTotalResults(), "Starting count should be size of juror checks");
        jurorCheckBatch.incrementResultsCounter();
        assertEquals(1, jurorCheckBatch.getTotalResults(), "After first increment count should be 1");
        jurorCheckBatch.incrementResultsCounter();
        assertEquals(2, jurorCheckBatch.getTotalResults(), "After second increment count should be 2");
        jurorCheckBatch.incrementResultsCounter();
        assertEquals(3, jurorCheckBatch.getTotalResults(), "After third increment count should be 3");
    }

    @Test
    void positiveIncrementResultsCounterOverLimit() {
        JurorCheckBatch jurorCheckBatch = new JurorCheckBatch(null, Set.of(mock(JurorCheckDetails.class)));

        assertEquals(0, jurorCheckBatch.getTotalResults(), "Starting count should be 0");
        jurorCheckBatch.incrementResultsCounter();
        assertEquals(1, jurorCheckBatch.getTotalResults(), "After first increment count should be 1");
        jurorCheckBatch.incrementResultsCounter();
        assertEquals(1, jurorCheckBatch.getTotalResults(), "Count should not exceed total juror checks");
    }
}
