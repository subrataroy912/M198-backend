package com.M198.Majorproject.core.coursework.dto;

import jakarta.validation.constraints.Size;

import com.M198.Majorproject.core.coursework.entity.SubmissionStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSubmissionRequest {

    @Size(max = 20000)
    private String answerText;

    private SubmissionStatus status;
}
