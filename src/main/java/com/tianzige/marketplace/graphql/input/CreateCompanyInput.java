package com.tianzige.marketplace.graphql.input;

import lombok.Data;

import java.util.Map;

@Data
public class CreateCompanyInput {
    private String ticker;
    private String cik;
    private String name;
    private String exchange;
    private String sector;
    private String industry;
    private String sic;
    private String countryOfIncorporation;
    private Map<String, Object> metadata;
}
