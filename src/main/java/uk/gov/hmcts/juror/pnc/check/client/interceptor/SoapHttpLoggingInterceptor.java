package uk.gov.hmcts.juror.pnc.check.client.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class SoapHttpLoggingInterceptor implements HttpResponseInterceptor {

    @Override
    public void process(HttpResponse response, HttpContext context)
        throws HttpException, IOException {

        HttpEntity entity = response.getEntity();
        if (entity == null) {
            return;
        }

        byte[] bytes = EntityUtils.toByteArray(entity);
        String xml = new String(bytes, StandardCharsets.UTF_8);

        log.info("PNC SOAP response:\n{}", xml);

        // IMPORTANT: reattach entity so Spring-WS can still read it
        response.setEntity(new ByteArrayEntity(bytes, ContentType.get(entity)));
    }

}
