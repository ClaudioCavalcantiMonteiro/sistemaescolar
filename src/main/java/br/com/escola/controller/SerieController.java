package br.com.escola.controller;

import br.com.escola.model.Serie;
import br.com.escola.service.SerieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/series")
public class SerieController {

    @Autowired
    private SerieService serieService;

    @GetMapping
    public String listar(@RequestParam(required = false) String nome, Model model) {
        List<Serie> series = serieService.listarTodas();

        if (nome != null && !nome.trim().isEmpty()) {
            String termo = nome.trim().toLowerCase();
            series = series.stream()
                    .filter(s -> s.getNome().toLowerCase().contains(termo))
                    .collect(Collectors.toList());
        }

        model.addAttribute("series", series);
        model.addAttribute("filtroNome", nome);
        model.addAttribute("totalSeries", serieService.listarTodas().size());
        model.addAttribute("totalFiltrado", series.size());
        return "series/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("serie", new Serie());
        return "series/cadastrar";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Serie serie, RedirectAttributes attributes) {
        try {
            serieService.salvar(serie);
            attributes.addFlashAttribute("mensagemSucesso", "Série salva com sucesso!");
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro", "Erro ao salvar a série: " + e.getMessage());
        }
        return "redirect:/series";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("serie", serieService.buscarPorId(id));
        return "series/cadastrar";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id, RedirectAttributes attributes) {
        try {
            serieService.excluir(id);
            attributes.addFlashAttribute("mensagemSucesso", "Série excluída com sucesso!");
        } catch (DataIntegrityViolationException e) {
            attributes.addFlashAttribute("mensagemErro",
                    "Não é possível excluir esta série, pois existem turmas ou alunos vinculados.");
        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro", "Erro ao excluir: " + e.getMessage());
        }
        return "redirect:/series";
    }
}