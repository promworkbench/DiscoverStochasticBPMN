package org.processmining.discoverstochasticbpmn.dialogs;

import org.deckfour.uitopia.api.event.TaskListener;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.util.ui.widgets.LeftAlignedHeader;
import org.processmining.discoverstochasticbpmn.models.DiscoverStochasticBPMN_Configuration;
import org.processmining.discoverstochasticbpmn.plugins.DiscoverStochasticBPMN_Plugin;
import org.processmining.uma.util.ui.widgets.ProMPropertiesPanel;

import javax.swing.*;
import java.awt.*;

public class DiscoverStochasticBPMN_UI extends ProMPropertiesPanel {
    private final javax.swing.JComboBox<String> alignmentType;

    private static final String DIALOG_NAME = "Options for calculating probabilities";

    public DiscoverStochasticBPMN_UI(DiscoverStochasticBPMN_Configuration config){
        super(null);

        addToProperties(new LeftAlignedHeader(DIALOG_NAME));
        addToProperties(Box.createVerticalStrut(20));

        JLabel optionsLabel = new JLabel("Which alignments shall be considered for probability?");
        optionsLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        optionsLabel.setMaximumSize(new Dimension(1000, 30));
        addToProperties(optionsLabel);
        addToProperties(Box.createVerticalStrut(10));
        String[] labelValues = {
                "Only perfectly fitting traces", //calculationType_PERFECTLYFIT,
                "Only synchronous moves of all traces", //calculationType_SYNCHRONOUS,
                "Synchronous and model moves of all traces", //calculationType_ALL
        };
//        alignmentType = addComboBox("From Alignments, use", labelValues, 1, 400);
        alignmentType = new JComboBox<>(labelValues);
        alignmentType.setSelectedIndex(config.calculateProbabilityUsing.ordinal());
        addToProperties(alignmentType);
    }


    /**
     * Open UI dialogue to populate the given configuration object with
     * settings chosen by the user.
     *
     * @param context the plugin context
     * @param config the configuration
     * @return result of the user interaction
     */
    public TaskListener.InteractionResult setParameters(UIPluginContext context, DiscoverStochasticBPMN_Configuration config) {
        TaskListener.InteractionResult wish = getUserChoice(context);
        if (wish != TaskListener.InteractionResult.CANCEL) getChosenParameters(config);
        return wish;
    }


    private void getChosenParameters(DiscoverStochasticBPMN_Configuration config) {
        config.calculateProbabilityUsing = DiscoverStochasticBPMN_Configuration.typeValue.values()[alignmentType.getSelectedIndex()];
    }

    /**
     * display a dialog to ask user what to do
     *
     * @param context context of the plugin
     * @return returns the user's choice
     */
    protected TaskListener.InteractionResult getUserChoice(UIPluginContext context) {
        return context.showConfiguration("Calculate Probabilities from Alignments", this);
    }

    /**
     * Generate proper cancelling information for User.
     * @param context context of the plugin
     * @return returns the cancel message
     */
    protected Object[] userCancel(PluginContext context) {
        return DiscoverStochasticBPMN_Plugin.cancel(context, "The user has cancelled DiscoverStochasticBPMN.");
    }
}
