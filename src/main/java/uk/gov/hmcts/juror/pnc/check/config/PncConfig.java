package uk.gov.hmcts.juror.pnc.check.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import uk.gov.hmcts.juror.standard.config.SoapConfig;
import uk.police.npia.juror.schema.v1.PncModeType;
import uk.police.npia.juror.schema.v1.PncTranCodeType;

@EqualsAndHashCode(callSuper = true)
@Data
public class PncConfig extends SoapConfig {
    @NotBlank
    private String pncTerminal;
    @NotBlank
    private String pncUserId;
    @NotBlank
    private PncModeType pncMode;
    @NotBlank
    private String pncAuthorisation;
    @NotBlank
    private PncTranCodeType pncTranCode;
    @NotBlank
    private String originator;
    @NotBlank
    private int reasonCode;
    @NotBlank
    private String gatewayId;
}
