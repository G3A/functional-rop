package co.g3a.functionalrop;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdFilter implements Filter {

    public static final String TRACE_ID = "traceId";
    public static final String CORRELATION_ID = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            String traceId = getOrGenerate(httpRequest, "X-Trace-Id");
            String correlationId = getOrGenerate(httpRequest, "X-Correlation-Id");

            MDC.put(TRACE_ID, traceId);
            MDC.put(CORRELATION_ID, correlationId);

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String getOrGenerate(HttpServletRequest request, String header) {
        String value = request.getHeader(header);
        return (value != null && !value.isBlank()) ? value : UUID.randomUUID().toString();
    }
}