package com.M198.Majorproject.features.inngest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.inngest.Event;
import com.inngest.FunctionContext;

class UserRegisteredFunctionTest {

    @Test
    void executeBuildsStructuredUserRegistrationPayload() {
        UserRegisteredFunction function = new UserRegisteredFunction();
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("userId", "user-42");
        data.put("email", "student@example.com");
        data.put("accountType", "STUDENT");
        Event event = new Event("event-id", "user-registered", data, new LinkedHashMap<>(), System.currentTimeMillis(), null);

        FunctionContext context = new FunctionContext(event, java.util.List.of(event), "run-1", "fn-1", 1);

        Object result = function.execute(context, null);

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> payload = (Map<?, ?>) result;
        assertEquals("user-42", payload.get("userId"));
        assertEquals("student@example.com", payload.get("email"));
        assertEquals("STUDENT", payload.get("accountType"));
        assertEquals("user-registered", payload.get("eventName"));
    }
}
