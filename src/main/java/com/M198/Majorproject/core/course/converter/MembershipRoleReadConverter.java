package com.M198.Majorproject.core.course.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import com.M198.Majorproject.core.course.entity.MembershipRole;

@ReadingConverter
public class MembershipRoleReadConverter implements Converter<String, MembershipRole> {

    @Override
    public MembershipRole convert(String source) {
        return MembershipRole.fromString(source);
    }
}
