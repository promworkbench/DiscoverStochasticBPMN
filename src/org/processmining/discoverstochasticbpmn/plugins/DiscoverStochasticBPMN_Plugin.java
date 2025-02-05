package org.processmining.discoverstochasticbpmn.plugins;

import java.util.List;
import java.util.Map;

import org.deckfour.uitopia.api.event.TaskListener;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.discoverstochasticbpmn.algorithms.*;
import org.processmining.discoverstochasticbpmn.dialogs.DiscoverStochasticBPMN_UI;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.connections.petrinets.behavioral.FinalMarkingConnection;
import org.processmining.models.connections.petrinets.behavioral.InitialMarkingConnection;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.BPMNEdge;
import org.processmining.models.graphbased.directed.bpmn.BPMNNode;
import org.processmining.models.graphbased.directed.bpmn.elements.Gateway;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.discoverstochasticbpmn.models.DiscoverStochasticBPMN_Configuration;
import org.processmining.discoverstochasticbpmn.models.XORChoiceMap;
import org.processmining.plugins.converters.bpmn2pn.BPMN2PetriNetConverter_Configuration;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogNotFiltered;
import org.processmining.poemsconformancecheckingforbpmn.algorithms.conformance_checking.BPMNStochasticConformanceChecking;
import org.processmining.poemsconformancecheckingforbpmn.algorithms.conformance_checking.poems.bpmn.BpmnPoemsConformanceChecking;
import org.processmining.poemsconformancecheckingforbpmn.algorithms.inputs.bpmn.statespace.BpmnNoOptionToCompleteException;
import org.processmining.poemsconformancecheckingforbpmn.algorithms.inputs.bpmn.statespace.BpmnUnboundedException;
import org.processmining.poemsconformancecheckingforbpmn.models.bpmn.conformance.result.POEMSConformanceCheckingResult;
import org.processmining.stochasticbpmn.models.graphbased.directed.bpmn.stochastic.StochasticBPMNDiagram;

import javax.swing.*;

/**
 * Discovery of Stochastic BPMN from a given log and BPMN model with probabilities obtained using alignments
 *
 * @author Sandhya Velagapudi Jul 28, 2024
 */

@Plugin(name = "Discover Stochastic BPMN", parameterLabels = { "BPMN Model", "Event Log" },
	    returnLabels = { "Stochastic BPMN Diagram", "Gateway Probability Map" }, returnTypes = { StochasticBPMNDiagram.class, Map.class },
		userAccessible = true, help = "Discover Stochastic BPMN with path probabilities derived from alignments")

public class DiscoverStochasticBPMN_Plugin {
	@UITopiaVariant(affiliation = "RWTH Aachen", author = "Sandhya Velagapudi", email = "sandhya.velagapudi@rwth-aachen.de")

	@PluginVariant(
			variantLabel = "Get Probabilities from BPMN and Event Log",
			requiredParameterLabels = {0, 1}
	)

	public Object[] discover(UIPluginContext context, BPMNDiagram bpmn, XLog log) throws BpmnNoOptionToCompleteException, BpmnUnboundedException, InterruptedException {
		DiscoverStochasticBPMN_Configuration config = new DiscoverStochasticBPMN_Configuration();
		DiscoverStochasticBPMN_UI ui = new DiscoverStochasticBPMN_UI(config);
		if(ui.setParameters(context,config) == TaskListener.InteractionResult.CANCEL) {
			cancel(context, "User canceled the plugin interaction");
			return null;
		}
		Progress progress = context.getProgress();
		progress.setCaption("Discovering Path Probabilities");

		DiscoverProbabilities discoverer = new DiscoverProbabilities(bpmn, log, config);
		boolean success = discoverer.discover();

		if(!success){
			showWarningsandErrors(context, discoverer);
			return cancel(context, "Failed to calculate path probabilities");
		}

		progress.setCaption("Creating Stochastic BPMN Diagram");
		Map<Gateway, XORChoiceMap> gatewayMap = discoverer.getGatewayMap();
		translateToSBPMN translator = new translateToSBPMN(bpmn, gatewayMap, "weight");
		translator.createSBPMN();
		translateToSBPMN translator_vis = new translateToSBPMN(bpmn, gatewayMap, "probability");
		translator_vis.createSBPMN();

		StochasticBPMNDiagram sbpmn = translator.getSBPMN();
		StochasticBPMNDiagram sbpmn_vis = translator_vis.getSBPMN();

		BpmnPoemsConformanceChecking conformanceChecker = BPMNStochasticConformanceChecking.poems();
		POEMSConformanceCheckingResult result = conformanceChecker.calculateConformance(sbpmn,log);
		System.out.println(result);

		return new Object[] {sbpmn_vis, gatewayMap};
	}

	private void showWarningsandErrors(PluginContext context, DiscoverProbabilities discoverer) {
		StringBuilder warnings = new StringBuilder();
		for (String error : discoverer.getErrors()) {
			warnings.append("Error: ").append(error);
			warnings.append('\n');
		}
		for (String warning : discoverer.getWarnings()) {
			warnings.append("Warning: ").append(warning);
			warnings.append('\n');
		}
		showMessage(context, warnings.toString());
	}

	private void showMessage(PluginContext context, String message) {
		if (context instanceof UIPluginContext) {
			JOptionPane.showMessageDialog(null, message, "Discover Stochastic BPMN", JOptionPane.WARNING_MESSAGE);
		} else {
			System.out.println(message);
			context.log(message);
		}
	}

	public static Object[] cancel(PluginContext context, String message) {
		System.out.println("[DiscoverStochasticBPMN]: " + message);
		context.log(message);
		context.getFutureResult(0).cancel(true);
		context.getFutureResult(1).cancel(true);
		return null;
	}
}
