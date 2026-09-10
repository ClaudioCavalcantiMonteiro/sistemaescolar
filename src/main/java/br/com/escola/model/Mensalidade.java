package br.com.escola.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Mensalidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    private LocalDate mesReferencia;
    private LocalDate dataVencimento;

    private Double valorOriginal;
    private Double desconto;
    private Double juros;
    private Double valorPago;

    private LocalDate dataPagamento;

    private String formaPagamento;
    private String status;

    @Column(length = 500)
    private String observacao;

    private LocalDateTime dataCriacao;
    private LocalDateTime dataAlteracao;

    @PrePersist
    public void prePersist() {
        this.dataCriacao = LocalDateTime.now();
        if (this.status == null) this.status = "PENDENTE";
        if (this.valorPago == null) this.valorPago = 0.0;
        if (this.desconto == null) this.desconto = 0.0;
        if (this.juros == null) this.juros = 0.0;
    }

    @PreUpdate
    public void preUpdate() {
        this.dataAlteracao = LocalDateTime.now();
    }

    @Transient
    public Double getValorTotal() {
        double total = (valorOriginal != null ? valorOriginal : 0)
                     - (desconto != null ? desconto : 0)
                     + (juros != null ? juros : 0);
        return Math.round(total * 100.0) / 100.0;
    }

    @Transient
    public boolean isPaga() {
        return "PAGA".equalsIgnoreCase(status);
    }

    @Transient
    public boolean isCancelada() {
        return "CANCELADA".equalsIgnoreCase(status);
    }

    @Transient
    public boolean isAtrasada() {
        if (isPaga() || isCancelada()) return false;
        return dataVencimento != null && dataVencimento.isBefore(LocalDate.now());
    }

    @Transient
    public long getDiasAtraso() {
        if (!isAtrasada()) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(dataVencimento, LocalDate.now());
    }

    // ============= Getters e Setters =============

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }

    public LocalDate getMesReferencia() { return mesReferencia; }
    public void setMesReferencia(LocalDate mesReferencia) { this.mesReferencia = mesReferencia; }

    public LocalDate getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(LocalDate dataVencimento) { this.dataVencimento = dataVencimento; }

    public Double getValorOriginal() { return valorOriginal; }
    public void setValorOriginal(Double valorOriginal) { this.valorOriginal = valorOriginal; }

    public Double getDesconto() { return desconto; }
    public void setDesconto(Double desconto) { this.desconto = desconto; }

    public Double getJuros() { return juros; }
    public void setJuros(Double juros) { this.juros = juros; }

    public Double getValorPago() { return valorPago; }
    public void setValorPago(Double valorPago) { this.valorPago = valorPago; }

    public LocalDate getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }

    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public LocalDateTime getDataAlteracao() { return dataAlteracao; }
    public void setDataAlteracao(LocalDateTime dataAlteracao) { this.dataAlteracao = dataAlteracao; }
}