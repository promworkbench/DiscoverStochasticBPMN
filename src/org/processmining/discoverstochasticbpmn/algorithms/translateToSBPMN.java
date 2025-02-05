package org.processmining.discoverstochasticbpmn.algorithms;

import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNEdge;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.bpmn.elements.*;
import org.processmining.discoverstochasticbpmn.models.XORChoiceMap;
import org.processmining.stochasticbpmn.models.graphbased.directed.bpmn.stochastic.*;
import org.processmining.stochasticbpmn.models.stochastic.Probability;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class translateToSBPMN {
    private final BPMNDiagram bpmn;
    private final Map<Gateway, XORChoiceMap> gatewayMap;
    private final String option;

    private final StochasticBPMNDiagram stochasticBPMN;

    private final Map<Gateway, Gateway> stochasticGatewayMap = new HashMap<>();
    private final Map<BPMNEdge, Flow> stochasticFlowMap = new HashMap<>();
    private final Map<Activity, Activity> stochasticActivityMap = new HashMap<>();
    private final Map<Event, Event> stochasticEventMap = new HashMap<>();
    private final Map<TextAnnotation, TextAnnotation> stochasticTextAnnotationMap = new HashMap<>();


    public translateToSBPMN(BPMNDiagram bpmn, Map<Gateway, XORChoiceMap> gatewayMap, String option){
        this.bpmn = bpmn;
        this.gatewayMap = gatewayMap;
        this.option = option;
        this.stochasticBPMN = new StochasticBPMNDiagramImpl(this.bpmn.getLabel());
    }

    public void createSBPMN(){
        translateActivities();
        translateEvents();
        translateTextAnnotations();
        translateGateways();
        translateFlows();
        linkFlowsToGateways();

//        printExistingNodes();
    }

    private void translateActivities() {
        for (Activity activity : bpmn.getActivities()) {
            Activity stochasticActivity = stochasticBPMN.addActivity(
                    activity.getLabel(),
                    activity.isBLooped(),
                    activity.isBAdhoc(),
                    activity.isBCompensation(),
                    activity.isBMultiinstance(),
                    activity.isBCollapsed()
            );
            stochasticActivityMap.put(activity, stochasticActivity);
        }
    }

    private void translateEvents() {
        for (Event event : bpmn.getEvents()) {
            Event stochasticEvent = stochasticBPMN.addEvent(
                    event.getLabel(),
                    event.getEventType(),
                    event.getEventTrigger(),
                    event.getEventUse(),
                    Boolean.parseBoolean(event.isInterrupting()),
                    null
            );
            stochasticEventMap.put(event, stochasticEvent);
        }
    }

    private void translateTextAnnotations() {
        for (TextAnnotation textAnnotation : bpmn.getTextAnnotations()) {
            TextAnnotation stochasticTextAnnotation = stochasticBPMN.addTextAnnotation(textAnnotation.getLabel());
            stochasticTextAnnotationMap.put(textAnnotation, stochasticTextAnnotation);
        }
    }

    private void translateGateways() {
        // If the gateway is XOR, add a stochastic gateway, else add the original gateway
        for (Gateway gateway : bpmn.getGateways()) {
            if (gateway.getGatewayType() == Gateway.GatewayType.DATABASED) {
                StochasticGatewayWeightedFlow weightedFlow = new StochasticGatewayWeightedFlow();
                StochasticGateway stochasticGateway = stochasticBPMN.addStochasticGateway(
                        gateway.getLabel(), gateway.getGatewayType(), weightedFlow
                );
                stochasticGatewayMap.put(gateway, stochasticGateway);
            } else {
                Gateway stochasticGateway = stochasticBPMN.addGateway(gateway.getLabel(), gateway.getGatewayType());
                stochasticGatewayMap.put(gateway, stochasticGateway);
            }
        }
    }

    private void translateFlows() {
        for (BPMNEdge<BPMNNode, BPMNNode> flow : bpmn.getFlows()) {
            // Get the corresponding stochastic source and target nodes for the edge
            BPMNNode source = getStochasticNode(flow.getSource());
            BPMNNode target = getStochasticNode(flow.getTarget());
            // If the source is an XOR gateway, add a stochastic flow, else add the original flow
            if (source instanceof Gateway && ((Gateway) source).getGatewayType() == Gateway.GatewayType.DATABASED) {
                StochasticFlow stochasticFlow = stochasticBPMN.addStochasticFlow(source, target, flow.getLabel());
                stochasticFlowMap.put(flow, stochasticFlow);
            } else {
                Flow stochasticFlow = stochasticBPMN.addFlow(source, target, flow.getLabel());
                stochasticFlowMap.put(flow, stochasticFlow);
            }
        }
    }

    private void linkFlowsToGateways() {
        for (Gateway gateway : bpmn.getGateways()) {
            XORChoiceMap choiceMap = gatewayMap.get(gateway);
            StochasticGateway stochasticGateway = (StochasticGateway) getStochasticNode(gateway);
                if (choiceMap != null) {
                    StochasticGatewayWeightedFlow weightedFlow = stochasticGateway != null ? stochasticGateway.getWeightedFlow() : null;
                    // For each outgoing edge of the gateway, add a flow weight to the weightedFlow
                    for (BPMNEdge<BPMNNode, BPMNNode> edge : choiceMap.getAllChoices().keySet()) {
                        StochasticFlow stochasticFlow = (StochasticFlow) stochasticFlowMap.get(edge);
                        String id = UUID.randomUUID().toString();
                        stochasticFlow.getAttributeMap().put("Original id", id);
                        BigDecimal weight = choiceMap.getTransitionCounts(edge).getCount();
                        Probability probability = choiceMap.getTransitionCounts(edge).getProbability();
                        StochasticGatewayFlowSet flowSet = new StochasticGatewayFlowSet(id);
                        if (weightedFlow != null) {
                            if(option.equals("weight")) {
                                weightedFlow.assignFlowWeight(weight, flowSet);
                            } else if (option.equals("probability")) {
                                weightedFlow.assignFlowWeight(probability.getValue(), flowSet);
                            }
                        }
                        stochasticFlow.setLabel(stochasticFlow.getLabel());
                    }
                }
        }
    }

    private BPMNNode getStochasticNode(BPMNNode node) {
        if (node instanceof Activity) {
            return stochasticActivityMap.get(node);
        } else if (node instanceof Event) {
            return stochasticEventMap.get(node);
        } else if (node instanceof TextAnnotation) {
            return stochasticTextAnnotationMap.get(node);
        }else if (node instanceof Gateway) {
            return stochasticGatewayMap.get(node);
        }
        return null;
    }

    public StochasticBPMNDiagram getSBPMN() { return stochasticBPMN; }

    private void printExistingNodes() {
        Set<BPMNNode> existingNodes = stochasticBPMN.getNodes();
        for (BPMNNode existingNode : existingNodes) {
            System.out.println(existingNode.getLabel());
        }
    }
}
