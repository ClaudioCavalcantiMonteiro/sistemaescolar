package br.com.escola.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Frequencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    @ManyToOne
    @JoinColumn(name = "turma_id")
    private Turma turma;

    @ManyToOne
    @JoinColumn(name = "materia_id")
    private Materia materia;

    private LocalDate data;

    private Boolean presente;

    @Column(length = 500)
    private String justificativa;

    @Column(length = 500)
    private String observacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }

    public Turma getTurma() { return turma; }
    public void setTurma(Turma turma) { this.turma = turma; }

    public Materia getMateria() { return materia; }
    public void setMateria(Materia materia) { this.materia = materia; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public Boolean getPresente() { return presente; }
    public void setPresente(Boolean presente) { this.presente = presente; }

    public String getJustificativa() { return justificativa; }
    public void setJustificativa(String justificativa) { this.justificativa = justificativa; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
}