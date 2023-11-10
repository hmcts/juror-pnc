package uk.gov.hmcts.juror.pnc.check.mapper;

import jakarta.xml.bind.JAXBElement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.model.pnc.HeaderTypeDto;
import uk.gov.hmcts.juror.pnc.check.model.pnc.PersonDetailsDto;
import uk.gov.hmcts.juror.pnc.check.model.pnc.PersonDto;
import uk.police.npia.juror.schema.v1.Disposal;
import uk.police.npia.juror.schema.v1.GetPersonDetailsResponse;
import uk.police.npia.juror.schema.v1.PNCAIHeaderType;
import uk.police.npia.juror.schema.v1.Person;
import uk.police.npia.juror.schema.v1.PersonDetails;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface PersonDetailsDtoMapper {
    @Mapping(target = "personDtos", source = "details")
    PersonDetailsDto mapToPersonDetailsDto(GetPersonDetailsResponse getPersonDetailsResponse);


    HeaderTypeDto mapHeader(PNCAIHeaderType header);

    DisposalDto mapDisposal(Disposal disposal);

    List<DisposalDto> mapDisposals(List<Disposal> disposals);

    PersonDto mapPerson(Person person);

    List<PersonDto> mapPersons(List<Person> person);

    default List<PersonDto> map(JAXBElement<PersonDetails> personDetails) {
        if (personDetails == null
            || personDetails.getValue() == null
            || personDetails.getValue().getPerson() == null) {
            return Collections.emptyList();
        }
        return mapPersons(personDetails.getValue().getPerson());
    }

}
