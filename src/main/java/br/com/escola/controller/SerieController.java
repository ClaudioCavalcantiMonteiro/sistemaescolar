package br.com.escola.controller;

import br.com.escola.model.Serie;
import br.com.escola.service.SerieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("unused")
@Controller
@RequestMapping("/series")
public class SerieController {

    private final SerieService serieService;

    SerieController(SerieService serieService) {
        this.serieService = serieService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("series", serieService.listarTodas());
        return "series/listar";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("serie", new Serie());
        return "series/cadastrar";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Serie serie) {
        serieService.salvar(serie);
        return "redirect:/series";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Serie serie = serieService.buscarPorId(id);
        model.addAttribute("serie", serie);
        return "series/cadastrar";
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable Long id) {
        serieService.excluir(id);
        return "redirect:/series";
    }
}