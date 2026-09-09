package com.M198.Majorproject.features.inngest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.inngest.Event;
import com.inngest.FunctionContext;

class CourseCreatedFunctionTest {

    @Test
    void executeBuildsStructuredEventPayload() {
        CourseCreatedFunction function = new CourseCreatedFunction();
        LinkedHashMap<String, Object> data = new LinkedHashMap<>();
        data.put("courseId", "course-123");
        data.put("ownerId", "teacher-1");
        data.put("title", "Algebra II");
        data.put("visibility", "PRIVATE");
        Event event = new Event("event-id", "course-created", data, new LinkedHashMap<>(), System.currentTimeMillis(), null);

        FunctionContext context = new FunctionContext(event, java.util.List.of(event), "run-1", "fn-1", 1);

        Object result = function.execute(context, null);

        assertNotNull(result);
        assertTrue(result instanceof Map<?, ?>);
        Map<?, ?> payload = (Map<?, ?>) result;
        assertEquals("course-123", payload.get("courseId"));
        assertEquals("teacher-1", payload.get("ownerId"));
        assertEquals("Algebra II", payload.get("title"));
        assertEquals("PRIVATE", payload.get("visibility"));
        assertEquals("course-created", payload.get("eventName"));
    }
}
