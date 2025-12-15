import pandas as pd

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

    
