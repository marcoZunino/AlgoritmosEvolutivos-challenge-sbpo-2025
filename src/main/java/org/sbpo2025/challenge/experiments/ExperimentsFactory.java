package org.sbpo2025.challenge.experiments;

import java.util.List;

import org.sbpo2025.challenge.genetic_algorithm.AbstractWavePickingProblem;
import org.sbpo2025.challenge.genetic_algorithm.binary_genetic_algorithm.BinaryWavePickingProblem;
import org.sbpo2025.challenge.genetic_algorithm.subset_genetic_algorithm.WavePickingProblem;
import org.sbpo2025.challenge.genetic_algorithm.subset_genetic_algorithm.operators.WaveBitFlipMutation;
import org.sbpo2025.challenge.genetic_algorithm.subset_genetic_algorithm.operators.WaveTournamentSelection;
import org.sbpo2025.challenge.genetic_algorithm.subset_genetic_algorithm.operators.WaveUniformCrossover;

import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.singleobjective.geneticalgorithm.GenerationalGeneticAlgorithm;
import org.uma.jmetal.algorithm.singleobjective.geneticalgorithm.SteadyStateGeneticAlgorithm;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.HUXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.BitFlipMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.evaluator.impl.SequentialSolutionListEvaluator;

public class ExperimentsFactory {

    public enum EncodingType { BINARY_ENCODING, SUBSET_ENCODING }
    public enum CrossoverType { ORDERS_UNION, DEFAULT } // for subset encoding only
    public enum StartType { RANDOM_START, WARM_START } // for subset encoding only
    public enum AlgorithmType { GENERATIONAL, STEADY_STATE }

    @SuppressWarnings("unchecked")
    public static <S extends Solution<?>> Algorithm<List<S>> algorithmBuilder(
        AbstractWavePickingProblem<S> problem,
        double cxProb,
        double mutProb,
        int generations,
        int populationSize,
        long randomSeed,
        AlgorithmType algorithmType,
        EncodingType encodingType,
        CrossoverType crossoverType,
        StartType startType
    ) {
        problem.setRandomSeed(randomSeed);

        CrossoverOperator<S> crossover; MutationOperator <S> mutation; SelectionOperator<List<S>,S> selection;

        switch (encodingType) {
            case BINARY_ENCODING:

                if (!(problem instanceof BinaryWavePickingProblem)) {
                    throw new IllegalArgumentException("Incorrect encoding type for the given problem instance.");
                }

                crossover = (CrossoverOperator<S>) new HUXCrossover(cxProb);
                mutation = (MutationOperator<S>) new BitFlipMutation(mutProb);
                selection = new BinaryTournamentSelection<>();
                break;
            
            case SUBSET_ENCODING:
                
                if (!(problem instanceof WavePickingProblem)) {
                    throw new IllegalArgumentException("Incorrect encoding type for the given problem instance.");
                }
                switch (startType) {
                    case RANDOM_START: ((WavePickingProblem) problem).randomStart();
                        break;
                    case WARM_START: // do nothing, default is warm start
                        break;
                }

                crossover = (CrossoverOperator<S>) new WaveUniformCrossover(cxProb, switch (crossoverType) {
                    case ORDERS_UNION -> true;
                    case DEFAULT -> false;
                }, problem.random);
                mutation = (MutationOperator<S>) new WaveBitFlipMutation(mutProb, problem.orders.size(), problem.aisles.size(), problem.random);
                selection = new WaveTournamentSelection<>(problem.random);

                break;

            default:
                throw new IllegalArgumentException("Unsupported encoding type: " + encodingType);
        }
        
        Algorithm<S> ga;
        switch (algorithmType) {
            case GENERATIONAL:
                ga = new GenerationalGeneticAlgorithm<>(
                    problem,
                    generations * populationSize,                 // maxEvaluations
                    populationSize,
                    crossover,
                    mutation,
                    selection,
                    new SequentialSolutionListEvaluator<>()
                );
                break;

            case STEADY_STATE:
                ga = new SteadyStateGeneticAlgorithm<>(
                    problem,
                    generations * populationSize,                 // maxEvaluations
                    populationSize,
                    crossover,
                    mutation,
                    selection
                );
                break;

            default:
                throw new IllegalArgumentException("Unsupported algorithm type: " + algorithmType);
        }

        return new SingleObjectiveAlgorithmAdapter<>(ga);
    }
    public static <S extends Solution<?>> Algorithm<List<S>> algorithmBuilder(
        AbstractWavePickingProblem<S> problem,
        double cxProb,
        double mutProb,
        int generations,
        int populationSize,
        long randomSeed,
        AlgorithmType algorithmType,
        EncodingType encodingType
    ) {
        return algorithmBuilder(problem, cxProb, mutProb, generations, populationSize, randomSeed, algorithmType, encodingType, CrossoverType.ORDERS_UNION, StartType.WARM_START);
    }
    public static <S extends Solution<?>> Algorithm<List<S>> algorithmBuilder(
        AbstractWavePickingProblem<S> problem,
        double cxProb,
        double mutProb,
        int generations,
        int populationSize,
        long randomSeed,
        AlgorithmType algorithmType
    ) {
        return algorithmBuilder(problem, cxProb, mutProb, generations, populationSize, randomSeed, algorithmType, EncodingType.SUBSET_ENCODING, CrossoverType.ORDERS_UNION, StartType.WARM_START);
    }


    public static class SingleObjectiveAlgorithmAdapter<S extends Solution<?>> implements Algorithm<List<S>> {

        private final Algorithm<S> innerAlgorithm;

        public SingleObjectiveAlgorithmAdapter(Algorithm<S> innerAlgorithm) {
            this.innerAlgorithm = innerAlgorithm;
        }

        @Override
        public void run() {
            innerAlgorithm.run();
        }

        @Override
        public List<S> getResult() {
            // jMetalLab expects a LIST
            return List.of(innerAlgorithm.getResult());
        }

        @Override
        public String getName() {
            return innerAlgorithm.getName();
        }

        @Override
        public String getDescription() {
            return innerAlgorithm.getDescription();
        }
    }


}



