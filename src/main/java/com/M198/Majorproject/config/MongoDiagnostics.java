package com.M198.Majorproject.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.M198.Majorproject.entity.course.Course;

@Component
public class MongoDiagnostics {

  private static final Logger logger = LoggerFactory.getLogger(MongoDiagnostics.class);

  private final MongoTemplate mongoTemplate;

  public MongoDiagnostics(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void logMongoStatus() {
    try {
      String databaseName = mongoTemplate.getDb().getName();
      long courseCount = mongoTemplate.count(new Query(), Course.class);
      logger.info("MongoDB status=CONNECTED database={} collection=courses courseCount={}",
          databaseName, courseCount);
    } catch (RuntimeException exception) {
      logger.error("MongoDB status=DISCONNECTED; unable to read database name or courses count", exception);
    }
  }
}