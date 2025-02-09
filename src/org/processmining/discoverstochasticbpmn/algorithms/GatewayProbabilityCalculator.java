package org.processmining.discoverstochasticbpmn.algorithms;

import org.processmining.discoverstochasticbpmn.models.DiscoverStochasticBPMN_Configuration;
import org.processmining.models.graphbased.directed.bpmn.BPMNEdge;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.bpmn.elements.Gateway;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.discoverstochasticbpmn.models.XORChoiceMap;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogNotFiltered;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMMove;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMTrace;

import java.util.Map;

import static org.processmining.discoverstochasticbpmn.models.DiscoverStochasticBPMN_Configuration.typeValue.calculationType_PERFECTLYFIT;
import static org.processmining.discoverstochasticbpmn.models.DiscoverStochasticBPMN_Configuration.typeValue.calculationType_SYNCHRONOUS;

public class GatewayProbabilityCalculator {

    private Map<Gateway, XORChoiceMap> gatewayMap;
    private final DiscoverStochasticBPMN_Configuration.typeValue strategy;
    private final IvMLogNotFiltered alignedLog;
    private final IvMModel model;

    public GatewayProbabilityCalculator(Map<Gateway, XORChoiceMap> gatewayMap, IvMLogNotFiltered alignedLog, IvMModel model, DiscoverStochasticBPMN_Configuration config){
        this.gatewayMap = gatewayMap;
        this.alignedLog = alignedLog;
        this.model = model;
        this.strategy = config.calculateProbabilityUsing;
    }

    public void calculateXORProbabilities() {

//        System.out.println("Successfully entered the method calculateXORProbabilities");

        int traceIndex = 0;
        for (IvMTrace ivMTrace : alignedLog) {
            traceIndex += 1;
            if (includeTrace(ivMTrace)) {
//                System.out.println("Trace " + traceIndex + ": " + Arrays.toString(ivMTrace.toArray()));
                for (IvMMove move : ivMTrace) {
                    if(!move.isLogMove()){
//                    System.out.println(model.getNetTransition(move.getTreeNode()).getLabel());
                        updateGatewayCounts(ivMTrace, move);
                    }
                }
            }
        }
        updateGatewayProbabilities();
    }

    public void updateGatewayCounts(IvMTrace trace, IvMMove move){
//        System.out.println("Successfully entered the method updateGatewayCounts");
        Transition t = model.getNetTransition(move.getTreeNode());
        for(XORChoiceMap choiceMap : gatewayMap.values()) {
            for (BPMNEdge<BPMNNode, BPMNNode> edge : choiceMap.getAllChoices().keySet()) {
                XORChoiceMap.TransitionCounts transitionCounts = choiceMap.getTransitionCounts(edge);
                if(transitionCounts.getTransition().getId() == t.getId()){
//                    System.out.println("Found a choice transition");
                    if(strategy.equals(calculationType_SYNCHRONOUS)){
                        int index = move.getIndexInAlignedTrace()+1;
                        while(index<trace.size() && (trace.get(index).isLogMove() || trace.get(index).isIgnoredModelMove() || model.getNetTransition(trace.get(index).getTreeNode()).isInvisible())) {
                            index += 1;
                        }
                        if(index == trace.size() || !trace.get(index).isModelMove())
                            transitionCounts.incrementCounts();
                    }
                    else transitionCounts.incrementCounts();
                }
            }
        }
    }

    private void updateGatewayProbabilities() {
//        System.out.println("Successfully entered the method updateGatewayProbabilities");
        int k = 1;
        for (XORChoiceMap choiceMap : gatewayMap.values()) {
            choiceMap.updateTotal();
            choiceMap.updateProbabilities();
//            System.out.println("Updated probabilities for gateway " + k + ": Total=" + choiceMap.getTotal());
            k += 1;
        }
    }

    public boolean includeTrace(IvMTrace trace){
        if(strategy.equals(calculationType_PERFECTLYFIT))
            return isPerfectlyFit(trace);
        return true;
    }

    public boolean isPerfectlyFit(IvMTrace trace) {
        for (IvMMove move : trace) {
            if (!move.isSyncMove()) { return false; }
        }
        return true;
    }

    public Map<Gateway, XORChoiceMap> getGatewayMap(){
        return this.gatewayMap;
    }
}