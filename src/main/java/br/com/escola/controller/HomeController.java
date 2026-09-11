package br.com.escola.controller;

import br.com.escola.service.EscolaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private EscolaService escolaService;

    @GetMapping("/home")
    public String home(Model model) {
        model.addAttribute("escola", escolaService.buscarEscola());
        return "home";
    }
}