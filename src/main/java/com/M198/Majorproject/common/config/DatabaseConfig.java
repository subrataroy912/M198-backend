package com.M198.Majorproject.common.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import com.M198.Majorproject.core.course.converter.CourseAccessTypeReadConverter;
import com.M198.Majorproject.core.course.converter.MembershipRoleReadConverter;

@Configuration
@EnableMongoAuditing
public class DatabaseConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new MembershipRoleReadConverter(),
                new CourseAccessTypeReadConverter()
        ));
    }
}
