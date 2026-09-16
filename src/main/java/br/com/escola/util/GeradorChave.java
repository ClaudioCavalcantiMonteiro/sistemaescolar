package br.com.escola.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Scanner;

/**
 * =====================================================================
 * GERADOR DE CHAVES COM HMAC - USO EXCLUSIVO DO DESENVOLVEDOR
 * =====================================================================
 * Este programa NAO faz parte do sistema instalado na escola.
 * Ele roda apenas no PC do desenvolvedor.
 * =====================================================================
 */
public class GeradorChave {

    // ⚠️ SENHA SECRETA — NUNCA COMPARTILHE COM NINGUÉM!
    // Troque por uma senha forte e só sua.
    private static final String SENHA_SECRETA = "ESCOLA@2026#Secreto#ClaudioFilho";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════╗");
        System.out.println("║   🔑 GERADOR DE CHAVES - SISTEMA ESCOLAR (HMAC)   ║");
        System.out.println("║   (uso exclusivo do desenvolvedor)                ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
        System.out.println();

        while (true) {
            System.out.print("📅 Data de vencimento (dd/MM/yyyy): ");
            String dataStr = scanner.nextLine().trim();

            try {
                LocalDate data = LocalDate.parse(dataStr,
                        DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                if (data.isBefore(LocalDate.now())) {
                    System.out.println("❌ Erro: A data deve ser no futuro!");
                    System.out.println();
                    continue;
                }

                String chave = gerarChave(data);

                System.out.println();
                System.out.println("╔════════════════════════════════════════════════════╗");
                System.out.println("║  ✅ CHAVE GERADA COM SUCESSO                      ║");
                System.out.println("╚════════════════════════════════════════════════════╝");
                System.out.println();
                System.out.println("   📅 Vencimento: " + data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                System.out.println("   📆 Dias restantes: " +
                        java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), data));
                System.out.println();
                System.out.println("   🔑 CHAVE: " + chave);
                System.out.println();
                System.out.println("════════════════════════════════════════════════════");
                System.out.println();
                System.out.println("   Envie esta chave para o cliente.");
                System.out.println();

            } catch (Exception e) {
                System.out.println("❌ Erro: Data inválida! Use o formato dd/MM/yyyy");
                System.out.println();
                continue;
            }

            System.out.print("Gerar outra chave? (s/n): ");
            String resposta = scanner.nextLine().trim().toLowerCase();
            if (!resposta.equals("s")) break;
            System.out.println();
        }

        System.out.println();
        System.out.println("👋 Até logo!");
        scanner.close();
    }

    /**
     * Gera uma chave com assinatura HMAC.
     * Formato: AAAA-MMDD-HHHH-HHHH (ex: 2026-1215-A8B2-C3D4)
     */
    public static String gerarChave(LocalDate dataVencimento) {
        int ano = dataVencimento.getYear();
        int mes = dataVencimento.getMonthValue();
        int dia = dataVencimento.getDayOfMonth();

        // Base da chave (sem assinatura)
        String base = String.format("%04d-%02d%02d", ano, mes, dia);

        // Calcula HMAC-SHA256
        String assinatura = calcularHmac(base);
        // Pega os 8 primeiros caracteres e formata com hífen
        String assinaturaCurta = assinatura.substring(0, 8);
        assinaturaCurta = assinaturaCurta.substring(0, 4) + "-" + assinaturaCurta.substring(4, 8);

        return base + "-" + assinaturaCurta;
    }

    /**
     * Calcula HMAC-SHA256 da base com a senha secreta.
     * Retorna em hexadecimal maiúsculo.
     */
    public static String calcularHmac(String dados) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    SENHA_SECRETA.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(dados.getBytes(StandardCharsets.UTF_8));

            // Converte para hexadecimal maiúsculo
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02X", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao calcular HMAC", e);
        }
    }
}