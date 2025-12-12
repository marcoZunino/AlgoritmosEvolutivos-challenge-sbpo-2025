package org.sbpo2025.challenge.genetic_algorithm;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.ArrayList;
import java.util.Collections;

import org.sbpo2025.challenge.Item;
import org.uma.jmetal.problem.AbstractGenericProblem;

public class WavePickingProblem extends AbstractGenericProblem<WaveSolution> {

    protected List<Map<Integer, Integer>> orders;
    protected List<Map<Integer, Integer>> aisles;
    protected List<Item> items;
    protected int waveSizeLB;
    protected int waveSizeUB;
    protected Random random;
    protected boolean showOutput;

    protected double waveSizePenalty;

    public WavePickingProblem(List<Map<Integer, Integer>> orders,
      List<Map<Integer, Integer>> aisles,
      List<Item> items,
      int waveSizeLB,
      int waveSizeUB,
      Random random
    ) {
    
      this.orders = orders;
      this.aisles = aisles;
      this.waveSizeLB = waveSizeLB;
      this.waveSizeUB = waveSizeUB;
      this.items = items;
      
      this.random = random;
      this.waveSizePenalty = orders.size() - waveSizeLB/aisles.size(); // default penalty
      this.showOutput = false;

      this.setNumberOfVariables(2);
      this.setNumberOfObjectives(1);
      this.setName("WavePickingProblem");      
      
    }


    public void setWaveSizePenalty(double penalty) {
        this.waveSizePenalty = penalty;
    }

    public void showOutput() {
        this.showOutput = true;
    }


    @Override
    public void evaluate(WaveSolution solution) {
        // Evaluation logic to be implemented
        if (!feasible(solution)) {
            feasibilityCorrection(solution);
            if (showOutput) System.out.println("Feasibility correction applied");
        }

        // Objective function: total units picked / number of visited aisles
        double objectiveValue = computeObjectiveValue(solution);
        int penalization = waveSizePenalization(solution);
        solution.setObjective(0, -(objectiveValue - waveSizePenalty*(double)penalization));
        
        if (showOutput) System.out.println(String.format("""
            Evaluated solution with objective value: %f %s
                fitness: %f""",
            objectiveValue, 
            penalization != 0 ? String.format("and penalization: %d",penalization) : "",
            solution.getObjective(0)));
    }

    @Override
    public WaveSolution createSolution() {
        return new WaveSolution(getRandomSubset(IntStream.range(0, orders.size()).boxed().collect(Collectors.toList())),
                                getRandomSubset(IntStream.range(0, aisles.size()).boxed().collect(Collectors.toList())));
    }

    private double computeObjectiveValue(WaveSolution solution) {
        
        List<Integer> selectedOrders = solution.getOrders();
        List<Integer> visitedAisles = solution.getAisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return 0.0;
        }
        int totalUnitsPicked = 0;

        // Calculate total units picked
        for (int order : selectedOrders) {
            totalUnitsPicked += orders.get(order).values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        // Calculate the number of visited aisles
        int numVisitedAisles = visitedAisles.size();
        return (double) totalUnitsPicked / numVisitedAisles;
        // return (double) selectedOrders.size();
        // return (double) 1.0 / numVisitedAisles;
        // return (double) numVisitedAisles;
        // return (double) selectedOrders.size() + numVisitedAisles;
    }

    private int waveSizePenalization(WaveSolution solution) {
        
        int totalUnitsPicked = totalDemand(solution.getOrders());

        if (totalUnitsPicked < waveSizeLB) {
            return waveSizeLB - totalUnitsPicked;
        } else if (totalUnitsPicked > waveSizeUB) {
            return totalUnitsPicked - waveSizeUB;
        } else {
            return 0;
        }
    }

    
    private void feasibilityCorrection(WaveSolution solution) {

        // int demand = totalDemand(solution.getOrders());
        // while (demand < waveSizeUB) {
        //     // add random order
        //     int o = random.nextInt(orders.size());
        //     if (!solution.getVariable(0).get(o) && (availableCapacity(List.of(o), solution.getAisles()))) {
        //         solution.getVariable(0).set(o, true);
        //         demand += totalDemand(List.of(o));
        //     }
        // }
        for (Item item : items) {
            int itemDemand = totalDemand(solution.getOrders(), List.of(item));
            int itemCapacity = totalCapacity(solution.getAisles(), List.of(item));
            while (itemDemand > itemCapacity) {
                // remove random order that contains item i
                List<Integer> itemOrders = item.orders.keySet().stream()
                    .filter(o -> solution.getOrders().contains(o))
                    .collect(Collectors.toList());
                int oToRemove = itemOrders.get(random.nextInt(itemOrders.size()));
                solution.removeOrder(oToRemove);
                itemDemand -= item.getOrderDemand(oToRemove);
                // demand -= totalDemand(List.of(oToRemove));

            }
        }
        // while (demand > waveSizeUB) {
        //     // remove random order
        //     List<Integer> selectedOrders = solution.getOrders().stream().collect(Collectors.toList());
        //     if (selectedOrders.isEmpty()) {
        //         break;
        //     }
        //     int oToRemove = selectedOrders.get(random.nextInt(selectedOrders.size()));
        //     solution.removeOrder(oToRemove);
        //     demand -= totalDemand(List.of(oToRemove));
        // }

        // removeUnusedAisles(solution);

        int capacity = totalCapacity(solution.getAisles());
        while (capacity < waveSizeLB) {
            // add random aisle
            int a = random.nextInt(aisles.size());
            if (!solution.getAisles().contains(a)) {
                solution.addAisle(a);
                capacity += totalCapacity(List.of(a));
            }
        }

        
    }

    // private void removeUnusedAisles(WaveSolution solution) {

    //     List<Integer> selectedOrders = solution.getOrders();
    //     List<Integer> visitedAisles = solution.getAisles();

    //     for (int aisle : visitedAisles) {
    //         // Check if removing this aisle still keeps the solution feasible
    //         List<Integer> aislesWithoutCurrent = visitedAisles.stream()
    //             .filter(a -> a != aisle)
    //             .collect(Collectors.toList());
    //         if (availableCapacity(selectedOrders, aislesWithoutCurrent)) {
    //             // Remove the aisle
    //             solution.removeAisle(aisle);
    //             removeUnusedAisles(solution);
    //             break;
    //         }
    //     }

    // }

    private boolean feasible(WaveSolution solution) {
        return availableCapacity(solution.getOrders(), solution.getAisles());
    }

    private int totalCapacity(List<Integer> aislesList, List<Item> itemsList) {

        int totalCapacity = 0;

        for (int aisle : aislesList) {
            for (Item item : itemsList) {
                totalCapacity += item.getAisleCapacity(aisle);
            }
        }

        return totalCapacity;
    }
    private int totalCapacity(List<Integer> aislesList) {
        return totalCapacity(aislesList, items);
    }

    private int totalDemand(List<Integer> ordersList, List<Item> itemsList) {
        int totalDemand = 0;

        for (int order : ordersList) {
            for (Item item : itemsList) {
                totalDemand += item.getOrderDemand(order);
            }
        }

        return totalDemand;
    }
    private int totalDemand(List<Integer> ordersList) {
        return totalDemand(ordersList, items);
    }

    private boolean availableCapacity(List<Integer> ordersList, List<Integer> aislesList) {
        
      for (Item item : items) {
            if (totalDemand(ordersList, List.of(item)) > totalCapacity(aislesList, List.of(item))) {
                return false;
            }
        }
        return true;
    }

    private List<Integer> getRandomSubset(List<Integer> set) {
        
        if (set.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> subset = new ArrayList<>(set); // copy the original set
        Collections.shuffle(subset, random);  // shuffle the copy

        int ordersCount = random.nextInt(set.size()) + 1; // at least one element
        return new ArrayList<>(subset.subList(0, ordersCount));  // return the first 'ordersCount' elements
    }


}