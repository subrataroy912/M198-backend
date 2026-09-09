package com.M198.Majorproject.features.inngest;

import java.util.LinkedHashMap;
import java.util.Map;

import com.inngest.Event;
import com.inngest.FunctionContext;
import com.inngest.InngestFunction;
import com.inngest.Step;

public class UserProfileVisibilityChangedFunction extends InngestFunction {

    @Override
    public Object execute(FunctionContext context, Step step) {
        Event event = context == null ? null : context.getEvent();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "accepted");
        payload.put("eventName", event == null ? "user-profile-visibility-changed" : event.getName());

        if (event != null && event.getData() != null) {
            payload.put("userId", event.getData().getOrDefault("userId", ""));
            payload.put("profileVisibility", event.getData().getOrDefault("profileVisibility", ""));
            payload.put("updatedAt", event.getData().getOrDefault("updatedAt", ""));
        }

        return payload;
    }
}
