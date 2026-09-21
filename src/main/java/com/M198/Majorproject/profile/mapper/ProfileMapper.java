package com.M198.Majorproject.profile.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.M198.Majorproject.identity.entity.ProfileLink;
import com.M198.Majorproject.identity.entity.User;
import com.M198.Majorproject.identity.entity.UserProfile;
import com.M198.Majorproject.profile.dto.PublicUserProfileResponse;
import com.M198.Majorproject.profile.dto.UserProfileResponse;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProfileMapper {

    @Mapping(target = "id", source = "profile.userId")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "admin", expression = "java((user != null && user.isAdmin()) || (profile != null && profile.isAdmin()))")
    @Mapping(target = "canCreateCourses", expression = "java((user != null && user.isCanCreateCourses()) || (profile != null && profile.isCanCreateCourses()))")
    @Mapping(target = "links", source = "profile.links")
    UserProfileResponse toOwnerResponse(User user, UserProfile profile);

    @Mapping(target = "id", source = "profile.userId")
    @Mapping(target = "canCreateCourses", source = "profile.canCreateCourses")
    @Mapping(target = "links", source = "profile.links")
    PublicUserProfileResponse toPublicResponse(UserProfile profile);

    List<PublicUserProfileResponse> toPublicResponseList(List<UserProfile> profiles);

    default List<ProfileLink> mapLinks(List<ProfileLink> links) {
        return links != null ? new ArrayList<>(links) : Collections.emptyList();
    }
}
