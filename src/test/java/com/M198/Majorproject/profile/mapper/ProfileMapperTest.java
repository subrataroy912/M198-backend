package com.M198.Majorproject.profile.mapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import com.M198.Majorproject.identity.entity.ProfileLink;
import com.M198.Majorproject.identity.entity.ProfileVisibility;
import com.M198.Majorproject.identity.entity.User;
import com.M198.Majorproject.identity.entity.UserProfile;
import com.M198.Majorproject.profile.dto.PublicUserProfileResponse;
import com.M198.Majorproject.profile.dto.UserProfileResponse;

class ProfileMapperTest {

    private ProfileMapper profileMapper;

    @BeforeEach
    void setUp() {
        profileMapper = Mappers.getMapper(ProfileMapper.class);
    }

    @Test
    void toOwnerResponse_MapsAllFieldsAndComputesCompositeFlags() {
        User user = User.builder()
                .id("u-101")
                .email("test@example.com")
                .isAdmin(false)
                .canCreateCourses(false)
                .build();

        UserProfile profile = UserProfile.builder()
                .userId("u-101")
                .handle("coder_subrata")
                .firstName("Subrata")
                .lastName("Roy")
                .displayName("Subrata Roy")
                .avatarUrl("https://example.com/avatar.jpg")
                .bannerUrl("https://example.com/banner.jpg")
                .headline("Software Engineer")
                .about("Full stack developer")
                .city("Siliguri")
                .country("India")
                .phone("+911234567890")
                .gender("Male")
                .dateOfBirth("2000-01-01")
                .address("Hill Cart Road")
                .profileVisibility(ProfileVisibility.PUBLIC)
                .isAdmin(true)
                .canCreateCourses(true)
                .links(List.of(
                        ProfileLink.builder().name("GitHub").url("https://github.com/subrataroy912").build(),
                        ProfileLink.builder().name("LinkedIn").url("https://linkedin.com/in/subrataroy").build()
                ))
                .build();

        UserProfileResponse response = profileMapper.toOwnerResponse(user, profile);

        assertNotNull(response);
        assertEquals("u-101", response.getId());
        assertEquals("test@example.com", response.getEmail());
        assertTrue(response.isAdmin(), "isAdmin should be true since profile.isAdmin is true");
        assertTrue(response.isCanCreateCourses(), "canCreateCourses should be true since profile.canCreateCourses is true");
        assertEquals("coder_subrata", response.getHandle());
        assertEquals("Subrata", response.getFirstName());
        assertEquals("Roy", response.getLastName());
        assertEquals("Subrata Roy", response.getDisplayName());
        assertEquals("https://example.com/avatar.jpg", response.getAvatarUrl());
        assertEquals("https://example.com/banner.jpg", response.getBannerUrl());
        assertEquals("Software Engineer", response.getHeadline());
        assertEquals("Full stack developer", response.getAbout());
        assertEquals("Siliguri", response.getCity());
        assertEquals("India", response.getCountry());
        assertEquals("+911234567890", response.getPhone());
        assertEquals("Male", response.getGender());
        assertEquals("2000-01-01", response.getDateOfBirth());
        assertEquals("Hill Cart Road", response.getAddress());
        assertEquals(ProfileVisibility.PUBLIC, response.getProfileVisibility());
        assertNotNull(response.getLinks());
        assertEquals(2, response.getLinks().size());
        assertEquals("GitHub", response.getLinks().get(0).getName());
    }

    @Test
    void toOwnerResponse_UserFlagsTakePrecedenceWhenProfileFlagsFalse() {
        User user = User.builder()
                .id("u-102")
                .email("admin@example.com")
                .isAdmin(true)
                .canCreateCourses(true)
                .build();

        UserProfile profile = UserProfile.builder()
                .userId("u-102")
                .isAdmin(false)
                .canCreateCourses(false)
                .build();

        UserProfileResponse response = profileMapper.toOwnerResponse(user, profile);

        assertNotNull(response);
        assertTrue(response.isAdmin(), "isAdmin should be true because user.isAdmin is true");
        assertTrue(response.isCanCreateCourses(), "canCreateCourses should be true because user.canCreateCourses is true");
    }

    @Test
    void toOwnerResponse_BothFlagsFalse_ResultsInFalse() {
        User user = User.builder()
                .id("u-103")
                .email("user@example.com")
                .isAdmin(false)
                .canCreateCourses(false)
                .build();

        UserProfile profile = UserProfile.builder()
                .userId("u-103")
                .isAdmin(false)
                .canCreateCourses(false)
                .build();

        UserProfileResponse response = profileMapper.toOwnerResponse(user, profile);

        assertNotNull(response);
        assertFalse(response.isAdmin());
        assertFalse(response.isCanCreateCourses());
    }

    @Test
    void toOwnerResponse_NullLinks_ReturnsEmptyList() {
        User user = User.builder().id("u-104").build();
        UserProfile profile = UserProfile.builder().userId("u-104").links(null).build();

        UserProfileResponse response = profileMapper.toOwnerResponse(user, profile);

        assertNotNull(response);
        assertNotNull(response.getLinks());
        assertTrue(response.getLinks().isEmpty());
    }

    @Test
    void toOwnerResponse_NullInputs_ReturnsNull() {
        assertNull(profileMapper.toOwnerResponse(null, null));
    }

    @Test
    void toPublicResponse_MapsPublicFieldsCorrectly() {
        UserProfile profile = UserProfile.builder()
                .userId("u-201")
                .handle("public_user")
                .firstName("Jane")
                .lastName("Doe")
                .displayName("Jane Doe")
                .avatarUrl("https://example.com/jane.jpg")
                .bannerUrl("https://example.com/banner.jpg")
                .headline("Researcher")
                .about("AI Researcher")
                .city("Kolkata")
                .country("India")
                .profileVisibility(ProfileVisibility.PUBLIC)
                .canCreateCourses(true)
                .links(List.of(ProfileLink.builder().name("Site").url("https://jane.dev").build()))
                .build();

        PublicUserProfileResponse response = profileMapper.toPublicResponse(profile);

        assertNotNull(response);
        assertEquals("u-201", response.getId());
        assertEquals("public_user", response.getHandle());
        assertEquals("Jane", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals("Jane Doe", response.getDisplayName());
        assertEquals("https://example.com/jane.jpg", response.getAvatarUrl());
        assertEquals("https://example.com/banner.jpg", response.getBannerUrl());
        assertEquals("Researcher", response.getHeadline());
        assertEquals("AI Researcher", response.getAbout());
        assertEquals("Kolkata", response.getCity());
        assertEquals("India", response.getCountry());
        assertEquals(ProfileVisibility.PUBLIC, response.getProfileVisibility());
        assertTrue(response.isCanCreateCourses());
        assertEquals(1, response.getLinks().size());
        assertEquals("Jane Doe", response.getName());
    }

    @Test
    void toPublicResponse_NullLinks_ReturnsEmptyList() {
        UserProfile profile = UserProfile.builder().userId("u-202").links(null).build();

        PublicUserProfileResponse response = profileMapper.toPublicResponse(profile);

        assertNotNull(response);
        assertNotNull(response.getLinks());
        assertTrue(response.getLinks().isEmpty());
    }

    @Test
    void toPublicResponse_NullInput_ReturnsNull() {
        assertNull(profileMapper.toPublicResponse(null));
    }

    @Test
    void toPublicResponseList_MapsListCorrectly() {
        UserProfile p1 = UserProfile.builder().userId("u-1").displayName("User 1").build();
        UserProfile p2 = UserProfile.builder().userId("u-2").displayName("User 2").build();

        List<PublicUserProfileResponse> responses = profileMapper.toPublicResponseList(List.of(p1, p2));

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("u-1", responses.get(0).getId());
        assertEquals("u-2", responses.get(1).getId());
    }

    @Test
    void toPublicResponseList_NullList_ReturnsNull() {
        assertNull(profileMapper.toPublicResponseList(null));
    }
}
