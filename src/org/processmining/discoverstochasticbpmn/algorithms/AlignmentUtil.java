package org.processmining.discoverstochasticbpmn.algorithms;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.acceptingpetrinet.models.AcceptingPetriNet;
import org.processmining.acceptingpetrinet.models.impl.AcceptingPetriNetImpl;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.ProMCanceller;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.inductiveVisualMiner.alignment.AlignmentComputerImpl;
import org.processmining.plugins.inductiveVisualMiner.alignment.AlignmentPerformance;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.decoration.IvMDecoratorDefault;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogNotFiltered;
import org.processmining.plugins.inductiveVisualMiner.performance.XEventPerformanceClassifier;
import org.deckfour.xes.classification.XEventClasses;

public class AlignmentUtil {
    public static IvMLogNotFiltered alignPetriNetWithLog(PluginContext context, IvMModel model, XLog log) throws Exception {
        XEventNameClassifier classifier = new XEventNameClassifier();
        XEventPerformanceClassifier performanceClassifier = new XEventPerformanceClassifier(classifier);
        XEventClasses activityEventClasses = XEventClasses.deriveEventClasses(classifier, log);
        XEventClasses performanceEventClasses = XEventClasses.deriveEventClasses(performanceClassifier, log);
        AlignmentComputerImpl alignmentComputer = new AlignmentComputerImpl();
        ProMCanceller canceller = () -> context.getProgress().isCancelled();
            return AlignmentPerformance.alignNet(alignmentComputer, model, performanceClassifier, log,
            activityEventClasses, performanceEventClasses, canceller, new IvMDecoratorDefault());
    }

    public static IvMModel getIvMModel(Petrinet petriNet, Marking initialMarking, Marking finalMarking) {
        AcceptingPetriNet acceptingPetriNet = new AcceptingPetriNetImpl(petriNet);

        return new IvMModel(acceptingPetriNet);
    }
}