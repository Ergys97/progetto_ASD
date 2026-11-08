"""Valida esperimenti.csv e limiti_scala.csv e genera i grafici della relazione.

I file CSV sono trattati come artefatti immutabili: lo script legge, valida e
produce esclusivamente immagini PNG nella cartella risultati/grafici, piu un
riepilogo numerico su console da riportare nella relazione.
"""

from __future__ import annotations

from pathlib import Path

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd


BASE_DIR = Path(__file__).resolve().parent
CSV_PATH = BASE_DIR / "esperimenti.csv"
LIMITI_CSV_PATH = BASE_DIR / "limiti_scala.csv"
OUTPUT_DIR = BASE_DIR / "grafici"

EXPECTED_COLUMNS = [
    "gridId", "variante", "tipoGriglia", "width", "height", "nOstacoli", "seed",
    "Ox", "Oy", "Dx", "Dy",
    "lunghezzaOD", "lunghezzaDO", "sequenzaLandmarkOD", "sequenzaLandmarkDO",
    "correttezza", "frontieraTot", "falsiRiga16", "falsiRiga17", "pruningTotale",
    "chiamateRicorsive", "cacheHit", "profonditaMassima", "chiusureAttivePicco",
    "celleChiusuraTotali", "memoriaMatriciPiccoBytesStimata",
    "warmupEseguiti", "ripetizioniTempo", "tempoMinNs", "tempoMedianoNs", "tempoMaxNs",
]

INTEGER_COLUMNS = [
    "width", "height", "nOstacoli", "seed", "Ox", "Oy", "Dx", "Dy",
    "frontieraTot", "falsiRiga16", "falsiRiga17", "pruningTotale",
    "chiamateRicorsive", "cacheHit", "profonditaMassima", "chiusureAttivePicco",
    "celleChiusuraTotali", "memoriaMatriciPiccoBytesStimata",
    "warmupEseguiti", "ripetizioniTempo", "tempoMinNs", "tempoMedianoNs", "tempoMaxNs",
]

VARIANTI = ["base", "senza-cache", "senza-ordinamento", "limite-propagato"]
ETICHETTE = {
    "base": "Base (traccia + ottimizzazioni)",
    "senza-cache": "Senza cache",
    "senza-ordinamento": "Senza ordinamento",
    "limite-propagato": "Limite propagato",
}

GRAPH_PATHS = {
    "g1": OUTPUT_DIR / "grafico1_lavoro_vs_tempo.png",
    "g2": OUTPUT_DIR / "grafico2_tempo_per_scenario.png",
    "g3": OUTPUT_DIR / "grafico3_ablation.png",
    "g5": OUTPUT_DIR / "grafico5_memoria_e_profondita.png",
    "g6": OUTPUT_DIR / "grafico6_limiti_scala.png",
}

LIMITI_COLUMNS = [
    "variante", "width", "height", "densitaPerc", "nOstacoli", "seed",
    "Ox", "Oy", "Dx", "Dy",
    "esitoOD", "esitoDO", "lunghezzaOD", "lunghezzaDO", "simmetria",
    "tempoODms", "tempoDOms", "chiamateOD", "profonditaOD", "frontieraOD",
    "cacheHitOD", "piccoHeapMBOD", "timeoutMs", "heapMax",
]
LIMITI_DIMENSIONI = [20, 30, 40, 50, 70, 100]
LIMITI_DENSITA = [0, 5, 10, 15, 20, 30]
LIMITI_SEED = 3
ESITI_ESECUZIONE = {"OK", "TIMEOUT", "OOM", "STACK", "ERRORE"}

BLUE = "#2563A6"
ORANGE = "#D97706"
GREEN = "#2F855A"
PURPLE = "#7C3AED"
NAVY = "#183B56"
SLATE = "#64748B"
GRID = "#DCE3EA"
COLORI = {"base": BLUE, "senza-cache": PURPLE, "senza-ordinamento": GREEN, "limite-propagato": ORANGE}


def require(condition: bool, message: str) -> None:
    """Interrompe la generazione se il contratto dati non e' rispettato."""
    if not bool(condition):
        raise ValueError(message)


# --------------------------------------------------------------------- lettura

def load_and_validate() -> pd.DataFrame:
    require(CSV_PATH.is_file(), f"CSV non trovato: {CSV_PATH}")
    raw = pd.read_csv(CSV_PATH, dtype=str, keep_default_na=False)

    require(list(raw.columns) == EXPECTED_COLUMNS, "Schema CSV o ordine delle colonne non conforme")
    require(len(raw) > 0, "esperimenti.csv e' vuoto")
    require(not raw.eq("").any().any(), "Il CSV contiene valori vuoti")
    require(raw["variante"].isin(VARIANTI).all(), "Variante sconosciuta in esperimenti.csv")
    require(not raw.duplicated(["gridId", "variante"]).any(), "Coppia gridId/variante non univoca")
    require((raw["tipoGriglia"] == "Casuale").all(), "Sono presenti griglie non casuali")
    require(set(raw.loc[raw["variante"] == "base", "gridId"]) == set(raw["gridId"]),
            "Ogni istanza deve essere eseguita anche con la variante base")

    data = raw.copy()
    for column in INTEGER_COLUMNS:
        numeric = pd.to_numeric(data[column], errors="raise")
        require(np.isfinite(numeric).all(), f"Valori non finiti nella colonna {column}")
        require((numeric == np.floor(numeric)).all(), f"Valori non interi nella colonna {column}")
        data[column] = numeric.astype(np.int64)

    for column in ("lunghezzaOD", "lunghezzaDO"):
        normalized = data[column].replace({"INF": "inf", "+INF": "inf"})
        data[f"{column}Numerica"] = pd.to_numeric(normalized, errors="raise")

    require((data["width"] > 0).all() and (data["height"] > 0).all(), "Dimensioni griglia non positive")
    require((data["nOstacoli"] <= data["width"] * data["height"] - 2).all(), "Troppi ostacoli per origine e destinazione")
    require((data[["frontieraTot", "falsiRiga16", "falsiRiga17", "chiamateRicorsive", "cacheHit",
                   "celleChiusuraTotali"]] >= 0).all().all(), "Metriche algoritmiche negative")
    require((data["warmupEseguiti"] == 1).all(), "Protocollo non conforme: warm-up diverso da 1")
    require((data["ripetizioniTempo"] == 5).all(), "Protocollo non conforme: ripetizioni diverse da 5")
    require((data["tempoMinNs"] <= data["tempoMedianoNs"]).all(), "tempoMinNs > tempoMedianoNs")
    require((data["tempoMedianoNs"] <= data["tempoMaxNs"]).all(), "tempoMedianoNs > tempoMaxNs")
    require((data["tempoMedianoNs"] > 0).all(), "Tempi mediani non positivi")
    require((data["pruningTotale"] == data["falsiRiga16"] + data["falsiRiga17"]).all(), "Formula pruningTotale incoerente")
    require((data.loc[data["variante"] == "senza-cache", "cacheHit"] == 0).all(), "La variante senza cache registra cacheHit")
    expected_memory = data["chiusureAttivePicco"] * data["width"] * data["height"] * 4
    require((data["memoriaMatriciPiccoBytesStimata"] == expected_memory).all(), "Formula della memoria stimata incoerente")
    require((data["chiusureAttivePicco"] <= data["profonditaMassima"]).all(), "Picco chiusure superiore alla profondita massima")

    finite_od = np.isfinite(data["lunghezzaODNumerica"])
    finite_do = np.isfinite(data["lunghezzaDONumerica"])
    require((finite_od == finite_do).all(), "Asimmetria finito/INF tra OD e DO")
    symmetric = np.isclose(data.loc[finite_od, "lunghezzaODNumerica"],
                           data.loc[finite_od, "lunghezzaDONumerica"], rtol=0.0, atol=1e-10)
    require(bool(symmetric.all()), "Lunghezze OD e DO non simmetriche")
    require((data["correttezza"].str.lower() == "true").all(), "Flag correttezza non vero per tutte le istanze")
    require((data.loc[~finite_od, "sequenzaLandmarkOD"] == "[]").all(), "Sequenza OD non vuota per un esito INF")
    require((data.loc[finite_od, "sequenzaLandmarkOD"] != "[]").all(), "Sequenza OD vuota per un esito finito")

    # Tutte le varianti devono restituire la stessa lunghezza su ogni istanza.
    base = data.loc[data["variante"] == "base"].set_index("gridId")["lunghezzaODNumerica"]
    for _, row in data.iterrows():
        attesa = base[row["gridId"]]
        ottenuta = row["lunghezzaODNumerica"]
        uguali = (np.isinf(attesa) and np.isinf(ottenuta)) or abs(attesa - ottenuta) <= 1e-9
        require(uguali, f"{row['gridId']}: la variante {row['variante']} cambia la lunghezza minima")

    data["esitoFinito"] = finite_od
    data["tempoMs"] = data["tempoMedianoNs"] / 1_000_000.0
    data["celleTotali"] = data["width"] * data["height"]
    data["chiusureCalcolate"] = data["chiamateRicorsive"] - data["cacheHit"]
    data["lavoro"] = data["chiusureCalcolate"] * data["celleTotali"]
    data["densitaPercento"] = (data["nOstacoli"] / data["celleTotali"] * 100.0).round().astype(int)
    data["dimensione"] = data["width"].astype(str) + "x" + data["height"].astype(str)
    data["gruppo"] = data["dimensione"] + "\n" + data["densitaPercento"].astype(str) + "%"
    return data


def load_and_validate_limiti() -> pd.DataFrame:
    """Legge la serie sui limiti di scala e ne verifica il contratto."""
    require(LIMITI_CSV_PATH.is_file(), f"CSV mancante: {LIMITI_CSV_PATH}")
    raw = pd.read_csv(LIMITI_CSV_PATH, dtype=str, keep_default_na=False)
    require(list(raw.columns) == LIMITI_COLUMNS, "Schema di limiti_scala.csv non conforme")
    require(raw["variante"].isin(VARIANTI).all(), "limiti_scala.csv: variante sconosciuta")
    expected_rows = len(LIMITI_DIMENSIONI) * len(LIMITI_DENSITA) * LIMITI_SEED
    for variante, parte in raw.groupby("variante"):
        require(len(parte) == expected_rows,
                f"limiti_scala.csv: attese {expected_rows} righe per {variante}, trovate {len(parte)}")
    require(raw["esitoOD"].isin(ESITI_ESECUZIONE).all(), "limiti_scala.csv: esitoOD non valido")
    require(raw["esitoDO"].isin(ESITI_ESECUZIONE | {"NON_ESEGUITO"}).all(), "limiti_scala.csv: esitoDO non valido")
    require(((raw["esitoOD"] == "OK") | (raw["esitoDO"] == "NON_ESEGUITO")).all(), "limiti_scala.csv: DO eseguito senza OD completato")
    both = (raw["esitoOD"] == "OK") & (raw["esitoDO"] == "OK")
    require((raw.loc[both, "simmetria"] == "true").all(), "limiti_scala.csv: coppia completata non simmetrica")
    require((raw.loc[~both, "simmetria"] == "ND").all(), "limiti_scala.csv: simmetria su coppia incompleta")

    data = raw.copy()
    for column in ["width", "height", "densitaPerc", "nOstacoli", "seed", "timeoutMs"]:
        data[column] = data[column].astype(int)
    for column in ["tempoODms", "tempoDOms", "chiamateOD", "piccoHeapMBOD"]:
        data[column] = pd.to_numeric(data[column].replace("", np.nan))
    data["lunghezzaODNumerica"] = pd.to_numeric(data["lunghezzaOD"].replace({"": np.nan, "INF": "inf"}))
    require(sorted(data["width"].unique().tolist()) == LIMITI_DIMENSIONI, "limiti_scala.csv: dimensioni inattese")
    require(sorted(data["densitaPerc"].unique().tolist()) == LIMITI_DENSITA, "limiti_scala.csv: densita inattese")
    data["completata"] = both
    data["tempoTotMs"] = data["tempoODms"] + data["tempoDOms"]

    # Dove due varianti completano la stessa coppia, la lunghezza deve coincidere.
    chiave = ["width", "densitaPerc", "seed"]
    completate = data.loc[data["completata"]]
    for _, parte in completate.groupby(chiave):
        valori = parte["lunghezzaODNumerica"].to_numpy(dtype=float)
        finite = np.isfinite(valori)
        require(finite.all() == finite.any(), "limiti_scala.csv: varianti discordi su INF")
        if finite.all():
            require(float(valori.max() - valori.min()) <= 1e-9, "limiti_scala.csv: varianti con lunghezze diverse")
    return data


# --------------------------------------------------------------------- grafici

def save_figure(fig: plt.Figure, path: Path) -> None:
    fig.savefig(path, dpi=180, bbox_inches="tight", facecolor="white")
    plt.close(fig)


def stile_assi(ax: plt.Axes) -> None:
    ax.grid(True, which="both", color=GRID, linewidth=0.6)
    ax.set_axisbelow(True)
    for spine in ("top", "right"):
        ax.spines[spine].set_visible(False)


def titolo(ax: plt.Axes, testo: str, sottotitolo: str) -> None:
    ax.set_title(testo, loc="left", fontsize=13, fontweight="bold", color=NAVY, pad=20)
    ax.text(0.0, 1.015, sottotitolo, transform=ax.transAxes, fontsize=8.5, color=SLATE)


def log_log_fit(x: np.ndarray, y: np.ndarray) -> tuple[float, float, float]:
    log_x, log_y = np.log10(x), np.log10(y)
    slope, intercept = np.polyfit(log_x, log_y, 1)
    predicted = slope * log_x + intercept
    r_squared = 1.0 - float(np.sum((log_y - predicted) ** 2)) / float(np.sum((log_y - log_y.mean()) ** 2))
    return float(slope), float(intercept), r_squared


def graph_1(data: pd.DataFrame) -> tuple[float, float]:
    """Tempo mediano rispetto al lavoro stimato: chiusure calcolate per celle della griglia."""
    fig, ax = plt.subplots(figsize=(9.5, 6.0))
    risultato = (float("nan"), float("nan"))
    for variante in ("base", "limite-propagato"):
        parte = data.loc[data["variante"] == variante]
        ax.scatter(parte["lavoro"], parte["tempoMs"], s=42, alpha=0.8,
                   color=COLORI[variante], edgecolor="white", linewidth=0.6,
                   label=f"{ETICHETTE[variante]} (n={len(parte)})")
        slope, intercept, r2 = log_log_fit(parte["lavoro"].to_numpy(float), parte["tempoMs"].to_numpy(float))
        xs = np.geomspace(parte["lavoro"].min(), parte["lavoro"].max(), 200)
        ax.plot(xs, 10 ** intercept * xs ** slope, color=COLORI[variante], linewidth=1.3,
                linestyle="-" if variante == "base" else "--",
                label=f"Fit {ETICHETTE[variante].lower()}: pendenza {slope:.2f}, R² = {r2:.2f}")
        if variante == "base":
            risultato = (slope, r2)
    ax.set_xscale("log")
    ax.set_yscale("log")
    ax.set_xlabel("Lavoro stimato: chiusure calcolate × celle della griglia (O→D più D→O)")
    ax.set_ylabel("Tempo mediano O→D più D→O (ms)")
    stile_assi(ax)
    ax.legend(frameon=False, fontsize=8.5, loc="upper left")
    titolo(ax, "Tempo e lavoro stimato",
           "Chiusure calcolate = chiamate ricorsive meno quelle servite dalla cache; scala log-log")
    save_figure(fig, GRAPH_PATHS["g1"])
    return risultato


def ordina_gruppi(data: pd.DataFrame) -> list[str]:
    gruppi = data[["gruppo", "celleTotali", "densitaPercento"]].drop_duplicates()
    return gruppi.sort_values(["celleTotali", "densitaPercento"])["gruppo"].tolist()


def graph_2(data: pd.DataFrame) -> None:
    """Tempo mediano per dimensione e densita, base e limite propagato affiancati."""
    varianti = ["base", "limite-propagato"]
    gruppi = ordina_gruppi(data)
    fig, ax = plt.subplots(figsize=(11.5, 5.6))
    larghezza = 0.38
    for i, variante in enumerate(varianti):
        parte = data.loc[data["variante"] == variante]
        sintesi = parte.groupby("gruppo")["tempoMs"].agg(
            mediana="median", q25=lambda s: s.quantile(0.25), q75=lambda s: s.quantile(0.75), n="size")
        sintesi = sintesi.reindex(gruppi)
        pos = np.arange(len(gruppi)) + (i - 0.5) * larghezza
        yerr = np.vstack((sintesi["mediana"] - sintesi["q25"], sintesi["q75"] - sintesi["mediana"]))
        ax.bar(pos, sintesi["mediana"], width=larghezza, yerr=yerr, capsize=2.5,
               color=COLORI[variante], alpha=0.9, label=ETICHETTE[variante],
               error_kw={"ecolor": NAVY, "elinewidth": 0.9})
    ax.set_yscale("log")
    ax.set_xticks(np.arange(len(gruppi)), gruppi, fontsize=8.5)
    ax.set_xlabel("Dimensione e densità ostacoli")
    ax.set_ylabel("Tempo mediano O→D più D→O (ms), scala log")
    stile_assi(ax)
    ax.legend(frameon=False, fontsize=9)
    titolo(ax, "Tempo mediano per dimensione e densità",
           "Barre: mediana delle istanze del gruppo; intervalli: scarto interquartile")
    save_figure(fig, GRAPH_PATHS["g2"])


def ablation_table(data: pd.DataFrame) -> pd.DataFrame:
    """Chiamate e tempo totali per gruppo e variante, per i gruppi del piano con ablation."""
    righe = []
    for gruppo, parte in data.loc[data["nOstacoli"] > 0].groupby("gruppo"):
        varianti = sorted(parte["variante"].unique(), key=VARIANTI.index)
        if "senza-cache" not in varianti:
            continue
        for variante in varianti:
            sel = parte.loc[parte["variante"] == variante]
            righe.append({"gruppo": gruppo, "variante": variante, "istanze": len(sel),
                          "chiamate": int(sel["chiamateRicorsive"].sum()),
                          "tempoMs": float(sel["tempoMs"].sum())})
    return pd.DataFrame(righe)


def graph_3(data: pd.DataFrame) -> pd.DataFrame:
    """Ablation: chiamate ricorsive totali per gruppo e variante."""
    tabella = ablation_table(data)
    gruppi = [g for g in ordina_gruppi(data) if g in set(tabella["gruppo"])]
    fig, ax = plt.subplots(figsize=(11.5, 5.6))
    larghezza = 0.2
    for i, variante in enumerate(VARIANTI):
        valori = tabella.loc[tabella["variante"] == variante].set_index("gruppo")["chiamate"].reindex(gruppi)
        pos = np.arange(len(gruppi)) + (i - 1.5) * larghezza
        ax.bar(pos, valori, width=larghezza, color=COLORI[variante], alpha=0.9, label=ETICHETTE[variante])
        for x, v in zip(pos, valori):
            if pd.isna(v):
                ax.text(x, 1.15, "n.e.", ha="center", va="bottom", fontsize=6.5, color=SLATE, rotation=90)
    ax.set_yscale("log")
    ax.set_ylim(1, float(tabella["chiamate"].max()) * 40)
    ax.set_xticks(np.arange(len(gruppi)), gruppi, fontsize=8.5)
    ax.set_xlabel("Dimensione e densità ostacoli")
    ax.set_ylabel("Chiamate ricorsive totali del gruppo, scala log")
    stile_assi(ax)
    ax.legend(frameon=False, fontsize=8.5, ncol=4, loc="upper left")
    titolo(ax, "Effetto delle ottimizzazioni sul numero di chiamate",
           "Somma su tutte le istanze del gruppo; n.e. = variante non eseguita nel piano")
    save_figure(fig, GRAPH_PATHS["g3"])
    return tabella


def graph_5(data: pd.DataFrame) -> None:
    base = data.loc[data["variante"] == "base"]
    memory_kib = base["memoriaMatriciPiccoBytesStimata"] / 1024.0
    fig, ax = plt.subplots(figsize=(8.8, 5.6))
    scatter = ax.scatter(base["celleTotali"], memory_kib, c=base["chiusureAttivePicco"], cmap="viridis",
                         s=58, alpha=0.82, edgecolor="white", linewidth=0.6)
    colorbar = fig.colorbar(scatter, ax=ax)
    colorbar.set_label("Chiusure attive al picco")
    ax.set_xlabel("Celle della griglia (width × height)")
    ax.set_ylabel("Memoria stimata al picco (KiB)")
    stile_assi(ax)
    titolo(ax, "Memoria stimata delle matrici di chiusura (variante base)",
           "Stima: chiusureAttivePicco × width × height × 4 byte; non include cache e overhead JVM")
    right_edge = float(base["celleTotali"].max()) * 0.90
    for _, row in base.nlargest(3, "memoriaMatriciPiccoBytesStimata").iterrows():
        near_right_edge = float(row["celleTotali"]) >= right_edge
        ax.annotate(str(row["gridId"]), xy=(row["celleTotali"], row["memoriaMatriciPiccoBytesStimata"] / 1024.0),
                    xytext=(-8 if near_right_edge else 7, 5), textcoords="offset points",
                    ha="right" if near_right_edge else "left", fontsize=7.7, color=NAVY)
    save_figure(fig, GRAPH_PATHS["g5"])


def graph_6(limiti: pd.DataFrame) -> None:
    varianti = [v for v in VARIANTI if v in set(limiti["variante"])]
    sizes, densities = LIMITI_DIMENSIONI, LIMITI_DENSITA
    timeout_s = int(limiti["timeoutMs"].iloc[0]) / 1000
    heap = str(limiti["heapMax"].iloc[0])
    heap = heap[:-1] + " GB" if heap.lower().endswith("g") else heap
    colors = {0: "#F4C7C3", 1: "#FBE3B5", 2: "#D9E8F5", 3: "#A9C9E8"}

    fig, axes = plt.subplots(1, len(varianti), figsize=(8.2 * len(varianti), 6.9), squeeze=False)
    for ax, variante in zip(axes[0], varianti):
        parte_v = limiti.loc[limiti["variante"] == variante]
        for row_index, size in enumerate(sizes):
            for col_index, density in enumerate(densities):
                cell = parte_v.loc[(parte_v["width"] == size) & (parte_v["densitaPerc"] == density)]
                completed = int(cell["completata"].sum())
                timeouts = int((cell["esitoOD"] == "TIMEOUT").sum() + (cell["esitoDO"] == "TIMEOUT").sum())
                ooms = int((cell["esitoOD"] == "OOM").sum() + (cell["esitoDO"] == "OOM").sum())
                others = int(cell["esitoOD"].isin(["STACK", "ERRORE"]).sum() + cell["esitoDO"].isin(["STACK", "ERRORE"]).sum())
                ax.add_patch(plt.Rectangle((col_index, row_index), 1, 1, facecolor=colors[min(completed, 3)],
                                           edgecolor="white", linewidth=2.5))
                ax.text(col_index + 0.5, row_index + 0.40, f"{completed}/{len(cell)}", ha="center", va="center",
                        fontsize=14, fontweight="bold", color=NAVY)
                if completed:
                    median_ms = float(cell.loc[cell["completata"], "tempoTotMs"].median())
                    detail = f"med {median_ms:.0f} ms" if median_ms >= 10 else f"med {median_ms:.1f} ms"
                else:
                    detail = "nessuna"
                failures = [f"{k}{v}" for k, v in (("T", timeouts), ("M", ooms), ("E", others)) if v]
                if failures:
                    detail += "  ·  " + " ".join(failures)
                ax.text(col_index + 0.5, row_index + 0.72, detail, ha="center", va="center", fontsize=7.8, color="#425466")
        ax.set_xlim(0, len(densities))
        ax.set_ylim(len(sizes), 0)
        ax.set_xticks(np.arange(len(densities)) + 0.5, [f"{d}%" for d in densities])
        ax.set_yticks(np.arange(len(sizes)) + 0.5, [f"{s}x{s}" for s in sizes])
        ax.set_xlabel("Densità ostacoli")
        ax.set_ylabel("Dimensione griglia")
        ax.tick_params(length=0)
        for spine in ax.spines.values():
            spine.set_visible(False)
        ax.set_title(ETICHETTE[variante], loc="left", fontsize=13, fontweight="bold", color=NAVY, pad=8)
    fig.suptitle("Limiti di scala: coppie completate in entrambe le direzioni", x=0.01, ha="left",
                 fontsize=15, fontweight="bold", color=NAVY)
    fig.text(0.01, 0.935, f"3 seed per cella; limite di {timeout_s:.0f} s e heap di {heap} per direzione; "
             "med = mediana OD+DO delle coppie completate; T = timeout, M = OutOfMemoryError",
             fontsize=9.0, color=SLATE)
    fig.tight_layout(rect=(0, 0, 1, 0.92))
    save_figure(fig, GRAPH_PATHS["g6"])


# --------------------------------------------------------------- riepiloghi

def stampa_riepilogo(data: pd.DataFrame, limiti: pd.DataFrame, fit: tuple[float, float],
                     ablation: pd.DataFrame) -> None:
    print(f"Contratto esperimenti.csv verificato: {len(data)} righe, "
          f"{data['gridId'].nunique()} istanze, {len(EXPECTED_COLUMNS)} colonne")
    print(f"Contratto limiti_scala.csv verificato: {len(limiti)} righe")

    print(f"\nG1 fit base tempo~lavoro (n={int((data['variante'] == 'base').sum())}): "
          f"pendenza={fit[0]:.3f}, R^2={fit[1]:.3f}")
    for variante in ("base", "limite-propagato"):
        parte = data.loc[data["variante"] == variante]
        for colonna in ("chiamateRicorsive", "chiusureCalcolate", "frontieraTot", "lavoro"):
            sel = parte.loc[parte[colonna] > 0]
            slope, _, r2 = log_log_fit(sel[colonna].to_numpy(float), sel["tempoMs"].to_numpy(float))
            rho = sel["tempoMs"].corr(sel[colonna], method="spearman")
            print(f"  {variante:17s} tempo~{colonna:18s} n={len(sel):2d}  Spearman={rho:.3f}  "
                  f"pendenza log-log={slope:.3f}  R^2={r2:.3f}")

    print("\nTotali per variante (sulle istanze eseguite):")
    totali = data.groupby("variante").agg(istanze=("gridId", "size"), chiamate=("chiamateRicorsive", "sum"),
                                          cacheHit=("cacheHit", "sum"), falsiRiga16=("falsiRiga16", "sum"),
                                          falsiRiga17=("falsiRiga17", "sum"), tempoMs=("tempoMs", "sum"))
    print(totali.reindex([v for v in VARIANTI if v in totali.index]).to_string())

    print("\nAblation per gruppo (chiamate totali e tempo totale in ms):")
    if not ablation.empty:
        pivot = ablation.pivot(index="gruppo", columns="variante", values="chiamate")
        pivot = pivot.reindex([g for g in ordina_gruppi(data) if g in pivot.index])
        pivot.index = [g.replace("\n", " ") for g in pivot.index]
        print(pivot.reindex(columns=[v for v in VARIANTI if v in pivot.columns]).to_string())

    print("\nLimiti di scala, coppie completate per variante:")
    for variante, parte in limiti.groupby("variante"):
        esiti = parte["esitoOD"].value_counts().to_dict()
        print(f"  {variante}: {int(parte['completata'].sum())}/{len(parte)} completate; esiti OD: {esiti}")


def configure_style() -> None:
    plt.rcParams.update({
        "font.family": "DejaVu Sans", "font.size": 10, "axes.labelcolor": NAVY,
        "axes.edgecolor": "#A8B3BF", "xtick.color": "#425466", "ytick.color": "#425466",
        "figure.facecolor": "white", "axes.facecolor": "white",
    })


def main() -> None:
    configure_style()
    data = load_and_validate()
    limiti = load_and_validate_limiti()
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    fit = graph_1(data)
    graph_2(data)
    ablation = graph_3(data)
    graph_5(data)
    graph_6(limiti)

    for path in GRAPH_PATHS.values():
        require(path.is_file() and path.stat().st_size > 0, f"Output mancante o vuoto: {path}")

    stampa_riepilogo(data, limiti, fit, ablation)
    print()
    for path in GRAPH_PATHS.values():
        print(f"Creato {path.relative_to(BASE_DIR.parent)} ({path.stat().st_size} byte)")


if __name__ == "__main__":
    main()
