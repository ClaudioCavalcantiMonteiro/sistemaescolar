package br.com.escola.model;

import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

@SuppressWarnings("unused")
@Entity
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataNascimento;
    private String matricula;

    // ===== CAMPOS COMPLEMENTARES (Histórico Escolar) =====
    private String naturalidade;
    private String nacionalidade;
    private String sexo;
    private String nomePai;
    private String nomeMae;
    private String cpfAluno;
    private String rgAluno;
    private String telefone;
    // ====================================================

    // ===== CAMPOS DE MATRÍCULA =====
    private Integer anoLetivo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataMatricula;
    private Boolean matriculaAtiva = true;
    // ================================

    @ManyToOne
    @JoinColumn(name = "serie_id")
    private Serie serie;

    @ManyToOne
    @JoinColumn(name = "turma_id")
    private Turma turma;

    @OneToOne(mappedBy = "aluno", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ResponsavelFinanceiro responsavelFinanceiro;

    @PrePersist
    public void prePersist() {
        if (this.anoLetivo == null) {
            this.anoLetivo = LocalDate.now().getYear();
        }
        if (this.dataMatricula == null) {
            this.dataMatricula = LocalDate.now();
        }
        if (this.matriculaAtiva == null) {
            this.matriculaAtiva = true;
        }
    }

    // ===== Getters e Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getNaturalidade() { return naturalidade; }
    public void setNaturalidade(String naturalidade) { this.naturalidade = naturalidade; }

    public String getNacionalidade() { return nacionalidade; }
    public void setNacionalidade(String nacionalidade) { this.nacionalidade = nacionalidade; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getNomePai() { return nomePai; }
    public void setNomePai(String nomePai) { this.nomePai = nomePai; }

    public String getNomeMae() { return nomeMae; }
    public void setNomeMae(String nomeMae) { this.nomeMae = nomeMae; }

    public String getCpfAluno() { return cpfAluno; }
    public void setCpfAluno(String cpfAluno) { this.cpfAluno = cpfAluno; }

    public String getRgAluno() { return rgAluno; }
    public void setRgAluno(String rgAluno) { this.rgAluno = rgAluno; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public Integer getAnoLetivo() { return anoLetivo; }
    public void setAnoLetivo(Integer anoLetivo) { this.anoLetivo = anoLetivo; }

    public LocalDate getDataMatricula() { return dataMatricula; }
    public void setDataMatricula(LocalDate dataMatricula) { this.dataMatricula = dataMatricula; }

    public Boolean getMatriculaAtiva() { return matriculaAtiva; }
    public void setMatriculaAtiva(Boolean matriculaAtiva) { this.matriculaAtiva = matriculaAtiva; }

    public Serie getSerie() { return serie; }
    public void setSerie(Serie serie) { this.serie = serie; }

    public Turma getTurma() { return turma; }
    public void setTurma(Turma turma) { this.turma = turma; }

    public ResponsavelFinanceiro getResponsavelFinanceiro() { return responsavelFinanceiro; }
    public void setResponsavelFinanceiro(ResponsavelFinanceiro responsavelFinanceiro) { this.responsavelFinanceiro = responsavelFinanceiro; }
}