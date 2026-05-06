package com.necronet.userregistryms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleRequest {

    @NotBlank(message = "Role name is required")
    private String name;

    @NotBlank(message = "Description is required")
    private String description;

    private Long levelRole;
    private Boolean isSystem = false;
}
