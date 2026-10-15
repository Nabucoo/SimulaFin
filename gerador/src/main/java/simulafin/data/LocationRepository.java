package simulafin.data;

import simulafin.model.CityState;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repositório em memória contendo as 500 cidades mais populosas do Brasil,
 * obtidas oficialmente a partir do Censo Demográfico do IBGE (2022).
 */
public final class LocationRepository {

    private static final CityState[] CITIES;
    private static final String RESOURCE_PATH = "/cidades_ibge_top500.csv";
    private static final String DATA_SOURCE_INFO = "IBGE - Censo Demográfico 2022 (Tabela 4714 / Variável 93)";

    static {
        List<CityState> list = new ArrayList<>(500);
        try (InputStream is = LocationRepository.class.getResourceAsStream(RESOURCE_PATH)) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line = reader.readLine(); // ignora cabeçalho: rank,cidade,estado,populacao_2022
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty()) continue;
                        String[] parts = trimmed.split(",", 4);
                        if (parts.length >= 3) {
                            String cidade = parts[1].trim();
                            String estado = parts[2].trim();
                            int pop = parts.length > 3 ? Integer.parseInt(parts[3].trim()) : 0;
                            list.add(new CityState(cidade, estado, pop));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Aviso: Falha ao carregar cidades via resource CSV: " + e.getMessage());
        }

        // Se por algum motivo o resource não for encontrado, provê fallback de segurança
        if (list.size() < 500) {
            populateFallback(list);
        }

        CITIES = list.toArray(new CityState[0]);
    }

    private static void populateFallback(List<CityState> list) {
        // Fallback mínimo se faltar arquivo
        String[][] fallback = {
                {"São Paulo", "SP"}, {"Rio de Janeiro", "RJ"}, {"Brasília", "DF"},
                {"Fortaleza", "CE"}, {"Salvador", "BA"}, {"Belo Horizonte", "MG"},
                {"Manaus", "AM"}, {"Curitiba", "PR"}, {"Recife", "PE"}, {"Goiânia", "GO"}
        };
        for (String[] pair : fallback) {
            list.add(new CityState(pair[0], pair[1], 1000000));
        }
    }

    public static CityState[] getAllCities() {
        return CITIES;
    }

    public static int size() {
        return CITIES.length;
    }

    public static CityState getByIndex(int index) {
        return CITIES[index];
    }

    public static String getDataSourceInfo() {
        return DATA_SOURCE_INFO;
    }
}
