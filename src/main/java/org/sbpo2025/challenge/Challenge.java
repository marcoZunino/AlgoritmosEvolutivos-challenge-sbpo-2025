package org.sbpo2025.challenge;

import org.apache.commons.lang3.time.StopWatch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Challenge {

    private List<Map<Integer, Integer>> orders;
    private List<Map<Integer, Integer>> aisles;
    private int nItems;
    private int waveSizeLB;
    private int waveSizeUB;

    public void readInput(String inputFilePath) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
            String line = reader.readLine();
            String[] firstLine = line.split(" ");
            int nOrders = Integer.parseInt(firstLine[0]);
            int nItems = Integer.parseInt(firstLine[1]);
            int nAisles = Integer.parseInt(firstLine[2]);

            // Initialize orders and aisles arrays
            orders = new ArrayList<>(nOrders);
            aisles = new ArrayList<>(nAisles);
            this.nItems = nItems;

            // Read orders
            readItemQuantityPairs(reader, nOrders, orders);

            // Read aisles
            readItemQuantityPairs(reader, nAisles, aisles);

            // Read wave size bounds
            line = reader.readLine();
            String[] bounds = line.split(" ");
            waveSizeLB = Integer.parseInt(bounds[0]);
            waveSizeUB = Integer.parseInt(bounds[1]);

            reader.close();
        } catch (IOException e) {
            System.err.println("Error reading input from " + inputFilePath);
            e.printStackTrace();
        }
    }

    private void readItemQuantityPairs(BufferedReader reader, int nLines, List<Map<Integer, Integer>> orders) throws IOException {
        String line;
        for (int orderIndex = 0; orderIndex < nLines; orderIndex++) {
            line = reader.readLine();
            String[] orderLine = line.split(" ");
            int nOrderItems = Integer.parseInt(orderLine[0]);
            Map<Integer, Integer> orderMap = new HashMap<>();
            for (int k = 0; k < nOrderItems; k++) {
                int itemIndex = Integer.parseInt(orderLine[2 * k + 1]);
                int itemQuantity = Integer.parseInt(orderLine[2 * k + 2]);
                orderMap.put(itemIndex, itemQuantity);
            }
            orders.add(orderMap);
        }
    }

    public void writeOutput(ChallengeSolution challengeSolution, String outputFilePath) {
        if (challengeSolution == null) {
            System.err.println("Solution not found");
            return;
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));
            var orders = challengeSolution.orders();
            var aisles = challengeSolution.aisles();

            // Write the number of orders
            writer.write(String.valueOf(orders.size()));
            writer.newLine();

            // Write each order
            for (int order : orders) {
                writer.write(String.valueOf(order));
                writer.newLine();
            }

            // Write the number of aisles
            writer.write(String.valueOf(aisles.size()));
            writer.newLine();

            // Write each aisle
            for (int aisle : aisles) {
                writer.write(String.valueOf(aisle));
                writer.newLine();
            }

            writer.close();
            System.out.println("Output written to " + outputFilePath);

        } catch (IOException e) {
            System.err.println("Error writing output to " + outputFilePath);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Start the stopwatch to track the running time
        StopWatch stopWatch = StopWatch.createStarted();
        Map<String, Object> params = new HashMap<>();

        String algorithm = "";
        String GAimplementation = "";

        if (args.length == 0) {

            String defaultInstance = "x/instance_0002.txt"; // Default instance number

            algorithm = "genetic";
            GAimplementation = "generational";

            args = new String[]{
                    "datasets/"+defaultInstance,                    
            };
        }

        String inputFilePath = args[0];

        // Usage: 
        // >> java -jar target/ChallengeSBPO2025-1.0.jar <inputfile> ...
        //    [genetic|greedy]
        //    params:<randomSeed>/<iterations>/<generations>/<populationSize>/<crossoverProbability>/[mutationProbability]
        //    [binaryEncoding] [showStats] [showOutput] [ordersUnionCrossover] [defaultCrossover]

        if (Arrays.asList(args).contains("genetic")) {
            
            algorithm = "genetic";
            
            if (Arrays.asList(args).contains("steadyState")) {
                GAimplementation = "steadyState";
            } else if (Arrays.asList(args).contains("generational")) {
                GAimplementation = "generational";
            }

        } else if (Arrays.asList(args).contains("greedy")) {
            algorithm = "greedy";
        }

        String paramsArg = null;

        // search for the argument that starts with "params:"
        for (String arg : args) {
            if (algorithm == "genetic" && arg.startsWith("params:")) {
                paramsArg = arg;
                break;
            }
        }

        long seed; int iterations; int populationSize; int generations; double crossoverProb; double mutationProb;
        // parse required parameters
        try {
            String raw = paramsArg.substring("params:".length());
            String[] parts = raw.split("/");

            seed = Long.parseLong(parts[0]);
            iterations = Integer.parseInt(parts[1]);
            generations = Integer.parseInt(parts[2]);
            populationSize = Integer.parseInt(parts[3]);
            crossoverProb = Double.parseDouble(parts[4]);
            mutationProb = parts.length > 5 ? Double.parseDouble(parts[5]) : -1; //optional

        } catch (Exception e) {
            System.out.println(e.getMessage() + "\n -> Default parameters will be used.");
            seed = 12345L;
            iterations = 1;
            populationSize = 50;
            generations = 100;
            crossoverProb = 0.9;
            mutationProb = -1; // will be set later based on encoding
        }

        params.put("randomSeed", seed);
        params.put("maxIterations", iterations);
        params.put("populationSize", populationSize);
        params.put("generations", generations);
        params.put("crossoverProbability", crossoverProb);
        if (mutationProb >= 0) params.put("mutationProbability", mutationProb);
        
        params.put("binaryEncoding", Arrays.asList(args).contains("binaryEncoding"));
        params.put("showStats", Arrays.asList(args).contains("showStats"));
        params.put("showOutput", Arrays.asList(args).contains("showOutput"));
        params.put("ordersUnionCrossover", !Arrays.asList(args).contains("defaultCrossover"));
        params.put("algorithm", algorithm); // "greedy" or "genetic"
        params.put("GAimplementation", GAimplementation); // "generational" or "steadyState"

        // System.out.println(params);

        Challenge challenge = new Challenge();
        challenge.readInput(inputFilePath);

        String[] split = inputFilePath.split("/");
        String instance = split[split.length-1];
        System.out.println("Processing instance: " + instance);
        String dataset = split[split.length-2];

        var challengeSolver = new ChallengeSolver(
                challenge.orders, challenge.aisles, challenge.nItems, challenge.waveSizeLB, challenge.waveSizeUB);

        ChallengeSolution challengeSolution = challengeSolver.solve(stopWatch, params);
        
        String outputFilePath = String.format("output/%s/%s/%s", algorithm, dataset, instance);
        System.out.println("Writing output to: " + outputFilePath);
        challenge.writeOutput(challengeSolution, outputFilePath);
        
    }
}
