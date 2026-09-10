package br.com.escola.model;

import jakarta.persistence.*;

@Entity
public class Nota {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    @ManyToOne
    @JoinColumn(name = "materia_id")
    private Materia materia;

    private Integer unidade; // 1, 2, 3, 4

    private Double nota1;
    private Double nota2;
    private Double nota3;
    private Double nota4;
    private Double recuperacao;

    public Double getMediaUnidade() {
        double soma = 0.0;
        int count = 0;
        if (nota1 != null) { soma += nota1; count++; }
        if (nota2 != null) { soma += nota2; count++; }
        if (nota3 != null) { soma += nota3; count++; }
        if (nota4 != null) { soma += nota4; count++; }
        return count == 0 ? null : soma / count;
    }

    public Double getMediaFinal() {
        Double media = getMediaUnidade();
        if (media == null) return null;
        if (recuperacao != null && recuperacao > media) {
            return recuperacao;
        }
        return media;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }
    public Materia getMateria() { return materia; }
    public void setMateria(Materia materia) { this.materia = materia; }
    public Integer getUnidade() { return unidade; }
    public void setUnidade(Integer unidade) { this.unidade = unidade; }
    public Double getNota1() { return nota1; }
    public void setNota1(Double nota1) { this.nota1 = nota1; }
    public Double getNota2() { return nota2; }
    public void setNota2(Double nota2) { this.nota2 = nota2; }
    public Double getNota3() { return nota3; }
    public void setNota3(Double nota3) { this.nota3 = nota3; }
    public Double getNota4() { return nota4; }
    public void setNota4(Double nota4) { this.nota4 = nota4; }
    public Double getRecuperacao() { return recuperacao; }
    public void setRecuperacao(Double recuperacao) { this.recuperacao = recuperacao; }
}