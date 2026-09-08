package br.com.escola.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Data
public class Turma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome; // ex: "A", "B"

    private String ano; // ex: "2025"

    private String turno; // ex: "Manhã", "Tarde"

    @ManyToOne
    @JoinColumn(name = "serie_id")
    private Serie serie;
}