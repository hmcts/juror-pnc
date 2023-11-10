package uk.gov.hmcts.juror.pnc.check.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.juror.pnc.check.config.PermissionConstants;
import uk.gov.hmcts.juror.pnc.check.mapper.JurorRequestMapper;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckDetails;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckRequest;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckRequestBulk;
import uk.gov.hmcts.juror.pnc.check.service.contracts.QueueService;

import java.util.List;

@RestController
@Tag(name = "PNC Check")
@Slf4j
@RequestMapping(value = "/jurors/check", consumes = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class JurorController {
    private final QueueService queueService;
    private final JurorRequestMapper jurorRequestMapper;

    public JurorController(QueueService queueService,
                           JurorRequestMapper jurorRequestMapper) {
        this.queueService = queueService;
        this.jurorRequestMapper = jurorRequestMapper;
    }


    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('" + PermissionConstants.CHECK_BULK + "')")
    public ResponseEntity<Void> processJurorPncChecksBulk(
        @Valid @RequestBody JurorCheckRequestBulk jurorCheckRequests
    ) {
        List<JurorCheckDetails> jurorCheckDetails =
            this.jurorRequestMapper.mapJurorCheckRequestToJurorCheckDetails(jurorCheckRequests.getChecks());
        this.queueService.queueRequests(jurorCheckDetails, jurorCheckRequests.getMetaData());
        return ResponseEntity.ok().build();
    }

    @PostMapping()
    @PreAuthorize("hasAuthority('" + PermissionConstants.CHECK + "')")
    public ResponseEntity<Void> processJurorPncCheckSingle(
        @Valid @RequestBody JurorCheckRequest jurorCheckRequest) {
        JurorCheckDetails jurorCheckDetails =
            this.jurorRequestMapper.mapJurorCheckRequestToJurorCheckDetails(jurorCheckRequest);
        this.queueService.queueRequest(jurorCheckDetails);
        return ResponseEntity.ok().build();
    }
}
