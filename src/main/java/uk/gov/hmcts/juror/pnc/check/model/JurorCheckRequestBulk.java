package uk.gov.hmcts.juror.pnc.check.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.juror.pnc.check.config.Constants;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JurorCheckRequestBulk {

    @JsonProperty("meta_data")
    @Valid
    private JurorCheckBatch.MetaData metaData;

    @Size(min = 1,max = Constants.MAX_BULK_CHECKS)
    @NotNull
    private List<@Valid JurorCheckRequest> checks;

}
