package com.M198.Majorproject.features.inngest;

import java.util.LinkedHashMap;
import java.util.Map;

import com.inngest.Event;
import com.inngest.FunctionContext;
import com.inngest.InngestFunction;
import com.inngest.Step;

public class CourseworkPublishedFunction extends InngestFunction {

    @Override
    public Object execute(FunctionContext context, Step step) {
        Event event = context == null ? null : context.getEvent();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "accepted");
        payload.put("eventName", event == null ? "coursework-published" : event.getName());

        if (event != null && event.getData() != null) {
            payload.put("courseId", event.getData().getOrDefault("courseId", ""));
            payload.put("courseworkId", event.getData().getOrDefault("courseworkId", ""));
            payload.put("creatorId", event.getData().getOrDefault("creatorId", ""));
            payload.put("title", event.getData().getOrDefault("title", ""));
            payload.put("type", event.getData().getOrDefault("type", ""));
            payload.put("publishedAt", event.getData().getOrDefault("publishedAt", ""));
        }

        return payload;
    }
}
