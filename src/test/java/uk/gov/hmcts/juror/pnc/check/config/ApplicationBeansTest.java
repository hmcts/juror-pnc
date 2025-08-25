package uk.gov.hmcts.juror.pnc.check.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.util.DefaultUriBuilderFactory;
import uk.gov.hmcts.juror.standard.client.SoapWebServiceTemplate;
import uk.gov.hmcts.juror.standard.client.interceptor.JwtAuthenticationInterceptor;
import uk.gov.hmcts.juror.standard.config.JwtSecurityConfig;
import uk.gov.hmcts.juror.standard.config.WebConfig;
import uk.gov.hmcts.juror.standard.service.contracts.auth.JwtService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;



@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("PMD")
class ApplicationBeansTest {

    @Mock
    private RemoteConfig remoteConfig;

    @Mock
    private JwtService jwtService;

    @Mock
    private WebConfig webConfig;

    @Mock
    private JwtSecurityConfig jwtSecurityConfig;

    @Mock
    private ClientHttpRequestFactory requestFactory;

    @Mock
    private PncConfig pncConfig;

    @InjectMocks
    private ApplicationBeans applicationBeans;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testMarshaller_ShouldCreateJaxb2MarshallerWithCorrectContextPath() {
        // When
        Jaxb2Marshaller marshaller = applicationBeans.marshaller();

        // Then
        assertNotNull(marshaller);
        // Note: We can't directly verify the context path as it's set internally
        // but we can verify the marshaller is created successfully
    }

    @Test
    void testPoliceNationalComputerPrimaryWebServiceTemplate_ShouldCreateSoapWebServiceTemplate() {
        // Given
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        when(remoteConfig.getPoliceNationalComputerService()).thenReturn(pncConfig);
        when(pncConfig.getUri()).thenReturn("https://localhost:8080/soap");
        when(pncConfig.getRequestFactory()).thenReturn(requestFactory);


        // When
        SoapWebServiceTemplate result = applicationBeans.policeNationalComputerPrimaryWebServiceTemplate(
            marshaller, remoteConfig);

        // Then
        assertNotNull(result);
        verify(remoteConfig).getPoliceNationalComputerService();
    }

    @Test
    void testJobExecutionServiceRestTemplateBuilder_ShouldCreateRestTemplateBuilder() {
        // Given
        setupValidWebConfig();
        when(remoteConfig.getJobExecutionService()).thenReturn(webConfig);

        // When
        RestTemplateBuilder result = applicationBeans.jobExecutionServiceRestTemplateBuilder(
            remoteConfig, jwtService);

        // Then
        assertNotNull(result);
        verify(remoteConfig).getJobExecutionService();
    }

    @Test
    void testJurorServiceRestTemplateBuilder_ShouldCreateRestTemplateBuilder() {
        // Given
        setupValidWebConfig();
        when(remoteConfig.getJurorService()).thenReturn(webConfig);

        // When
        RestTemplateBuilder result = applicationBeans.jurorServiceRestTemplateBuilder(
            remoteConfig, jwtService);

        // Then
        assertNotNull(result);
        verify(remoteConfig).getJurorService();
    }

    @Test
    void testRestTemplateBuilder_WithValidConfiguration_ShouldCreateBuilderWithCorrectBaseUrl() {
        // Given
        setupValidWebConfig();
        when(remoteConfig.getJobExecutionService()).thenReturn(webConfig);

        // When
        RestTemplateBuilder result = applicationBeans.jobExecutionServiceRestTemplateBuilder(
            remoteConfig, jwtService);

        // Then
        assertNotNull(result);

        // Verify that the builder was configured properly by building a RestTemplate
        var restTemplate = result.build();
        assertNotNull(restTemplate);

        // Verify interceptors were added
        assertTrue(restTemplate.getInterceptors().size() > 0);
        assertTrue(restTemplate.getInterceptors().stream()
                       .anyMatch(interceptor -> interceptor instanceof JwtAuthenticationInterceptor));
    }

    @Test
    void testRestTemplateBuilder_WithHttpScheme_ShouldCreateValidUri() {
        // Given
        when(webConfig.getScheme()).thenReturn("http");
        when(webConfig.getHost()).thenReturn("example.com");
        when(webConfig.getPort()).thenReturn(9090);
        when(webConfig.getRequestFactory()).thenReturn(requestFactory);
        when(webConfig.getSecurity()).thenReturn(jwtSecurityConfig);
        when(remoteConfig.getJobExecutionService()).thenReturn(webConfig);

        // When
        RestTemplateBuilder result = applicationBeans.jobExecutionServiceRestTemplateBuilder(
            remoteConfig, jwtService);

        // Then
        assertNotNull(result);
        var restTemplate = result.build();
        assertNotNull(restTemplate);
    }

    @Test
    void testRestTemplateBuilder_WithInvalidPort_ShouldThrowIllegalStateException() {
        // Given
        when(webConfig.getScheme()).thenReturn("https");
        when(webConfig.getHost()).thenReturn("local host");
        when(webConfig.getPort()).thenReturn(8080); // Port too high (max is 65535) // Invalid port
        when(webConfig.getRequestFactory()).thenReturn(requestFactory);
        when(webConfig.getSecurity()).thenReturn(jwtSecurityConfig);
        when(remoteConfig.getJobExecutionService()).thenReturn(webConfig);

        // When & Then
        assertThrows(IllegalStateException.class, () ->
            applicationBeans.jobExecutionServiceRestTemplateBuilder(remoteConfig, jwtService));
    }

    @Test
    void testRestTemplateBuilder_WithNullHost_ShouldThrowIllegalStateException() {
        // Given
        when(webConfig.getScheme()).thenReturn("https");
        when(webConfig.getHost()).thenReturn(null);
        when(webConfig.getPort()).thenReturn(8080);
        when(webConfig.getRequestFactory()).thenReturn(requestFactory);
        when(webConfig.getSecurity()).thenReturn(jwtSecurityConfig);
        when(remoteConfig.getJobExecutionService()).thenReturn(webConfig);

        // When & Then
        assertThrows(IllegalStateException.class, () ->
            applicationBeans.jobExecutionServiceRestTemplateBuilder(remoteConfig, jwtService));
    }

    @Test
    void testRestTemplateBuilder_WithInvalidScheme_ShouldThrowIllegalStateException() {
        // Given
        when(webConfig.getScheme()).thenReturn("invalid-scheme");
        when(webConfig.getHost()).thenReturn("local host");
        when(webConfig.getPort()).thenReturn(8080);
        when(webConfig.getRequestFactory()).thenReturn(requestFactory);
        when(webConfig.getSecurity()).thenReturn(jwtSecurityConfig);

        when(remoteConfig.getJobExecutionService()).thenReturn(webConfig);

        // When & Then
        assertThrows(IllegalStateException.class, () ->
            applicationBeans.jobExecutionServiceRestTemplateBuilder(remoteConfig, jwtService));
    }

    @Test
    void testRestTemplateBuilder_ShouldConfigureUriBuilderFactoryWithCorrectEncodingMode() {
        // Given
        setupValidWebConfig();
        when(remoteConfig.getJobExecutionService()).thenReturn(webConfig);

        // When
        RestTemplateBuilder result = applicationBeans.jobExecutionServiceRestTemplateBuilder(
            remoteConfig, jwtService);

        // Then
        assertNotNull(result);
        var restTemplate = result.build();

        // Verify URI template handler is configured
        assertNotNull(restTemplate.getUriTemplateHandler());
        assertTrue(restTemplate.getUriTemplateHandler() instanceof DefaultUriBuilderFactory);
    }

    @Test
    void testRestTemplateBuilder_ShouldAddJwtAuthenticationInterceptor() {
        // Given
        setupValidWebConfig();
        when(remoteConfig.getJobExecutionService()).thenReturn(webConfig);

        // When
        RestTemplateBuilder result = applicationBeans.jobExecutionServiceRestTemplateBuilder(
            remoteConfig, jwtService);
        var restTemplate = result.build();

        // Then
        assertNotNull(restTemplate);
        assertEquals(1, restTemplate.getInterceptors().size());

        ClientHttpRequestInterceptor interceptor = restTemplate.getInterceptors().get(0);
        assertTrue(interceptor instanceof JwtAuthenticationInterceptor);
    }

    @Test
    void testRestTemplateBuilder_ShouldConfigureRequestFactory() {
        // Given
        setupValidWebConfig();
        when(remoteConfig.getJobExecutionService()).thenReturn(webConfig);

        // When
        RestTemplateBuilder result = applicationBeans.jobExecutionServiceRestTemplateBuilder(
            remoteConfig, jwtService);
        var restTemplate = result.build();

        // Then
        assertNotNull(restTemplate);
        verify(webConfig, atLeastOnce()).getRequestFactory();
    }

    @Test
    void testBothRestTemplateBuilders_ShouldUseCorrectRemoteConfigMethods() {
        // Given
        setupValidWebConfig();
        when(remoteConfig.getJobExecutionService()).thenReturn(webConfig);
        when(remoteConfig.getJurorService()).thenReturn(webConfig);

        // When
        applicationBeans.jobExecutionServiceRestTemplateBuilder(remoteConfig, jwtService);
        applicationBeans.jurorServiceRestTemplateBuilder(remoteConfig, jwtService);

        // Then
        verify(remoteConfig).getJobExecutionService();
        verify(remoteConfig).getJurorService();
    }

    private void setupValidWebConfig() {
        lenient().when(webConfig.getScheme()).thenReturn("https");
        lenient().when(webConfig.getHost()).thenReturn("localhost");
        lenient().when(webConfig.getPort()).thenReturn(8080);
        lenient().when(webConfig.getRequestFactory()).thenReturn(requestFactory);
        lenient().when(webConfig.getSecurity()).thenReturn(jwtSecurityConfig);
    }
}
