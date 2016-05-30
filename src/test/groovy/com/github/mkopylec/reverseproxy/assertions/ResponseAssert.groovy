package com.github.mkopylec.reverseproxy.assertions

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

import static org.apache.commons.lang3.StringUtils.isEmpty

class ResponseAssert {

    private ResponseEntity actual

    protected ResponseAssert(ResponseEntity actual) {
        assert actual != null
        this.actual = actual
    }

    ResponseAssert hasBody(String body) {
        assert actual.body == body
        return this
    }

    ResponseAssert hasNoBody() {
        assert isEmpty(actual.body as CharSequence)
        return this
    }

    ResponseAssert hasStatus(HttpStatus status) {
        assert actual.statusCode == status
        return this
    }

    ResponseAssert containsHeaders(Map<String, String> headers) {
        headers.each { k, v -> assert actual.headers.get(k).join(', ') == v }
        return this
    }
}
