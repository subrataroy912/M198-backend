package com.M198.Majorproject.features.inngest;

import java.util.LinkedHashMap;
import java.util.Map;

import com.inngest.Event;
import com.inngest.FunctionContext;
import com.inngest.InngestFunction;
import com.inngest.Step;

public class SubmissionGradedFunction extends InngestFunction {

    @Override
    public Object execute(FunctionContext context, Step step) {
        Event event = context == null ? null : context.getEvent();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "accepted");
        payload.put("eventName", event == null ? "submission-graded" : event.getName());

        if (event != null && event.getData() != null) {
            payload.put("courseId", event.getData().getOrDefault("courseId", ""));
            payload.put("courseworkId", event.getData().getOrDefault("courseworkId", ""));
            payload.put("submissionId", event.getData().getOrDefault("submissionId", ""));
            payload.put("studentId", event.getData().getOrDefault("studentId", ""));
            payload.put("graderId", event.getData().getOrDefault("graderId", ""));
            payload.put("score", event.getData().getOrDefault("score", ""));
            payload.put("gradedAt", event.getData().getOrDefault("gradedAt", ""));
        }

        return payload;
    }
}
