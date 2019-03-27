package com.github.mkopylec.charon;

import com.github.mkopylec.charon.interceptors.resilience.CircuitBreakerHandlerConfigurer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.CircuitBreakerMetrics;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;

import static com.github.mkopylec.charon.configuration.CharonConfigurer.charonConfiguration;
import static com.github.mkopylec.charon.configuration.CustomConfigurer.custom;
import static com.github.mkopylec.charon.configuration.RequestForwardingConfigurer.requestForwarding;
import static com.github.mkopylec.charon.configuration.TimeoutConfigurer.timeout;
import static com.github.mkopylec.charon.forwarding.OkHttpClientFactoryConfigurer.okHttpClientFactory;
import static com.github.mkopylec.charon.interceptors.async.AsynchronousForwardingHandlerConfigurer.asynchronousForwardingHandler;
import static com.github.mkopylec.charon.interceptors.async.ThreadPoolConfigurer.threadPool;
import static com.github.mkopylec.charon.interceptors.rewrite.RegexRequestPathRewriterConfigurer.regexRequestPathRewriter;
import static com.github.mkopylec.charon.interceptors.rewrite.RemovingResponseCookieRewriterConfigurer.removingResponseCookieRewriter;
import static java.time.Duration.ofMillis;
import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;

public class Main {

    private static final Logger log = getLogger(Main.class);

    public static void main(String[] args) {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom().ringBufferSizeInClosedState(5).build();
        RetryConfig retryConfig = RetryConfig.custom().build();
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom().build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("c1", circuitBreakerConfig);
        Retry retry = Retry.ofDefaults("r1");
        Runnable runnable = CircuitBreaker.decorateRunnable(circuitBreaker, () -> {
            System.out.println("dupa");
            throw new RuntimeException();
        });
        Runnable runnable1 = Retry.decorateRunnable(retry, runnable);
//        runnable1.run();
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig);
        circuitBreakerRegistry.circuitBreaker("d");

        CircuitBreakerMetrics metrics = CircuitBreakerMetrics.ofIterable(singletonList(circuitBreaker));
        metrics.bindTo(new SimpleMeterRegistry());

        CircuitBreakerHandlerConfigurer.circuitBreakerHandler()
                .enabled(true)
                .measured(false);
        charonConfiguration()
                .set(okHttpClientFactory())
                .set(removingResponseCookieRewriter())
                .set(regexRequestPathRewriter())
                .set(asynchronousForwardingHandler()
                        .set(threadPool().initialSize(3)))
                .add(requestForwarding("proxy 1")
                        .set(timeout().connection(ofMillis(100)).read(ofMillis(500)))
                        .set(custom().set("name", "value")))
                .add(requestForwarding("proxy 2"));

        Retry.decorateSupplier(retry, () -> {
            throw new IllegalArgumentException("dupa");
        }).get();

//        List<RequestForwardingInterceptor> interceptors = new ArrayList<>();
//        interceptors.add((request, forwarder) -> {
//            System.out.println("start 1");
//            HttpResponse forward = forwarder.forward(request);
//            System.out.println("end 1");
//            return null;
//        });
//        interceptors.add((request, forwarder) -> {
//            System.out.println("start 2");
//            HttpResponse forward = CircuitBreaker.decorateSupplier(circuitBreaker, () -> forwarder.forward(request)).get();
//            System.out.println("end 2");
//            return forward;
//        });
//        interceptors.add((request, forwarder) -> {
//            System.out.println("start 3");
//            HttpResponse forward = Retry.decorateSupplier(retry, () -> forwarder.forward(request)).get();
//            System.out.println("end 3");
//            return forward;
//        });
//        for (int i = 0; i < 10; i++) {
//            RequestForwarder forwarder = new RequestForwarder(interceptors);
//            try {
//                forwarder.forward(null);
//            } catch (Exception e) {
//                log.error("error", e);
//                log.info("CB state {}", circuitBreaker.getState());
//            }
//        }
    }
}
