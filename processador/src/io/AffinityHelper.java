package io;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utilitário para gerenciar a integração com a biblioteca OpenHFT Affinity.
 *
 * Utiliza carregamento dinâmico para garantir que:
 * 1. O código compile e execute perfeitamente mesmo se a IDE (Eclipse/VS Code)
 *    ainda não tiver adicionado os JARs de lib/ ao Build Path.
 * 2. Quando as bibliotecas (lib/affinity-*.jar) estiverem presentes no classpath,
 *    a fixação de threads em núcleos físicos (AffinityStrategies.DIFFERENT_CORE)
 *    funcione com 100% da sua capacidade.
 */
public class AffinityHelper {

    private static Boolean disponivel = null;

    /**
     * Verifica se a biblioteca OpenHFT Affinity está disponível no classpath.
     */
    public static boolean isDisponivel() {
        if (disponivel == null) {
            try {
                Class.forName("net.openhft.affinity.AffinityThreadFactory");
                disponivel = true;
            } catch (Throwable t) {
                disponivel = false;
            }
        }
        return disponivel;
    }

    /**
     * Cria a ThreadFactory apropriada:
     * - Se comAfinidade == true e OpenHFT estiver disponível: usa AffinityThreadFactory com DIFFERENT_CORE
     * - Caso contrário: usa ThreadFactory padrão nomeada
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ThreadFactory criarThreadFactory(boolean comAfinidade) {
        if (comAfinidade) {
            if (isDisponivel()) {
                try {
                    Class stratClass = Class.forName("net.openhft.affinity.AffinityStrategies");
                    Object differentCore = Enum.valueOf(stratClass, "DIFFERENT_CORE");

                    Class stratInterface = Class.forName("net.openhft.affinity.AffinityStrategy");
                    Class factoryClass = Class.forName("net.openhft.affinity.AffinityThreadFactory");

                    // AffinityThreadFactory(String name, AffinityStrategy... strategies)
                    Constructor<?>[] constructors = factoryClass.getConstructors();
                    for (Constructor<?> ctor : constructors) {
                        Class<?>[] params = ctor.getParameterTypes();
                        if (params.length == 2 && params[0] == String.class) {
                            // Cria array do tipo correto para varargs
                            Object arrayEstrategias = java.lang.reflect.Array.newInstance(stratInterface, 1);
                            java.lang.reflect.Array.set(arrayEstrategias, 0, differentCore);
                            return (ThreadFactory) ctor.newInstance("worker-affinity", arrayEstrategias);
                        }
                    }
                } catch (Throwable t) {
                    System.out.println("[AVISO] Não foi possível ativar OpenHFT Affinity: " + t.getMessage());
                    System.out.println("        Executando com pool padrão de threads.");
                }
            } else {
                System.out.println("[AVISO] Bibliotecas do OpenHFT não encontradas no classpath da IDE.");
                System.out.println("        Para ativar afinidade, adicione os .jar de 'lib/' às Bibliotecas Referenciadas.");
                System.out.println("        Executando com pool padrão de threads.");
            }
        }

        // ThreadFactory padrão da JVM
        return new ThreadFactory() {
            private final AtomicInteger id = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "worker-" + id.getAndIncrement());
            }
        };
    }

    /**
     * Retorna a informação da CPU Core atual (ex: " [CPU Core: 2]"), se disponível.
     */
    public static String obterInfoCpu() {
        if (isDisponivel()) {
            try {
                Class<?> affClass = Class.forName("net.openhft.affinity.Affinity");
                Method getCpu = affClass.getMethod("getCpu");
                Object cpu = getCpu.invoke(null);
                return " [CPU Core: " + cpu + "]";
            } catch (Throwable ignored) {}
        }
        return "";
    }
}
