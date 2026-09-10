package br.com.escola.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Entity
public class Aluno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome;

    private LocalDate dataNascimento;
    private String matricula;

    @ManyToOne
    @JoinColumn(name = "serie_id")
    private Serie serie;

    @ManyToOne
    @JoinColumn(name = "turma_id")
    private Turma turma;

    @OneToOne(mappedBy = "aluno", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ResponsavelFinanceiro responsavelFinanceiro;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public Serie getSerie() { return serie; }
    public void setSerie(Serie serie) { this.serie = serie; }

    public Turma getTurma() { return turma; }
    public void setTurma(Turma turma) { this.turma = turma; }

    public ResponsavelFinanceiro getResponsavelFinanceiro() { return responsavelFinanceiro; }
    public void setResponsavelFinanceiro(ResponsavelFinanceiro responsavelFinanceiro) { this.responsavelFinanceiro = responsavelFinanceiro; }
}