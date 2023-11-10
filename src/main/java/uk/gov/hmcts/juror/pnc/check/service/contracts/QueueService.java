package uk.gov.hmcts.juror.pnc.check.service.contracts;

import uk.gov.hmcts.juror.pnc.check.model.JurorCheckBatch;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckDetails;

import java.util.Collection;

public interface QueueService {
    void queueRequest(JurorCheckDetails request);


    void queueRequests(Collection<JurorCheckDetails> requests, JurorCheckBatch.MetaData metaData);
}
