package org.sbpo2025.challenge.genetic_algorithm.subset_genetic_algorithm.operators;

import java.util.List;
import java.util.Random;

import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.checking.Check;

public class WaveTournamentSelection<S extends Solution<?>> implements SelectionOperator<List<S>, S> {
    
    private Random random;

    public WaveTournamentSelection(Random random) {
        this.random = random;
    }

    @Override
    public S execute(List<S> solutionList) {
        Check.isNotNull(solutionList);
        Check.collectionIsNotEmpty(solutionList);
        S result;

        if (solutionList.size() == 1) {
            result = solutionList.get(0);
        } else {
            List<S> candidates = SolutionListUtils.selectNRandomDifferentSolutions(2, solutionList, (low, up) -> random.nextInt(low, up));

            if (candidates.get(0).getObjective(0) < candidates.get(1).getObjective(0)) {
                result = candidates.get(0);
            } else {
                result = candidates.get(1);
            }
            
        }
        return result;
    }
}

