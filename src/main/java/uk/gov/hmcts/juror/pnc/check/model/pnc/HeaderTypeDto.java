package uk.gov.hmcts.juror.pnc.check.model.pnc;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import uk.police.npia.juror.schema.v1.PncModeType;
import uk.police.npia.juror.schema.v1.PncTranCodeType;

@Getter
@Setter
@Builder
public class HeaderTypeDto {
    protected long sequenceNumber;
    protected String localDateTime;
    protected String pncTerminal;
    protected String pncUserid;
    protected PncModeType pncMode;
    protected String pncAuthorisation;
    protected PncTranCodeType pncTranCode;
    protected String originator;
    protected int reasonCode;
    protected String gatewayID;
}
