package com.smilezevil.hotelbooking.idempotency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;

public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String KEY_HEADER = "Idempotency-Key";
    public static final String REPLAY_HEADER = "Idempotent-Replay";

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);

    private final IdempotencyStore store;

    public IdempotencyFilter(IdempotencyStore store) {
        this.store = store;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !StringUtils.hasText(request.getHeader(KEY_HEADER));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = owner(request) + ":" + request.getHeader(KEY_HEADER);
        IdempotencyStore.Begin begin = store.begin(key);
        switch (begin.state()) {
            case DONE -> replay(key, response, begin.saved());
            case IN_PROGRESS -> reject(key, response);
            case STARTED -> process(key, request, response, chain);
        }
    }

    private void process(String key, HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        log.info("IDEMPOTENCY {}: first request, processing", key);
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, wrapper);
        } catch (ServletException | IOException | RuntimeException e) {
            store.forget(key);
            throw e;
        }
        if (wrapper.getStatus() >= 500) {
            store.forget(key);
            log.info("IDEMPOTENCY {}: response {} is not stored, the client may retry", key, wrapper.getStatus());
        } else {
            store.complete(key, new SavedResponse(
                    wrapper.getStatus(), wrapper.getContentType(), wrapper.getContentAsByteArray()));
            log.info("IDEMPOTENCY {}: response {} stored", key, wrapper.getStatus());
        }
        wrapper.copyBodyToResponse();
    }

    private void replay(String key, HttpServletResponse response, SavedResponse saved) throws IOException {
        log.info("IDEMPOTENCY {}: repeated request, returning stored response {}", key, saved.status());
        response.setStatus(saved.status());
        if (saved.contentType() != null) {
            response.setContentType(saved.contentType());
        }
        response.setHeader(REPLAY_HEADER, "true");
        response.getOutputStream().write(saved.body());
    }

    private void reject(String key, HttpServletResponse response) throws IOException {
        log.info("IDEMPOTENCY {}: same key is still being processed, 409", key);
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"Запит з цим Idempotency-Key ще виконується\"}");
    }

    private String owner(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        return principal == null ? "anonymous" : principal.getName();
    }
}