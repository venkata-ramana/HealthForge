package dev.healthforge.platform.auth;

import dev.healthforge.platform.web.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void generatesRequestIdWhenHeaderMissing() throws Exception {
        var request = new MockHttpServletRequest("GET", "/v1/briefs");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).startsWith("req_");
        assertThat(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)).isEqualTo(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER));
    }

    @Test
    void preservesProvidedRequestId() throws Exception {
        var request = new MockHttpServletRequest("GET", "/v1/briefs");
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "demo-request");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("demo-request");
    }
}
