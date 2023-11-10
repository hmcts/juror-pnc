package uk.gov.hmcts.juror.pnc.check.mapper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.juror.pnc.check.config.Constants;
import uk.gov.hmcts.juror.pnc.check.config.PncConfig;
import uk.gov.hmcts.juror.pnc.check.config.RemoteConfig;
import uk.gov.hmcts.juror.pnc.check.model.JurorCheckDetails;
import uk.gov.hmcts.juror.standard.config.SoapConfig;
import uk.police.npia.juror.schema.v1.GetPersonDetails;
import uk.police.npia.juror.schema.v1.PNCAIHeaderType;

import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Mapper(componentModel = "spring", uses = SoapConfig.class)
@Slf4j
public abstract class GetPersonDetailsMapper {
    private final SimpleDateFormat dateTimeFormatter;

    @Getter private final AtomicLong headerSequenceNumber;
    private RemoteConfig remoteConfig;
    private Clock clock;

    protected GetPersonDetailsMapper() {
        headerSequenceNumber = new AtomicLong(1);
        dateTimeFormatter = new SimpleDateFormat("yyyyMMddhh:mm:ss", Constants.LOCALE);
    }

    @Autowired
    public void setClock(Clock clock) {
        this.clock = clock;
    }

    @Autowired
    public void setRemoteConfig(RemoteConfig remoteConfig) {
        this.remoteConfig = remoteConfig;
    }

    @Mapping(target = "header", ignore = true)
    @Mapping(target = "location", constant = Constants.DEFAULT_PERSON_DETAILS_LOCATION)
    @Mapping(target = "jurorReference", source = "jurorNumber")
    @Mapping(target = "searchName", source = "name.combinedName")
    @Mapping(target = "searchDateOfBirth", source = "dateOfBirth", qualifiedByName = "dateFormatter")
    @Mapping(target = "postCode", source = "postCode")
    public abstract GetPersonDetails mapJurorCheckRequestToGetPersonDetails(JurorCheckDetails jurorCheckDetails);


    @AfterMapping
    void mapJurorCheckRequestToGetPersonDetailsAfterMapping(@MappingTarget GetPersonDetails getPersonDetails) {
        Optional.ofNullable(remoteConfig.getPoliceNationalComputerService().getRequestLocation())
            .ifPresent(getPersonDetails::setLocation);
        getPersonDetails.setHeader(mapSoapConfigToHeaderType(remoteConfig.getPoliceNationalComputerService()));
    }

    @Named("dateFormatter")
    String dateFormatter(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("-", "");
    }


    @Mapping(target = "pncTerminal", source = "pncTerminal")
    @Mapping(target = "pncUserid", source = "pncUserId")
    @Mapping(target = "pncAuthorisation", source = "pncAuthorisation")
    @Mapping(target = "originator", source = "originator")
    @Mapping(target = "reasonCode", source = "reasonCode")
    @Mapping(target = "gatewayID", source = "gatewayId")
    @Mapping(target = "localDateTime", qualifiedByName = "mapCurrentDateTimeStr", constant = "")
    @Mapping(target = "sequenceNumber", qualifiedByName = "mapHeaderSequenceNumber", constant = "")
    @Mapping(target = "pncTranCode", source = "pncTranCode")
    @Mapping(target = "pncMode", source = "pncMode")
    abstract PNCAIHeaderType mapSoapConfigToHeaderType(PncConfig pncConfig);


    @Named("mapCurrentDateTimeStr")
    String mapCurrentDateTimeStr(String ignored) {
        return dateTimeFormatter.format(Date.from(clock.instant()));
    }

    @Named("mapHeaderSequenceNumber")
    long mapHeaderSequenceNumber(String ignored) {
        long sequenceNumber = getHeaderSequenceNumber().getAndIncrement();
        if (sequenceNumber >= Long.MAX_VALUE - 100) {
            getHeaderSequenceNumber().set(1);
        }
        return sequenceNumber;
    }
}

