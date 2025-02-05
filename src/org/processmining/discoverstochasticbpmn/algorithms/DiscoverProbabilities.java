package org.processmining.discoverstochasticbpmn.algorithms;

import org.deckfour.xes.model.XLog;
import org.processmining.discoverstochasticbpmn.models.DiscoverStochasticBPMN_Configuration;
import org.processmining.discoverstochasticbpmn.models.XORChoiceMap;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.elements.Gateway;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.converters.bpmn2pn.BPMN2PetriNetConverter_Configuration;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogNotFiltered;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DiscoverProbabilities {
    protected BPMNDiagram bpmn;
    protected XLog log;

    protected Map<Gateway, XORChoiceMap> gatewayMap;

    protected Petrinet net;
    protected Marking m;
    protected Marking mf;
    List<Place> finalPlaces;

    IvMLogNotFiltered alignedLog;
    IvMModel model;

    protected List<String> warnings = new ArrayList<String>();
    protected List<String> errors = new ArrayList<String>();

    protected DiscoverStochasticBPMN_Configuration config;

    public DiscoverProbabilities(BPMNDiagram bpmn, XLog log, DiscoverStochasticBPMN_Configuration config) {
        this.bpmn = bpmn;
        this.log = log;
        this.config = config;
    }

    public DiscoverProbabilities(BPMNDiagram bpmn, XLog log) {
        this(bpmn, log, new DiscoverStochasticBPMN_Configuration());
    }

    public boolean discover() {
        if(convertToPetrinet()){
            if(getAlignedLog()){
                GatewayProbabilityCalculator calc = new GatewayProbabilityCalculator(gatewayMap, alignedLog, model, config);
                calc.calculateXORProbabilities();
                gatewayMap = calc.getGatewayMap();
            }
        }
        return errors.isEmpty();
    }


    private boolean convertToPetrinet() {
        BPMN2PetriNetConverter_Configuration conv_config = new BPMN2PetriNetConverter_Configuration();
        conv_config.labelNodesWith = BPMN2PetriNetConverter_Configuration.LabelValue.PREFIX_NONTASK_BY_BPMN_TYPE;
        BPMN2PetriNetConverterExtension conv = new BPMN2PetriNetConverterExtension(bpmn, conv_config);
        boolean result = conv.convert();
        if(!result){
            errors.add("Failed to convert BPMN to Petri net");
            errors.addAll(conv.getErrors());
        }

        net = conv.getPetriNet();
        m = conv.getMarking();
        finalPlaces = conv.getFinalPlaces();
        gatewayMap = conv.getGatewayMap();

        if (finalPlaces.size() == 1) {
            mf = new Marking(finalPlaces);
        } else {
            warnings.add("More than 1 final place, could not construct generic final marking.");
        }

        return result;
    }

    private boolean getAlignedLog() {
        try {
            model = AlignmentUtil.getIvMModel(net, m, mf);
            alignedLog = AlignmentUtil.alignPetriNetWithLog(model, log);
            return true;
        } catch (Exception e) {
            errors.add("Failed to align the log to the petri net");
            errors.add("Alignment calculation failed due to an exception: " + e.getMessage());
            return false;
        }
    }

    public Map<Gateway, XORChoiceMap> getGatewayMap(){
        return this.gatewayMap;
    }

    public Petrinet getPetrinet() { return net; }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getErrors() {
        return errors;
    }
}
