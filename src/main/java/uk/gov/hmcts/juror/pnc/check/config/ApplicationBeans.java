package uk.gov.hmcts.juror.pnc.check.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;
import uk.gov.hmcts.juror.standard.client.SoapWebServiceTemplate;
import uk.gov.hmcts.juror.standard.client.contract.ClientType;
import uk.gov.hmcts.juror.standard.client.interceptor.JwtAuthenticationInterceptor;
import uk.gov.hmcts.juror.standard.config.SoapConfig;
import uk.gov.hmcts.juror.standard.config.WebConfig;
import uk.gov.hmcts.juror.standard.service.contracts.auth.JwtService;

import java.util.List;
import java.util.function.Function;

@Component("PNCApplicationBeans")
public class ApplicationBeans {

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("uk.police.npia.juror.schema.v1");

        return marshaller;
    }

    @Bean
    @ClientType("PoliceNationalComputerClient")
    public SoapWebServiceTemplate policeNationalComputerPrimaryWebServiceTemplate(
        Jaxb2Marshaller marshaller,
        RemoteConfig remoteConfig) {
        SoapConfig soapConfig = remoteConfig.getPoliceNationalComputerService();

        Function<WebConfig, WebServiceTemplate> webServiceTemplatefunction = webConfig -> {
            WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
            webServiceTemplate.setDefaultUri(webConfig.getUri());
            webServiceTemplate.setMarshaller(marshaller);
            webServiceTemplate.setUnmarshaller(marshaller);
            ClientHttpRequestMessageSender messageSender = new ClientHttpRequestMessageSender();
            messageSender.setRequestFactory(webConfig.getRequestFactory());
            webServiceTemplate.setMessageSender(messageSender);

            return webServiceTemplate;
        };
        return SoapWebServiceTemplate.create(soapConfig, webServiceTemplatefunction);
    }


    @Bean
    @ClientType("JobExecutionService")
    public RestTemplateBuilder jobExecutionServiceRestTemplateBuilder(final RemoteConfig remoteConfig,
                                                                      final JwtService jwtService
    ) {
        return restTemplateBuilder(remoteConfig.getJobExecutionService(), jwtService);
    }

    @Bean
    @ClientType("JurorService")
    public RestTemplateBuilder jurorServiceRestTemplateBuilder(
        final RemoteConfig remoteConfig,
        final JwtService jwtService
    ) {
        return restTemplateBuilder(remoteConfig.getJurorService(), jwtService);
    }

    @SuppressWarnings("removal")
    private RestTemplateBuilder restTemplateBuilder(final WebConfig webConfig,
                                                    final JwtService jwtService) {
        final List<ClientHttpRequestInterceptor> clientHttpRequestInterceptorList =
            List.of(new JwtAuthenticationInterceptor(jwtService, webConfig.getSecurity()));
        return new RestTemplateBuilder()
            .requestFactory(webConfig::getRequestFactory)
            .uriTemplateHandler(new RootUriTemplateHandler(
                webConfig.getScheme() + "://" + webConfig.getHost() + ":" + webConfig.getPort()))
            .additionalInterceptors(clientHttpRequestInterceptorList);
    }
}
