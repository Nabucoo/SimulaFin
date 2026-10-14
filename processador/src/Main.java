import java.io.File;
import java.util.Scanner;
import io.LeitorCsv;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println(System.getProperty("user.dir"));

        // 1. Arquivo de entrada
        String arquivo = "data/transactions_10M.csv";
        if (args.length > 0) {
            arquivo = args[0];
        } else {
            System.out.println("Arquivo padrão: " + arquivo);
            if (!new File(arquivo).exists()) {
                System.out.print("Digite o caminho do arquivo CSV: ");
                arquivo = sc.nextLine().trim();
            }
        }

        // 2. Natureza
        String natureza;
        int threads = 1;
        boolean afinidade = false;

        while (true) {
            System.out.println("\n=== CONFIGURACAO DO PROCESSADOR ===");
            System.out.println("1 - Sequencial");
            System.out.println("2 - Paralelo");
            System.out.print("Escolha: ");
            String opcao = sc.nextLine().trim();

            if (opcao.equals("1")) {
                natureza = "sequencial";
                threads = 1;
                break;
            } else if (opcao.equals("2")) {
                natureza = "paralelo";
                break;
            }
            System.out.println("Opção inválida.\n");
        }

        // 3. Threads e Afinidade
        if (natureza.equals("paralelo")) {
            while (true) {
                System.out.println("\nNúmero de threads (2, 4, 6 ou 8):");
                System.out.print("Escolha: ");
                try {
                    threads = Integer.parseInt(sc.nextLine().trim());
                    if (threads == 2 || threads == 4 || threads == 6 || threads == 8) {
                        break;
                    }
                } catch (NumberFormatException ignored) {}
                System.out.println("Quantidade inválida.");
            }

            while (true) {
                System.out.println("\nAfinidade de CPU (OpenHFT):");
                System.out.println("1 - Com afinidade");
                System.out.println("2 - Sem afinidade");
                System.out.print("Escolha: ");
                String opcao = sc.nextLine().trim();
                if (opcao.equals("1")) {
                    afinidade = true;
                    break;
                } else if (opcao.equals("2")) {
                    afinidade = false;
                    break;
                }
                System.out.println("Opção inválida.");
            }
        }

        // 4. Executa o leitor
        try {
            LeitorCsv leitor = new LeitorCsv(arquivo, natureza, threads, afinidade);
            leitor.ler();
        } catch (Exception e) {
            System.out.println("Erro: " + e.getMessage());
            e.printStackTrace();
        }

        sc.close();
    }
}
