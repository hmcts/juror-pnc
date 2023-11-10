package uk.gov.hmcts.juror.pnc.check.testsupport;


import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.SneakyThrows;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collection;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({"PMD.TestClassWithoutTestCases"})
public final class TestUtil {

    private TestUtil() {

    }


    @SneakyThrows
    public static String readResource(String url, String... prefixes) {

        try (Reader reader = new InputStreamReader(getUrl(url, prefixes).openStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static URL getUrl(String url, String... prefixes) {
        String prefix = String.join("/", prefixes);
        return Objects.requireNonNull(TestUtil.class.getResource(String.join("/", prefix, url)));
    }


    public static ResultMatcher jsonMatcher(JSONCompareMode mode, String expectedPayload) {
        return result -> {
            String actual = result.getResponse().getContentAsString(UTF_8);
            JSONAssert.assertEquals(
                "",
                expectedPayload,
                actual,
                mode
            );
        };
    }


    public static String replaceJsonPath(String json, String jsonPath, Object replacement) {
        DocumentContext parsed = JsonPath.parse(json);
        parsed.set(jsonPath, replacement);
        return parsed.jsonString();
    }

    public static String addJsonPath(String json, String jsonPath, String key, Object value) {
        DocumentContext parsed = JsonPath.parse(json);
        parsed.put(jsonPath, key, value);
        return parsed.jsonString();
    }

    public static String deleteJsonPath(String json, String jsonPath) {
        DocumentContext parsed = JsonPath.parse(json);
        parsed.delete(jsonPath);
        return parsed.jsonString();
    }

    public static void isUnmodifiable(Collection<?> collection) {
        assertThrows(UnsupportedOperationException.class,
            () -> collection.add(null), "Collection should be unmodifiable");
    }
}
