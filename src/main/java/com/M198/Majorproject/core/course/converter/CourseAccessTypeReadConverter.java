package com.M198.Majorproject.core.course.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import com.M198.Majorproject.core.course.entity.CourseAccessType;

@ReadingConverter
public class CourseAccessTypeReadConverter implements Converter<String, CourseAccessType> {

    @Override
    public CourseAccessType convert(String source) {
        return CourseAccessType.fromString(source);
    }
}
