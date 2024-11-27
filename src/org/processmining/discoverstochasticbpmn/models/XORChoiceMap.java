package org.processmining.discoverstochasticbpmn.models;

import org.processmining.models.graphbased.directed.bpmn.BPMNEdge;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import java.util.HashMap;
import java.util.Map;

public class XORChoiceMap {
    private double total = 0;

    private final Map<BPMNEdge<BPMNNode, BPMNNode>, TransitionCounts> edgeToTransitionMap = new HashMap<>();

    public void addChoice(BPMNEdge<BPMNNode, BPMNNode> edge, Transition transition, double count) {
        edgeToTransitionMap.put(edge, new TransitionCounts(transition, count));
    }

    public TransitionCounts getTransitionCounts(BPMNEdge<BPMNNode, BPMNNode> edge) {
        return edgeToTransitionMap.get(edge);
    }

    public Map<BPMNEdge<BPMNNode, BPMNNode>, TransitionCounts> getAllChoices() {
        return edgeToTransitionMap;
    }

    public double getTotal(){
        return this.total;
    }

    public void updateTotal(){
        for(TransitionCounts transitionCounts : edgeToTransitionMap.values()){
            this.total += transitionCounts.getCount();
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
        private double count;
        private double probability;

        public TransitionCounts(Transition transition, double count) {
            this.transition = transition;
            this.count = count;
            this.probability = 0;
        }

        public Transition getTransition() {
            return transition;
        }

        public double getCount() {
            return count;
        }

        public double getProbability(){
            return this.probability;
        }

        public void setTransition(Transition t){
            this.transition = t;
        }

        public void setCount(double c){
            this.count = c;
        }

        public void setProbability(double total){
            this.probability = this.count / total;
        }
    }
}
