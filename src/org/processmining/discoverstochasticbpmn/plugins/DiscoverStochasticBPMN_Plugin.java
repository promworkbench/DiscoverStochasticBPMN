package org.processmining.newpackageivy.plugins;

import java.util.List;
import java.util.Map;

import org.deckfour.uitopia.api.event.TaskListener;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
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
import org.processmining.newpackageivy.algorithms.BPMN2PetriNetConverterExtension;
import org.processmining.newpackageivy.algorithms.CalculateProbabilities;
import org.processmining.newpackageivy.algorithms.GetSBPMN;
import org.processmining.newpackageivy.dialogs.DiscoverStochasticBPMN_UI;
import org.processmining.newpackageivy.models.DiscoverStochasticBPMN_Configuration;
import org.processmining.newpackageivy.models.XORChoiceMap;
import org.processmining.plugins.converters.bpmn2pn.BPMN2PetriNetConverter_Configuration;
import org.processmining.newpackageivy.algorithms.GetAlignments;
import org.processmining.plugins.inductiveVisualMiner.helperClasses.IvMModel;
import org.processmining.plugins.inductiveVisualMiner.ivmlog.IvMLogNotFiltered;

import javax.swing.*;

/**
 * Discovery of Stochastic BPMN from a given log and BPMN model with probabilities obtained using alignments
 *
 * @author Sandhya Velagapudi Jul 28, 2024
 */

@Plugin(name = "Discover Stochastic BPMN", parameterLabels = { "BPMN Model", "Event Log" },
	    returnLabels = { "BPMN Diagram", "Gateway Probability Map" }, returnTypes = { BPMNDiagram.class, Map.class },
		userAccessible = true, help = "Discover Stochastic BPMN with probabilities derived from alignments")

public class DiscoverStochasticBPMN_Plugin {
	@UITopiaVariant(affiliation = "RWTH Aachen", author = "Sandhya Velagapudi", email = "sandhya.velagapudi@rwth-aachen.de")

	@PluginVariant(
			variantLabel = "Get Probabilities from BPMN and Event Log",
			requiredParameterLabels = {0, 1}
	)
	public Object[] align(UIPluginContext context, BPMNDiagram bpmn, XLog log) {
		DiscoverStochasticBPMN_Configuration config = new DiscoverStochasticBPMN_Configuration();
		DiscoverStochasticBPMN_UI ui = new DiscoverStochasticBPMN_UI(config);
		return ui.setParameters(context, config) != TaskListener.InteractionResult.CANCEL ? this.align(context, bpmn, log, config) : cancel(context, "Cancelled by user.");
	}

	@PluginVariant(
			variantLabel = "Get Probabilities from BPMN and Event Log",
			requiredParameterLabels = {0, 1, 2}
	)
	public Object[] align(UIPluginContext context, BPMNDiagram bpmn, XLog log, DiscoverStochasticBPMN_Configuration config) {

		// Create an instance of BPMN to PN converter
		BPMN2PetriNetConverter_Configuration conv_config = new BPMN2PetriNetConverter_Configuration();
		conv_config.labelNodesWith = BPMN2PetriNetConverter_Configuration.LabelValue.PREFIX_NONTASK_BY_BPMN_TYPE;
		BPMN2PetriNetConverterExtension conv = new BPMN2PetriNetConverterExtension(bpmn, conv_config);

		Progress progress = context.getProgress();
		progress.setCaption("Converting BPMN diagram to Petri net");

		boolean success = conv.convert();

		if (success) {
            Petrinet net = conv.getPetriNet();
//			System.out.println(net.getTransitions());
            Marking m = conv.getMarking();
            context.getConnectionManager().addConnection(new InitialMarkingConnection(net, m));

            List<Place> finalPlaces = conv.getFinalPlaces();
            Marking mf = null;
            if (finalPlaces.size() == 1) {
//				System.out.println("There is a final place, entered the if condition");
                mf = new Marking(finalPlaces);
                context.getConnectionManager().addConnection(new FinalMarkingConnection(net, mf));
                context.getProvidedObjectManager().createProvidedObject("Final marking of the PN from " + bpmn.getLabel(), mf, context);
            } else {
                conv.getWarnings().add("More than 1 final place, could not construct generic final marking.");
            }

            if (!conv.getWarnings().isEmpty())
                showWarningsandErrors(context, conv);

//            context.getFutureResult(0).setLabel("Petri net from " + bpmn.getLabel());
//            context.getFutureResult(1).setLabel("Initial marking of the PN from " + bpmn.getLabel());

			progress.setCaption("Aligning Log to Petri net");
			IvMLogNotFiltered alignedLog = null;
			CalculateProbabilities calc = new CalculateProbabilities();
			try {
				IvMModel model = GetAlignments.getIvMModel(net, m, mf);
				alignedLog = GetAlignments.alignPetriNetWithLog(context, model, log);
//				System.out.println("Successfully got the aligned log");

				progress.setCaption("Calculating Probabilities from Alignments");
				Map<Gateway, XORChoiceMap> gatewayMap = calc.calculateXORProbabilities(alignedLog, model, conv.getGatewayMap());
				System.out.println("Done calculating probabilities. Final result...");

				for (Gateway g : gatewayMap.keySet()) {
					System.out.println(g.getLabel());
					XORChoiceMap choiceMap = gatewayMap.get(g);
					for (BPMNEdge<BPMNNode, BPMNNode> edge : choiceMap.getAllChoices().keySet()) {
						System.out.println(choiceMap.getTransitionCounts(edge).getTransition().getId());
						System.out.println(choiceMap.getTransitionCounts(edge).getTransition().toString());
						System.out.println(choiceMap.getTransitionCounts(edge).getProbability());
					}
				}

//				GetSBPMN.createSBPMN(bpmn, gatewayMap);

			} catch (Exception e){
//				System.out.println("For some reason, came into the catch block :(");
				showMessage(context,"Alignment calculation failed due to an exception: " + e.getMessage());
				return cancel(context, "Could not translate BPMN diagram");
			}
			return new Object[]{bpmn, calc.getGatewayMap()};
        } else {
			if (!conv.getErrors().isEmpty() || !conv.getWarnings().isEmpty())
				showWarningsandErrors(context, conv);

			return cancel(context, "Could not translate BPMN diagram");
		}
	}

	private void showWarningsandErrors(PluginContext context, BPMN2PetriNetConverterExtension conv) {
		StringBuilder warnings = new StringBuilder();
		for (String error : conv.getErrors()) {
			warnings.append("Error: ").append(error);
			warnings.append('\n');
		}
		for (String warning : conv.getWarnings()) {
			warnings.append("Warning: ").append(warning);
			warnings.append('\n');
		}
		showMessage(context, warnings.toString());
	}

	private void showMessage(PluginContext context, String message) {
		if (context instanceof UIPluginContext) {
			JOptionPane.showMessageDialog(null, message, "BPMN2PetriNet conversion", JOptionPane.WARNING_MESSAGE);
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
