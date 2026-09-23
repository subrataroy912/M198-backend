package com.M198.Majorproject.core.course.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
public class CourseDataMigrationRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(CourseDataMigrationRunner.class);
    private final MongoTemplate mongoTemplate;

    public CourseDataMigrationRunner(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 1. Migrate courses: OPEN -> PUBLIC
            var res1 = mongoTemplate.updateMulti(
                    Query.query(Criteria.where("access_type").is("OPEN")),
                    Update.update("access_type", "PUBLIC"),
                    "courses"
            );
            if (res1.getModifiedCount() > 0) {
                logger.info("Migrated {} courses from OPEN to PUBLIC", res1.getModifiedCount());
            }

            // 2. Migrate courses: CODE / INVITE -> LINK_ONLY
            var res2 = mongoTemplate.updateMulti(
                    Query.query(Criteria.where("access_type").in("CODE", "INVITE")),
                    Update.update("access_type", "LINK_ONLY"),
                    "courses"
            );
            if (res2.getModifiedCount() > 0) {
                logger.info("Migrated {} courses from CODE/INVITE to LINK_ONLY", res2.getModifiedCount());
            }

            // 3. Remove obsolete fields from courses
            var res3 = mongoTemplate.updateMulti(
                    new Query(),
                    new Update().unset("space_type").unset("meeting_type").unset("location").unset("visibility"),
                    "courses"
            );
            if (res3.getModifiedCount() > 0) {
                logger.info("Cleaned obsolete fields from {} course documents", res3.getModifiedCount());
            }

            // 4. Migrate course_discovery: OPEN -> PUBLIC
            var res4 = mongoTemplate.updateMulti(
                    Query.query(Criteria.where("accessType").is("OPEN")),
                    Update.update("accessType", "PUBLIC"),
                    "course_discovery"
            );
            if (res4.getModifiedCount() > 0) {
                logger.info("Migrated {} course_discovery from OPEN to PUBLIC", res4.getModifiedCount());
            }

            // 5. Migrate course_discovery: CODE / INVITE -> LINK_ONLY
            var res5 = mongoTemplate.updateMulti(
                    Query.query(Criteria.where("accessType").in("CODE", "INVITE")),
                    Update.update("accessType", "LINK_ONLY"),
                    "course_discovery"
            );
            if (res5.getModifiedCount() > 0) {
                logger.info("Migrated {} course_discovery from CODE/INVITE to LINK_ONLY", res5.getModifiedCount());
            }

            // 6. Remove obsolete spaceType from course_discovery
            var res6 = mongoTemplate.updateMulti(
                    new Query(),
                    new Update().unset("spaceType"),
                    "course_discovery"
            );
            if (res6.getModifiedCount() > 0) {
                logger.info("Cleaned obsolete spaceType from {} course_discovery documents", res6.getModifiedCount());
            }
        } catch (Exception e) {
            logger.warn("Course data migration skipped or failed: {}", e.getMessage());
        }
    }
}
