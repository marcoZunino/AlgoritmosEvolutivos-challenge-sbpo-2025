package org.sbpo2025.challenge.genetic_algorithm.binary_genetic_algorithm;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.sbpo2025.challenge.ChallengeSolution;
import org.sbpo2025.challenge.ChallengeSolver;
import org.sbpo2025.challenge.Item;
import org.uma.jmetal.algorithm.impl.AbstractGeneticAlgorithm;
import org.uma.jmetal.algorithm.singleobjective.geneticalgorithm.GenerationalGeneticAlgorithm;
import org.uma.jmetal.operator.crossover.impl.HUXCrossover;
import org.uma.jmetal.operator.mutation.impl.BitFlipMutation;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;


public class BinaryGeneticAlgorithmRunner {

    public static boolean SHOW_OUTPUT = false;

    public static ChallengeSolution run(ChallengeSolver solver, Map<String, Object> params) {

        List<Map<Integer, Integer>> orders = solver.orders;
        List<Map<Integer, Integer>> aisles = solver.aisles;
        List<Item> items = solver.items;
        int waveSizeLB = solver.waveSizeLB;
        int waveSizeUB = solver.waveSizeUB;
        
        long randomSeed = (long) params.getOrDefault("randomSeed", 1234L);
        double mutationProbability = (double) params.getOrDefault("mutationProbability", 1.0/(orders.size() + aisles.size()));
        double crossoverProbability = (double) params.getOrDefault("crossoverProbability", 0.9);

        int populationSize = (int) params.getOrDefault("populationSize", 100);
        int maxEvaluations = populationSize * (int) params.getOrDefault("generations", 100);


        BinaryWavePickingProblem problem = new BinaryWavePickingProblem(orders, aisles, items, waveSizeLB, waveSizeUB, randomSeed);
        
        if ((boolean) params.getOrDefault("showOutput", false)) {
            problem.showOutput();
            SHOW_OUTPUT = true;
        }
        
        // problem.setWaveSizePenalty((double) params.getOrDefault("waveSizePenalty", 10));        

        HUXCrossover crossover = new HUXCrossover(crossoverProbability);
        BitFlipMutation mutation = new BitFlipMutation(mutationProbability);
        BinaryTournamentSelection<BinarySolution> selection = new BinaryTournamentSelection<>();
        SolutionListEvaluator<BinarySolution> evaluator = new SequentialSolutionListEvaluator<>();

        AbstractGeneticAlgorithm<BinarySolution, BinarySolution> algorithm = new GenerationalGeneticAlgorithm<>(
                problem, maxEvaluations, populationSize, crossover, mutation, selection, evaluator);
        
        algorithm.run();

        BinarySolution result = algorithm.getResult();
        return new ChallengeSolution(
            problem.getSelectedOrders(result).stream().collect(Collectors.toSet()),
            problem.getVisitedAisles(result).stream().collect(Collectors.toSet())
        ); // return best solution

    }

}
