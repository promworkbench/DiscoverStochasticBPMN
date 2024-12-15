package org.processmining.discoverstochasticbpmn.models;

import org.processmining.models.graphbased.directed.bpmn.BPMNEdge;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class XORChoiceMap {
    private BigDecimal total = BigDecimal.ZERO;

    private final Map<BPMNEdge<BPMNNode, BPMNNode>, TransitionCounts> edgeToTransitionMap = new HashMap<>();

    public void addChoice(BPMNEdge<BPMNNode, BPMNNode> edge, Transition transition, BigDecimal count) {
        edgeToTransitionMap.put(edge, new TransitionCounts(transition, count));
    }

    public TransitionCounts getTransitionCounts(BPMNEdge<BPMNNode, BPMNNode> edge) {
        return edgeToTransitionMap.get(edge);
    }

    public Map<BPMNEdge<BPMNNode, BPMNNode>, TransitionCounts> getAllChoices() {
        return edgeToTransitionMap;
    }

    public BigDecimal getTotal(){
        return this.total;
    }

    public void updateTotal(){
        for(TransitionCounts transitionCounts : edgeToTransitionMap.values()){
            this.total = this.total.add(transitionCounts.getCount());
        }
    }

    public void updateProbabilities(){
//        System.out.println("Inside update probability");
        for(TransitionCounts transitionCounts : edgeToTransitionMap.values()){
            transitionCounts.setProbability(this.total);
//            System.out.println("Updated Probability: " + transitionCounts.getProbability());
        }
    }

    public static class TransitionCounts {
        private Transition transition;
        private BigDecimal count;
        private double probability;

        public TransitionCounts(Transition transition, BigDecimal count) {
            this.transition = transition;
            this.count = count;
            this.probability = 0;
        }

        public Transition getTransition() {
            return transition;
        }

        public BigDecimal getCount() {
            return count;
        }

        public double getProbability(){
            return this.probability;
        }

        public void setTransition(Transition t){
            this.transition = t;
        }

        public void setCount(BigDecimal c){
            this.count = c;
        }

        public void setProbability(BigDecimal total){
            this.probability = this.count.divide(total, 10, RoundingMode.HALF_UP).doubleValue();
        }
    }
}

