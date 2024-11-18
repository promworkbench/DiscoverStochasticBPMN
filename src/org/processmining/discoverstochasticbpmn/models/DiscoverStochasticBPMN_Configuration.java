package org.processmining.newpackageivy.models;

public class DiscoverStochasticBPMN_Configuration {
//    public DiscoverStochasticBPMN_Configuration.typeValue calculateProbabilityUsing;
//
//    public DiscoverStochasticBPMN_Configuration() {
//        this.calculateProbabilityUsing = DiscoverStochasticBPMN_Configuration.typeValue.calculationType_PERFECTLYFIT;
//    }

    public static enum typeValue {
        calculationType_PERFECTLYFIT,
        calculationType_SYNCHRONOUS,
        calculationType_ALL;
    }

    public typeValue calculateProbabilityUsing = typeValue.calculationType_PERFECTLYFIT;
}
