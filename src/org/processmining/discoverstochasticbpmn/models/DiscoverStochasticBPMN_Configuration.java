package org.processmining.discoverstochasticbpmn.models;

public class DiscoverStochasticBPMN_Configuration {
    public static enum typeValue {
        calculationType_PERFECTLYFIT,
        calculationType_SYNCHRONOUS,
        calculationType_ALL;
    }

    public typeValue calculateProbabilityUsing;

    public DiscoverStochasticBPMN_Configuration() {
        this.calculateProbabilityUsing = typeValue.calculationType_PERFECTLYFIT;
    }
}
