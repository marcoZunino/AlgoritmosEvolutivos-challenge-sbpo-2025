package org.sbpo2025.challenge.binary_genetic_algorithm;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.sbpo2025.challenge.ChallengeSolution;
import org.sbpo2025.challenge.Item;
import org.uma.jmetal.algorithm.singleobjective.geneticalgorithm.GenerationalGeneticAlgorithm;
import org.uma.jmetal.operator.crossover.impl.SinglePointCrossover;
import org.uma.jmetal.operator.mutation.impl.BitFlipMutation;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;

public class BinaryGeneticAlgorithmRunner {

    public static ChallengeSolution run(
        List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles,
        List<Item> items, int waveSizeLB, int waveSizeUB,
        int populationSize, int maxEvaluations, Random random) {

        BinaryWavePickingProblem problem = new BinaryWavePickingProblem(orders, aisles, items, waveSizeLB, waveSizeUB, random);
        // problem.setDistanceLambda(lambda);
        // problem.setWaveSizePenalty(10);
        
        double mutationProbability = 1.0/(orders.size() + aisles.size());        
        // mutationProbability = 0.1;
        double crossoverProbability = 0.9;

        SinglePointCrossover crossover = new SinglePointCrossover(crossoverProbability);
        BitFlipMutation mutation = new BitFlipMutation(mutationProbability);
        BinaryTournamentSelection<BinarySolution> selection = new BinaryTournamentSelection<>();
        SolutionListEvaluator<BinarySolution> evaluator = new SequentialSolutionListEvaluator<>();

        GenerationalGeneticAlgorithm<BinarySolution> algorithm = new GenerationalGeneticAlgorithm<>(
                problem, maxEvaluations, populationSize, crossover, mutation, selection, evaluator);
        
        algorithm.run();

        BinarySolution result = algorithm.getResult();
        return new ChallengeSolution(
            problem.getSelectedOrders(result).stream().collect(Collectors.toSet()),
            problem.getVisitedAisles(result).stream().collect(Collectors.toSet())
        ); // return best solution

    }

}
