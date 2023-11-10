package uk.gov.hmcts.juror.pnc.check.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckDetails;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckRequest;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class JurorRequestMapper {

    @Mapping(target = "result", ignore = true)
    @Mapping(target = "batch", ignore = true)
    @Mapping(target = "retryCount", constant = "0")
    public abstract JurorCheckDetails mapJurorCheckRequestToJurorCheckDetails(JurorCheckRequest jurorCheckRequest);

    public abstract List<JurorCheckDetails> mapJurorCheckRequestToJurorCheckDetails(
        List<JurorCheckRequest> jurorCheckRequest);
}
