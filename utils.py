import pandas as pd
from scipy.stats import wilcoxon
import matplotlib.pyplot as plt
import os

# TikZ-style fonts
plt.rcParams.update({
    "font.family": "serif",
    "mathtext.fontset": "cm",
    "mathtext.rm": "serif",
})

# TikZ/PGFPlots color palette
TIKZ_COLORS = [
    "#1f77b4", "#ff7f0e", "#2ca02c", "#d62728",
    "#9467bd", "#8c564b", "#e377c2", "#7f7f7f",
    "#bcbd22", "#17becf"
]

LINE_STYLES = ["-", "--", "-.", ":"]
MARKERS = ["o", "s", "D", "^", "v", "x", "*"]
FIGURE_SIZE = (4, 3)

# LaTeX PGF backend
plt.rcParams.update({
    "pgf.texsystem": "pdflatex",
    "font.family": "serif",
    "text.usetex": True,
    "pgf.rcfonts": False,
})



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


def plot_evolution_start_conf(experiments, instance=None, algorithm=None, start=None):

    rows = []
    
    for exp in experiments:
        obj = exp.objective_value or 0
        rows.append({
            "instance": f"{exp.instance.dataset}/{exp.instance.id}",
            "algorithm": exp.algorithm,
            "start": exp.parameters["start"],
            "generations": exp.parameters["generations"],
            "objective_value": obj / exp.instance.best_result * 100
        })

    df = pd.DataFrame(rows)

    # Optional filtering
    if instance:
        df = df[df["instance"] == instance]
    if algorithm:
        df = df[df["algorithm"] == algorithm]
    if start:
        df = df[df["start"] == start]

    df = df.sort_values("generations")


    plt.figure(figsize=FIGURE_SIZE)   # smaller figure


    MARKER_MAP = {"warm": "s", "random": "o"}
    LINESTYLE_MAP = {"warm": "-", "random": "--"}

    for idx, ((inst, algo, st), group) in enumerate(df.groupby(["instance", "algorithm", "start"])):
        group = group.sort_values("generations")
        label = f"{inst} | {algo} | start={st}"

        plt.plot(
            group["generations"],
            group["objective_value"],
            marker=MARKER_MAP.get(st, "o"),
            linestyle=LINESTYLE_MAP.get(st, "-"),
            color=TIKZ_COLORS[idx % len(TIKZ_COLORS)],
            linewidth=1.5,
            markersize=6,
            label=label
        )

    plt.title(r"Objective Value vs Generations")
    plt.xlabel(r"Generations")
    plt.ylabel(r"Objective Value (\%)")
    plt.grid(True, linestyle="--", alpha=0.5)
    plt.legend(fontsize=8)


    os.makedirs("figures", exist_ok=True)

    # Build filename
    fname = "figures/evolution"
    if instance: fname += f"_{instance.replace('/', '-')}"
    if algorithm: fname += f"_{algorithm}"
    if start: fname += f"_{start}"
    fname += ".pgf"

    # Save PGF
    plt.savefig(fname, bbox_inches="tight")

    plt.show()



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


def display_experiment_2(experiments, group_by=["instance", "pop_size", "cx_prob", "mut_prob"]):

    rows = []

    for exp in experiments:
        rows.append({
            "instance": f"{exp.instance.dataset}/{exp.instance.id}",
            "cx_prob": exp.parameters["crossover_rate"],
            "mut_prob": exp.parameters["mutation_rate"],
            "pop_size": exp.parameters["population_size"],
            "run": exp.run_id,
            "objective_value": exp.objective_value,
            "execution_time": exp.execution_time,
        })

    df = pd.DataFrame(rows)

    summary = (
        df.groupby(group_by)
        .agg(
            mean_objective=("objective_value", "mean"),
            # std_objective=("objective_value", "std"),
            # max_objective=("objective_value", "max"),
            mean_time=("execution_time", "mean"),
            # std_time=("execution_time", "std"),
            # max_time=("execution_time", "max")
        )
        .reset_index()
    )

    summary["efficiency (%obj/time)"] = (summary["mean_objective"]/exp.instance.best_result*100) / summary["mean_time"]

    summary = summary.round(4)
    
    return summary

    
