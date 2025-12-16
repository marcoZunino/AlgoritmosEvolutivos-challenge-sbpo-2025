import pandas as pd
from scipy.stats import wilcoxon, shapiro, kruskal
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

    # Nueva columna para p-values del test de Shapiro–Wilk
    summary["obj_shapiro_p"] = None
    summary["time_shapiro_p"] = None

    # Aplicar Shapiro–Wilk por grupo
    for idx, row in summary.iterrows():
        instance = row["instance"]
        algorithm = row["algorithm"]
        crossover = row["encoding"]

        # Extraer valores del grupo correspondiente
        obj_vals = df[
            (df["instance"] == instance) &
            (df["algorithm"] == algorithm) &
            (df["encoding"] == crossover)
        ]["objective_value"].values

        time_vals = df[
            (df["instance"] == instance) &
            (df["algorithm"] == algorithm) &
            (df["encoding"] == crossover)
        ]["execution_time"].values

        # Shapiro requiere al menos 3 valores
        if len(obj_vals) >= 3:
            _, obj_p = shapiro(obj_vals)
            _, time_p = shapiro(time_vals)
        else:
            obj_p = None
            time_p = None
        
        summary.at[idx, "obj_shapiro_p"] = round(obj_p, 4)
        summary.at[idx, "time_shapiro_p"] = round(time_p, 4)

    summary = summary.round(4)

    return summary



def display_experiment_1_1_wilcoxon(experiments):

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

    results = []

    # calcular p-values comparando operadores de crossover por instancia y algoritmo
    for (instance, algorithm), group in df.groupby(["instance", "algorithm"]):

        obj_subset_vals = group[group["encoding"] == "binary"]["objective_value"].values
        obj_binary_vals = group[group["encoding"] == "subset"]["objective_value"].values

        time_subset_vals = group[group["encoding"] == "binary"]["execution_time"].values
        time_binary_vals = group[group["encoding"] == "subset"]["execution_time"].values

        if len(obj_subset_vals) == len(obj_binary_vals) and len(obj_subset_vals) > 0:
            _, obj_p = wilcoxon(obj_subset_vals, obj_binary_vals)
            _, time_p = wilcoxon(time_subset_vals, time_binary_vals)
        else:
            obj_p = None
            time_p = None

        results.append({
            "instance": instance,
            "algorithm": algorithm,
            "obj_wilcoxon_p": obj_p,
            "time_wilcoxon_p": time_p
        })

    return pd.DataFrame(results).round(4)



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

    # Nueva columna para p-values del test de Shapiro–Wilk
    summary["obj_shapiro_p"] = None
    summary["time_shapiro_p"] = None

    # Aplicar Shapiro–Wilk por grupo
    for idx, row in summary.iterrows():
        instance = row["instance"]
        algorithm = row["algorithm"]
        crossover = row["crossover_type"]

        # Extraer valores del grupo correspondiente
        obj_vals = df[
            (df["instance"] == instance) &
            (df["algorithm"] == algorithm) &
            (df["crossover_type"] == crossover)
        ]["objective_value"].values

        time_vals = df[
            (df["instance"] == instance) &
            (df["algorithm"] == algorithm) &
            (df["crossover_type"] == crossover)
        ]["execution_time"].values

        # Shapiro requiere al menos 3 valores
        if len(obj_vals) >= 3:
            _, obj_p = shapiro(obj_vals)
            _, time_p = shapiro(time_vals)
        else:
            obj_p = None
            time_p = None
        
        summary.at[idx, "obj_shapiro_p"] =round(obj_p, 4)
        summary.at[idx, "time_shapiro_p"] = round(time_p, 4)

    return summary.round(4)



def display_experiment_1_2_wilcoxon(experiments):

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

    results = []

    # calcular p-values comparando operadores de crossover por instancia y algoritmo
    for (instance, algorithm), group in df.groupby(["instance", "algorithm"]):

        obj_subset_vals = group[group["crossover_type"] == "default"]["objective_value"].values
        obj_binary_vals = group[group["crossover_type"] == "orders_union"]["objective_value"].values

        time_subset_vals = group[group["crossover_type"] == "default"]["execution_time"].values
        time_binary_vals = group[group["crossover_type"] == "orders_union"]["execution_time"].values

        if len(obj_subset_vals) == len(obj_binary_vals) and len(obj_subset_vals) > 0:
            _, obj_p = wilcoxon(obj_subset_vals, obj_binary_vals)
            _, time_p = wilcoxon(time_subset_vals, time_binary_vals)
        else:
            obj_p = None
            time_p = None

        results.append({
            "instance": instance,
            "algorithm": algorithm,
            "obj_wilcoxon_p": obj_p,
            "time_wilcoxon_p": time_p
        })

    return pd.DataFrame(results).round(4)




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




def display_experiment_1_4_wilcoxon(experiments):

    rows = []

    for exp in experiments:
        rows.append({
            "instance": f"{exp.instance.dataset}/{exp.instance.id}",
            "algorithm": exp.algorithm,
            "run": exp.run_id,
            "objective_value": exp.objective_value,
            "execution_time": exp.execution_time,
        })

    df = pd.DataFrame(rows)

    results = []

    # calcular p-values comparando operadores de crossover por instancia y algoritmo
    for instance, group in df.groupby(["instance"]):

        obj_gGA_vals = group[group["algorithm"] == "gGA"]["objective_value"].values
        obj_ssGA_vals = group[group["algorithm"] == "ssGA"]["objective_value"].values

        time_gGA_vals = group[group["algorithm"] == "gGA"]["execution_time"].values
        time_ssGA_vals = group[group["algorithm"] == "ssGA"]["execution_time"].values
        if len(obj_gGA_vals) == len(obj_ssGA_vals) and len(obj_gGA_vals) > 0:
            _, obj_p = wilcoxon(obj_gGA_vals, obj_ssGA_vals)
            _, time_p = wilcoxon(time_gGA_vals, time_ssGA_vals)
        else:
            obj_p = None
            time_p = None

        results.append({
            "instance": instance,
            "obj_wilcoxon_p": obj_p,
            "time_wilcoxon_p": time_p,
            "ssGA_obj_improvement (%)": ( (obj_ssGA_vals.mean()/obj_gGA_vals.mean() - 1) * 100 ),
            "ssGA_time_improvement (%)": ( -(time_ssGA_vals.mean()/time_gGA_vals.mean() - 1) * 100 )

        })

    return pd.DataFrame(results).round(4)


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



def display_shapiro_test_exp_1_1(experiments):

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

        subset_vals = group[group["encoding"] == "subset"]["objective_value"].values
        binary_vals = group[group["encoding"] == "binary"]["objective_value"].values

        # Shapiro requires at least 3 values
        if len(subset_vals) >= 3:
            stat_subset, p_subset = shapiro(subset_vals)
        else:
            stat_subset, p_subset = None, None

        if len(binary_vals) >= 3:
            stat_binary, p_binary = shapiro(binary_vals)
        else:
            stat_binary, p_binary = None, None

        results.append({
            "instance": instance,
            "algorithm": algorithm,
            "subset_stat": stat_subset,
            "subset_p": p_subset,
            "binary_stat": stat_binary,
            "binary_p": p_binary
        })
    
    return pd.DataFrame(results).round(4)



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
            max_objective=("objective_value", "max"),
            mean_time=("execution_time", "mean"),
        )
        .reset_index()
    )

    # eficiencia
    summary["efficiency (%obj/time)"] = (
        (summary["mean_objective"] / exp.instance.best_result * 100) 
        / summary["mean_time"]
    )

    # columna para p-values del test de Shapiro–Wilk
    summary["shapiro_p"] = None

    # aplicar shapiro por cada combinación group_by
    for idx, row in summary.iterrows():

        # construir máscara dinámica según group_by
        mask = pd.Series([True] * len(df))
        for col in group_by:
            mask &= (df[col] == row[col])

        vals = df.loc[mask, "objective_value"].values

        # Shapiro requiere al menos 3 valores
        if len(vals) >= 3:
            stat, p = shapiro(vals)
        else:
            p = None

        summary.at[idx, "shapiro_p"] = round(p, 4)

    return summary.round(4)




def display_kruskal_exp_2(experiments, group_by=["pop_size", "cx_prob", "mut_prob"]):

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

    results = []

    # agrupar por instancia
    for instance, group in df.groupby("instance"):

        # agrupar por combinaciones de parámetros
        obj_combos = []
        time_combos = []

        for combo, sub in group.groupby(group_by):
            obj_vals = sub["objective_value"].values
            if len(obj_vals) > 0:
                obj_combos.append(obj_vals)
            time_vals = sub["execution_time"].values
            if len(time_vals) > 0:
                time_combos.append(time_vals)

        # aplicar Kruskal–Wallis si hay al menos 2 grupos
        if len(obj_combos) >= 2:
            _, obj_p = kruskal(*obj_combos)
        if len(time_combos) >= 2:
            _, time_p = kruskal(*time_combos)

        else:
            p = None

        results.append({
            "instance": instance,
            "num_combinations": len(obj_combos),
            "obj_kruskal_p": round(obj_p, 4),
            "time_kruskal_p": round(time_p, 4)
        })

    return pd.DataFrame(results)



    
