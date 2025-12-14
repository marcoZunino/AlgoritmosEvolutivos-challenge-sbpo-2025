package org.sbpo2025.challenge.genetic_algorithm.subset_genetic_algorithm;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.sbpo2025.challenge.ChallengeSolution;
import org.sbpo2025.challenge.ChallengeSolver;
import org.sbpo2025.challenge.Item;
import org.sbpo2025.challenge.genetic_algorithm.subset_genetic_algorithm.operators.*;
import org.uma.jmetal.algorithm.impl.AbstractGeneticAlgorithm;
import org.uma.jmetal.algorithm.singleobjective.geneticalgorithm.GenerationalGeneticAlgorithm;
import org.uma.jmetal.algorithm.singleobjective.geneticalgorithm.SteadyStateGeneticAlgorithm;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;


public class GeneticAlgorithmRunner {

    public static boolean SHOW_OUTPUT = false;

    public static ChallengeSolution run(ChallengeSolver solver, Map<String, Object> params) {

        List<Map<Integer, Integer>> orders = solver.orders;
        List<Map<Integer, Integer>> aisles = solver.aisles;
        List<Item> items = solver.items;
        int waveSizeLB = solver.waveSizeLB;
        int waveSizeUB = solver.waveSizeUB;

        Random random = new Random((long) params.getOrDefault("randomSeed", 1234L));
        double mutationProbability = (double) params.getOrDefault("mutationProbability", 1.0/(orders.size() + aisles.size()));
        double crossoverProbability = (double) params.getOrDefault("crossoverProbability", 0.9);

        int populationSize = (int) params.getOrDefault("populationSize", 100);
        int maxEvaluations = populationSize * (int) params.getOrDefault("generations", 100);

        WavePickingProblem problem = new WavePickingProblem(orders, aisles, items, waveSizeLB, waveSizeUB, random);

        if (!(boolean) params.getOrDefault("warmStart", true)) problem.randomStart();

        if ((boolean) params.getOrDefault("showOutput", false)) {
            problem.showOutput();
            SHOW_OUTPUT = true;
        }

        // problem.setWaveSizePenalty((double) params.getOrDefault("waveSizePenalty", 10));        

        CrossoverOperator<WaveSolution> crossover = new WaveUniformCrossover(crossoverProbability, (boolean) params.getOrDefault("ordersUnionCrossover", true), random);
        MutationOperator<WaveSolution> mutation = new WaveBitFlipMutation(mutationProbability, problem.orders.size(), problem.aisles.size(), random);
        SelectionOperator<List<WaveSolution>,WaveSolution> selection = new WaveTournamentSelection<>(random);

        AbstractGeneticAlgorithm<WaveSolution, WaveSolution> algorithm = null;

        switch ((String) params.getOrDefault("GAimplementation", "steadyState")) {
            
            case "steadyState":
                algorithm = new SteadyStateGeneticAlgorithm<>(problem, maxEvaluations, populationSize, crossover, mutation, selection);
                break;

            case "generational":
                SolutionListEvaluator<WaveSolution> evaluator = new SequentialSolutionListEvaluator<>();
                algorithm = new GenerationalGeneticAlgorithm<>(problem, maxEvaluations, populationSize, crossover, mutation, selection, evaluator);
                
                break;
        }
        
        algorithm.run();

        WaveSolution result = algorithm.getResult();
        return new ChallengeSolution(
            result.getOrders().stream().collect(Collectors.toSet()),
            result.getAisles().stream().collect(Collectors.toSet())
        ); // return best solution

    }

    
    }

