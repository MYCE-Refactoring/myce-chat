package com.myce.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberInfo {
    private String name;
    private String role;
    private String grade;
}
