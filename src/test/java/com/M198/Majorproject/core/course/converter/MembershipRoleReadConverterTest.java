package com.M198.Majorproject.core.course.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.M198.Majorproject.core.course.entity.MembershipRole;

class MembershipRoleReadConverterTest {

    private final MembershipRoleReadConverter converter = new MembershipRoleReadConverter();

    @Test
    void convertsStandardRoles() {
        assertThat(converter.convert("OWNER")).isEqualTo(MembershipRole.OWNER);
        assertThat(converter.convert("ADMIN")).isEqualTo(MembershipRole.ADMIN);
        assertThat(converter.convert("MEMBER")).isEqualTo(MembershipRole.MEMBER);
    }

    @Test
    void mapsLegacyTeacherAndAssistantRolesToAdmin() {
        assertThat(converter.convert("TEACHER")).isEqualTo(MembershipRole.ADMIN);
        assertThat(converter.convert("teacher")).isEqualTo(MembershipRole.ADMIN);
        assertThat(converter.convert("ASSISTANT")).isEqualTo(MembershipRole.ADMIN);
        assertThat(converter.convert("assistant")).isEqualTo(MembershipRole.ADMIN);
        assertThat(converter.convert("TA")).isEqualTo(MembershipRole.ADMIN);
        assertThat(converter.convert("INSTRUCTOR")).isEqualTo(MembershipRole.ADMIN);
    }

    @Test
    void mapsLegacyStudentAndUnknownRolesToMember() {
        assertThat(converter.convert("STUDENT")).isEqualTo(MembershipRole.MEMBER);
        assertThat(converter.convert("student")).isEqualTo(MembershipRole.MEMBER);
        assertThat(converter.convert("LEARNER")).isEqualTo(MembershipRole.MEMBER);
        assertThat(converter.convert("USER")).isEqualTo(MembershipRole.MEMBER);
        assertThat(converter.convert(null)).isEqualTo(MembershipRole.MEMBER);
        assertThat(converter.convert("")).isEqualTo(MembershipRole.MEMBER);
        assertThat(converter.convert("   ")).isEqualTo(MembershipRole.MEMBER);
        assertThat(converter.convert("NON_EXISTENT_ROLE")).isEqualTo(MembershipRole.MEMBER);
    }
}
