import io
from pathlib import Path

import matplotlib.pyplot as plt
import pandas as pd


PASTA_ANALISES = Path(__file__).resolve().parent
PASTA_PROCESSADOR = PASTA_ANALISES.parent
RAIZ_PROJETO = PASTA_PROCESSADOR.parent
PASTA_GRAFICOS = PASTA_PROCESSADOR / "graficos"

ARQUIVOS_CANDIDATOS = [
    PASTA_PROCESSADOR / "benchmark_resultados.csv",
    RAIZ_PROJETO / "benchmark_resultados.csv",
]
ARQUIVO_CSV = next((caminho for caminho in ARQUIVOS_CANDIDATOS if caminho.is_file()), None)
if ARQUIVO_CSV is None:
    caminhos = "\n".join(f"  - {caminho}" for caminho in ARQUIVOS_CANDIDATOS)
    raise FileNotFoundError(f"Não encontrei benchmark_resultados.csv. Procurei em:\n{caminhos}")

PASTA_GRAFICOS.mkdir(parents=True, exist_ok=True)


def carregar_benchmark(caminho):
    """Lê o CSV padrão e também corrige arquivos antigos com vírgula decimal."""
    linhas = [caminho.read_text(encoding="utf-8").splitlines()[0]]
    for linha_numero, linha in enumerate(caminho.read_text(encoding="utf-8").splitlines()[1:], start=2):
        if not linha.strip():
            continue
        partes = linha.split(",")
        if len(partes) == 9:
            # Formato antigo: os decimais com vírgula eram confundidos com separadores de coluna.
            partes = partes[:3] + [
                f"{partes[3]}.{partes[4]}",
                f"{partes[5]}.{partes[6]}",
                f"{partes[7]}.{partes[8]}",
            ]
        if len(partes) != 6:
            raise ValueError(
                f"Linha {linha_numero} de {caminho.name}: esperava 6 colunas, encontrei {len(partes)}."
            )
        linhas.append(",".join(partes))

    df = pd.read_csv(io.StringIO("\n".join(linhas)))
    colunas_numericas = ["threads", "tempo_ms", "speedup", "eficiencia_percent"]
    for coluna in colunas_numericas:
        df[coluna] = pd.to_numeric(df[coluna], errors="raise")
    df["afinidade"] = df["afinidade"].astype(str).str.lower().map({"true": True, "false": False})
    if df["afinidade"].isna().any():
        raise ValueError("A coluna 'afinidade' deve conter apenas true ou false.")
    return df


def criar_figura_com_formulas(titulo, formulas):
    figura = plt.figure(figsize=(13, 6), constrained_layout=True)
    grade = figura.add_gridspec(1, 2, width_ratios=[3.2, 1.5])
    eixo = figura.add_subplot(grade[0, 0])
    painel = figura.add_subplot(grade[0, 1])
    painel.axis("off")
    painel.text(
        0.03,
        0.98,
        "FÓRMULAS\n\n" + "\n\n".join(formulas),
        va="top",
        ha="left",
        fontsize=11,
        family="serif",
        linespacing=1.55,
        bbox={"boxstyle": "round,pad=0.8", "facecolor": "#f2f4f7", "edgecolor": "#c8cdd3"},
    )
    figura.suptitle(titulo, fontsize=15, fontweight="bold")
    return figura, eixo


def salvar(figura, nome):
    destino = PASTA_GRAFICOS / nome
    figura.savefig(destino, dpi=300, bbox_inches="tight")
    plt.close(figura)
    print(f"Gráfico salvo: {destino}")


df = carregar_benchmark(ARQUIVO_CSV)
print("=== DADOS DO BENCHMARK CARREGADOS ===")
print(df.to_string(index=False))
print("=====================================\n")

sequencial = df[df["modo"] == "Sequencial"]
sem_afim = df[(df["modo"] == "Paralelo") & (~df["afinidade"])].sort_values("threads")
com_afim = df[(df["modo"] == "Com Afinidade") & (df["afinidade"])].sort_values("threads")
if len(sequencial) != 1 or sem_afim.empty or com_afim.empty:
    raise ValueError("O CSV precisa ter uma linha sequencial e resultados paralelos com e sem afinidade.")

seq_tempo = float(sequencial.iloc[0]["tempo_ms"])
threads = sem_afim["threads"].tolist()

# Gráfico 1: tempo de execução
figura, eixo = plt.subplots(figsize=(9, 5))
figura.suptitle("Tempo de Execução vs Número de Threads", fontsize=15, fontweight="bold")
eixo.axhline(y=seq_tempo, color="red", linestyle="--", label=f"Sequencial (1 thread: {seq_tempo / 1000:.2f} s)")
eixo.plot(sem_afim["threads"], sem_afim["tempo_ms"], marker="o", linewidth=2, label="Paralelo (sem afinidade)")
eixo.plot(com_afim["threads"], com_afim["tempo_ms"], marker="s", linewidth=2, label="Paralelo (com afinidade)")
eixo.set_xlabel("Número de threads")
eixo.set_ylabel("Tempo (ms)")
eixo.set_xticks([1] + threads)
eixo.grid(True, linestyle=":", alpha=0.6)
eixo.legend()
salvar(figura, "grafico_tempo_execucao.png")

# Gráfico 2: speedup
figura, eixo = criar_figura_com_formulas(
    "Speedup vs Número de Threads",
    [
        r"$S = \frac{T_1}{T_p}$",
        r"$T_1$ = tempo sequencial",
        r"$T_p$ = tempo com $p$ threads",
        r"$S_{\mathrm{ideal}} = p$",
    ],
)
eixo.plot(threads, threads, color="gray", linestyle="--", label="Ideal linear (S = p)")
eixo.plot(sem_afim["threads"], sem_afim["speedup"], marker="o", linewidth=2, label="Sem afinidade")
eixo.plot(com_afim["threads"], com_afim["speedup"], marker="s", linewidth=2, label="Com afinidade")
eixo.set_xlabel("Número de threads (p)")
eixo.set_ylabel("Speedup (S)")
eixo.set_xticks(threads)
eixo.set_ylim(0, 4)
eixo.grid(True, linestyle=":", alpha=0.6)
eixo.legend()
salvar(figura, "grafico_speedup.png")

# Gráfico 3: eficiência
figura, eixo = criar_figura_com_formulas(
    "Eficiência Paralela vs Número de Threads",
    [
        r"$E = \frac{S}{p}$",
        r"$E_{\%} = \frac{S}{p} \times 100\%$",
        r"$S$ = speedup medido",
        r"$p$ = quantidade de threads",
    ],
)
eixo.axhline(y=100, color="gray", linestyle="--", label="Ideal (100%)")
eixo.plot(sem_afim["threads"], sem_afim["eficiencia_percent"], marker="o", linewidth=2, label="Sem afinidade")
eixo.plot(com_afim["threads"], com_afim["eficiencia_percent"], marker="s", linewidth=2, label="Com afinidade")
eixo.set_xlabel("Número de threads (p)")
eixo.set_ylabel("Eficiência (%)")
eixo.set_xticks(threads)
eixo.set_ylim(0, 110)
eixo.grid(True, linestyle=":", alpha=0.6)
eixo.legend()
salvar(figura, "grafico_eficiencia.png")


def estimar_fracao_paralelizavel(speedup, quantidade_threads):
    if quantidade_threads <= 1 or speedup <= 0:
        return float("nan")
    return (1 - 1 / speedup) / (1 - 1 / quantidade_threads)


def estimar_amdahl(dados):
    return dados.apply(
        lambda linha: estimar_fracao_paralelizavel(linha["speedup"], linha["threads"]),
        axis=1,
    )


sem_afim = sem_afim.copy()
com_afim = com_afim.copy()
sem_afim["p_efetivo"] = estimar_amdahl(sem_afim)
com_afim["p_efetivo"] = estimar_amdahl(com_afim)
sem_afim["speedup_max_amdahl"] = 1 / (1 - sem_afim["p_efetivo"])
com_afim["speedup_max_amdahl"] = 1 / (1 - com_afim["p_efetivo"])
threads_modelo = sorted(set(sem_afim["threads"].tolist() + com_afim["threads"].tolist()))
formulas_amdahl = [
    r"$P = \frac{1 - \frac{1}{S}}{1 - \frac{1}{N}}$",
    r"$S = \frac{T_1}{T_N}$",
    r"$S_{\max} = \frac{1}{1-P}$",
    r"$P$ é estimado separadamente para cada $N$ medido.",
    "O máximo é calculado com esse P efetivo; ele incorpora sobrecargas e variações do benchmark."
]

figura = plt.figure(figsize=(12, 10), constrained_layout=True)
grade = figura.add_gridspec(2, 2, width_ratios=[1.7, 1], height_ratios=[1, 1])
eixo_sem = figura.add_subplot(grade[0, 0])
eixo_com = figura.add_subplot(grade[1, 0], sharex=eixo_sem, sharey=eixo_sem)
painel = figura.add_subplot(grade[:, 1])
painel.axis("off")
painel.text(
    0.03,
    0.98,
    "FÓRMULAS\n\n" + "\n\n".join(formulas_amdahl),
    va="top",
    ha="left",
    fontsize=11,
    family="serif",
    linespacing=1.55,
    bbox={"boxstyle": "round,pad=0.8", "facecolor": "#f2f4f7", "edgecolor": "#c8cdd3"},
)
figura.suptitle("Lei de Amdahl: speedup alcançado e máximo estimado", fontsize=15, fontweight="bold")

for eixo, dados, titulo in [
    (eixo_sem, sem_afim, "Sem afinidade"),
    (eixo_com, com_afim, "Com afinidade"),
]:
    eixo.plot(dados["threads"], dados["speedup"], marker="o", linewidth=2, label="Alcançado")
    eixo.plot(
        dados["threads"], dados["speedup_max_amdahl"], marker="o", linestyle="--", linewidth=2,
        label="Máximo de Amdahl estimado"
    )
    eixo.set_title(titulo)
    eixo.set_xlabel("Número de threads (N)")
    eixo.set_xticks(threads_modelo)
    eixo.set_ylim(bottom=0)
    eixo.grid(True, linestyle=":", alpha=0.6)
    eixo.legend(fontsize=8)
eixo_sem.set_ylabel("Speedup (S)")
maior_speedup = max(
    float(sem_afim["speedup_max_amdahl"].max()),
    float(com_afim["speedup_max_amdahl"].max()),
)
eixo_sem.set_ylim(0, maior_speedup * 1.1)
salvar(figura, "grafico_lei_amdahl.png")

print("\nGráficos e análise da Lei de Amdahl foram gerados em:")
print(PASTA_GRAFICOS)
