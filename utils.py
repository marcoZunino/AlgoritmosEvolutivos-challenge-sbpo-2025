import pandas as pd
from scipy.stats import wilcoxon

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



def display_experiment_results(instances):
    
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

def display_experiment_1_1(experiments):

    rows = []

    for exp in experiments:
        rows.append({
            "instance": f"{exp.instance.dataset}/{exp.instance.id}",
            "algorithm": exp.algorithm,
            "encoding": exp.parameters["encoding"],
            "run": exp.run_id,
            "objective_value": exp.objective_value,
            "execution_time": exp.execution_time,
        })

    df = pd.DataFrame(rows)

    summary = (
        df.groupby(["instance", "algorithm", "encoding"])
        .agg(
            mean_objective=("objective_value", "mean"),
            std_objective=("objective_value", "std"),
            max_objective=("objective_value", "max"),
            mean_time=("execution_time", "mean"),
            std_time=("execution_time", "std"),
            max_time=("execution_time", "max")
        )
        .reset_index()
    )

    summary = summary.round(2)
    
    return summary


def display_experiment_1_2(experiments):

    rows = []

    for exp in experiments:
        rows.append({
            "instance": f"{exp.instance.dataset}/{exp.instance.id}",
            "algorithm": exp.algorithm,
            "crossover_type": exp.parameters["crossover_type"],
            "run": exp.run_id,
            "objective_value": exp.objective_value,
            "execution_time": exp.execution_time,
        })

    df = pd.DataFrame(rows)

    summary = (
        df.groupby(["instance", "algorithm", "crossover_type"])
        .agg(
            mean_objective=("objective_value", "mean"),
            std_objective=("objective_value", "std"),
            max_objective=("objective_value", "max"),
            mean_time=("execution_time", "mean"),
            std_time=("execution_time", "std"),
            max_time=("execution_time", "max")
        )
        .reset_index()
    )

    summary = summary.round(2)
    
    return summary


def display_experiment_1_3(experiments):

    rows = []

    for exp in experiments:
        rows.append({
            "instance": f"{exp.instance.dataset}/{exp.instance.id}",
            "algorithm": exp.algorithm,
            "start": exp.parameters["start"],
            "final_objective_value": exp.objective_value,
            "total_execution_time": exp.execution_time,
            "greedy_objective_value": exp.instance.greedy_result,
            "best_result" : exp.instance.best_result,
        })

    df = pd.DataFrame(rows)

    return df.round(2)

def display_best_results(instances):

    rows = []

    for inst in instances:
        rows.append({
            "instance": f"{inst.dataset}/{inst.id}",
            "best_result": inst.best_result,
            "greedy_result": inst.greedy_result,
        })

    df = pd.DataFrame(rows)

    return df.round(2)




def display_wilcoxon_test(experiments):

    df = pd.DataFrame([
        {
            "instance": f"{exp.instance.dataset}/{exp.instance.id}",
            "algorithm": exp.algorithm,
            "encoding": exp.parameters["encoding"],
            "run": exp.run_id,
            "objective_value": exp.objective_value,
            "execution_time": exp.execution_time
        }
        for exp in experiments
    ])
    
    results = []

    for (instance, algorithm), group in df.groupby(["instance", "algorithm"]):

        # separar valores por compared_parameter
        subset_vals = group[group["encoding"] == "subset"]["objective_value"].values
        binary_vals = group[group["encoding"] == "binary"]["objective_value"].values

        # asegurar que hay pares
        if len(subset_vals) == len(binary_vals) and len(subset_vals) > 0:
            stat, p = wilcoxon(subset_vals, binary_vals)
            results.append({
                "instance": instance,
                "algorithm": algorithm,
                "wilcoxon_stat": stat,
                "p_value": p
                # p < 0.05 → diferencia significativa
                # p ≥ 0.05 → no hay evidencia suficiente
            })
    
    results_df = pd.DataFrame(results)
    return results_df.round(4)


    
    
