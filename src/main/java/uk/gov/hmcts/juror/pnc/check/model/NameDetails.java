package uk.gov.hmcts.juror.pnc.check.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import org.apache.logging.log4j.util.Strings;
import uk.gov.hmcts.juror.pnc.check.config.Constants;
import uk.gov.hmcts.juror.pnc.check.utils.Utilities;


@Data
@Builder
public class NameDetails {
    @JsonProperty("first_name")
    @NotBlank
    private String firstName;

    @JsonProperty("middle_name")
    private String middleName;

    @JsonProperty("last_name")
    @NotBlank
    private String lastName;

    @JsonIgnore
    public String getCombinedName() {
        StringBuilder name = new StringBuilder(Utilities.stripBadChars(this.lastName))
            .append('/')
            .append(Utilities.stripBadChars(this.firstName));
        if (this.middleName != null) {
            String[] segments = this.middleName.split(" ");
            for (String segment : segments) {
                final String processedMiddleName = Utilities.stripBadChars(segment);
                if (Strings.isNotEmpty(processedMiddleName)) {
                    name.append('/').append(processedMiddleName);
                }
            }
        }
        return name.toString().toUpperCase(Constants.LOCALE);
    }
}

