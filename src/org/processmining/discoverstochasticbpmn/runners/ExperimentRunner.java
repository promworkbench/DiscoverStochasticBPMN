package org.processmining.discoverstochasticbpmn.runners;

import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import org.apache.commons.lang3.ArrayUtils;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.discoverstochasticbpmn.algorithms.DiscoverProbabilities;
import org.processmining.discoverstochasticbpmn.algorithms.translateToSBPMN;
import org.processmining.discoverstochasticbpmn.models.DiscoverStochasticBPMN_Configuration;
import org.processmining.discoverstochasticbpmn.models.XORChoiceMap;
import org.processmining.models.graphbased.directed.bpmn.BPMNDiagram;
import org.processmining.models.graphbased.directed.bpmn.elements.Gateway;
import org.processmining.poemsconformancecheckingforbpmn.algorithms.conformance_checking.BPMNStochasticConformanceChecking;
import org.processmining.poemsconformancecheckingforbpmn.algorithms.conformance_checking.poems.bpmn.BpmnPoemsConformanceChecking;
import org.processmining.poemsconformancecheckingforbpmn.algorithms.inputs.bpmn.statespace.BpmnNoOptionToCompleteException;
import org.processmining.poemsconformancecheckingforbpmn.algorithms.inputs.bpmn.statespace.BpmnUnboundedException;
import org.processmining.poemsconformancecheckingforbpmn.models.bpmn.conformance.result.POEMSConformanceCheckingResult;
import org.processmining.poemsconformancecheckingforbpmn.utils.log.XLogReader;
import org.processmining.stochasticbpmn.algorithms.diagram.reader.BpmnDiagramReader;
import org.processmining.stochasticbpmn.algorithms.reader.ObjectReader;
import org.processmining.stochasticbpmn.models.graphbased.directed.bpmn.stochastic.StochasticBPMNDiagram;
import org.processmining.stochasticbpmn.models.graphbased.directed.bpmn.stochastic.StochasticFlow;
import org.processmining.stochasticbpmn.models.graphbased.directed.bpmn.stochastic.StochasticGateway;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ExperimentRunner {
    private final ObjectReader<File, XLog> logReader;
    private final ObjectReader<File, BPMNDiagram> modelReader;

    public ExperimentRunner() {
        this.logReader = XLogReader.fromFile();
        this.modelReader = BpmnDiagramReader.fromFile();
    }

    public static void main(String[] args) {
        File logsFolder = null;
        File modelsFolder = null;
        File resultsFolder = null;

        if (args.length == 1) {
            System.out.println(args[0]);
            logsFolder = new File(args[0] + File.separator + "logs");
            modelsFolder = new File(args[0] + File.separator + "models");
            resultsFolder = new File(args[0] + File.separator + "results");
        } else if (args.length == 3) {
            logsFolder = new File(args[0]);
            modelsFolder = new File(args[1]);
            resultsFolder = new File(args[2]);
        }
        new ExperimentRunner().runExperiments(
                logsFolder,
                modelsFolder,
                resultsFolder
        );
    }

    private void runExperiments(File logsFolder, File modelsFolder, File resultsFolder) {
        System.out.println("Inside void runExperiments()");
        // logging
        ImmutableTable.Builder<String, String, Integer> logInfoTB = new ImmutableTable.Builder<>();
        ImmutableTable.Builder<String, String, String> modelInfoTB = new ImmutableTable.Builder<>();
        ImmutableTable.Builder<String, String, String> stModelInfoTB = new ImmutableTable.Builder<>();
        ImmutableTable.Builder<String, String, String> resultsInfoTB = new ImmutableTable.Builder<>();
        File resultFolderConcreteRun = setupLogging(resultsFolder);

        Map<String, File> logFiles = getLogs(logsFolder);
        System.out.println("Got the log files");
        for (Map.Entry<String, File> entry : logFiles.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue().getAbsolutePath());
        }
        Map<String, File> modelFiles = getModels(modelsFolder);
        System.out.println("Got the model files");
        for (Map.Entry<String, File> entry : modelFiles.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue().getAbsolutePath());
        }

        for (Map.Entry<String, File> logFile : logFiles.entrySet()) {
            System.out.println("Processing log: " + logFile.getKey());
            try {
                XLog log = logReader.read(logFile.getValue());
                System.out.println("Got the XLog");
                BPMNDiagram model = null;
                for (Map.Entry<String, File> modelFile : modelFiles.entrySet()) {
                    if (!modelFile.getKey().equals(logFile.getKey())) {
                        continue; // Check the next model file
                    }
                    System.out.println("Found the corresponding model file: " + modelFile.getKey());
                    model = modelReader.read(modelFile.getValue());
                    System.out.println("Got the BpmnDiagram model");
                    recordLogInfo(logFile.getKey(), log, logInfoTB);
                    recordModelInfo(logFile.getKey(), model, modelInfoTB);
                }

                if (model == null) {
                    System.err.printf("No matching model found for log: %s\n", logFile.getValue().getName());
                    continue; // Skip to the next log file
                }

                System.out.println("Run: " + logFile.getValue().getName().replaceFirst("[.][^.]+$", ""));
                try {
                    for (DiscoverStochasticBPMN_Configuration.typeValue strategyType: DiscoverStochasticBPMN_Configuration.typeValue.values()) {
                        ExperimentResult result = runExperiments(log, model, strategyType);
//                    recordStochasticModelInfo(logFile.getKey(), result, stModelInfoTB);
                        recordResults(logFile.getKey() + "_" + strategyType.ordinal(), result, resultsInfoTB);
                        writeResults(resultFolderConcreteRun, logInfoTB, modelInfoTB, resultsInfoTB);
                    }

                } catch (Exception e) {
                    System.err.printf("Failed to execute %s", logFile.getValue().getName());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private ExperimentResult runExperiments(XLog log, BPMNDiagram bpmn, DiscoverStochasticBPMN_Configuration.typeValue strategy) {
        long discoveryTime = 0;
        long evaluationTime = 0;
        StochasticBPMNDiagram sbpmn;
        POEMSConformanceCheckingResult result = null;

        // Discovering Stochastic BPMN
        long startTime = System.currentTimeMillis();
        DiscoverProbabilities discoverer = new DiscoverProbabilities(bpmn, log, strategy);
        boolean success = discoverer.discover();
        if(success){
            Map<Gateway, XORChoiceMap> gatewayMap = discoverer.getGatewayMap();
            translateToSBPMN translator = new translateToSBPMN(bpmn, gatewayMap, "weight");
            translator.createSBPMN();
            sbpmn = translator.getSBPMN();
//                translateToSBPMN translator_exp = new translateToSBPMN(bpmn, gatewayMap, "probability");
//                translator_exp.createSBPMN();
//                StochasticBPMNDiagram sbpmn_exp = translator_exp.getSBPMN();
            long endTime = System.currentTimeMillis();
            discoveryTime += endTime - startTime;


            // Evaluating Stochastic BPMN using POEMS Conformance Checker
            startTime = System.currentTimeMillis();
            BpmnPoemsConformanceChecking conformanceChecker = BPMNStochasticConformanceChecking.poems();
            try {
                result = conformanceChecker.calculateConformance(sbpmn,log);
            } catch (BpmnNoOptionToCompleteException | BpmnUnboundedException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            endTime = System.currentTimeMillis();
            evaluationTime += endTime - startTime;
        }

        return new ExperimentResult(log, bpmn, strategy, result, discoveryTime, evaluationTime);
    }

    private static class ExperimentResult {
        private final XLog log;
        private final BPMNDiagram bpmn;
        private final DiscoverStochasticBPMN_Configuration.typeValue strategy;
        private final POEMSConformanceCheckingResult ccResult;
        private final long discoveryTime;
        private final long evaluationTime;

        private ExperimentResult(XLog log, BPMNDiagram bpmn, DiscoverStochasticBPMN_Configuration.typeValue strategy, POEMSConformanceCheckingResult ccResult, long discoveryTime, long evaluationTime) {
            this.log = log;
            this.bpmn = bpmn;
            this.strategy = strategy;
            this.ccResult = ccResult;
            this.discoveryTime = discoveryTime;
            this.evaluationTime = evaluationTime;
        }

//        @Override
//        public String toString() {
//            long numSGates = bpmnDiagram.getNodes().stream().filter(n -> n instanceof StochasticGateway).count();
//            long numSEdges = bpmnDiagram.getEdges().stream().filter(n -> n instanceof StochasticFlow).count();
//            return String.format(
//                    "%s - #nodes: %d #sgates: %d, #edges: %d, #sedges: %d, %s, languageSize: %d, " +
//                            "languageProbability:" + " %s, executionTime: %d",
//                    ccResult,
//                    bpmnDiagram.getNodes().size(),
//                    numSGates,
//                    bpmnDiagram.getEdges().size(),
//                    numSEdges,
//                    reachabilityGraphStaticAnalysis,
//                    modelLanguage.size(),
//                    modelLanguage.getProbability().getValue().setScale(
//                            5,
//                            RoundingMode.HALF_EVEN
//                    ).stripTrailingZeros(),
//                    executionTimeMilis
//            );
//        }
    }

    private static File setupLogging(File resultFolder) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
        File runResultFolder = new File(
                resultFolder,
                LocalDateTime.now().format(formatter)
        );
        runResultFolder.mkdirs();

        return runResultFolder;
    }

    private Map<String, File> getLogs(File logsFolder) {
        Map<String, File> logs = new HashMap<>();
        if (!logsFolder.exists() || !logsFolder.isDirectory()) {
            return logs;
        }
        File[] logFiles = logsFolder.listFiles();
        if (Objects.isNull(logFiles)) {
            return logs;
        }
        for (File logFile : logFiles) {
            String[] logNameParts = logFile.getName().split("\\.");
            if ("xes".equals(logNameParts[logNameParts.length - 1])) {
                logs.put(logFile.getName().replaceFirst("[.][^.]+$", ""), logFile);
            }
        }
        return logs;
    }

    private Map<String, File> getModels(File modelsFolder) {
        Map<String, File> models = new HashMap<>();
        if (!modelsFolder.exists() | !modelsFolder.isDirectory()) {
            return models;
        }

        File[] modelFiles = modelsFolder.listFiles();
        if (Objects.isNull(modelFiles)) {
            return models;
        }

        for (File modelFile : modelFiles) {
            String[] modelNameParts = modelFile.getName().split("\\.");
            if ("bpmn".equals(modelNameParts[modelNameParts.length - 1])) {
                models.put(modelFile.getName().replaceFirst("[.][^.]+$", ""), modelFile);
            }
        }
        return models;
    }

    private static void recordLogInfo(
            String key,
            XLog log,
            ImmutableTable.Builder<String, String, Integer> logInfoTB
    ) {

//        XLogInfo info = log.getInfo(new XEventNameClassifier());
//        System.out.printf(
//                "%s - traces: %d, events: %d, event classes: %d\n",
//                key,
//                info.getNumberOfTraces(),
//                info.getNumberOfEvents(),
//                info.getEventClasses().size()
//        );
        logInfoTB.put(
                key,
                "Number of Traces",
                log.size()
        );
//        logInfoTB.put(
//                key,
//                "Number of Events",
//                info.getNumberOfEvents()
//        );
//        logInfoTB.put(
//                key,
//                "Number of Event Classes",
//                info.getEventClasses().size()
//        );
    }

    private static void recordModelInfo(
            String key,
            BPMNDiagram bpmn,
            ImmutableTable.Builder<String, String, String> modelInfoTB
    ) {
        modelInfoTB.put(
                key,
                "Number of Nodes",
                String.valueOf(bpmn.getNodes().size())
        );
        modelInfoTB.put(
                key,
                "Number of Edges",
                String.valueOf(bpmn.getEdges().size())
        );
        long numEGates = bpmn.getNodes().stream().filter(n -> n instanceof Gateway).filter(g -> ((Gateway) g).getGatewayType() == Gateway.GatewayType.DATABASED).count();
        modelInfoTB.put(
                key,
                "Number of XOR Gates",
                String.valueOf(numEGates)
        );
    }

    private static void recordStochasticModelInfo(
            String key,
            ExperimentResult result,
            ImmutableTable.Builder<String, String, String> stModelInfoTB
    ) {
        stModelInfoTB.put(
                key,
                "BPMN Nodes Count",
                String.valueOf(result.bpmn.getNodes().size())
        );
        stModelInfoTB.put(
                key,
                "BPMN Edge Count",
                String.valueOf(result.bpmn.getEdges().size())
        );
        long numSGates = result.bpmn.getNodes().stream().filter(n -> n instanceof StochasticGateway).count();
        long numSEdges = result.bpmn.getEdges().stream().filter(n -> n instanceof StochasticFlow).count();
        stModelInfoTB.put(
                key,
                "Stochastic Gates Count",
                String.valueOf(numSGates)
        );
        stModelInfoTB.put(
                key,
                "Stochastic Edges Count",
                String.valueOf(numSEdges)
        );
    }

    private static void recordResults(
            String key,
            ExperimentResult result,
            ImmutableTable.Builder<String, String, String> resultsInfoTB
    ) {
        resultsInfoTB.put(
                key,
                "Trace/Move Selection Strategy",
                result.strategy.toString()
        );
        resultsInfoTB.put(
                key,
                "CC Result Lower Bound",
                result.ccResult.getConformanceLowerBound().toString()
        );
        resultsInfoTB.put(
                key,
                "CC Result Upper Bound",
                result.ccResult.getConformanceUpperBound().toString()
        );
        resultsInfoTB.put(
                key,
                "Discovery Algorithm Execution Time",
                String.valueOf(result.discoveryTime)
        );
        resultsInfoTB.put(
                key,
                "Evaluation Execution Time",
                String.valueOf(result.evaluationTime)
        );
    }

    private static void writeResults(File resultFolder,
            ImmutableTable.Builder<String, String, Integer> logInfoTB,
            ImmutableTable.Builder<String, String, String> modelInfoTB,
            ImmutableTable.Builder<String, String, String> resultsTB
    ) {
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
//        File runResultFolder = new File(resultFolder, LocalDateTime.now().format(formatter));
//        runResultFolder.mkdirs();
        // logs
        writeTableInCsv(
                resultFolder.getPath() + File.separator + "logs.csv",
                logInfoTB
        );
        writeTableInCsv(
                resultFolder.getPath() + File.separator + "models.csv",
                modelInfoTB
        );
        writeTableInCsv(
                resultFolder.getPath() + File.separator + "results.csv",
                resultsTB
        );
    }

    private static void writeTableInCsv(String path, ImmutableTable.Builder<String, String, ?> tablebuilder) {
        Table<String, String, ?> table = tablebuilder.build();
        List<String> columns = new ArrayList<>(table.columnKeySet());
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(path))) {
            csvWriter.writeNext(
                    ArrayUtils.add(
                            columns.toArray(new String[0]),
                            0,
                            "Key"
                    ),
                    true
            );
            for (String rowKey : table.rowMap().keySet()) {
                Map<String, ?> row = table.rowMap().get(rowKey);
                String[] nextLine = ArrayUtils.add(
                        columns.stream().map(row::get).map(Object::toString).toArray(String[]::new),
                        0,
                        rowKey
                );
                csvWriter.writeNext(
                        nextLine,
                        true
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
