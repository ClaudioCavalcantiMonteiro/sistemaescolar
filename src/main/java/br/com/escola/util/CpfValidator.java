package br.com.escola.util;

public class CpfValidator {

    /**
     * Validação SIMPLIFICADA:
     * - Aceita qualquer CPF com 11 dígitos
     * - Rejeita CPFs com menos de 11 dígitos
     * - Rejeita CPFs com todos os dígitos iguais (ex: 111.111.111-11)
     * - Rejeita CPF zerado (000.000.000-00)
     */
    public static boolean isValid(String cpf) {
        if (cpf == null) return false;

        // Remove pontos, traços e espaços
        String apenasNumeros = cpf.replaceAll("[^0-9]", "");

        // Deve ter exatamente 11 dígitos
        if (apenasNumeros.length() != 11) return false;

        // Não pode ser tudo igual (111.111.111-11, 222.222.222-22, etc)
        if (apenasNumeros.matches("(\\d)\\1{10}")) return false;

        // Não pode ser 000.000.000-00
        if (apenasNumeros.equals("00000000000")) return false;

        return true;
    }

    /**
     * Formata o CPF no padrão 000.000.000-00
     */
    public static String formatar(String cpf) {
        if (cpf == null) return null;
        cpf = cpf.replaceAll("[^0-9]", "");
        if (cpf.length() != 11) return cpf;
        return cpf.substring(0, 3) + "." +
               cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" +
               cpf.substring(9);
    }
}