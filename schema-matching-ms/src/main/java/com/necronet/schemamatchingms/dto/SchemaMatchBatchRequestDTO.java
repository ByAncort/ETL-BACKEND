package com.necronet.schemamatchingms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class SchemaMatchBatchRequestDTO {

    @NotEmpty(message = "Matches list cannot be empty")
    @Valid
    private List<SchemaMatchRequestDTO> matches;

    public List<SchemaMatchRequestDTO> getMatches() { return matches; }
    public void setMatches(List<SchemaMatchRequestDTO> matches) { this.matches = matches; }
}
