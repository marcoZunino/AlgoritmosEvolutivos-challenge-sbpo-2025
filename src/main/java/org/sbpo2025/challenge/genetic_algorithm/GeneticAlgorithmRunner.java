package org.sbpo2025.challenge.genetic_algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.sbpo2025.challenge.ChallengeSolution;
import org.sbpo2025.challenge.Item;

import org.uma.jmetal.algorithm.singleobjective.geneticalgorithm.GenerationalGeneticAlgorithm;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.util.JMetalException;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.SolutionUtils;
import org.uma.jmetal.util.checking.Check;
import org.uma.jmetal.util.comparator.DominanceComparator;
import org.uma.jmetal.util.evaluator.SolutionListEvaluator;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;

public class GeneticAlgorithmRunner {

    public static ChallengeSolution run(
        List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles,
        List<Item> items, int waveSizeLB, int waveSizeUB,
        int populationSize, int maxEvaluations, Random random) {

        WavePickingProblem problem = new WavePickingProblem(orders, aisles, items, waveSizeLB, waveSizeUB, random);
        // problem.setDistanceLambda(lambda);
        // problem.setWaveSizePenalty(10);


        CrossoverOperator<WaveSolution> crossover = new SinglePointCrossover(0.9, random, aisles.size());
        MutationOperator<WaveSolution> mutation = new BitFlipMutation(1.0/(orders.size() + aisles.size()), random, orders.size(), aisles.size());
        SelectionOperator<List<WaveSolution>,WaveSolution> selection = new BinaryTournamentSelection(random);
        SolutionListEvaluator<WaveSolution> evaluator = new SequentialSolutionListEvaluator<>();

        GenerationalGeneticAlgorithm<WaveSolution> algorithm = new GenerationalGeneticAlgorithm<>(
                problem, maxEvaluations, populationSize, crossover, mutation, selection, evaluator);
        
        algorithm.run();

        WaveSolution result = algorithm.getResult();
        return new ChallengeSolution(
            result.getOrders().stream().collect(Collectors.toSet()),
            result.getAisles().stream().collect(Collectors.toSet())
        ); // return best solution

    }

    private static class SinglePointCrossover implements CrossoverOperator<WaveSolution> {
        
        private double crossoverProbability;
        private Random random;
        private Integer totalAislesNumber;

        public SinglePointCrossover(double crossoverProbability, Random random, Integer totalAislesNumber) {
            if (crossoverProbability < 0) {
                throw new JMetalException("Crossover probability is negative: " + crossoverProbability);
            }
            this.crossoverProbability = crossoverProbability;
            this.random = random;
            this.totalAislesNumber = totalAislesNumber;
        }

        @Override
        public List<WaveSolution> execute(List<WaveSolution> parents) {
            Check.isNotNull(parents);
            Check.that(parents.size() == 2, "There must be two parents instead of " + parents.size());

            return doCrossover(crossoverProbability, parents);
        }

        public List<WaveSolution> doCrossover(double probability, List<WaveSolution> parents) {
            List<WaveSolution> offspring = new ArrayList<>(2);
            offspring.add((WaveSolution) parents.get(0).copy());
            offspring.add((WaveSolution) parents.get(1).copy());

            if (random.nextDouble() < probability) {

                // 1. Calculate the point to make the crossover
                int crossoverPoint = random.nextInt(totalAislesNumber-1); // between 0 and totalBits-1

                // 2. Swap aisles from parents after the crossover point
                for (int k = 0; k < 2; k++) { // for each parent
                    for (int i = 0; i < parents.get(k).getAisles().size(); i++) { // for each aisle in parent k
                        int aisleId = parents.get(k).getAisles().get(i);
                        if (aisleId > crossoverPoint && // aisle is after crossover point
                            !parents.get(1-k).getAisles().contains(aisleId) // skip if aisle is already in both parents
                        ) {
                            offspring.get(k).removeAisle(aisleId);
                            offspring.get(1-k).addAisle(aisleId);
                            // swap aisle between offspring
                        }
                    }
                }

                // 3. Set orders subset as the union of both parents' orders
                for (int k = 0; k < 2; k++) { // for each parent
                    for (int i = 0; i < parents.get(1-k).getOrders().size(); i++) { // for each order in parent 1-k
                        int orderId = parents.get(1-k).getOrders().get(i);
                        if (!parents.get(k).getOrders().contains(orderId)) {
                            offspring.get(k).addOrder(orderId);
                            // add missing orders to offspring
                        }
                    }
                }

            }

            return offspring;
        }

        @Override
        public int getNumberOfRequiredParents() {
            return 2;
        }
        @Override
        public int getNumberOfGeneratedChildren(){
            return 2;
        }

        @Override
        public double getCrossoverProbability() {
            return crossoverProbability;
        }
    }

    private static class BitFlipMutation implements MutationOperator<WaveSolution> {
        
        private double mutationProbability;
        private Random random;
        private int totalOrdersNumber;
        private int totalAislesNumber;

        public BitFlipMutation(double mutationProbability, Random random, int totalOrdersNumber, int totalAislesNumber) {
            this.mutationProbability = mutationProbability;
            this.random = random;
            this.totalOrdersNumber = totalOrdersNumber;
            this.totalAislesNumber = totalAislesNumber;
        }

        @Override
        public WaveSolution execute(WaveSolution solution) {
            Check.isNotNull(solution);

            doMutation(mutationProbability, solution);
            return solution;
        }

        public void doMutation(double probability, WaveSolution solution) {
            for (int i = 0; i < totalOrdersNumber; i++) {
                if (random.nextDouble() < probability) {
                    // flip order bit
                    if (solution.getOrders().contains(i)) {
                        solution.removeOrder(i);
                    } else {
                        solution.addOrder(i);
                    }
                }
            }
            for (int i = 0; i < totalAislesNumber; i++) {
                if (random.nextDouble() < probability) {
                    // flip aisle bit
                    if (solution.getAisles().contains(i)) {
                        solution.removeAisle(i);
                    } else {
                        solution.addAisle(i);
                    }
                }
            }
        }

        @Override
        public double getMutationProbability() {
            return mutationProbability;
        }

    }

    private static class BinaryTournamentSelection implements SelectionOperator<List<WaveSolution>,WaveSolution> {
        
        private Comparator<WaveSolution> comparator;
        private final int n_arity;
        private Random random;

        public BinaryTournamentSelection(Random random) {
            this.comparator = new DominanceComparator<WaveSolution>();
            this.random = random;
            this.n_arity = 2;
        }

        @Override
        public WaveSolution execute(List<WaveSolution> solutionList) {
            Check.isNotNull(solutionList);
            Check.collectionIsNotEmpty(solutionList);

            WaveSolution result;
            if (solutionList.size() == 1) {
            result = solutionList.get(0);
            } else {
            result = SolutionListUtils.selectNRandomDifferentSolutions(1, solutionList, (low, up) -> random.nextInt(low, up)).get(0);
            int count = 1; // at least 2 solutions are compared
            do {
                WaveSolution candidate = SolutionListUtils.selectNRandomDifferentSolutions(1, solutionList, (low, up) -> random.nextInt(low, up)).get(0);
                result = SolutionUtils.getBestSolution(result, candidate, comparator) ;
            } while (++count < this.n_arity);
            }

            return result;
        }
    }
}

