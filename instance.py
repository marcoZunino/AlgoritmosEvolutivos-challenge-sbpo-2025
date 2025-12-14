import os
import json
import pandas as pd

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
            print(f"Stats loaded from {self.stats_file}")

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
        
        print(f"Stats saved to {file_path}")

    @property
    def stats_file(self):
        return os.path.join("stats", self.dataset, f"instance_{self.id}.json")
        

def display_stats_table(instances):
    rows = []

    for inst in instances:
        rows.append({
            "instance": f"{inst.dataset}/{inst.id}",
            "#aisles": inst.aisles_count,
            "#orders": inst.orders_count,
            "#items": inst.items_count,
            "LB": inst.wave_size_lb,
            "UB": inst.wave_size_ub,
            "mean_aisle_capacity": inst.mean_aisle_capacity,
            "mean_order_size": inst.mean_order_size,
            "mean_items_per_aisle": inst.mean_items_per_aisle,
            "mean_items_per_order": inst.mean_items_per_order,
        })

    df = pd.DataFrame(rows)
    df = df.round(2)

    return df
    

