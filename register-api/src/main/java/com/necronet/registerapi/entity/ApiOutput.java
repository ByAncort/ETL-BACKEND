package com.necronet.registerapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "api_outputs")
public class ApiOutput {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "expected_schema", columnDefinition = "TEXT")
    private String expectedSchema;

    @Column(name = "ml_mapped_schema", columnDefinition = "TEXT")
    private String mlMappedSchema;

    @Column(name = "target_table_name", length = 100)
    private String targetTableName;

    @Column(name = "response_data_path", length = 200)
    private String responseDataPath; // JSONPath para extraer datos

    @Column(name = "success_condition", length = 200)
    private String successCondition;

    @Column(name = "error_path", length = 200)
    private String errorPath;

    @Column(name = "transformation_rules", columnDefinition = "TEXT")
    private String transformationRules;

    @Column(name = "field_mappings", columnDefinition = "TEXT")
    private String fieldMappings;

    @Enumerated(EnumType.STRING)
    @Column(name = "output_format")
    private OutputFormat outputFormat = OutputFormat.JSON;

    @Column(name = "batch_size")
    private Integer batchSize = 100;

    @OneToOne(mappedBy = "apiOutput")
    private IntegrationApis integrationApi;

    public enum OutputFormat {
        JSON, XML, CSV, PROTOBUF, AVRO
    }
}