package uk.gov.hmcts.juror.pnc.check.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JurorCheckRequest {

    @JsonProperty("juror_number")
    @NotBlank
    @Pattern(regexp = "^\\d{9}$")
    private String jurorNumber;

    @JsonProperty("date_of_birth")
    @Pattern(regexp = "^[0-3][0-9]-((0[0-9])|(1[0-2]))-[0-9]{4}$")
    @NotNull
    private String dateOfBirth;

    @JsonProperty("post_code")
    @NotBlank
    @Pattern(regexp = "^[A-Z0-9]{5,8}$")
    private String postCode;

    @Valid
    @NotNull
    private NameDetails name;
}
