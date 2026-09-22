package com.M198.Majorproject.core.course.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.M198.Majorproject.core.course.entity.StudentGradebookEntry;

public interface StudentGradebookEntryRepository extends MongoRepository<StudentGradebookEntry, String> {

    List<StudentGradebookEntry> findAllByCourseIdAndStudentIdOrderByDueAtAsc(
            String courseId, String studentId);

    List<StudentGradebookEntry> findAllByCourseIdOrderByDueAtAsc(String courseId);

    void deleteAllByCourseId(String courseId);

    void deleteAllByCourseworkId(String courseworkId);
}
