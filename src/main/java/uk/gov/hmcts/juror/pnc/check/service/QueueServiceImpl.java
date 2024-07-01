package uk.gov.hmcts.juror.pnc.check.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.juror.pnc.check.config.ApplicationConfig;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckBatch;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckDetails;
import uk.gov.hmcts.juror.pnc.check.service.contracts.PoliceNationalComputerCheckService;
import uk.gov.hmcts.juror.pnc.check.service.contracts.QueueService;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;


@Service
@Slf4j
@SuppressWarnings("PMD.DoNotUseThreads")//Will be removed if we moved to a JMS queue else we have to use threads
public class QueueServiceImpl implements QueueService {

    @Getter //Required for testing
    private final ForkJoinPool executorService;
    @Getter //Required for testing
    private final ForkJoinPool batchCheckExecutorService;
    private final PoliceNationalComputerCheckService policeNationalComputerCheckService;

    @Autowired
    public QueueServiceImpl(ApplicationConfig applicationConfig,
                            PoliceNationalComputerCheckService policeNationalComputerCheckService) {
        this.batchCheckExecutorService = (ForkJoinPool) Executors.newWorkStealingPool(1);
        this.executorService = (ForkJoinPool) Executors.newWorkStealingPool(applicationConfig.getPncCheckParallelism());
        this.policeNationalComputerCheckService = policeNationalComputerCheckService;
    }

    @Override
    public void queueRequest(JurorCheckDetails request) {
        this.executorService.submit(performPoliceCheckRunnable(request));
    }

    public Runnable performPoliceCheckRunnable(JurorCheckDetails request) {
        return () -> this.policeNationalComputerCheckService.performPoliceCheck(request);
    }

    @Override
    @SuppressWarnings({
        "PMD.AvoidReassigningParameters",
        "java:S2142"
    })
    public void queueRequests(Collection<JurorCheckDetails> requests, JurorCheckBatch.MetaData metaData) {
        log.info("{} checks are being queued", requests.size());
        final JurorCheckBatch jurorCheckBatch = new JurorCheckBatch(
            Optional.ofNullable(metaData)
                .orElseGet(() -> JurorCheckBatch.MetaData.builder()
                    .jobKey(null)
                    .taskId(null)
                    .build()
                ), requests);

        try {
            requests.forEach(this::queueRequest);
            batchCheckExecutorService.execute(() -> awaitJurorBatchCompletionThenReportResults(jurorCheckBatch));
        } catch (Exception e) {
            throw new InternalServerException("Failed to queue JurorChecks", e);
        }
    }

    void awaitJurorBatchCompletionThenReportResults(final JurorCheckBatch jurorCheckBatch) {
        try {
            log.debug("Waiting for all results to process");
            jurorCheckBatch.awaitAllResults();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerException("Thread interrupted", e);
        } catch (Exception e) {
            throw new InternalServerException("Failure when waiting for all results to process", e);
        }
        this.policeNationalComputerCheckService.reportResults(jurorCheckBatch);
    }
}
