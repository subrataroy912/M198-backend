package com.M198.Majorproject.repository.analytics;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.entity.analytics.StudentGradebookEntry;

public interface StudentGradebookEntryRepository extends MongoRepository<StudentGradebookEntry, String> {

    List<StudentGradebookEntry> findAllByCourseIdAndStudentIdOrderByDueAtAsc(
            String courseId, String studentId);

    List<StudentGradebookEntry> findAllByCourseIdOrderByDueAtAsc(String courseId);
}
