package uk.gov.hmcts.juror.pnc.check.service;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.juror.pnc.check.client.contracts.JobExecutionServiceClient;
import uk.gov.hmcts.juror.pnc.check.client.contracts.JurorServiceClient;
import uk.gov.hmcts.juror.pnc.check.client.contracts.PoliceNationalComputerClient;
import uk.gov.hmcts.juror.pnc.check.config.Constants;
import uk.gov.hmcts.juror.pnc.check.mapper.GetPersonDetailsMapper;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckBatch;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckDetails;
import uk.gov.hmcts.juror.pnc.check.model.PoliceNationalComputerCheckResult;
import uk.gov.hmcts.juror.pnc.check.model.pnc.PersonDetailsDto;
import uk.gov.hmcts.juror.pnc.check.model.pnc.PersonDto;
import uk.gov.hmcts.juror.pnc.check.rules.support.RuleSets;
import uk.gov.hmcts.juror.pnc.check.rules.support.RuleValidationResult;
import uk.gov.hmcts.juror.pnc.check.service.contracts.PoliceNationalComputerCheckService;
import uk.gov.hmcts.juror.pnc.check.service.contracts.RuleService;
import uk.gov.hmcts.juror.standard.service.exceptions.RemoteGatewayException;
import uk.police.npia.juror.schema.v1.GetPersonDetails;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PoliceNationalComputerCheckServiceImpl implements PoliceNationalComputerCheckService {

    private final PoliceNationalComputerClient policeNationalComputerClient;
    private final JobExecutionServiceClient jobExecutionServiceClient;
    private final GetPersonDetailsMapper getPersonDetailsMapper;
    private final RuleService ruleService;
    private final JurorServiceClient jurorServiceClient;

    @Autowired
    public PoliceNationalComputerCheckServiceImpl(
        PoliceNationalComputerClient policeNationalComputerClient,
        JobExecutionServiceClient jobExecutionServiceClient,
        JurorServiceClient jurorServiceClient,
        GetPersonDetailsMapper getPersonDetailsMapper,
        RuleService ruleService
    ) {
        this.policeNationalComputerClient = policeNationalComputerClient;
        this.jobExecutionServiceClient = jobExecutionServiceClient;
        this.getPersonDetailsMapper = getPersonDetailsMapper;
        this.jurorServiceClient = jurorServiceClient;
        this.ruleService = ruleService;
    }

    @Override
    public PoliceNationalComputerCheckResult performPoliceCheck(
        final JurorCheckDetails jurorCheckDetails) {
        log.info("Performing police check for Juror: " + jurorCheckDetails.getJurorNumber());
        PoliceNationalComputerCheckResult result = getPoliceCheckResult(jurorCheckDetails);
        savePoliceCheckResult(jurorCheckDetails, result);
        log.info("Juror check complete for juror: " + jurorCheckDetails.getJurorNumber());
        return result;
    }

    public PoliceNationalComputerCheckResult getPoliceCheckResult(final JurorCheckDetails jurorCheckDetails) {
        try {
            GetPersonDetails getPersonDetails =
                getPersonDetailsMapper
                    .mapJurorCheckRequestToGetPersonDetails(jurorCheckDetails);

            if (getPersonDetails.getSearchName().matches(".*\\d.*")) {
                log.warn("Juror: " + jurorCheckDetails.getJurorNumber() + " not processed as name contains numerics");
                return new PoliceNationalComputerCheckResult(
                    PoliceNationalComputerCheckResult.Status.ERROR_RETRY_NAME_HAS_NUMERICS);
            }
            PersonDetailsDto response = policeNationalComputerClient.call(getPersonDetails);
            return validatePoliceCheckResponse(jurorCheckDetails.getJurorNumber(), response);
        } catch (RemoteGatewayException exception) {
            log.error("Error getting PNC check result, ", exception);
            return new PoliceNationalComputerCheckResult(
                PoliceNationalComputerCheckResult.Status.ERROR_RETRY_CONNECTION_ERROR);
        } catch (Exception e) {
            log.error("Unexpected exception when performing police check", e);
            return new PoliceNationalComputerCheckResult(
                PoliceNationalComputerCheckResult.Status.ERROR_RETRY_UNEXPECTED_EXCEPTION);
        }
    }

    private void savePoliceCheckResult(JurorCheckDetails jurorCheckDetails, PoliceNationalComputerCheckResult result) {
        jurorCheckDetails.setResult(result);
        log.trace("Attempting to save result: " + result + " for juror " + jurorCheckDetails.getJurorNumber());
        if (result != null) {
            try {
                this.jurorServiceClient.call(jurorCheckDetails.getJurorNumber(),
                    new JurorServiceClient.Payload(result.getStatus()));
                log.info("Juror check result saved for juror: " + jurorCheckDetails.getJurorNumber());
            } catch (RemoteGatewayException exception) {
                result.setStatus(PoliceNationalComputerCheckResult.Status.ERROR_RETRY_FAILED_TO_UPDATE_BACKEND);
                log.error("Failed to save juror result on backend: " + jurorCheckDetails.getJurorNumber(), exception);
            }
        }
    }

    @Override
    @SuppressWarnings({
        "PMD.CyclomaticComplexity",
        "PMD.LawOfDemeter"
    })
    public void reportResults(JurorCheckBatch jurorCheckBatch) {
        JurorCheckBatch.MetaData metaData = jurorCheckBatch.getMetaData();
        if (metaData == null || metaData.getJobKey() == null || metaData.getTaskId() == null) {
            return;//No need to continue if jobKey/taskId are not provided as these are required for reporting back
        }

        JobExecutionServiceClient.StatusUpdatePayload.Status status =
            JobExecutionServiceClient.StatusUpdatePayload.Status.SUCCESS;
        Map<String, String> metaDataMap = new ConcurrentHashMap<>();

        long totalNullResults = 0;
        for (JurorCheckDetails jurorCheckDetails : jurorCheckBatch.getJurorCheckDetails()) {
            PoliceNationalComputerCheckResult result = jurorCheckDetails.getResult();

            if (result == null) {
                totalNullResults++;
                status = JobExecutionServiceClient.StatusUpdatePayload.Status.FAILED_UNEXPECTED_EXCEPTION;
                metaDataMap.put(jurorCheckDetails.getJurorNumber(), "Failed to find result");
            } else if (result.getStatus()
                == PoliceNationalComputerCheckResult.Status.ERROR_RETRY_UNEXPECTED_EXCEPTION) {
                status = JobExecutionServiceClient.StatusUpdatePayload.Status.FAILED_UNEXPECTED_EXCEPTION;
                metaDataMap.put(jurorCheckDetails.getJurorNumber(), "Unexpected exception");
            } else if (result.getStatus() != PoliceNationalComputerCheckResult.Status.ELIGIBLE
                && result.getStatus() != PoliceNationalComputerCheckResult.Status.INELIGIBLE) {
                status = JobExecutionServiceClient.StatusUpdatePayload.Status.INDETERMINATE;
            }
        }

        Map<PoliceNationalComputerCheckResult.Status, Long> countMap = jurorCheckBatch.getJurorCheckDetails().stream()
            .filter(details -> details.getResult() != null)
            .collect(Collectors.groupingBy(o -> o.getResult().getStatus(), Collectors.counting()));
        metaDataMap.put("TOTAL_CHECKS_IN_BATCH", String.valueOf(jurorCheckBatch.getJurorCheckDetails().size()));
        metaDataMap.put("TOTAL_NULL_RESULTS", String.valueOf(totalNullResults));
        for (PoliceNationalComputerCheckResult.Status resultStatus :
            PoliceNationalComputerCheckResult.Status.values()) {
            long count = countMap.getOrDefault(resultStatus, 0L);
            metaDataMap.put("TOTAL_WITH_STATUS_" + resultStatus.name(), String.valueOf(count));
        }

        this.jobExecutionServiceClient.call(
            new JobExecutionServiceClient.StatusUpdatePayload(status, "Juror check completed.", metaDataMap),
            metaData.getJobKey(), metaData.getTaskId());
    }

    PoliceNationalComputerCheckResult validatePoliceCheckResponse(String jurorNumber,
                                                                  @NotNull PersonDetailsDto personDetails) {
        log.info("Validating Juror: " + jurorNumber);
        Optional<PoliceNationalComputerCheckResult> resultOptional = checkErrorReason(jurorNumber, personDetails);
        if (resultOptional.isPresent()) {
            log.info("Error Reason checks failed for juror: " + jurorNumber);
            return resultOptional.get();
        }
        log.info("Error Reason checks passed for juror: " + jurorNumber);
        log.info("Validating person details for juror: " + jurorNumber + " - "
            + personDetails.getPersonDtos().size() + " persons entries found");
        for (PersonDto person : personDetails.getPersonDtos()) {
            RuleValidationResult ruleValidationResult = this.ruleService.fireRules(RuleSets.PERSON_RULE_SET, person);

            if (ruleValidationResult.isPassed() && person.getDisposals() != null) {
                ruleValidationResult = this.ruleService.fireRules(RuleSets.DISPOSAL_RULE_SET,
                    person.getDisposals().stream()
                        .filter(disposal -> Strings.isNotEmpty(disposal.getDisposalCode())).collect(
                            Collectors.toUnmodifiableSet()));
            }

            if (!ruleValidationResult.isPassed()) {
                log.info("Validation rules failed for juror: " + jurorNumber);
                return new PoliceNationalComputerCheckResult(PoliceNationalComputerCheckResult.Status.INELIGIBLE,
                    ruleValidationResult.getMessage());
            }
        }
        return PoliceNationalComputerCheckResult.passed();
    }

    @SuppressWarnings("PMD.LawOfDemeter")
    Optional<PoliceNationalComputerCheckResult> checkErrorReason(
        final String jurorNumber,
        final PersonDetailsDto personDetailsDto) {

        final String errorReason = personDetailsDto.getErrorReason();
        log.info("ResponseCode: " + errorReason);
        if (errorReason == null) {
            log.info("No data returned for juror " + jurorNumber);
            return Optional.of(
                new PoliceNationalComputerCheckResult(
                    PoliceNationalComputerCheckResult.Status.ERROR_RETRY_NO_ERROR_REASON,
                    "No data returned for juror. Unable to check"));
        }

        if (!errorReason.isBlank()) {
            if (errorReason.startsWith(Constants.NO_RECORDS_FOUND_ERROR_CODE)) {
                log.debug("No PNC data for juror " + jurorNumber + ", response code " + errorReason);
                return Optional.empty();// Pass if onBail check passes
            } else {
                log.error("Error checking juror " + jurorNumber);
                log.error("Returned error code: " + errorReason + ", for juror " + jurorNumber);
                return Optional.of(
                    new PoliceNationalComputerCheckResult(
                        PoliceNationalComputerCheckResult.Status.ERROR_RETRY_OTHER_ERROR_CODE,
                        "Returned error code: " + errorReason + " for juror"));
            }
        }
        return Optional.empty();
    }
}


