package br.com.escola.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "licenca")
public class Licenca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_instalacao", nullable = false)
    private LocalDate dataInstalacao;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_ultima_ativacao")
    private LocalDateTime dataUltimaAtivacao;

    @Column(name = "chave_ativacao", length = 50)
    private String chaveAtivacao;

    @Column(name = "id_instalacao", length = 50, unique = true)
    private String idInstalacao;

    @Column(length = 500)
    private String observacao;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    public void prePersist() {
        if (this.dataCriacao == null) {
            this.dataCriacao = LocalDateTime.now();
        }
        if (this.dataInstalacao == null) {
            this.dataInstalacao = LocalDate.now();
        }
    }

    // ============== Regras de Negócio (métodos úteis) ==============

    /**
     * Retorna o total de dias de atraso desde o vencimento.
     * Se ainda não venceu, retorna 0.
     */
    @Transient
    public long getDiasAtraso() {
        if (dataVencimento == null) return 0;
        LocalDate hoje = LocalDate.now();
        if (hoje.isBefore(dataVencimento) || hoje.isEqual(dataVencimento)) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(dataVencimento, hoje);
    }

    /**
     * Retorna quantos dias faltam para o vencimento.
     * Se já venceu, retorna negativo (ex: -3 = venceu 3 dias atrás).
     */
    @Transient
    public long getDiasParaVencer() {
        if (dataVencimento == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dataVencimento);
    }

    /**
     * Sistema deve ser BLOQUEADO?
     * Regra: 5 dias após o vencimento.
     */
    @Transient
    public boolean isBloqueada() {
        return getDiasAtraso() > 5;
    }

    /**
     * Sistema está em AVISO?
     * Regra: entre 5 dias antes e 5 dias depois do vencimento.
     */
    @Transient
    public boolean isEmAviso() {
        long dias = getDiasParaVencer();
        return dias <= 5 && dias >= -5;
    }

    /**
     * Sistema está ATIVO (sem aviso)?
     */
    @Transient
    public boolean isAtiva() {
        return !isBloqueada() && !isEmAviso();
    }

    // ============== Getters e Setters ==============

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDataInstalacao() { return dataInstalacao; }
    public void setDataInstalacao(LocalDate dataInstalacao) { this.dataInstalacao = dataInstalacao; }

    public LocalDate getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }

    public LocalDateTime getDataUltimaAtivacao() { return dataUltimaAtivacao; }
    public void setDataUltimaAtivacao(LocalDateTime dataUltimaAtivacao) { this.dataUltimaAtivacao = dataUltimaAtivacao; }

    public String getChaveAtivacao() { return chaveAtivacao; }
    public void setChaveAtivacao(String chaveAtivacao) { this.chaveAtivacao = chaveAtivacao; }

    public String getIdInstalacao() { return idInstalacao; }
    public void setIdInstalacao(String idInstalacao) { this.idInstalacao = idInstalacao; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }
}
