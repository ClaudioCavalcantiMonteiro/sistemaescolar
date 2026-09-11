package br.com.escola.controller;

import br.com.escola.model.Escola;
import br.com.escola.service.EscolaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/escola")
public class EscolaController {

    @Autowired
    private EscolaService escolaService;

    @GetMapping
    public String formulario(Model model) {
        Escola escola = escolaService.buscarEscola();
        if (escola == null) {
            escola = new Escola();
        }
        model.addAttribute("escola", escola);
        return "escola/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Escola escola) {
        Escola existente = escolaService.buscarEscola();
        if (existente != null && escola.getId() == null) {
            escola.setId(existente.getId());
        }
        escolaService.salvar(escola);
        return "redirect:/escola?sucesso";
    }
}