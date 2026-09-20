package br.com.escola.model;

import jakarta.persistence.*;

@Entity
public class ItemHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "historico_id", nullable = false)
    private HistoricoEscolar historico;

    @ManyToOne
    @JoinColumn(name = "materia_id")
    private Materia materia;

    // Snapshot do nome da matéria (preserva mesmo se a matéria for renomeada/excluída)
    @Column(length = 150)
    private String nomeMateria;

    private Double notaFinal;
    private Integer faltas;
    private Integer cargaHoraria;

    // APROVADO, REPROVADO, CURSANDO
    @Column(length = 30)
    private String resultado;

    // Ordem de exibição no histórico
    private Integer ordem;

    // ===== Métodos auxiliares (@Transient) =====

    @Transient
    public boolean isAprovado() {
        return "APROVADO".equalsIgnoreCase(resultado);
    }

    @Transient
    public boolean isReprovado() {
        return "REPROVADO".equalsIgnoreCase(resultado);
    }

    @Transient
    public String getStatusCor() {
        if (isAprovado()) return "#198754";   // verde
        if (isReprovado()) return "#dc3545";  // vermelho
        return "#6c757d";                      // cinza
    }

    // ===== Getters e Setters =====
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public HistoricoEscolar getHistorico() { return historico; }
    public void setHistorico(HistoricoEscolar historico) { this.historico = historico; }

    public Materia getMateria() { return materia; }
    public void setMateria(Materia materia) { this.materia = materia; }

    public String getNomeMateria() { return nomeMateria; }
    public void setNomeMateria(String nomeMateria) { this.nomeMateria = nomeMateria; }

    public Double getNotaFinal() { return notaFinal; }
    public void setNotaFinal(Double notaFinal) { this.notaFinal = notaFinal; }

    public Integer getFaltas() { return faltas; }
    public void setFaltas(Integer faltas) { this.faltas = faltas; }

    public Integer getCargaHoraria() { return cargaHoraria; }
    public void setCargaHoraria(Integer cargaHoraria) { this.cargaHoraria = cargaHoraria; }

    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }

    public Integer getOrdem() { return ordem; }
    public void setOrdem(Integer ordem) { this.ordem = ordem; }
}
