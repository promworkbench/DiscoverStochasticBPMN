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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.processmining.discoverstochasticbpmn.models.DiscoverStochasticBPMN_Configuration.typeValue.calculationType_PERFECTLYFIT;
import static org.processmining.discoverstochasticbpmn.models.DiscoverStochasticBPMN_Configuration.typeValue.calculationType_SYNCHRONOUS;

public class GatewayProbabilityCalculator {

    private Map<Gateway, XORChoiceMap> gatewayMap = new HashMap<Gateway, XORChoiceMap>();

    public GatewayProbabilityCalculator(Map<Gateway, XORChoiceMap> gatewayMap){
        this.gatewayMap = gatewayMap;
    }

    public Map<Gateway, XORChoiceMap> calculateXORProbabilities(
            IvMLogNotFiltered alignedLog, IvMModel model, DiscoverStochasticBPMN_Configuration config) {

//        System.out.println("Successfully entered the method calculateXORProbabilities");
        DiscoverStochasticBPMN_Configuration.typeValue strategy = config.calculateProbabilityUsing;

        int traceIndex = 0;
        for (IvMTrace ivMTrace : alignedLog) {
            traceIndex += 1;
            if (includeTrace(ivMTrace, strategy)) {
                System.out.println("Trace " + traceIndex + ": " + Arrays.toString(ivMTrace.toArray()));
                for (IvMMove move : ivMTrace) {
                    if(!move.isLogMove()){
//                    System.out.println(model.getNetTransition(move.getTreeNode()).getLabel());
                        updateGatewayCounts(gatewayMap, model, ivMTrace, move, strategy);
                    }
                }
            }
        }
        updateGatewayProbabilities(gatewayMap);
        return gatewayMap;
    }

    public void updateGatewayCounts(Map<Gateway, XORChoiceMap> gatewayMap, IvMModel model, IvMTrace trace, IvMMove move, DiscoverStochasticBPMN_Configuration.typeValue strategy){
//        System.out.println("Successfully entered the method updateGatewayCounts");
        Transition t = model.getNetTransition(move.getTreeNode());
        for(XORChoiceMap choiceMap : gatewayMap.values()) {
            for (BPMNEdge<BPMNNode, BPMNNode> edge : choiceMap.getAllChoices().keySet()) {
                XORChoiceMap.TransitionCounts transitionCounts = choiceMap.getTransitionCounts(edge);
                if(transitionCounts.getTransition().getId() == t.getId()){
//                    System.out.println("Found a choice transition");
                    if(strategy.equals(calculationType_SYNCHRONOUS)){
                        int index = move.getIndexInAlignedTrace()+1;
                        while(model.getNetTransition(trace.get(index).getTreeNode()).isInvisible())
                            index += 1;
                        index += 2;
//                        System.out.println("Came out of while, index to be checked: " + index);
                        if(!trace.get(index).isModelMove())
                            incrementCounts(transitionCounts);
                    }
                    else incrementCounts(transitionCounts);
                }
            }
        }
    }

    public void incrementCounts(XORChoiceMap.TransitionCounts transitionCounts){
//        System.out.println("Successfully entered the method incrementCounts");
        transitionCounts.setCount(transitionCounts.getCount() + 1);
    }

    private void updateGatewayProbabilities(Map<Gateway, XORChoiceMap> gatewayMap) {
//        System.out.println("Successfully entered the method updateGatewayProbabilities");
        int k = 1;
        for (XORChoiceMap choiceMap : gatewayMap.values()) {
            choiceMap.updateTotal();
            choiceMap.updateProbabilities();
            System.out.println("Updated probabilities for gateway " + k + ": Total=" + choiceMap.getTotal());
            k += 1;
        }
    }

    public boolean includeTrace(IvMTrace trace, DiscoverStochasticBPMN_Configuration.typeValue strategy){
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