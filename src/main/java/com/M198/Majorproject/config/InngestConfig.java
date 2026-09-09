package com.M198.Majorproject.config;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.M198.Majorproject.features.inngest.CourseCreatedFunction;
import com.M198.Majorproject.features.inngest.CourseworkPublishedFunction;
import com.M198.Majorproject.features.inngest.SubmissionGradedFunction;
import com.M198.Majorproject.features.inngest.UserLoggedInFunction;
import com.M198.Majorproject.features.inngest.UserOAuthLinkedFunction;
import com.M198.Majorproject.features.inngest.UserProfileUpdatedFunction;
import com.M198.Majorproject.features.inngest.UserProfileVisibilityChangedFunction;
import com.M198.Majorproject.features.inngest.UserRegisteredFunction;
import com.inngest.Inngest;
import com.inngest.InngestFunction;
import com.inngest.ServeConfig;
import com.inngest.springboot.InngestConfiguration;

@Configuration
public class InngestConfig extends InngestConfiguration {

    @Value("${inngest.api.key:}")
    private String apiKey;

    @Value("${inngest.signing.key:}")
    private String signingKey;

    @Value("${inngest.app.id:majorproject}")
    private String appId;

    @Override
    protected Inngest inngestClient() {
        return new Inngest(appId, apiKey);
    }

    @Override
    protected HashMap<String, InngestFunction> functions() {
        HashMap<String, InngestFunction> functions = new HashMap<>();
        functions.put("course-created", new CourseCreatedFunction());
        functions.put("coursework-published", new CourseworkPublishedFunction());
        functions.put("submission-graded", new SubmissionGradedFunction());
        functions.put("user-registered", new UserRegisteredFunction());
        functions.put("user-logged-in", new UserLoggedInFunction());
        functions.put("user-profile-updated", new UserProfileUpdatedFunction());
        functions.put("user-oauth-linked", new UserOAuthLinkedFunction());
        functions.put("user-profile-visibility-changed", new UserProfileVisibilityChangedFunction());
        return functions;
    }

    @Override
    protected ServeConfig serve(Inngest inngestClient) {
        return new ServeConfig(inngestClient, appId, signingKey, "/api/inngest", "/api/inngest", "INFO");
    }
}
