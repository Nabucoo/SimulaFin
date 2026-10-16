#!/usr/bin/env python3
"""Gera gráficos PNG a partir do resumo exportado pelo processador."""

import argparse
import csv
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")

import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter


PASTA_PROCESSADOR = Path(__file__).resolve().parent.parent
ARQUIVO_CSV = "relatorio_processamento.csv"
MESES_DO_ANO = [
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro",
]
COLUNAS_OBRIGATORIAS = {
    "secao",
    "metrica",
    "categoria",
    "quantidade",
    "valor",
    "transacao_id",
    "cliente",
    "data",
    "detalhes",
}


def localizar_csv(caminho_informado=None):
    if caminho_informado:
        caminho = Path(caminho_informado).expanduser().resolve()
        if caminho.is_file():
            return caminho
        raise FileNotFoundError(f"Arquivo CSV não encontrado: {caminho}")

    candidatos = [
        PASTA_PROCESSADOR / ARQUIVO_CSV,
        PASTA_PROCESSADOR.parent / ARQUIVO_CSV,
    ]
    for caminho in candidatos:
        if caminho.is_file():
            return caminho

    esperados = "\n".join(f"  - {caminho}" for caminho in candidatos)
    raise FileNotFoundError(
        f"Não encontrei {ARQUIVO_CSV}. Procurei em:\n{esperados}\n"
        "Execute o processador para gerar o resumo ou informe o caminho com --csv."
    )


def ler_resumo(caminho_csv):
    with caminho_csv.open("r", encoding="utf-8-sig", newline="") as arquivo:
        leitor = csv.DictReader(arquivo)
        colunas = set(leitor.fieldnames or [])
        faltantes = COLUNAS_OBRIGATORIAS - colunas
        if faltantes:
            nomes = ", ".join(sorted(faltantes))
            raise ValueError(f"O CSV não tem as colunas esperadas: {nomes}")
        linhas = list(leitor)

    def linha_unica(metrica):
        encontradas = [linha for linha in linhas if linha["metrica"] == metrica]
        if not encontradas:
            raise ValueError(f"A métrica '{metrica}' não foi encontrada no CSV.")
        if len(encontradas) > 1:
            raise ValueError(f"A métrica '{metrica}' aparece mais de uma vez no CSV.")
        return encontradas[0]

    def numero(celula, descricao):
        try:
            return float(celula)
        except (TypeError, ValueError) as erro:
            raise ValueError(f"Valor numérico inválido para {descricao}: {celula!r}") from erro

    def linha_unica_secao_metrica(secao, metrica):
        encontradas = [
            linha for linha in linhas
            if linha["secao"] == secao and linha["metrica"] == metrica
        ]
        if len(encontradas) != 1:
            raise ValueError(
                f"Esperava exatamente uma linha para '{secao}/{metrica}', "
                f"mas encontrei {len(encontradas)}."
            )
        return encontradas[0]

    total = numero(linha_unica("transacoes_analisadas")["quantidade"], "transações analisadas")
    suspeitas = numero(linha_unica("transacoes_suspeitas_unicas")["quantidade"], "transações suspeitas")
    if total < 0 or suspeitas < 0 or suspeitas > total:
        raise ValueError("As quantidades de transações no CSV são inconsistentes.")

    soma_total = numero(
        linha_unica_secao_metrica("analise", "soma_total_transacoes")["valor"],
        "soma dos valores das transações",
    )
    media_valores = numero(
        linha_unica_secao_metrica("analise", "valor_medio")["valor"],
        "média dos valores das transações",
    )
    maior = linha_unica_secao_metrica("analise", "maior_transacao")
    menor = linha_unica_secao_metrica("analise", "menor_transacao")
    estatisticas_valores = {
        "soma": soma_total,
        "media": media_valores,
        "maior": {
            "valor": numero(maior["valor"], "valor da maior transação"),
            "id": maior["transacao_id"],
            "cliente": maior["cliente"],
            "data": maior["data"],
            "categoria": maior["categoria"],
        },
        "menor": {
            "valor": numero(menor["valor"], "valor da menor transação"),
            "id": menor["transacao_id"],
            "cliente": menor["cliente"],
            "data": menor["data"],
            "categoria": menor["categoria"],
        },
    }
    if total > 0 and (soma_total < 0 or media_valores < 0):
        raise ValueError("O resumo contém estatísticas de valores negativas.")

    contagens = {}
    somas = {}
    for linha in linhas:
        if linha["secao"] != "categoria":
            continue
        categoria = linha["categoria"]
        if linha["metrica"] == "transacoes":
            if categoria in contagens:
                raise ValueError(f"A categoria '{categoria}' aparece mais de uma vez.")
            contagens[categoria] = numero(linha["quantidade"], f"contagem de {categoria}")
        elif linha["metrica"] == "soma_transacoes":
            if categoria in somas:
                raise ValueError(f"A soma da categoria '{categoria}' aparece mais de uma vez.")
            somas[categoria] = numero(linha["valor"], f"soma de {categoria}")

    if not contagens or set(contagens) != set(somas):
        raise ValueError("As contagens e somas por categoria estão ausentes ou inconsistentes.")
    if sum(contagens.values()) != total:
        raise ValueError(
            "A soma das contagens por categoria não bate com o total de transações analisadas."
        )

    motivos = {}
    for linha in linhas:
        if linha["metrica"] == "contagem_por_regra":
            motivo = linha["detalhes"]
            motivos[motivo] = numero(linha["quantidade"], f"contagem do motivo {motivo}")
    if any(contagem < 0 or contagem > suspeitas for contagem in motivos.values()):
        raise ValueError("Uma contagem por motivo excede o total de transações suspeitas.")

    transacoes_por_cidade = {}
    for linha in linhas:
        if linha["secao"] != "cidade" or linha["metrica"] != "transacoes_por_cidade":
            continue
        cidade = linha["detalhes"].strip()
        if not cidade:
            raise ValueError("Foi encontrada uma contagem por cidade sem nome de cidade/UF.")
        if cidade in transacoes_por_cidade:
            raise ValueError(f"A cidade '{cidade}' aparece mais de uma vez no resumo.")
        transacoes_por_cidade[cidade] = numero(
            linha["quantidade"], f"contagem de transações de {cidade}"
        )

    if not transacoes_por_cidade:
        raise ValueError(
            "O resumo não contém contagens por cidade. Execute novamente o processador "
            "para gerar o CSV atualizado."
        )
    if any(contagem < 0 for contagem in transacoes_por_cidade.values()):
        raise ValueError("O resumo contém uma contagem negativa de transações por cidade.")
    if sum(transacoes_por_cidade.values()) != total:
        raise ValueError(
            "A soma das contagens por cidade não bate com o total de transações analisadas."
        )

    transacoes_por_mes = {}
    for linha in linhas:
        if linha["secao"] != "mes" or linha["metrica"] != "transacoes_por_mes":
            continue
        mes = linha["detalhes"].strip()
        if mes not in MESES_DO_ANO:
            raise ValueError(f"Mês inválido encontrado no resumo: {mes!r}.")
        if mes in transacoes_por_mes:
            raise ValueError(f"O mês '{mes}' aparece mais de uma vez no resumo.")
        transacoes_por_mes[mes] = numero(linha["quantidade"], f"contagem de {mes}")

    if set(transacoes_por_mes) != set(MESES_DO_ANO):
        raise ValueError(
            "O resumo não contém as contagens de todos os meses. Execute novamente o "
            "processador para gerar o CSV atualizado."
        )
    if any(contagem < 0 for contagem in transacoes_por_mes.values()):
        raise ValueError("O resumo contém uma contagem mensal negativa.")
    if sum(transacoes_por_mes.values()) != total:
        raise ValueError(
            "A soma das contagens mensais não bate com o total de transações analisadas."
        )

    return (
        total, suspeitas, contagens, somas, motivos,
        transacoes_por_cidade, transacoes_por_mes, estatisticas_valores,
    )


def formatar_inteiro(valor):
    return f"{valor:,.0f}".replace(",", ".")


def formatar_percentual(valor):
    return f"{valor:.1f}%".replace(".", ",")


def formatar_moeda(valor):
    formatado = f"{valor:,.2f}"
    return "R$ " + formatado.replace(",", "X").replace(".", ",").replace("X", ".")


def salvar_grafico(figura, caminho):
    figura.savefig(caminho, dpi=180, bbox_inches="tight", facecolor="white")
    plt.close(figura)
    print(f"Gráfico salvo: {caminho}")


def gerar_graficos(
    total, suspeitas, contagens, somas, motivos, transacoes_por_cidade,
    transacoes_por_mes, estatisticas_valores, pasta_saida
):
    pasta_saida.mkdir(parents=True, exist_ok=True)
    plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 10})

    # 1. Quantidade de transações por categoria.
    categorias = sorted(contagens, key=contagens.get)
    valores = [contagens[categoria] for categoria in categorias]
    altura = max(4.5, 0.55 * len(categorias) + 1.8)
    figura, eixo = plt.subplots(figsize=(10, altura))
    barras = eixo.barh(categorias, valores, color="#3B82F6")
    eixo.set_title("Quantidade de transações por categoria", pad=14, weight="bold")
    eixo.set_xlabel("Transações (milhões)")
    eixo.xaxis.set_major_formatter(
        FuncFormatter(lambda valor, _: f"{valor / 1_000_000:.1f}".replace(".", ","))
    )
    eixo.grid(axis="x", linestyle=":", alpha=0.35)
    eixo.set_axisbelow(True)
    eixo.bar_label(barras, labels=[formatar_inteiro(v) for v in valores], padding=5)
    eixo.margins(x=0.16)
    figura.tight_layout()
    salvar_grafico(figura, pasta_saida / "transacoes_por_categoria.png")

    # 2. Participação das transações suspeitas no total analisado.
    figura, eixo = plt.subplots(figsize=(8, 6))
    if total == 0:
        eixo.text(0.5, 0.5, "Nenhuma transação analisada", ha="center", va="center")
        eixo.axis("off")
    else:
        nao_suspeitas = total - suspeitas
        rotulos = [
            f"Suspeitas ({formatar_inteiro(suspeitas)})",
            f"Não suspeitas ({formatar_inteiro(nao_suspeitas)})",
        ]
        eixo.pie(
            [suspeitas, nao_suspeitas],
            labels=rotulos,
            autopct=formatar_percentual,
            startangle=90,
            colors=["#EF4444", "#22C55E"],
            wedgeprops={"edgecolor": "white", "linewidth": 1.5},
            textprops={"fontsize": 10},
        )
        eixo.set_title("Percentual de transações suspeitas", pad=16, weight="bold")
        eixo.axis("equal")
    figura.tight_layout()
    salvar_grafico(figura, pasta_saida / "percentual_transacoes_suspeitas.png")

    # 3. Percentual das transações suspeitas que correspondem a cada motivo.
    figura, eixo = plt.subplots(figsize=(11, max(4.5, 0.65 * max(1, len(motivos)) + 2)))
    if not motivos or suspeitas == 0:
        eixo.text(0.5, 0.5, "Nenhum motivo de suspeita registrado", ha="center", va="center")
        eixo.axis("off")
    else:
        motivos_ordenados = sorted(motivos, key=motivos.get)
        percentuais = [motivos[motivo] / suspeitas * 100 for motivo in motivos_ordenados]
        barras = eixo.barh(motivos_ordenados, percentuais, color="#F59E0B")
        eixo.set_title("Transações suspeitas associadas a cada motivo", pad=14, weight="bold")
        eixo.set_xlabel("Percentual das transações suspeitas")
        eixo.grid(axis="x", linestyle=":", alpha=0.35)
        eixo.set_axisbelow(True)
        rotulos = [f"{formatar_percentual(percentual)} (n={formatar_inteiro(motivos[motivo])})"
                   for motivo, percentual in zip(motivos_ordenados, percentuais)]
        eixo.bar_label(barras, labels=rotulos, padding=5, fontsize=9)
        eixo.margins(x=0.24)
        figura.text(
            0.5,
            0.015,
            "Uma transação pode corresponder a mais de um motivo; os percentuais podem somar mais de 100%.",
            ha="center",
            fontsize=8,
            color="#555555",
        )
    figura.tight_layout(rect=(0, 0.05, 1, 1))
    salvar_grafico(figura, pasta_saida / "transacoes_suspeitas_por_motivo.png")

    # 4. Soma do valor das transações em cada categoria.
    categorias = sorted(somas, key=somas.get)
    valores = [somas[categoria] for categoria in categorias]
    figura, eixo = plt.subplots(figsize=(10, max(4.5, 0.55 * len(categorias) + 1.8)))
    barras = eixo.barh(categorias, valores, color="#8B5CF6")
    eixo.set_title("Soma dos valores por categoria", pad=14, weight="bold")
    eixo.set_xlabel("Valor total (R$)")
    eixo.xaxis.set_major_formatter(
        FuncFormatter(lambda valor, _: f"R$ {valor / 1_000_000_000:.1f} bi".replace(".", ","))
    )
    eixo.grid(axis="x", linestyle=":", alpha=0.35)
    eixo.set_axisbelow(True)
    rotulos = [f"R$ {valor / 1_000_000_000:.2f} bi".replace(".", ",") for valor in valores]
    eixo.bar_label(barras, labels=rotulos, padding=5, fontsize=9)
    eixo.margins(x=0.22)
    figura.tight_layout()
    salvar_grafico(figura, pasta_saida / "soma_valores_por_categoria.png")

    # 5. Dez cidades com mais transações.
    cidades_ordenadas = sorted(
        transacoes_por_cidade.items(), key=lambda item: item[1], reverse=True
    )[:10]
    cidades_ordenadas.reverse()
    nomes_cidades = [cidade for cidade, _ in cidades_ordenadas]
    quantidades_cidades = [quantidade for _, quantidade in cidades_ordenadas]
    figura, eixo = plt.subplots(figsize=(11, max(5, 0.55 * len(nomes_cidades) + 2)))
    barras = eixo.barh(nomes_cidades, quantidades_cidades, color="#0EA5A4")
    eixo.set_title("10 cidades com mais transações", pad=14, weight="bold")
    eixo.set_xlabel("Quantidade de transações")
    eixo.grid(axis="x", linestyle=":", alpha=0.35)
    eixo.set_axisbelow(True)
    eixo.bar_label(barras, labels=[formatar_inteiro(v) for v in quantidades_cidades], padding=5)
    eixo.margins(x=0.16)
    figura.tight_layout()
    salvar_grafico(figura, pasta_saida / "top_10_cidades_por_transacoes.png")

    # 6. Quantidade de transações em cada mês, sempre de janeiro a dezembro.
    quantidades_mensais = [transacoes_por_mes[mes] for mes in MESES_DO_ANO]
    figura, eixo = plt.subplots(figsize=(13, 6))
    barras = eixo.bar(MESES_DO_ANO, quantidades_mensais, color="#6366F1")
    eixo.set_title("Transações por mês", pad=14, weight="bold")
    eixo.set_xlabel("Mês")
    eixo.set_ylabel("Quantidade de transações")
    eixo.yaxis.set_major_formatter(FuncFormatter(lambda valor, _: formatar_inteiro(valor)))
    eixo.grid(axis="y", linestyle=":", alpha=0.35)
    eixo.set_axisbelow(True)
    eixo.bar_label(barras, labels=[formatar_inteiro(v) for v in quantidades_mensais], padding=4, fontsize=8)
    eixo.margins(y=0.12)
    figura.tight_layout()
    salvar_grafico(figura, pasta_saida / "transacoes_por_mes.png")

    # 7. Resumo financeiro geral e atributos das transações de maior/menor valor.
    figura, eixo = plt.subplots(figsize=(13, 4.2))
    eixo.axis("off")
    linhas_tabela = [
        ["Soma total", formatar_moeda(estatisticas_valores["soma"]), "—", "—", "—", "—"],
        ["Média por transação", formatar_moeda(estatisticas_valores["media"]), "—", "—", "—", "—"],
    ]
    for chave, rotulo in (("maior", "Maior transação"), ("menor", "Menor transação")):
        transacao = estatisticas_valores[chave]
        linhas_tabela.append([
            rotulo,
            formatar_moeda(transacao["valor"]),
            transacao["id"] or "—",
            transacao["cliente"] or "—",
            transacao["data"] or "—",
            transacao["categoria"] or "—",
        ])

    tabela = eixo.table(
        cellText=linhas_tabela,
        colLabels=["Estatística", "Valor", "ID", "Cliente", "Data", "Categoria"],
        cellLoc="center",
        colLoc="center",
        loc="center",
        colWidths=[0.19, 0.15, 0.10, 0.23, 0.17, 0.16],
    )
    tabela.auto_set_font_size(False)
    tabela.set_fontsize(9)
    tabela.scale(1, 1.8)
    for (linha, _), celula in tabela.get_celld().items():
        celula.set_edgecolor("#D1D5DB")
        if linha == 0:
            celula.set_facecolor("#1E3A8A")
            celula.set_text_props(color="white", weight="bold")
        elif linha in (1, 2):
            celula.set_facecolor("#EFF6FF")
        else:
            celula.set_facecolor("#F8FAFC")
    eixo.set_title("Resumo dos valores das transações", pad=22, weight="bold", fontsize=14)
    figura.text(
        0.5, 0.035,
        "Atributos exibidos para maior/menor: ID, cliente, data e categoria (campos disponíveis no relatório CSV).",
        ha="center", fontsize=8, color="#555555",
    )
    figura.tight_layout(rect=(0, 0.08, 1, 0.95))
    salvar_grafico(figura, pasta_saida / "resumo_valores_transacoes.png")


def main():
    parser = argparse.ArgumentParser(
        description="Analisa relatorio_processamento.csv e exporta gráficos PNG."
    )
    parser.add_argument(
        "--csv",
        help="Caminho do CSV; por padrão, procura na pasta processador e na raiz do projeto.",
    )
    argumentos = parser.parse_args()

    try:
        caminho_csv = localizar_csv(argumentos.csv)
        dados = ler_resumo(caminho_csv)
        pasta_saida = PASTA_PROCESSADOR / "graficos"
        print(f"CSV analisado: {caminho_csv}")
        gerar_graficos(*dados, pasta_saida)
        print(f"\nAnálise concluída. Os PNGs estão em: {pasta_saida}")
    except (FileNotFoundError, ValueError, OSError) as erro:
        print(f"Erro: {erro}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
