package uk.gov.hmcts.juror.pnc.check.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.juror.pnc.check.mapper.JurorRequestMapper;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckDetails;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckRequest;
import uk.gov.hmcts.juror.pnc.check.service.contracts.QueueService;
import uk.gov.hmcts.juror.pnc.check.testsupport.controller.ControllerWithPayloadTest;
import uk.gov.hmcts.juror.pnc.check.testsupport.controller.InvalidPayloadArgument;
import uk.gov.hmcts.juror.pnc.check.testsupport.controller.SuccessRequestArgument;
import uk.gov.hmcts.juror.standard.api.ExceptionHandling;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.juror.pnc.check.controller.JurorControllerTest.CheckJurorBulk.POST_CHECK_JUROR_BULK;
import static uk.gov.hmcts.juror.pnc.check.controller.JurorControllerTest.CheckJurorSingle.POST_CHECK_JUROR_SINGLE;
import static uk.gov.hmcts.juror.pnc.check.testsupport.TestUtil.addJsonPath;
import static uk.gov.hmcts.juror.pnc.check.testsupport.TestUtil.deleteJsonPath;
import static uk.gov.hmcts.juror.pnc.check.testsupport.TestUtil.readResource;
import static uk.gov.hmcts.juror.pnc.check.testsupport.TestUtil.replaceJsonPath;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = JurorController.class,
    excludeAutoConfiguration = {SecurityAutoConfiguration.class})
@ContextConfiguration(
    classes = {
        JurorController.class,
        ExceptionHandling.class
    }
)
@DisplayName("Controller: /jurors/check")
@SuppressWarnings("PMD.ExcessiveImports")
public class JurorControllerTest {

    @MockBean
    private QueueService queueService;

    @MockBean
    private JurorRequestMapper jurorRequestMapper;
    private static final String CONTROLLER_BASEURL = "/jurors/check";
    private static final String RESOURCE_PREFIX = "/testData/jurorController";

    @Nested
    @DisplayName("POST " + POST_CHECK_JUROR_BULK)
    class CheckJurorBulk extends ControllerWithPayloadTest {
        static final String POST_CHECK_JUROR_BULK = CONTROLLER_BASEURL + "/bulk";

        CheckJurorBulk() {
            super(HttpMethod.POST, POST_CHECK_JUROR_BULK, HttpStatus.OK);
        }

        @Override
        protected String getTypicalPayload() {
            return readResource("checkJurorBulkRequest.json", RESOURCE_PREFIX);
        }


        private SuccessRequestArgument createSuccessRequestArgument(String name,
                                                                    String payload) {

            List<JurorCheckDetails> jurorCheckDetailsList = List.of();

            return new SuccessRequestArgument(name,
                builder -> {
                    when(jurorRequestMapper.mapJurorCheckRequestToJurorCheckDetails(anyList())).thenReturn(
                        jurorCheckDetailsList);
                },
                resultActions -> {
                    verify(jurorRequestMapper, times(1)).mapJurorCheckRequestToJurorCheckDetails(anyList());
                    verify(queueService, times(1)).queueRequests(eq(jurorCheckDetailsList), any());
                },
                payload, null);
        }

        @Override
        protected Stream<SuccessRequestArgument> getSuccessRequestArgument() {
            String payload = getTypicalPayload();
            return Stream.of(
                createSuccessRequestArgument("Typical", payload),
                createSuccessRequestArgument("No Meta Data", deleteJsonPath(payload, "$.meta_data")),
                createSuccessRequestArgument("No Middle name", deleteJsonPath(payload, "$.checks[0].name.middle_name"))
            );
        }

        @Override
        protected Stream<InvalidPayloadArgument> getInvalidPayloadArgumentSource() {
            String payload = getTypicalPayload();
            Consumer<ResultActions> postActions =
                resultActions -> {
                    verifyNoInteractions(queueService);
                    verifyNoInteractions(jurorRequestMapper);
                };

            return Stream.of(
                new InvalidPayloadArgument(deleteJsonPath(payload, "$.checks"),
                    "checks: must not be null")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(
                    addJsonPath(deleteJsonPath(payload, "$.checks"), "$", "checks", new ArrayList<>()),
                    "checks: size must be between 1 and 500")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(deleteJsonPath(payload, "$.meta_data.job_key"),
                    "metaData.jobKey: must not be blank")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(deleteJsonPath(payload, "$.meta_data.task_id"),
                    "metaData.taskId: must not be null")
                    .setPostActions(postActions),


                new InvalidPayloadArgument(deleteJsonPath(payload, "$.checks[0].juror_number"),
                    "checks[0].jurorNumber: must not be blank")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(replaceJsonPath(payload, "$.checks[0].juror_number", "invalid"),
                    "checks[0].jurorNumber: must match \"^\\\\d{9}$\"")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(deleteJsonPath(payload, "$.checks[0].date_of_birth"),
                    "checks[0].dateOfBirth: must not be null")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(replaceJsonPath(payload, "$.checks[0].date_of_birth", "INVALID"),
                    "checks[0].dateOfBirth: must match \"^[0-3][0-9]-((0[0-9])|(1[0-2]))-[0-9]{4}$\"")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(deleteJsonPath(payload, "$.checks[0].post_code"),
                    "checks[0].postCode: must not be blank")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(replaceJsonPath(payload, "$.checks[0].post_code", "INV"),
                    "checks[0].postCode: must match \"^[A-Z0-9]{5,8}$\"")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(deleteJsonPath(payload, "$.checks[0].name"),
                    "checks[0].name: must not be null")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(deleteJsonPath(payload, "$.checks[0].name.first_name"),
                    "checks[0].name.firstName: must not be blank")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(deleteJsonPath(payload, "$.checks[0].name.last_name"),
                    "checks[0].name.lastName: must not be blank")
                    .setPostActions(postActions)
            );
        }
    }

    @Nested
    @DisplayName("POST " + POST_CHECK_JUROR_SINGLE)
    class CheckJurorSingle extends ControllerWithPayloadTest {
        static final String POST_CHECK_JUROR_SINGLE = CONTROLLER_BASEURL;


        public CheckJurorSingle() {
            super(HttpMethod.POST, POST_CHECK_JUROR_SINGLE, HttpStatus.OK);
        }

        @Override
        protected String getTypicalPayload() {
            return readResource("checkJurorSingleRequest.json", RESOURCE_PREFIX);
        }


        private SuccessRequestArgument createSuccessRequestArgument(String name,
                                                                    String payload) {

            JurorCheckDetails jurorCheckDetails = mock(JurorCheckDetails.class);

            return new SuccessRequestArgument(name,
                builder -> {
                    when(jurorRequestMapper.mapJurorCheckRequestToJurorCheckDetails(
                        any(JurorCheckRequest.class))).thenReturn(jurorCheckDetails);
                },
                resultActions -> {
                    verify(jurorRequestMapper, times(1)).mapJurorCheckRequestToJurorCheckDetails(
                        any(JurorCheckRequest.class));
                    verify(queueService, times(1)).queueRequest(jurorCheckDetails);
                },
                payload, null);
        }

        @Override
        protected Stream<SuccessRequestArgument> getSuccessRequestArgument() {
            String payload = getTypicalPayload();
            return Stream.of(
                createSuccessRequestArgument("Typical", payload),
                createSuccessRequestArgument("No Middle name", deleteJsonPath(payload, "$.name.middle_name"))
            );
        }

        @Override
        protected Stream<InvalidPayloadArgument> getInvalidPayloadArgumentSource() {
            String payload = getTypicalPayload();
            Consumer<ResultActions> postActions =
                resultActions -> {
                    verifyNoInteractions(queueService);
                    verifyNoInteractions(jurorRequestMapper);
                };

            return Stream.of(
                new InvalidPayloadArgument(deleteJsonPath(payload, "$.juror_number"),
                    "jurorNumber: must not be blank")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(replaceJsonPath(payload, "$.juror_number", "invalid"),
                    "jurorNumber: must match \"^\\\\d{9}$\"")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(deleteJsonPath(payload, "$.date_of_birth"),
                    "dateOfBirth: must not be null")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(replaceJsonPath(payload, "$.date_of_birth", "INVALID"),
                    "dateOfBirth: must match \"^[0-3][0-9]-((0[0-9])|(1[0-2]))-[0-9]{4}$\"")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(deleteJsonPath(payload, "$.post_code"),
                    "postCode: must not be blank")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(replaceJsonPath(payload, "$.post_code", "INV"),
                    "postCode: must match \"^[A-Z0-9]{5,8}$\"")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(deleteJsonPath(payload, "$.name"),
                    "name: must not be null")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(deleteJsonPath(payload, "$.name.first_name"),
                    "name.firstName: must not be blank")
                    .setPostActions(postActions),
                new InvalidPayloadArgument(deleteJsonPath(payload, "$.name.last_name"),
                    "name.lastName: must not be blank")
                    .setPostActions(postActions)
            );
        }
    }
}
