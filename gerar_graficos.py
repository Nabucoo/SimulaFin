import pandas as pd
import matplotlib.pyplot as plt
import io

# 1. Lê o arquivo e corrige as vírgulas decimais nas colunas numéricas
linhas_corrigidas = []
with open('benchmark_resultados.csv', 'r', encoding='utf-8') as f:
    cabecalho = f.readline().strip()
    linhas_corrigidas.append(cabecalho)
    for linha in f:
        linha = linha.strip()
        if not linha:
            continue
        partes = linha.split(',')
        # Junta os pares de números divididos pela vírgula decimal
        # partes: [modo, threads, afinidade, tempo_int, tempo_dec, sp_int, sp_dec, ef_int, ef_dec]
        if len(partes) >= 9:
            modo = partes[0]
            threads = partes[1]
            afinidade = partes[2]
            tempo = f"{partes[3]}.{partes[4]}"
            speedup = f"{partes[5]}.{partes[6]}"
            eficiencia = f"{partes[7]}.{partes[8]}"
            linhas_corrigidas.append(f"{modo},{threads},{afinidade},{tempo},{speedup},{eficiencia}")
        else:
            linhas_corrigidas.append(linha)

# 2. Carrega no pandas a partir dos dados normalizados
csv_data = "\n".join(linhas_corrigidas)
df = pd.read_csv(io.StringIO(csv_data))

print("=== DADOS DO BENCHMARK CARREGADOS ===")
print(df)
print("=====================================\n")

# Filtra os dados
seq_tempo = df[df['modo'] == 'Sequencial']['tempo_ms'].values[0]
sem_afim = df[(df['modo'] == 'Paralelo') & (df['afinidade'] == False)].sort_values('threads')
com_afim = df[(df['modo'] == 'Com Afinidade') & (df['afinidade'] == True)].sort_values('threads')

threads = sem_afim['threads'].values

# ==========================================
# Gráfico 1: Tempo de Execução vs Threads
# ==========================================
plt.figure(figsize=(8, 5))
plt.axhline(y=seq_tempo, color='red', linestyle='--', label=f'Sequencial (1 thread: {seq_tempo/1000:.2f}s)')
plt.plot(sem_afim['threads'], sem_afim['tempo_ms'], marker='o', linewidth=2, label='Paralelo (Sem Afinidade)')
plt.plot(com_afim['threads'], com_afim['tempo_ms'], marker='s', linewidth=2, label='Paralelo (Com Afinidade OpenHFT)')

plt.title('Tempo de Execução vs Número de Threads')
plt.xlabel('Número de Threads')
plt.ylabel('Tempo de Execução (ms)')
plt.xticks([1, 2, 4, 6, 8])
plt.grid(True, linestyle=':', alpha=0.6)
plt.legend()
plt.tight_layout()
plt.savefig('grafico_tempo_execucao.png', dpi=300)
plt.close()

# ==========================================
# Gráfico 2: Speedup vs Threads
# ==========================================
plt.figure(figsize=(8, 5))
plt.plot(threads, threads, color='gray', linestyle='--', label='Speedup Ideal Linear (S = p)')
plt.plot(sem_afim['threads'], sem_afim['speedup'], marker='o', linewidth=2, label='Speedup (Sem Afinidade)')
plt.plot(com_afim['threads'], com_afim['speedup'], marker='s', linewidth=2, label='Speedup (Com Afinidade OpenHFT)')

plt.title('Speedup vs Número de Threads')
plt.xlabel('Número de Threads (p)')
plt.ylabel('Speedup (S)')
plt.xticks([2, 4, 6, 8])
plt.grid(True, linestyle=':', alpha=0.6)
plt.legend()
plt.tight_layout()
plt.savefig('grafico_speedup.png', dpi=300)
plt.close()

# ==========================================
# Gráfico 3: Eficiência Paralela vs Threads
# ==========================================
plt.figure(figsize=(8, 5))
plt.axhline(y=100, color='gray', linestyle='--', label='Eficiência Ideal (100%)')
plt.plot(sem_afim['threads'], sem_afim['eficiencia_percent'], marker='o', linewidth=2, label='Eficiência (Sem Afinidade)')
plt.plot(com_afim['threads'], com_afim['eficiencia_percent'], marker='s', linewidth=2, label='Eficiência (Com Afinidade OpenHFT)')

plt.title('Eficiência Paralela vs Número de Threads')
plt.xlabel('Número de Threads (p)')
plt.ylabel('Eficiência (%)')
plt.xticks([2, 4, 6, 8])
plt.ylim(0, 110)
plt.grid(True, linestyle=':', alpha=0.6)
plt.legend()
plt.tight_layout()
plt.savefig('grafico_eficiencia.png', dpi=300)
plt.close()

print("Gráficos gerados com sucesso na sua pasta:")
print("1. grafico_tempo_execucao.png")
print("2. grafico_speedup.png")
print("3. grafico_eficiencia.png")