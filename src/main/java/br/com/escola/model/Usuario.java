package br.com.escola.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@SuppressWarnings("unused")
@Entity

public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @NotBlank
    private String nome;
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    @NotBlank
    @Email
    @Column(unique = true)
    private String email;
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @NotBlank
    private String senha;
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }


    private String role = "USER";
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}