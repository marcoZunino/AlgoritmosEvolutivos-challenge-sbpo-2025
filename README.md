# Project Structure

This workspace contains a solution for the SBPO 2025 Challenge, implementing genetic algorithms for a Wave Order Picking optimization problem.

## Core Components

### 📁 Main Directories

- **`datasets`** - Contains problem instances organized by difficulty:
  - `a/` - Small instances (e.g., `datasets/a/instance_0002.txt`)
  - `b/` - Medium instances
  - `x/` - Large/complex instances

- **`src/main/java/org/sbpo2025/challenge`** - Java implementation:
  - `Challenge.java` - Main entry point, handles I/O
  - `ChallengeSolver.java` - Core solver with greedy heuristics
  - `ChallengeSolution.java` - Solution representation (orders + aisles)

### 🧬 Genetic Algorithm Implementation

**genetic_algorithm/**

Two encoding strategies:

1. **Binary Encoding** (`binary_genetic_algorithm/`):
   - `BinaryWavePickingProblem.java` - Problem definition
   - `BinaryGeneticAlgorithmRunner.java` - Runner with HUX crossover + bit-flip mutation

2. **Subset Encoding** (`subset_genetic_algorithm/`):
   - `WavePickingProblem.java` - Problem with warm/random start
   - `WaveSolution.java` - Custom solution type
   - `GeneticAlgorithmRunner.java` - Runner with custom operators

### 🧪 Experimentation Framework

- **experiments/ExperimentsFactory.java** - Factory for creating algorithm configurations:
  - Enums: `EncodingType`, `CrossoverType`, `StartType`, `AlgorithmType`
  - `algorithmBuilder()` - Configures GA variants (generational/steady-state)

- **experiments/ParametersConfiguration.java** - Parameter tuning experiments

### 🐍 Python Analysis Layer

- **`entities.py`** - Core Python classes:
  - `Instance` - Represents problem instances, stores stats
  - `Experiment` - Manages experiment execution and result collection

- **`utils.py`** - Analysis utilities:
  - `display_experiment_1_1()` - Encoding comparison
  - `display_experiment_1_2()` - Crossover type comparison
  - `display_experiment_2()` - Parameter tuning results
  - `plot_evolution_start_conf()` - Visualization

- **`checker.py`** - `WaveOrderPicking` class for solution validation:
  - `is_solution_feasible()` - Constraint checking
  - `compute_objective_function()` - Objective value calculation

- **`experimental_results.ipynb`** - Jupyter notebook with comprehensive experimental analysis

### 📊 Output Structure

- **`output`** - Solution files by algorithm/dataset
- **`experiments`** - Experimental results organized by batch:
  - `solutions/` - Raw solution files
  - `results/` - JSON files with feasibility, objective values, execution times
- **`stats`** - Precomputed instance statistics (JSON)
- **`best_solutions`** - Best known solutions (`best_solutions/best_objectives.csv`)

## Key Files

- **`pom.xml`** - Maven build configuration (jMetal dependency)
- **`requirements.txt`** - Python dependencies
- **running_X.py** - Scripts for batch experiment execution

## Execution Flow

1. Java solver runs via `Challenge.main()`
2. Python `Experiment.run()` orchestrates execution
3. Results validated by `checker.py`
4. Analysis performed in `experimental_results.ipynb`