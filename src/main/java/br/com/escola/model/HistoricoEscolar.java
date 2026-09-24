package br.com.escola.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
public class HistoricoEscolar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "aluno_id", nullable = false)
    private Aluno aluno;

    @ManyToOne
    @JoinColumn(name = "escola_id")
    private Escola escola;

    private Integer anoLetivo;

    @Column(length = 100)
    private String serie;

    @Column(length = 100)
    private String turma;

    @Column(length = 50)
    private String turno;

    // ===== Campos para histórico externo (transferência) =====
    // Se preenchido, o histórico é de OUTRA escola (aluno veio transferido)
    @Column(length = 200)
    private String escolaOrigem;

    @Column(length = 150)
    private String cidadeOrigem;

    // Média mínima usada para aprovação NESTE histórico
    private Double mediaAprovacao;

    // APROVADO, APROVADO_COM_DEPENDENCIA, REPROVADO, TRANSFERIDO, CURSANDO, EVADIDO
    @Column(length = 30)
    private String situacaoFinal;

    private Integer cargaHorariaTotal;
    private Integer diasLetivos;

    @Column(length = 500)
    private String observacoes;

    private Boolean emitido;

    private LocalDateTime dataEmissao;

    private LocalDateTime dataCriacao;
    private LocalDateTime dataAlteracao;

    @OneToMany(mappedBy = "historico", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC, id ASC")
    private List<ItemHistorico> items = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.dataCriacao = LocalDateTime.now();
        if (this.emitido == null) this.emitido = false;
        if (this.mediaAprovacao == null) this.mediaAprovacao = 6.0;
    }

    @PreUpdate
    public void preUpdate() {
        this.dataAlteracao = LocalDateTime.now();
    }

    // ===== Métodos auxiliares =====

    @Transient
    public int getTotalDisciplinas() {
        return items != null ? items.size() : 0;
    }

    @Transient
    public int getTotalAprovadas() {
        if (items == null) return 0;
        return (int) items.stream()
                .filter(i -> "APROVADO".equalsIgnoreCase(i.getResultado()))
                .count();
    }

    @Transient
    public int getTotalReprovadas() {
        if (items == null) return 0;
        return (int) items.stream()
                .filter(i -> "REPROVADO".equalsIgnoreCase(i.getResultado()))
                .count();
    }

    @Transient
    public boolean isConcluido() {
        return "APROVADO".equalsIgnoreCase(situacaoFinal)
                || "APROVADO_COM_DEPENDENCIA".equalsIgnoreCase(situacaoFinal)
                || "TRANSFERIDO".equalsIgnoreCase(situacaoFinal);
    }

    @Transient
    public boolean isEmitido() {
        return emitido != null && emitido;
    }

    @Transient
    public double getMediaAprovacaoSegura() {
        return mediaAprovacao != null ? mediaAprovacao : 6.0;
    }

    /**
     * Retorna true se este histórico é de outra escola (transferência).
     */
    @Transient
    public boolean isExterno() {
        return escolaOrigem != null && !escolaOrigem.trim().isEmpty();
    }

    /**
     * Retorna a lista de itens (matérias) que ficaram em dependência.
     */
    @Transient
    public List<ItemHistorico> getItemsEmDependencia() {
        if (items == null) return new ArrayList<>();
        return items.stream()
                .filter(i -> "REPROVADO".equalsIgnoreCase(i.getResultado()))
                .collect(Collectors.toList());
    }

    /**
     * Retorna um resumo em texto das matérias em dependência.
     */
    @Transient
    public String getResumoDependencia() {
        List<ItemHistorico> deps = getItemsEmDependencia();
        if (deps.isEmpty()) return "";
        return deps.stream()
                .map(i -> i.getNomeMateria() != null ? i.getNomeMateria() : "-")
                .collect(Collectors.joining(", "));
    }

    /**
     * Retorna a média geral de todas as matérias.
     */
    @Transient
    public double getMediaGeral() {
        if (items == null || items.isEmpty()) return 0.0;
        double soma = 0;
        int count = 0;
        for (ItemHistorico i : items) {
            if (i.getNotaFinal() != null) {
                soma += i.getNotaFinal();
                count++;
            }
        }
        return count > 0 ? Math.round((soma / count) * 100.0) / 100.0 : 0.0;
    }

    // ===== Helper para relacionamento bidirecional =====
    public void addItem(ItemHistorico item) {
        item.setHistorico(this);
        this.items.add(item);
    }

    public void removeItem(ItemHistorico item) {
        item.setHistorico(null);
        this.items.remove(item);
    }

    // ===== Getters e Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }

    public Escola getEscola() { return escola; }
    public void setEscola(Escola escola) { this.escola = escola; }

    public Integer getAnoLetivo() { return anoLetivo; }
    public void setAnoLetivo(Integer anoLetivo) { this.anoLetivo = anoLetivo; }

    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }

    public String getTurma() { return turma; }
    public void setTurma(String turma) { this.turma = turma; }

    public String getTurno() { return turno; }
    public void setTurno(String turno) { this.turno = turno; }

    public String getEscolaOrigem() { return escolaOrigem; }
    public void setEscolaOrigem(String escolaOrigem) { this.escolaOrigem = escolaOrigem; }

    public String getCidadeOrigem() { return cidadeOrigem; }
    public void setCidadeOrigem(String cidadeOrigem) { this.cidadeOrigem = cidadeOrigem; }

    public Double getMediaAprovacao() { return mediaAprovacao; }
    public void setMediaAprovacao(Double mediaAprovacao) { this.mediaAprovacao = mediaAprovacao; }

    public String getSituacaoFinal() { return situacaoFinal; }
    public void setSituacaoFinal(String situacaoFinal) { this.situacaoFinal = situacaoFinal; }

    public Integer getCargaHorariaTotal() { return cargaHorariaTotal; }
    public void setCargaHorariaTotal(Integer cargaHorariaTotal) { this.cargaHorariaTotal = cargaHorariaTotal; }

    public Integer getDiasLetivos() { return diasLetivos; }
    public void setDiasLetivos(Integer diasLetivos) { this.diasLetivos = diasLetivos; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public Boolean getEmitido() { return emitido; }
    public void setEmitido(Boolean emitido) { this.emitido = emitido; }

    public LocalDateTime getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDateTime dataEmissao) { this.dataEmissao = dataEmissao; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public LocalDateTime getDataAlteracao() { return dataAlteracao; }
    public void setDataAlteracao(LocalDateTime dataAlteracao) { this.dataAlteracao = dataAlteracao; }

    public List<ItemHistorico> getItems() { return items; }
    public void setItems(List<ItemHistorico> items) { this.items = items; }
}