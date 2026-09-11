package br.com.escola.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Escola {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String cnpj;
    private String telefone;
    private String email;
    private String site;

    private String endereco;
    private String numero;
    private String bairro;
    private String cidade;
    private String estado;
    private String cep;

    private String diretor;
    private String slogan;

    @Column(length = 500)
    private String observacao;

    private LocalDateTime dataCriacao;
    private LocalDateTime dataAlteracao;

    @PrePersist
    public void prePersist() {
        this.dataCriacao = LocalDateTime.now();
        this.dataAlteracao = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.dataAlteracao = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getDiretor() { return diretor; }
    public void setDiretor(String diretor) { this.diretor = diretor; }

    public String getSlogan() { return slogan; }
    public void setSlogan(String slogan) { this.slogan = slogan; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public LocalDateTime getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(LocalDateTime dataCriacao) { this.dataCriacao = dataCriacao; }

    public LocalDateTime getDataAlteracao() { return dataAlteracao; }
    public void setDataAlteracao(LocalDateTime dataAlteracao) { this.dataAlteracao = dataAlteracao; }

    @Transient
    public String getEnderecoCompleto() {
        StringBuilder sb = new StringBuilder();
        if (endereco != null && !endereco.isEmpty()) sb.append(endereco);
        if (numero != null && !numero.isEmpty()) sb.append(", ").append(numero);
        if (bairro != null && !bairro.isEmpty()) sb.append(" - ").append(bairro);
        if (cidade != null && !cidade.isEmpty()) sb.append(", ").append(cidade);
        if (estado != null && !estado.isEmpty()) sb.append("/").append(estado);
        if (cep != null && !cep.isEmpty()) sb.append(" - CEP: ").append(cep);
        return sb.toString();
    }
}