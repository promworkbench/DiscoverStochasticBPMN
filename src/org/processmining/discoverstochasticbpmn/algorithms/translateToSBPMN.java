package org.processmining.discoverstochasticbpmn.algorithms;

import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNEdge;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.bpmn.elements.*;
import org.processmining.discoverstochasticbpmn.models.XORChoiceMap;
import org.processmining.stochasticbpmn.models.graphbased.directed.bpmn.stochastic.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class translateToSBPMN {
    private final BPMNDiagram bpmn;
    private final Map<Gateway, XORChoiceMap> gatewayMap;
    private final StochasticBPMNDiagram stochasticBPMN;
    private final Map<Gateway, Gateway> stochasticGatewayMap = new HashMap<>();
    private final Map<BPMNEdge, Flow> stochasticFlowMap = new HashMap<>();
    private final Map<Activity, Activity> stochasticActivityMap = new HashMap<>();
    private final Map<Event, Event> stochasticEventMap = new HashMap<>();
    private final Map<TextAnnotation, TextAnnotation> stochasticTextAnnotationMap = new HashMap<>();


    public translateToSBPMN(BPMNDiagram bpmn, Map<Gateway, XORChoiceMap> gatewayMap){
        this.bpmn = bpmn;
        this.gatewayMap = gatewayMap;
        this.stochasticBPMN = new StochasticBPMNDiagramImpl(this.bpmn.getLabel());
    }

    public StochasticBPMNDiagram createSBPMN(){
        translateActivities();
        translateEvents();
        translateTextAnnotations();
        translateGateways();
        translateFlows();
        linkFlowsToGateways();

        printExistingNodes();

        return stochasticBPMN;
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
            BPMNNode source = getStochasticNode(flow.getSource());
            BPMNNode target = getStochasticNode(flow.getTarget());

            if (source instanceof Gateway && ((Gateway) source).getGatewayType() == Gateway.GatewayType.DATABASED) {
                System.out.println("This is a stochastic flow: " + flow.getEdgeID());
                StochasticFlow stochasticFlow = stochasticBPMN.addStochasticFlow(source, target, flow.getLabel());
                stochasticFlowMap.put(flow, stochasticFlow);
            } else {
                System.out.println("This is a normal flow: " + flow.getEdgeID());
                Flow stochasticFlow = stochasticBPMN.addFlow(source, target, flow.getLabel());
                stochasticFlowMap.put(flow, stochasticFlow);
            }
        }
    }

    private void linkFlowsToGateways() {
        for (Gateway gateway : bpmn.getGateways()) {
            XORChoiceMap choiceMap = gatewayMap.get(gateway);
            StochasticGateway stochasticGateway = (StochasticGateway) stochasticGatewayMap.get(gateway);
                if (choiceMap != null) {
                    for (BPMNEdge<BPMNNode, BPMNNode> edge : choiceMap.getAllChoices().keySet()) {
                        BigDecimal weight = choiceMap.getTransitionCounts(edge).getCount();
                        System.out.println("Calculated weight: " + weight);
                        StochasticFlow stochasticFlow = (StochasticFlow) stochasticFlowMap.get(edge);
                        StochasticGatewayFlowSet flowSet = new StochasticGatewayFlowSet(edge.getEdgeID().toString());
                        System.out.println("Edge ID of flow: " + edge.getEdgeID().toString());
                        System.out.println("Edge ID with his stupid method: " + edge.getAttributeMap().get("Original id").toString());
                        stochasticGateway.getWeightedFlow().assignFlowWeight(weight, flowSet);
                        System.out.println(stochasticGateway.getWeight(stochasticFlow));
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
        return null; // Return null if the node type is not recognized
    }

    private void printExistingNodes() {
        Set<BPMNNode> existingNodes = stochasticBPMN.getNodes();
        for (BPMNNode existingNode : existingNodes) {
            System.out.println(existingNode.getLabel());
        }
    }
}
