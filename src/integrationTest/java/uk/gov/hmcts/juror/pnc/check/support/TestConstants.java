package uk.gov.hmcts.juror.pnc.check.support;

import uk.gov.hmcts.juror.pnc.check.model.NameDetails;

@SuppressWarnings({
    "PMD.TestClassWithoutTestCases"//False positive support class
})
public final class TestConstants {
    public static final String BULK_URL = "/jurors/check/bulk";
    public static final String SINGLE_URL = "/jurors/check";
    public static final String NAMESPACE_PREFIX = "ns2";
    public static final String NAMESPACE_URL = "http://www.npia.police.uk/juror/schema/v1";
    public static final int WIRE_MOCK_PORT = 8085;
    public static final String JOB_KEY = "ABC";
    public static final Long TASK_ID = 1L;
    public static final String VALID_POSTCODE = "AA00BB";
    public static final String VALID_DATE_OF_BIRTH = "01-01-2000";

    public static final NameDetails VALID_NAME = NameDetails.builder()
        .firstName("Ben")
        .middleName("Someone")
        .lastName("Edwards")
        .build();
    public static final int MAX_BULK_CHECKS = 500;
    public static final long MAX_TIME_OUT_SECONDS = 180;

    private TestConstants() {

    }

}
