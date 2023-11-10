package uk.gov.hmcts.juror.pnc.check.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.gov.hmcts.juror.pnc.check.config.ApplicationConfig;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckBatch;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckDetails;
import uk.gov.hmcts.juror.pnc.check.service.contracts.PoliceNationalComputerCheckService;
import uk.gov.hmcts.juror.pnc.check.testsupport.TestConstants;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings({
    "unchecked",
    "PMD.DoNotUseThreads",//We must use threads for this app this can be removed when we move to a JMS queue
    "PMD.ExcessiveImports",
    "PMD.LawOfDemeter"
})
class QueueServiceImplTest {


    private PoliceNationalComputerCheckService policeNationalComputerCheckService;
    private ExecutorService executorService;
    private MockedStatic<Executors> executorsMockedStatic;

    private QueueServiceImpl queueService;

    @BeforeEach
    void beforeEach() {
        this.executorsMockedStatic = Mockito.mockStatic(Executors.class);
        this.executorService = mock(ForkJoinPool.class);

        this.executorsMockedStatic.when(() -> Executors.newWorkStealingPool(anyInt()))
            .thenReturn(executorService);

        this.policeNationalComputerCheckService = mock(PoliceNationalComputerCheckService.class);
        ApplicationConfig config = new ApplicationConfig();
        config.setPncCheckParallelism(1);
        this.queueService = spy(
            new QueueServiceImpl(config, policeNationalComputerCheckService)
        );

        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return Void.class;
        }).when(executorService).execute(any());

    }

    @AfterEach
    void afterEach() {
        if (this.executorsMockedStatic != null) {
            this.executorsMockedStatic.close();
        }
    }


    @Test
    @DisplayName("Queue single request - Typical")
    void positiveQueueRequest() {
        doNothing().when(this.queueService).queueRequests(any(), isNull());

        JurorCheckDetails jurorCheckDetails = JurorCheckDetails.builder().build();
        this.queueService.queueRequest(jurorCheckDetails);

        verify(this.executorService, times(1))
            .submit(any(Runnable.class));

    }

    @Test
    @DisplayName("public Runnable performPoliceCheckRunnable(JurorCheckDetails request)")
    void performPoliceCheckRunnable() throws Exception {
        JurorCheckDetails details = new JurorCheckDetails();

        Runnable runnable = queueService.performPoliceCheckRunnable(details);

        verify(policeNationalComputerCheckService, never()).performPoliceCheck(details);
        runnable.run();
        verify(policeNationalComputerCheckService, times(1)).performPoliceCheck(details);
    }

    @DisplayName("public void queueRequests("
        + "Collection<JurorCheckDetails> requests, JurorCheckBatch.MetaData metaData)")
    @Nested
    class QueueRequests {
        @Test
        @DisplayName("With meta data")
        void positiveTypicalRequestsWithMetaData() throws InterruptedException {
            List<JurorCheckDetails> requests = List.of(
                JurorCheckDetails.builder().jurorNumber("1").build(),
                JurorCheckDetails.builder().jurorNumber("2").build(),
                JurorCheckDetails.builder().jurorNumber("3").build()
            );
            List<Future<Object>> futures = List.of(
                mock(Future.class),
                mock(Future.class),
                mock(Future.class)
            );
            when(executorService.invokeAll(anyCollection())).thenReturn(futures);

            JurorCheckBatch.MetaData metaData = JurorCheckBatch.MetaData.builder()
                .jobKey(TestConstants.JOB_KEY)
                .taskId(TestConstants.TASK_ID)
                .build();

            doNothing().when(queueService)
                .awaitJurorBatchCompletionThenReportResults(any());

            queueService.queueRequests(requests, metaData);

            for (JurorCheckDetails details : requests) {
                verify(queueService, times(1))
                    .performPoliceCheckRunnable(details);
            }

            verify(executorService, times(requests.size())).submit(any(Runnable.class));

            ArgumentCaptor<JurorCheckBatch> jurorCheckBatchArgumentCaptor =
                ArgumentCaptor.forClass(JurorCheckBatch.class);

            verify(queueService, times(1)).awaitJurorBatchCompletionThenReportResults(
                jurorCheckBatchArgumentCaptor.capture());

            JurorCheckBatch jurorCheckBatch = jurorCheckBatchArgumentCaptor.getValue();

            assertEquals(metaData, jurorCheckBatch.getMetaData(),
                "Meta data must match");
            assertEquals(requests.size(), jurorCheckBatch.getJurorCheckDetails().size(),
                "Request size must match");

            assertThat("Juror check details must match", jurorCheckBatch.getJurorCheckDetails(),
                hasItems(requests.toArray(new JurorCheckDetails[0])));
        }


        @Test
        @DisplayName("Without meta data")
        void positiveTypicalRequestsWithoutMetaData() throws InterruptedException {
            List<JurorCheckDetails> requests = List.of(
                JurorCheckDetails.builder().jurorNumber("1").build(),
                JurorCheckDetails.builder().jurorNumber("2").build(),
                JurorCheckDetails.builder().jurorNumber("3").build()
            );

            doNothing().when(queueService)
                .awaitJurorBatchCompletionThenReportResults(any());

            queueService.queueRequests(requests, null);


            for (JurorCheckDetails details : requests) {
                verify(queueService, times(1))
                    .performPoliceCheckRunnable(details);
            }


            verify(executorService, times(requests.size())).submit(any(Runnable.class));
            verify(executorService, times(1)).execute(any(Runnable.class));

            ArgumentCaptor<JurorCheckBatch> jurorCheckBatchArgumentCaptor =
                ArgumentCaptor.forClass(JurorCheckBatch.class);


            verify(queueService, times(1)).awaitJurorBatchCompletionThenReportResults(
                jurorCheckBatchArgumentCaptor.capture());

            JurorCheckBatch jurorCheckBatch = jurorCheckBatchArgumentCaptor.getValue();

            assertNotNull(jurorCheckBatch.getMetaData(),
                "Meta data should be defaulted as such should not be null");
            assertNull(jurorCheckBatch.getMetaData().getJobKey(),
                "Job key should default to null");
            assertNull(jurorCheckBatch.getMetaData().getTaskId(),
                "Task id should default to null");

            assertEquals(requests.size(), jurorCheckBatch.getJurorCheckDetails().size(),
                "Request size must match");

            assertThat("Juror check details must match", jurorCheckBatch.getJurorCheckDetails(),
                hasItems(requests.toArray(new JurorCheckDetails[0])));
        }

        @Test
        @DisplayName("Single request")
        void positiveSingleRequest() throws InterruptedException {
            List<JurorCheckDetails> requests = List.of(
                JurorCheckDetails.builder().jurorNumber("1").build()
            );
            List<Future<Object>> futures = List.of(
                mock(Future.class)
            );
            when(executorService.invokeAll(anyCollection())).thenReturn(futures);

            JurorCheckBatch.MetaData metaData = JurorCheckBatch.MetaData.builder()
                .jobKey(TestConstants.JOB_KEY)
                .taskId(TestConstants.TASK_ID)
                .build();

            doNothing().when(queueService)
                .awaitJurorBatchCompletionThenReportResults(any());

            queueService.queueRequests(requests, metaData);

            for (JurorCheckDetails details : requests) {
                verify(queueService, times(1))
                    .performPoliceCheckRunnable(details);
            }


            verify(executorService, times(requests.size())).submit(any(Runnable.class));

            ArgumentCaptor<JurorCheckBatch> jurorCheckBatchArgumentCaptor =
                ArgumentCaptor.forClass(JurorCheckBatch.class);


            verify(queueService, times(1)).awaitJurorBatchCompletionThenReportResults(
                jurorCheckBatchArgumentCaptor.capture());

            JurorCheckBatch jurorCheckBatch = jurorCheckBatchArgumentCaptor.getValue();

            assertEquals(metaData, jurorCheckBatch.getMetaData(),
                "Meta data must match");
            assertEquals(requests.size(), jurorCheckBatch.getJurorCheckDetails().size(),
                "Request size must match");

            assertThat("Juror check details must match", jurorCheckBatch.getJurorCheckDetails(),
                hasItems(requests.toArray(new JurorCheckDetails[0])));
        }

        @Test
        @DisplayName("Unexpected exception")
        void negativeUnexpectedException() throws InterruptedException {
            List<JurorCheckDetails> requests = List.of(
                JurorCheckDetails.builder().jurorNumber("1").build()
            );
            RuntimeException cause = new RuntimeException("Some exception");
            when(executorService.submit(any(Runnable.class))).thenThrow(cause);


            InternalServerException exception = assertThrows(InternalServerException.class,
                () -> queueService.queueRequests(requests, null),
                "Should throw exception");
            assertEquals("Failed to queue JurorChecks", exception.getMessage(), "Message must match");
            assertEquals(cause, exception.getCause(), "Cause must match");
        }
    }


    @DisplayName("void awaitJurorBatchCompletionThenReportResults"
        + "(final JurorCheckBatch jurorCheckBatch)")
    @Nested
    class AwaitJurorBatchCompletionThenReportResults {

        @Test
        @DisplayName("Typical")
        void typical() throws InterruptedException {
            JurorCheckBatch jurorCheckBatch = mock(JurorCheckBatch.class);
            queueService.awaitJurorBatchCompletionThenReportResults(jurorCheckBatch);

            verify(jurorCheckBatch, times(1)).awaitAllResults();
            verify(policeNationalComputerCheckService, times(1))
                .reportResults(jurorCheckBatch);
        }

        @Test
        @DisplayName("Unexpected exception")
        void unexpectedException() throws InterruptedException {
            JurorCheckBatch jurorCheckBatch = mock(JurorCheckBatch.class);
            RuntimeException cause = new RuntimeException("My Exception message");
            doThrow(cause).when(jurorCheckBatch).awaitAllResults();

            InternalServerException internalServerException =
                assertThrows(InternalServerException.class, () ->
                        queueService.awaitJurorBatchCompletionThenReportResults(jurorCheckBatch),
                    "Exception must be thrown");

            assertEquals("Failure when waiting for all results to process",
                internalServerException.getMessage(), "Message must match");
            assertEquals(cause, internalServerException.getCause(), "Cause must match");

            verify(jurorCheckBatch, times(1)).awaitAllResults();
            verifyNoInteractions(policeNationalComputerCheckService);
        }

        @Test
        @DisplayName("Interrupted Exception")
        void interruptedException() throws InterruptedException {
            JurorCheckBatch jurorCheckBatch = mock(JurorCheckBatch.class);
            InterruptedException cause = new InterruptedException("My Exception message");
            doThrow(cause).when(jurorCheckBatch).awaitAllResults();

            InternalServerException internalServerException =
                assertThrows(InternalServerException.class, () ->
                        queueService.awaitJurorBatchCompletionThenReportResults(jurorCheckBatch),
                    "Exception must be thrown");

            assertEquals("Thread interrupted", internalServerException.getMessage(),
                "Message must match");
            assertEquals(cause, internalServerException.getCause(), "Cause must match");


            verify(jurorCheckBatch, times(1)).awaitAllResults();
            verifyNoInteractions(policeNationalComputerCheckService);
        }
    }
}

