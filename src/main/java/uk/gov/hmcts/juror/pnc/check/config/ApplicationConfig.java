package uk.gov.hmcts.juror.pnc.check.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.juror.standard.config.JwtSecurityConfig;

@Component
@ConfigurationProperties("uk.gov.hmcts.juror.pnc.check")
@Data
public class ApplicationConfig {

    @NotNull
    @Min(1)
    private Integer maxRetryCount;
    @NotNull
    @Min(0)
    private Integer retryDelayMs;

    @NotNull
    private JwtSecurityConfig security;

    @NotNull
    @Min(1)
    private Integer pncCheckParallelism;
}