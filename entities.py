import os
import json
import time
import subprocess


from checker import WaveOrderPicking

class Instance:

    def __init__(self, dataset, id, input_file):

        self.dataset = dataset
        self.id = id
        self.input_file = input_file
        self.aisles_count = None
        self.orders_count = None
        self.items_count = None
        self.wave_size_lb = None
        self.wave_size_ub = None
        self.mean_aisle_capacity = 0
        self.mean_order_size = 0
        self.mean_items_per_aisle = 0
        self.mean_items_per_order = 0
        self.experiments = []
        self.best_result = None
        self.greedy_result = None
        
        try:
            self.read_stats()
        except FileNotFoundError:
            self.compute_stats()
            self.save_stats()


    def read_stats(self):
        try:
            with open(self.stats_file, "r") as f:
                data = json.load(f)
                self.aisles_count = data["aisles_count"]
                self.orders_count = data["orders_count"]
                self.items_count = data["items_count"]
                self.wave_size_lb = data["wave_size_lb"]
                self.wave_size_ub = data["wave_size_ub"]
                self.mean_aisle_capacity = data["mean_aisle_capacity"]
                self.mean_order_size = data["mean_order_size"]
                self.mean_items_per_aisle = data["mean_items_per_aisle"]
                self.mean_items_per_order = data["mean_items_per_order"]
            # print(f"Stats loaded from {self.stats_file}")

        except:
            raise FileNotFoundError("Stats file not found.")
        

    def compute_stats(self):
        
        with open(self.input_file, 'r') as file:
            lines = file.readlines()
            first_line = lines[0].strip().split()
            o, i, a = int(first_line[0]), int(first_line[1]), int(first_line[2])

            # Read orders

            for j in range(o):
                order_line = lines[j + 1].strip().split()
                d = int(order_line[0])
                self.mean_items_per_order += d
                order_map = {int(order_line[2 * k + 1]): int(order_line[2 * k + 2]) for k in range(d)}
                self.mean_order_size += sum(order_map.values())

            # Read aisles

            for j in range(a):
                aisle_line = lines[j + o + 1].strip().split()
                d = int(aisle_line[0])
                self.mean_items_per_aisle += d
                aisle_map = {int(aisle_line[2 * k + 1]): int(aisle_line[2 * k + 2]) for k in range(d)}
                self.mean_aisle_capacity += sum(aisle_map.values())

            # Read wave size bounds
            bounds = lines[o + a + 1].strip().split()
            self.wave_size_lb, self.wave_size_ub = int(bounds[0]), int(bounds[1])
            self.orders_count, self.items_count, self.aisles_count = o, i, a
            self.mean_aisle_capacity /= a
            self.mean_order_size /= o
            self.mean_items_per_aisle /= a
            self.mean_items_per_order /= o

    def save_stats(self):
        # Build directory path: stats/{dataset}/instance_{id}
        dir_path = os.path.join("stats", self.dataset)
        os.makedirs(dir_path, exist_ok=True)

        # File path: stats/{dataset}/instance_{id}.json
        file_path = self.stats_file

        # Collect all attributes except methods
        data = {
            "input_file": self.input_file,
            "aisles_count": self.aisles_count,
            "orders_count": self.orders_count,
            "items_count": self.items_count,
            "wave_size_lb": self.wave_size_lb,
            "wave_size_ub": self.wave_size_ub,
            "mean_aisle_capacity": self.mean_aisle_capacity,
            "mean_order_size": self.mean_order_size,
            "mean_items_per_aisle": self.mean_items_per_aisle,
            "mean_items_per_order": self.mean_items_per_order
        }

        # Save JSON
        with open(file_path, "w") as f:
            json.dump(data, f, indent=4)
        
        # print(f"Stats saved to {file_path}")

    @property
    def stats_file(self):
        return os.path.join("stats", self.dataset, f"instance_{self.id}.json")
        

class Experiment:

    default_parameters = {
        "generations" : 50,
        "population_size" : 60,
        "crossover_rate" : 0.9,
        "mutation_rate" : 0.001,
        "initial_seed" : 12345,
        "iterations" : 1,
        # "encoding": "subset",
        # "crossover_type": "orders_union",
        # "start" : "warm_start"
    }

    algo_map = {
        "gGA": ["genetic", "generational"],
        "ssGA": ["genetic", "steadyState"],
        "greedy": ["greedy"]
    }
    
    checker = WaveOrderPicking()

    def __init__(self, batch_name, instance, algorithm, run_id):
        
        self.batch_name = batch_name
        self.instance = instance
        
        self.algorithm = algorithm # "greedy" / "gGA" / "ssGA"
        self.parameters = {
            **Experiment.default_parameters
        }
        
        self.run_id =run_id

        self.feasibility = None
        self.objective_value = None
        self.execution_time = None
    
    def set_param(self, param_name, param_value):
        self.parameters[param_name] = param_value
    
    @property
    def seed(self):
        s = self.run_id
        if not self.parameters is None and "initial_seed" in self.parameters:
            s += self.parameters["initial_seed"]
        return s


    def run(self, show_output=False):
        
        if self.compute_result():
            return

        # Ensure output directory exists
        out_dir = os.path.dirname(self.solution_file)
        os.makedirs(out_dir, exist_ok=True)

        cmd = self.run_cmd(show_output)

        # Run solver and measure time
        start = time.time()
        print(cmd)
        subprocess.run(cmd, check=True)
        end = time.time()

        self.execution_time = end - start

        self.compute_result()

    def run_cmd(self, show_output=False):

        # Build algorithm mode
        # "gGA" → "genetic generational"
        # "ssGA" → "genetic steadyState"
        # "greedy" → "greedy"
        algo_args = Experiment.algo_map[self.algorithm]

        # Build full command
        cmd = [
            "java", "-jar", "target/ChallengeSBPO2025-1.0.jar",
            self.instance.input_file, *algo_args, f"output:{self.solution_file}"
        ]

        # Build parameter string for the Java solver
        # Example: params:629/1/10/60/0.9
        if not self.parameters is None:
            params = f"params:{self.seed}/" \
                 f"{self.parameters['iterations']}/" \
                 f"{self.parameters['generations']}/" \
                 f"{self.parameters['population_size']}/" \
                 f"{self.parameters['crossover_rate']}/" \
                 f"{self.parameters['mutation_rate']}"
            cmd.append(params)
            if self.encoding == "binary": cmd.append("binaryEncoding")
            if self.crossover_type == "default": cmd.append("defaultCrossover")
            if self.start == "random": cmd.append("randomStart")

        if show_output: cmd.append("showOutput")

        return cmd
        

    def compute_result(self):

        try:
            with open(self.result_file, "r") as f:
                data = json.load(f)
                self.feasibility = data["feasibility"]
                self.objective_value = data["objective_value"]
                self.execution_time = data["execution_time"]
            return True
        except:
            try:
                result = Experiment.checker.check_result(self.instance.input_file, self.solution_file)
            except:
                return False
        
        self.feasibility = result["is_feasible"]
        if self.feasibility:
            self.objective_value = result["objective_value"]

        os.makedirs(os.path.dirname(self.result_file), exist_ok=True)

        file_path = self.result_file

        data = {
            "objective_value": self.objective_value,
            "feasibility": self.feasibility,
            "execution_time": self.execution_time
        }

        # Save JSON
        with open(file_path, "w") as f:
            json.dump(data, f, indent=4)

        return True
        
    @property
    def encoding(self):
        return self.parameters.get("encoding", None) # None / "subset" / "binary"
    
    @property
    def crossover_type(self):
        return self.parameters.get("crossover_type", None) # None / "orders_union" / "default"
    
    @property
    def start(self):
        return self.parameters.get("start", None) # None / "warm" / "random"

    @property
    def solution_file(self):
        return os.path.join("experiments", self.batch_name, "solutions", f"{self.instance.dataset}_{self.instance.id}", f"{self.algorithm}_{self.parameters_string()}", f"run{self.run_id}.txt")
    
    @property
    def result_file(self):
        return os.path.join("experiments", self.batch_name, "results", f"{self.instance.dataset}_{self.instance.id}", f"{self.algorithm}_{self.parameters_string()}", f"run{self.run_id}.json")
    
    def __str__(self):
        return f"Experiment(batch: {self.batch_name}, instance: {self.instance.dataset}/{self.instance.id}, algorithm: {self.algorithm}, run: {self.run_id}, feasibility: {self.feasibility}, objective_value: {self.objective_value}, execution_time: {self.execution_time})"
    
    def parameters_string(self):
        string = ""
        if self.parameters is None:
            return ""
        if self.encoding is not None:
            string += f"{self.encoding}_"
        if self.crossover_type is not None:
            string += f"{self.crossover_type}_"
        if self.start is not None:
            string += f"{self.start}_"
        string += f"gen{self.parameters['generations']}_pop{self.parameters['population_size']}_cr{self.parameters['crossover_rate']}_mr{self.parameters['mutation_rate']}"
        return string
    



datasets = {"a": {}, "b": {}, "x": {}}
instances = {}


for dataset in ["a", "b", "x"]:
    for file in os.listdir(f"datasets/{dataset}"):
        instanceId = file.split("_")[1].split(".")[0]
        instance = Instance(dataset, instanceId, f"datasets/{dataset}/{file}")
        instances[f"{dataset}/{instanceId}"] = instance
        datasets[dataset][instanceId] = instance

experiment_instances = {
    "a": [i for i in datasets["a"].values() if i.id in ["0001", "0004", "0009", "0017", "0019"]],
    "b": [i for i in datasets["b"].values() if i.id in ["0001", "0003", "0007", "0009"]],
    "x": [i for i in datasets["x"].values() if i.id in ["0001", "0003", "0007"]]
}


experiments = {}
  
batch_name = "evaluation"
algorithm = "ssGA"

cx_prob = 1.0 # ajustar
mut_prob = 0.001
pop_size = 80
independent_runs = 30

experiments[batch_name] = []

for instance in experiment_instances["x"]: # 1, 3, 7, 8
    for run in range(independent_runs):
        exp = Experiment(batch_name, instance, algorithm, run)
        exp.set_param("crossover_rate", cx_prob)
        exp.set_param("mutation_rate", mut_prob)
        exp.set_param("population_size", pop_size)
        experiments[batch_name].append(exp)



