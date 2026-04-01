package com.tianzige.marketplace.graphql.input;

import lombok.Data;

@Data
public class CompanyFilter {
    private String ticker;
    private String cik;
    private String name;
    private String exchange;
    private String sector;
    private String industry;
}
