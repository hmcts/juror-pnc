package uk.gov.hmcts.juror.pnc.check.mapper;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.model.pnc.HeaderTypeDto;
import uk.gov.hmcts.juror.pnc.check.model.pnc.PersonDetailsDto;
import uk.gov.hmcts.juror.pnc.check.model.pnc.PersonDto;
import uk.gov.hmcts.juror.pnc.check.utils.Utilities;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;
import uk.police.npia.juror.schema.v1.Disposal;
import uk.police.npia.juror.schema.v1.GetPersonDetailsResponse;
import uk.police.npia.juror.schema.v1.PNCAIHeaderType;
import uk.police.npia.juror.schema.v1.Person;
import uk.police.npia.juror.schema.v1.PersonDetails;

import java.io.StringWriter;
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

        String xmlString;

        try {
            JAXBContext context = JAXBContext.newInstance(PersonDetails.class);
            Marshaller marshaller = context.createMarshaller();

            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            StringWriter writer = new StringWriter();
            marshaller.marshal(personDetails, writer);

            xmlString = writer.toString();
        } catch (Exception e) {
            throw new InternalServerException("Failed to marshal JAXB element", e);
        }

        Utilities.logSomeInfo("Received PersonDetails XML: " + xmlString);

        return mapPersons(personDetails.getValue().getPerson());
    }

}
