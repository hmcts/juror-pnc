package uk.gov.hmcts.juror.pnc.check.model.pnc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public class DisposalDto {
    protected String disposalCode;
    protected String disposalEffectiveDate;
    protected String fineAmount;
    protected String fineUnits;
    protected String qualAmount;
    protected String qualLiteral;
    protected String qualPeriod;
    protected String sentenceAmount;
    protected String sentencePeriod;

    public DisposalDto disposalEffectiveDate(LocalDate effectiveDate) {
        this.disposalEffectiveDate = effectiveDate.format(
            DateTimeFormatter.ofPattern("dd/MM/yy")
        );
        return this;
    }
}
