package org.sbpo2025.challenge.experiments;

import java.util.ArrayList;
import java.util.List;

import org.sbpo2025.challenge.Challenge;
import org.sbpo2025.challenge.ChallengeSolver;
import org.sbpo2025.challenge.genetic_algorithm.subset_genetic_algorithm.WavePickingProblem;
import org.sbpo2025.challenge.genetic_algorithm.subset_genetic_algorithm.WaveSolution;
import org.uma.jmetal.lab.experiment.Experiment;
import org.uma.jmetal.lab.experiment.ExperimentBuilder;
import org.uma.jmetal.lab.experiment.util.ExperimentAlgorithm;
import org.uma.jmetal.lab.experiment.util.ExperimentProblem;
import org.uma.jmetal.lab.experiment.component.impl.ExecuteAlgorithms;
import org.uma.jmetal.lab.experiment.component.impl.ComputeQualityIndicators;
import org.uma.jmetal.lab.experiment.component.impl.GenerateLatexTablesWithStatistics;
import org.uma.jmetal.lab.experiment.component.impl.GenerateBoxplotsWithR;
import org.sbpo2025.challenge.experiments.ExperimentsFactory.AlgorithmType;


public class ParametersConfiguration {

    public static void main(String[] args) throws Exception {

        String instance = "a/instance_0010.txt"; // Default instance number
        String inputFilePath = "datasets/"+instance;

        Challenge challenge = new Challenge();
        challenge.readInput(inputFilePath);

        var challengeSolver = new ChallengeSolver(
                challenge.orders, challenge.aisles, challenge.nItems, challenge.waveSizeLB, challenge.waveSizeUB);

        WavePickingProblem problem = new WavePickingProblem(challengeSolver.orders, challengeSolver.aisles, challengeSolver.items, challengeSolver.waveSizeLB, challengeSolver.waveSizeUB, 12345L);
        ExperimentProblem<WaveSolution> expProblem = new ExperimentProblem<>(problem, instance); // name: instance

        double[] crossoverProbabilities = {0.7, 0.9};
        double[] mutationProbabilities = {0.01, -1};
        int[] populationSizes = {40, 60};
        int generations = 30;

        List<ExperimentAlgorithm<WaveSolution, List<WaveSolution>>> algorithms = new ArrayList<>();
        
        int runId = 0;

        for (double cx : crossoverProbabilities) {
            for (double mut : mutationProbabilities) {
                for (int pop : populationSizes) {

                    // Generational GA
                    algorithms.add(
                        new ExperimentAlgorithm<>(
                            ExperimentsFactory.algorithmBuilder(problem, cx, mut, generations, pop, runId, AlgorithmType.GENERATIONAL),
                        expProblem, runId)
                    );
                    runId++;

                    // Steady-State GA
                    algorithms.add(
                        new ExperimentAlgorithm<>(
                            ExperimentsFactory.algorithmBuilder(problem, cx, mut, generations, pop, runId, AlgorithmType.STEADY_STATE),
                        expProblem, runId)
                    );
                    runId++;
                }
            }
        }

        Experiment<WaveSolution, List<WaveSolution>> experiment =
                new ExperimentBuilder<WaveSolution, List<WaveSolution>>("WaveExperiment")
                        .setAlgorithmList(algorithms)
                        .setProblemList(List.of(expProblem))
                        .setExperimentBaseDirectory("experimentResults")
                        .setIndependentRuns(30)
                        .build();

        new ExecuteAlgorithms<>(experiment).run();
        new ComputeQualityIndicators<>(experiment).run();
        new GenerateLatexTablesWithStatistics(experiment).run();
        new GenerateBoxplotsWithR<>(experiment).setRows(1).setColumns(1).run();
    }
}
