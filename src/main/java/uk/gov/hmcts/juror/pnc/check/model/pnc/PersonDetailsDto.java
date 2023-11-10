package uk.gov.hmcts.juror.pnc.check.model.pnc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonDetailsDto {
    protected HeaderTypeDto header;
    protected String errorReason;
    protected BigDecimal numberMatchesFound;
    protected String jurorReference;
    protected List<PersonDto> personDtos;
}
