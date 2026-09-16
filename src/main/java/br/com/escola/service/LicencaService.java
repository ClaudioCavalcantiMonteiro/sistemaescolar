package br.com.escola.service;

import br.com.escola.model.Licenca;
import br.com.escola.repository.LicencaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class LicencaService {

    @Autowired
    private LicencaRepository licencaRepository;

    // SENHA SECRETA - DEVE SER A MESMA DO GERADOR
    private static final String SENHA_SECRETA = "ESCOLA@2026#Secreto#ClaudioFilho";

    public Licenca buscarOuCriar() {
        Optional<Licenca> opt = licencaRepository.buscarUnica();
        if (opt.isPresent()) {
            return opt.get();
        }

        Licenca licenca = new Licenca();
        licenca.setDataInstalacao(LocalDate.now());
        licenca.setDataVencimento(LocalDate.now().plusDays(30));
        licenca.setIdInstalacao(gerarIdInstalacao());
        licenca.setObservacao("Licenca inicial de avaliacao - 30 dias");
        return licencaRepository.save(licenca);
    }

    private String gerarIdInstalacao() {
        String uuid = UUID.randomUUID().toString().toUpperCase().replace("-", "");
        return uuid.substring(0, 4) + "-" +
               uuid.substring(4, 8) + "-" +
               uuid.substring(8, 12);
    }

    public String getStatus() {
        Licenca l = buscarOuCriar();
        if (l.isBloqueada()) return "BLOQUEADA";
        if (l.isEmAviso()) return "EM_AVISO";
        return "ATIVA";
    }

    public Licenca getLicencaAtual() {
        return buscarOuCriar();
    }

    public Licenca salvar(Licenca licenca) {
        return licencaRepository.save(licenca);
    }

    /**
     * Ativa uma nova licenca atraves de uma chave.
     * Formato esperado: AAAA-MMDD-HHHH-HHHH (com assinatura HMAC)
     */
    public boolean ativarComChave(String chave) {
        if (chave == null || chave.trim().isEmpty()) {
            return false;
        }

        chave = chave.trim().toUpperCase();

        if (!chave.matches("\\d{4}-\\d{4}-[A-F0-9]{4}-[A-F0-9]{4}")) {
            return false;
        }

        try {
            String[] partes = chave.split("-");
            String base = partes[0] + "-" + partes[1];

            String assinaturaEsperada = calcularHmac(base).substring(0, 8);
            String assinaturaEsperadaFormatada = assinaturaEsperada.substring(0, 4) + "-" + assinaturaEsperada.substring(4, 8);
            String assinaturaRecebida = partes[2] + "-" + partes[3];

            if (!assinaturaEsperadaFormatada.equals(assinaturaRecebida)) {
                return false;
            }

            int ano = Integer.parseInt(partes[0]);
            String mmdd = partes[1];
            int mes = Integer.parseInt(mmdd.substring(0, 2));
            int dia = Integer.parseInt(mmdd.substring(2, 4));

            if (mes < 1 || mes > 12 || dia < 1 || dia > 31) {
                return false;
            }

            LocalDate novaData = LocalDate.of(ano, mes, dia);

            if (novaData.isBefore(LocalDate.now())) {
                return false;
            }

            Licenca licenca = buscarOuCriar();
            licenca.setDataVencimento(novaData);
            licenca.setChaveAtivacao(chave);
            licenca.setDataUltimaAtivacao(LocalDateTime.now());
            licenca.setObservacao("Reativada com chave " + chave);
            licencaRepository.save(licenca);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String calcularHmac(String dados) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(
                    SENHA_SECRETA.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(dados.getBytes(java.nio.charset.StandardCharsets.UTF_8));

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