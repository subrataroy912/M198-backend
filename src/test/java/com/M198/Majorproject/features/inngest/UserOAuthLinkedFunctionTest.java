package com.M198.Majorproject.features.inngest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.inngest.Event;
import com.inngest.FunctionContext;

class UserOAuthLinkedFunctionTest {

    @Test
    void executeBuildsStructuredOAuthLinkPayload() {
        UserOAuthLinkedFunction function = new UserOAuthLinkedFunction();
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("userId", "user-7");
        data.put("provider", "GOOGLE");
        data.put("providerUserId", "google-123");
        Event event = new Event("event-id", "user-oauth-linked", data, new LinkedHashMap<>(), System.currentTimeMillis(), null);

        FunctionContext context = new FunctionContext(event, java.util.List.of(event), "run-1", "fn-1", 1);

        Object result = function.execute(context, null);

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> payload = (Map<?, ?>) result;
        assertEquals("user-7", payload.get("userId"));
        assertEquals("GOOGLE", payload.get("provider"));
        assertEquals("google-123", payload.get("providerUserId"));
        assertEquals("user-oauth-linked", payload.get("eventName"));
    }
}
