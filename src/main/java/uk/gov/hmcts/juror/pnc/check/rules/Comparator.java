package uk.gov.hmcts.juror.pnc.check.rules;

import java.util.Objects;
import java.util.function.BiPredicate;

public enum Comparator {
    EQUAL_TO(Objects::equals),
    NOT_EQUAL_TO((aLong, aLong2) -> !aLong.equals(aLong2)),
    GREATER_THAN((number, number2) -> number > number2),
    LESS_THAN((number, number2) -> number < number2),
    GREATER_THAN_OR_EQUAL_TO((number, number2) -> number >= number2),
    LESS_THAN_OR_EQUAL_TO((number, number2) -> number <= number2);


    private final BiPredicate<Long, Long> compareFunction;

    Comparator(BiPredicate<Long, Long> compareFunction) {
        this.compareFunction = compareFunction;
    }

    public boolean compare(long firstNumber, long secondNumber) {
        return this.compareFunction.test(firstNumber, secondNumber);
    }

}
