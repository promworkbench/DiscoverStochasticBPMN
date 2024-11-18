package org.processmining.newpackageivy.algorithms;

import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNEdge;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.bpmn.elements.*;
import org.processmining.newpackageivy.models.XORChoiceMap;
import org.processmining.stochasticbpmn.models.graphbased.directed.bpmn.stochastic.*;

import java.util.Map;

public class GetSBPMN {
    public static void createSBPMN(BPMNDiagram bpmn, Map<Gateway, XORChoiceMap> gatewayMap){
        StochasticBPMNDiagramImpl stochasticBPMN = new StochasticBPMNDiagramImpl(bpmn.getLabel());

        // Convert Gateways
        for (Gateway gateway : bpmn.getGateways()) {
            if (gateway.getGatewayType() == Gateway.GatewayType.DATABASED) {
                StochasticGatewayWeightedFlow weightedFlow = new StochasticGatewayWeightedFlow();
                XORChoiceMap choiceMap = gatewayMap.get(gateway);
                if (choiceMap != null) {
                    for (BPMNEdge<BPMNNode, BPMNNode> edge : choiceMap.getAllChoices().keySet()) {
                        Double weight = choiceMap.getTransitionCounts(edge).getCount();
                        StochasticGatewayFlowSet flowSet = new StochasticGatewayFlowSet(edge.getLabel());
                        weightedFlow.assignFlowWeight(weight, flowSet);
                    }
                }
                StochasticGateway stochasticGateway = stochasticBPMN.addStochasticGateway(
                        gateway.getLabel(), gateway.getGatewayType(), weightedFlow);
            }
        }

        // Convert Flows
        for (BPMNEdge<BPMNNode, BPMNNode> flow : bpmn.getFlows()) {
            StochasticFlow stochasticFlow = stochasticBPMN.addStochasticFlow(
                    flow.getSource(), flow.getTarget(), flow.getLabel());
        }

        // Add activities
        for (Activity activity : bpmn.getActivities()) {
            stochasticBPMN.addActivity(
                    activity.getLabel(),
                    activity.isBLooped(),
                    activity.isBAdhoc(),
                    activity.isBCompensation(),
                    activity.isBMultiinstance(),
                    activity.isBCollapsed()
            );
        }

        // Add subprocesses
        for (SubProcess subProcess : bpmn.getSubProcesses()) {
            stochasticBPMN.addSubProcess(
                    subProcess.getLabel(),
                    subProcess.isBLooped(),
                    subProcess.isBAdhoc(),
                    subProcess.isBCompensation(),
                    subProcess.isBMultiinstance(),
                    subProcess.isBCollapsed()
            );
        }

        // Add events
        for (Event event : bpmn.getEvents()) {
            stochasticBPMN.addEvent(
                    event.getLabel(),
                    event.getEventType(),
                    event.getEventTrigger(),
                    event.getEventUse(),
                    Boolean.parseBoolean(event.isInterrupting()),
                    null
            );
        }

        // Add text annotations
        for (TextAnnotation textAnnotation : bpmn.getTextAnnotations()) {
            stochasticBPMN.addTextAnnotation(textAnnotation.getLabel());
        }
    }
}
