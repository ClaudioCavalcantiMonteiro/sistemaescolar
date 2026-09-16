package br.com.escola.controller;

import br.com.escola.model.Licenca;
import br.com.escola.service.LicencaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/licenca")
public class LicencaController {

    @Autowired
    private LicencaService licencaService;

    @GetMapping("/status")
    public String status(Model model) {
        Licenca licenca = licencaService.getLicencaAtual();
        model.addAttribute("licenca", licenca);
        model.addAttribute("status", licencaService.getStatus());
        return "licenca/status";
    }

    @GetMapping("/ativar")
    public String ativarForm(@RequestParam(required = false) String bloqueado, Model model) {
        Licenca licenca = licencaService.getLicencaAtual();
        model.addAttribute("licenca", licenca);

        if (bloqueado != null) {
            return "licenca/bloqueada";
        }
        return "licenca/ativar";
    }

    @PostMapping("/ativar")
    public String ativar(@RequestParam String chave, RedirectAttributes attributes) {
        boolean sucesso = licencaService.ativarComChave(chave);

        if (sucesso) {
            attributes.addFlashAttribute("mensagemSucesso", "Licença ativada com sucesso!");
            return "redirect:/home";
        } else {
            attributes.addFlashAttribute("mensagemErro", "Chave inválida ou expirada. Verifique e tente novamente.");
            return "redirect:/licenca/ativar";
        }
    }
}