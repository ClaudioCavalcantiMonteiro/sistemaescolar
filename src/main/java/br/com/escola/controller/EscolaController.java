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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

        // Garante valores padrão para os campos pedagógicos (caso ainda sejam null)
        if (escola.getMediaAprovacao() == null) {
            escola.setMediaAprovacao(6.0);
        }
        if (escola.getRegraAprovacao() == null || escola.getRegraAprovacao().isEmpty()) {
            escola.setRegraAprovacao("DEPENDENCIA");
        }
        if (escola.getMaxMateriasDependencia() == null) {
            escola.setMaxMateriasDependencia(2);
        }

        model.addAttribute("escola", escola);
        return "escola/formulario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Escola escola,
                         RedirectAttributes attributes) {
        try {
            Escola existente = escolaService.buscarEscola();
            if (existente != null && escola.getId() == null) {
                escola.setId(existente.getId());
            }

            // Garante valores padrão nos campos pedagógicos
            if (escola.getMediaAprovacao() == null) {
                escola.setMediaAprovacao(6.0);
            }
            if (escola.getRegraAprovacao() == null || escola.getRegraAprovacao().isEmpty()) {
                escola.setRegraAprovacao("DEPENDENCIA");
            }
            if (escola.getMaxMateriasDependencia() == null) {
                escola.setMaxMateriasDependencia(2);
            }

            escolaService.salvar(escola);
            return "redirect:/escola?sucesso";

        } catch (Exception e) {
            attributes.addFlashAttribute("mensagemErro", "Erro ao salvar: " + e.getMessage());
            return "redirect:/escola";
        }
    }
}