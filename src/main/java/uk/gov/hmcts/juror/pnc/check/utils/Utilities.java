package uk.gov.hmcts.juror.pnc.check.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

@Slf4j
public final class Utilities {
    private Utilities() {
    }

    public static String stripBadChars(final String originalTextToClean) {
        if (Strings.isEmpty(originalTextToClean)) {
            return originalTextToClean;
        }
        String textBeingCleaned = originalTextToClean
            .replaceAll("\\([^)]*\\)", "") //Remove braces
            .replaceAll("\\[[^]]*]", "") //Remove square braces
            .replaceAll("[^/_'A-Za-z0-9 -]", "") //Removes all none support characters
            .replaceAll("\\s+", "") //Removes spaces
            ;
        log.trace("Juror name {} contained unsupported chars and has been changed to {}", originalTextToClean,
            textBeingCleaned);
        return textBeingCleaned;
    }

    public static Integer getInteger(String intString) {
        if (Strings.isBlank(intString)) {
            return null;
        }
        try {
            return Integer.parseInt(intString);
        } catch (NumberFormatException e) {
            log.error("Unable to parse input ' {}' into an integer", intString, e);
            return null;
        }
    }
}
