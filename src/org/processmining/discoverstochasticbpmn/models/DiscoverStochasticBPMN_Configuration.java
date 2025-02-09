package org.processmining.discoverstochasticbpmn.models;

public class DiscoverStochasticBPMN_Configuration {
    public enum typeValue {
        calculationType_PERFECTLYFIT,
        calculationType_SYNCHRONOUS,
        calculationType_ALL
    }

    public typeValue calculateProbabilityUsing;

    public DiscoverStochasticBPMN_Configuration() {
        this.calculateProbabilityUsing = typeValue.calculationType_PERFECTLYFIT;
    }

    public DiscoverStochasticBPMN_Configuration(typeValue strategy) {
        this.calculateProbabilityUsing = strategy;
    }
}
