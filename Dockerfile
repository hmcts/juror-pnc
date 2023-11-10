 # renovate: datasource=github-releases depName=microsoft/ApplicationInsights-Java
ARG APP_INSIGHTS_AGENT_VERSION=3.4.16
FROM hmctspublic.azurecr.io/base/java:17-distroless

COPY lib/applicationinsights.json /opt/app/
COPY build/libs/juror-pnc-check-service.jar /opt/app/

EXPOSE 8084
CMD [ "juror-pnc-check-service.jar" ]