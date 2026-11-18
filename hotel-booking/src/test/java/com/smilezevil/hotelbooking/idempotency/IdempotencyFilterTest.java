package com.smilezevil.hotelbooking.idempotency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyFilterTest {

    private final IdempotencyFilter filter = new IdempotencyFilter(new IdempotencyStore());
    private final AtomicInteger bookingsCreated = new AtomicInteger();

    private final FilterChain createBooking = (request, response) -> {
        int id = bookingsCreated.incrementAndGet();
        HttpServletResponse http = (HttpServletResponse) response;
        http.setStatus(201);
        http.setContentType("application/json");
        http.getWriter().write("{\"id\":" + id + "}");
    };

    @Test
    void repeatedRequestWithSameKey_returnsStoredResponse_andCreatesOnlyOneBooking() throws Exception {
        MockHttpServletResponse first = post("bob", "key-1", createBooking);
        MockHttpServletResponse second = post("bob", "key-1", createBooking);

        assertThat(bookingsCreated).hasValue(1);
        assertThat(first.getStatus()).isEqualTo(201);
        assertThat(first.getContentAsString()).isEqualTo("{\"id\":1}");
        assertThat(first.getHeader(IdempotencyFilter.REPLAY_HEADER)).isNull();
        assertThat(second.getStatus()).isEqualTo(201);
        assertThat(second.getContentAsString()).isEqualTo("{\"id\":1}");
        assertThat(second.getHeader(IdempotencyFilter.REPLAY_HEADER)).isEqualTo("true");
    }

    @Test
    void sameKeyFromDifferentUsers_isProcessedSeparately() throws Exception {
        post("bob", "key-1", createBooking);
        MockHttpServletResponse other = post("carol", "key-1", createBooking);

        assertThat(bookingsCreated).hasValue(2);
        assertThat(other.getContentAsString()).isEqualTo("{\"id\":2}");
        assertThat(other.getHeader(IdempotencyFilter.REPLAY_HEADER)).isNull();
    }

    @Test
    void serverError_isNotStored_soTheClientCanRetry() throws Exception {
        FilterChain failing = (request, response) -> ((HttpServletResponse) response).setStatus(500);

        MockHttpServletResponse failed = post("bob", "key-1", failing);
        MockHttpServletResponse retried = post("bob", "key-1", createBooking);

        assertThat(failed.getStatus()).isEqualTo(500);
        assertThat(retried.getStatus()).isEqualTo(201);
        assertThat(retried.getHeader(IdempotencyFilter.REPLAY_HEADER)).isNull();
        assertThat(bookingsCreated).hasValue(1);
    }

    @Test
    void exceptionInController_isNotStored() throws Exception {
        FilterChain exploding = (request, response) -> {
            throw new IllegalStateException("boom");
        };

        assertThatThrownBy(() -> post("bob", "key-1", exploding)).isInstanceOf(IllegalStateException.class);
        MockHttpServletResponse retried = post("bob", "key-1", createBooking);

        assertThat(retried.getStatus()).isEqualTo(201);
        assertThat(bookingsCreated).hasValue(1);
    }

    @Test
    void requestWithoutKey_isProcessedEveryTime() throws Exception {
        post("bob", null, createBooking);
        MockHttpServletResponse second = post("bob", null, createBooking);

        assertThat(bookingsCreated).hasValue(2);
        assertThat(second.getHeader(IdempotencyFilter.REPLAY_HEADER)).isNull();
    }

    @Test
    void sameKeyWhileFirstIsStillProcessing_returns409() throws Exception {
        AtomicReference<MockHttpServletResponse> concurrent = new AtomicReference<>();
        FilterChain slowCreate = (request, response) -> {
            try {
                concurrent.set(post("bob", "key-1", createBooking));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            createBooking.doFilter(request, response);
        };

        MockHttpServletResponse first = post("bob", "key-1", slowCreate);

        assertThat(concurrent.get().getStatus()).isEqualTo(409);
        assertThat(first.getStatus()).isEqualTo(201);
        assertThat(bookingsCreated).hasValue(1);
    }

    private MockHttpServletResponse post(String user, String key, FilterChain chain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/bookings");
        request.setUserPrincipal(() -> user);
        if (key != null) {
            request.addHeader(IdempotencyFilter.KEY_HEADER, key);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }
}