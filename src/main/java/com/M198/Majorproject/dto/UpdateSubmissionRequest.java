package com.M198.Majorproject.dto;

import jakarta.validation.constraints.Size;

import com.M198.Majorproject.entity.submission.SubmissionStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSubmissionRequest {

    @Size(max = 20000)
    private String answerText;

    private SubmissionStatus status;
}
