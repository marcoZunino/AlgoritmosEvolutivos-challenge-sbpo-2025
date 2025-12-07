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


        SinglePointCrossover crossover = new SinglePointCrossover(0.9);
        BitFlipMutation mutation = new BitFlipMutation(1.0 / problem.getTotalNumberOfBits());
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

    // // Example usage inside main
    // public static void main(String[] args) {
    //     ChallengeSolution best = run(100, 100, 10000);

    //     System.out.println("Best solution bitstring: " + best.getVariableValue(0));
    //     System.out.println("Number of ones: " + -best.getObjective(0));
    // }
}
