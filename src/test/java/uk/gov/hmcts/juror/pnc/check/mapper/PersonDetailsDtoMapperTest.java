package uk.gov.hmcts.juror.pnc.check.mapper;

import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.model.pnc.HeaderTypeDto;
import uk.gov.hmcts.juror.pnc.check.model.pnc.PersonDetailsDto;
import uk.gov.hmcts.juror.pnc.check.model.pnc.PersonDto;
import uk.police.npia.juror.schema.v1.Disposal;
import uk.police.npia.juror.schema.v1.GetPersonDetailsResponse;
import uk.police.npia.juror.schema.v1.PNCAIHeaderType;
import uk.police.npia.juror.schema.v1.Person;
import uk.police.npia.juror.schema.v1.PersonDetails;
import uk.police.npia.juror.schema.v1.PncModeType;
import uk.police.npia.juror.schema.v1.PncTranCodeType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SuppressWarnings({
    "PMD.AvoidDuplicateLiterals",
    "PMD.TooManyMethods"
})
class PersonDetailsDtoMapperTest {

    private final PersonDetailsDtoMapper personDetailsDtoMapper;

    public PersonDetailsDtoMapperTest() {
        this.personDetailsDtoMapper = new PersonDetailsDtoMapperImpl();
    }


    @Test
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert"//False positive done via internal methods
    })
    void validateDisposalMapping() {
        Disposal disposal = createDisposal();
        validateDisposal(personDetailsDtoMapper.mapDisposal(disposal), disposal);
    }

    @Test
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert"//False positive done via internal methods
    })
    void validateDisposalsMapping() {
        List<Disposal> disposals = List.of(createDisposal(), createDisposal(), createDisposal());
        validateDisposals(personDetailsDtoMapper.mapDisposals(disposals), disposals);
    }

    @Test
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert"//False positive done via internal methods
    })
    void validatePersonMapping() {
        Person person = createPerson();
        validatePerson(personDetailsDtoMapper.mapPerson(person), person);
    }

    @Test
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert"//False positive done via internal methods
    })
    void validatePersonsMapping() {
        List<Person> persons = List.of(createPerson(), createPerson(), createPerson());
        validatePersons(personDetailsDtoMapper.mapPersons(persons), persons);
    }

    @Test
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert"//False positive done via internal methods
    })
    void validateHeaderMapping() {
        PNCAIHeaderType headerType = createHeaderType();
        validateHeader(personDetailsDtoMapper.mapHeader(headerType), headerType);
    }


    @Test
    @SuppressWarnings({
        "PMD.JUnitTestsShouldIncludeAssert"//False positive done via internal methods
    })
    void validatePersonDetailsMapping() {
        GetPersonDetailsResponse getPersonDetailsResponse = createGetPersonDetailsResponse();
        validateGetPersonDetailsResponse(personDetailsDtoMapper.mapToPersonDetailsDto(getPersonDetailsResponse),
            getPersonDetailsResponse);
    }


    private JAXBElement<String> randomStringJaxBElement() {
        return new JAXBElement<>(QName.valueOf("ABC"), String.class, RandomStringUtils.random(5));
    }

    private JAXBElement<Boolean> randomBooleanJaxBElement() {
        return new JAXBElement<>(QName.valueOf("ABC"), Boolean.class, RandomUtils.nextBoolean());
    }


    private Disposal createDisposal() {
        Disposal disposal = new Disposal();
        disposal.setDisposalCode(RandomStringUtils.random(5));
        disposal.setDisposalEffectiveDate(RandomStringUtils.random(5));

        disposal.setFineAmount(randomStringJaxBElement());
        disposal.setFineUnits(randomStringJaxBElement());
        disposal.setQualAmount(randomStringJaxBElement());
        disposal.setQualLiteral(randomStringJaxBElement());
        disposal.setQualPeriod(randomStringJaxBElement());
        disposal.setSentenceAmount(randomStringJaxBElement());
        disposal.setSentencePeriod(randomStringJaxBElement());
        return disposal;
    }

    private Person createPerson() {
        Person person = new Person();
        person.setPncId(randomStringJaxBElement());
        person.setFileName(randomStringJaxBElement());
        person.setDateOfBirth(randomStringJaxBElement());
        person.setPostCode(randomStringJaxBElement());
        person.getDisposals().addAll(List.of(createDisposal(), createDisposal(), createDisposal()));
        person.setOnBail(randomBooleanJaxBElement());
        return person;
    }

    private PersonDetails createPersonDetails() {
        PersonDetails personDetails = new PersonDetails();
        personDetails.getPerson().addAll(Set.of(createPerson(), createPerson(), createPerson()));
        return personDetails;
    }

    private PNCAIHeaderType createHeaderType() {
        PNCAIHeaderType headerType = new PNCAIHeaderType();
        headerType.setSequenceNumber(RandomUtils.nextLong());
        headerType.setLocalDateTime(RandomStringUtils.random(5));
        headerType.setPncTerminal(RandomStringUtils.random(5));
        headerType.setPncUserid(RandomStringUtils.random(5));
        headerType.setPncMode(getRandomEnum(PncModeType.class));
        headerType.setPncAuthorisation(RandomStringUtils.random(5));
        headerType.setPncTranCode(getRandomEnum(PncTranCodeType.class));
        headerType.setOriginator(RandomStringUtils.random(5));
        headerType.setReasonCode(RandomUtils.nextInt());
        headerType.setGatewayID(RandomStringUtils.random(5));
        return headerType;
    }

    private GetPersonDetailsResponse createGetPersonDetailsResponse() {
        GetPersonDetailsResponse getPersonDetailsResponse = new GetPersonDetailsResponse();
        getPersonDetailsResponse.setHeader(createHeaderType());
        getPersonDetailsResponse.setErrorReason(RandomStringUtils.random(5));
        getPersonDetailsResponse.setNumberMatchesFound(BigDecimal.valueOf(RandomUtils.nextDouble()));
        getPersonDetailsResponse.setJurorReference(RandomStringUtils.random(5));
        getPersonDetailsResponse.setDetails(new JAXBElement<>(QName.valueOf("ABC"),
            PersonDetails.class, createPersonDetails()));
        return getPersonDetailsResponse;
    }


    private <T extends Enum<T>> T getRandomEnum(Class<T> enumClass) {
        return enumClass.getEnumConstants()[RandomUtils.nextInt(0, enumClass.getEnumConstants().length)];
    }

    private void validateGetPersonDetailsResponse(PersonDetailsDto dto,
                                                  GetPersonDetailsResponse getPersonDetailsResponse) {
        if (dto == null) {
            assertNull(getPersonDetailsResponse, "Expect null");
            return;
        }
        assertNotNull(getPersonDetailsResponse, "Expect not null");
        validateHeader(dto.getHeader(), getPersonDetailsResponse.getHeader());
        assertEquals(dto.getErrorReason(), getPersonDetailsResponse.getErrorReason(),
            "Error reason must match");
        assertEquals(dto.getNumberMatchesFound(), getPersonDetailsResponse.getNumberMatchesFound(),
            "Number matches found must match");
        assertEquals(dto.getJurorReference(), getPersonDetailsResponse.getJurorReference(),
            "Juror Reference must match");
        validatePersonDetails(dto.getPersonDtos(), getPersonDetailsResponse.getDetails());
    }


    private void validateHeader(HeaderTypeDto dto, PNCAIHeaderType headerType) {
        if (dto == null) {
            assertNull(headerType, "Expect null");
            return;
        }
        assertNotNull(headerType, "Expect not null");
        assertEquals(dto.getSequenceNumber(), headerType.getSequenceNumber(),
            "Sequence number must match");
        assertEquals(dto.getLocalDateTime(), headerType.getLocalDateTime(),
            "Local date time must match");
        assertEquals(dto.getPncTerminal(), headerType.getPncTerminal(),
            "Pnc terminal must match");
        assertEquals(dto.getPncUserid(), headerType.getPncUserid(),
            "Pnc User Id must match");
        assertEquals(dto.getPncMode(), headerType.getPncMode(),
            "Pnc Mode must match");
        assertEquals(dto.getPncAuthorisation(), headerType.getPncAuthorisation(),
            "Pnc authorisation must match");
        assertEquals(dto.getPncTranCode(), headerType.getPncTranCode(),
            "Pnc tran code must match");
        assertEquals(dto.getOriginator(), headerType.getOriginator(),
            "Originator must match");
        assertEquals(dto.getReasonCode(), headerType.getReasonCode(),
            "Reason code must match");
        assertEquals(dto.getGatewayID(), headerType.getGatewayID(),
            "Gateway Id must match");
    }

    private void validatePersonDetails(List<PersonDto> dto, JAXBElement<PersonDetails> details) {
        if (dto == null) {
            assertNull(details, "Expect null");
            return;
        }
        assertNotNull(details, "Expect not null");
        assertNotNull(details.getValue(), "Expect not null");
        validatePersons(dto, details.getValue().getPerson());
    }

    private void validatePersons(List<PersonDto> dto, List<Person> person) {
        if (dto == null) {
            assertNull(person, "Expect null");
            return;
        }
        assertNotNull(person, "Expect not null");
        assertEquals(dto.size(), person.size(), "Size must match");
        for (int index = 0; index < dto.size(); index++) {
            validatePerson(dto.get(index), person.get(index));
        }
    }

    private void validatePerson(PersonDto dto, Person person) {
        if (dto == null) {
            assertNull(person, "Expect null");
            return;
        }
        assertNotNull(person, "Expect not null");
        assertEquals(dto.getPncId(), person.getPncId().getValue(), "PncId must match");
        assertEquals(dto.getFileName(), person.getFileName().getValue(), "FileName must match");
        assertEquals(dto.getDateOfBirth(), person.getDateOfBirth().getValue(), "DateOfBirth must match");
        assertEquals(dto.getPostCode(), person.getPostCode().getValue(), "PostCode must match");
        validateDisposals(dto.getDisposals(), person.getDisposals());
    }


    private void validateDisposals(List<DisposalDto> dto, List<Disposal> disposals) {
        if (dto == null) {
            assertNull(disposals, "Expect null");
            return;
        }
        assertNotNull(disposals, "Expect not null");
        assertEquals(dto.size(), disposals.size(), "Size must match");
        for (int index = 0; index < dto.size(); index++) {
            validateDisposal(dto.get(index), disposals.get(index));
        }
    }


    private void validateDisposal(DisposalDto dto, Disposal disposal) {
        if (dto == null) {
            assertNull(disposal, "Expect null");
            return;
        }
        assertNotNull(disposal, "Expect not null");
        assertEquals(dto.getDisposalCode(), disposal.getDisposalCode(),
            "Disposal code must match");
        assertEquals(dto.getDisposalEffectiveDate(), disposal.getDisposalEffectiveDate(),
            "Disposal effective date must match");
        assertEquals(dto.getFineAmount(), disposal.getFineAmount().getValue(),
            "Fine amount code must match");
        assertEquals(dto.getFineUnits(), disposal.getFineUnits().getValue(),
            "Fine units code must match");
        assertEquals(dto.getQualAmount(), disposal.getQualAmount().getValue(),
            "Qual amount code must match");
        assertEquals(dto.getQualLiteral(), disposal.getQualLiteral().getValue(),
            "Qual literal code must match");
        assertEquals(dto.getQualPeriod(), disposal.getQualPeriod().getValue(),
            "QUal period code must match");
        assertEquals(dto.getSentenceAmount(), disposal.getSentenceAmount().getValue(),
            "Sentence amount code must match");
        assertEquals(dto.getSentencePeriod(), disposal.getSentencePeriod().getValue(),
            "Sentence period code must match");

    }
}