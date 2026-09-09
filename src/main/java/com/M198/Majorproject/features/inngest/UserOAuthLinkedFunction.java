package com.M198.Majorproject.features.inngest;

import java.util.LinkedHashMap;
import java.util.Map;

import com.inngest.Event;
import com.inngest.FunctionContext;
import com.inngest.InngestFunction;
import com.inngest.Step;

public class UserOAuthLinkedFunction extends InngestFunction {

    @Override
    public Object execute(FunctionContext context, Step step) {
        Event event = context == null ? null : context.getEvent();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "accepted");
        payload.put("eventName", event == null ? "user-oauth-linked" : event.getName());

        if (event != null && event.getData() != null) {
            payload.put("userId", event.getData().getOrDefault("userId", ""));
            payload.put("provider", event.getData().getOrDefault("provider", ""));
            payload.put("providerUserId", event.getData().getOrDefault("providerUserId", ""));
            payload.put("linkedAt", event.getData().getOrDefault("linkedAt", ""));
        }

        return payload;
    }
}
