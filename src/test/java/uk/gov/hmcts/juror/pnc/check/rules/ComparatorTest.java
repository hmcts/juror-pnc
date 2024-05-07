package uk.gov.hmcts.juror.pnc.check.rules;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class ComparatorTest {

    public static Stream<Arguments> comparatorArguments() {
        return Stream.of(
            arguments(Comparator.EQUAL_TO, new ComparatorRules(false, false, true)),
            arguments(Comparator.NOT_EQUAL_TO, new ComparatorRules(true, true, false)),
            arguments(Comparator.GREATER_THAN, new ComparatorRules(false, true, false)),
            arguments(Comparator.LESS_THAN, new ComparatorRules(true, false, false)),
            arguments(Comparator.GREATER_THAN_OR_EQUAL_TO, new ComparatorRules(false, true, true)),
            arguments(Comparator.LESS_THAN_OR_EQUAL_TO, new ComparatorRules(true, false, true))
        );
    }

    @ParameterizedTest
    @MethodSource("comparatorArguments")
    void equalToTestValuesAreSame(Comparator comparator, ComparatorRules rules) {
        assertComparator(rules.equalToPass, comparator, 1, 1);
        assertComparator(rules.equalToPass, comparator, 2, 2);
        assertComparator(rules.equalToPass, comparator, 3, 3);
    }

    @ParameterizedTest
    @MethodSource("comparatorArguments")
    void lessThanTestValuesAreLess(Comparator comparator, ComparatorRules rules) {
        assertComparator(rules.justBelowPass, comparator, 0, 1);
        assertComparator(rules.justBelowPass, comparator, 1, 2);
        assertComparator(rules.justBelowPass, comparator, 0, 3);
    }

    @ParameterizedTest
    @MethodSource("comparatorArguments")
    void greaterThanTestValuesAreLess(Comparator comparator, ComparatorRules rules) {
        assertComparator(rules.justAbovePass, comparator, 1, 0);
        assertComparator(rules.justAbovePass, comparator, 2, 1);
        assertComparator(rules.justAbovePass, comparator, 3, 0);
    }

    private void assertComparator(boolean expectedValue, Comparator comparator, long value1, long value2) {
        assertEquals(expectedValue, comparator.compare(value1, value2),
            value1 + " " + comparator.name() + " " + value2 + " should return " + expectedValue);
    }

    private record ComparatorRules(boolean justBelowPass, boolean justAbovePass, boolean equalToPass) {

    }
}
