package uk.gov.hmcts.juror.pnc.check.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.juror.standard.config.WebConfig;

@ConfigurationProperties("uk.gov.hmcts.juror.pnc.check.remote")
@Data
@Configuration
public class RemoteConfig {

    @NotNull
    @NestedConfigurationProperty
    private WebConfig jobExecutionService;
    @NotNull
    @NestedConfigurationProperty
    private WebConfig jurorService;
    @NotNull
    @NestedConfigurationProperty
    private PncConfig policeNationalComputerService;
}
