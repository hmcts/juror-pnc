package uk.gov.hmcts.juror.pnc.check.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;


@Slf4j
public class JurorCheckBatch {

    @Getter
    private final MetaData metaData;

    private final List<JurorCheckDetails> jurorCheckDetails;

    private final CountDownLatch countDownLatch;

    public JurorCheckBatch(MetaData metaData, Collection<JurorCheckDetails> jurorCheckDetails) {
        this.metaData = metaData;
        this.jurorCheckDetails = new ArrayList<>(Optional.ofNullable(jurorCheckDetails).orElse(Collections.emptySet()));
        this.countDownLatch = new CountDownLatch(this.jurorCheckDetails.size());
        this.jurorCheckDetails.forEach(jurorCheckDetails1 -> jurorCheckDetails1.setBatch(this));
    }

    public void incrementResultsCounter() {
        this.countDownLatch.countDown();
        log.trace("{} checks left in this batch", this.countDownLatch.getCount());
    }

    public long getTotalResults() {
        return this.jurorCheckDetails.size() - this.countDownLatch.getCount();
    }

    public void awaitAllResults() throws InterruptedException {
        this.countDownLatch.await();
    }

    public List<JurorCheckDetails> getJurorCheckDetails() {
        return Collections.unmodifiableList(this.jurorCheckDetails);
    }

    @Data
    @Builder
    public static class MetaData {
        @JsonProperty("job_key")
        @NotBlank
        private String jobKey;
        @JsonProperty("task_id")
        @NotNull
        private Long taskId;
    }
}
