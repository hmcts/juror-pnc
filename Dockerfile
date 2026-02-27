 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.7.4
FROM hmctsprod.azurecr.io/base/java:21-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/juror-pnc.jar /opt/app/

EXPOSE 8080
ENTRYPOINT ["/usr/bin/java"]
CMD ["-Djavax.net.debug=ssl,handshake", "-jar", "juror-pnc.jar"]
