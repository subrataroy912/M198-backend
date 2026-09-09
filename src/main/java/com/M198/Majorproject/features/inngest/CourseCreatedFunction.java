package com.M198.Majorproject.features.inngest;

import java.util.LinkedHashMap;
import java.util.Map;

import com.inngest.Event;
import com.inngest.FunctionContext;
import com.inngest.InngestFunction;
import com.inngest.Step;

public class CourseCreatedFunction extends InngestFunction {

    @Override
    public Object execute(FunctionContext context, Step step) {
        Event event = context == null ? null : context.getEvent();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "accepted");
        payload.put("eventName", event == null ? "course-created" : event.getName());

        if (event != null && event.getData() != null) {
            payload.put("courseId", event.getData().getOrDefault("courseId", ""));
            payload.put("ownerId", event.getData().getOrDefault("ownerId", ""));
            payload.put("title", event.getData().getOrDefault("title", ""));
            payload.put("visibility", event.getData().getOrDefault("visibility", ""));
            payload.put("enrollmentCode", event.getData().getOrDefault("enrollmentCode", ""));
            payload.put("createdAt", event.getData().getOrDefault("createdAt", ""));
        }

        return payload;
    }
}
