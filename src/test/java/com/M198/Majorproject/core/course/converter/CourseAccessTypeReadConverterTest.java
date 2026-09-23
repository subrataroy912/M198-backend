package com.M198.Majorproject.core.course.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.M198.Majorproject.core.course.entity.CourseAccessType;

class CourseAccessTypeReadConverterTest {

    private final CourseAccessTypeReadConverter converter = new CourseAccessTypeReadConverter();

    @Test
    void convertsStandardAccessTypes() {
        assertThat(converter.convert("PUBLIC")).isEqualTo(CourseAccessType.PUBLIC);
        assertThat(converter.convert("PRIVATE")).isEqualTo(CourseAccessType.PRIVATE);
        assertThat(converter.convert("LINK_ONLY")).isEqualTo(CourseAccessType.LINK_ONLY);
    }

    @Test
    void mapsLegacyOpenToPublic() {
        assertThat(converter.convert("OPEN")).isEqualTo(CourseAccessType.PUBLIC);
        assertThat(converter.convert("open")).isEqualTo(CourseAccessType.PUBLIC);
    }

    @Test
    void mapsLegacyCodeAndInviteToLinkOnly() {
        assertThat(converter.convert("CODE")).isEqualTo(CourseAccessType.LINK_ONLY);
        assertThat(converter.convert("code")).isEqualTo(CourseAccessType.LINK_ONLY);
        assertThat(converter.convert("INVITE")).isEqualTo(CourseAccessType.LINK_ONLY);
        assertThat(converter.convert("invite")).isEqualTo(CourseAccessType.LINK_ONLY);
        assertThat(converter.convert("LINK")).isEqualTo(CourseAccessType.LINK_ONLY);
    }

    @Test
    void mapsNullAndUnknownToPublicDefault() {
        assertThat(converter.convert(null)).isEqualTo(CourseAccessType.PUBLIC);
        assertThat(converter.convert("")).isEqualTo(CourseAccessType.PUBLIC);
        assertThat(converter.convert("   ")).isEqualTo(CourseAccessType.PUBLIC);
        assertThat(converter.convert("UNKNOWN_TYPE")).isEqualTo(CourseAccessType.PUBLIC);
    }
}
