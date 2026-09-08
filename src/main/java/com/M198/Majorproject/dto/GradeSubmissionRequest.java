package com.M198.Majorproject.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GradeSubmissionRequest {

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal score;

    @Size(max = 10000)
    private String feedback;
}
