package br.com.escola.service;

import br.com.escola.model.Aluno;
import br.com.escola.model.Materia;
import br.com.escola.model.Nota;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PdfService {

    @Autowired
    private AlunoService alunoService;

    @Autowired
    private MateriaService materiaService;

    @Autowired
    private NotaService notaService;

    public ByteArrayInputStream gerarBoletimPdf(Long alunoId) {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // ===== Fontes =====
            Font titulo = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(44, 62, 80));
            Font subtitulo = new Font(Font.HELVETICA, 12, Font.NORMAL, new Color(108, 117, 125));
            Font sectionTitle = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(74, 111, 165));
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(33, 37, 41));
            Font cellBold = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(33, 37, 41));
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(108, 117, 125));

            // ===== Cabeçalho =====
            Paragraph sistema = new Paragraph("SISTEMA ESCOLAR", titulo);
            sistema.setAlignment(Element.ALIGN_CENTER);
            document.add(sistema);

            Paragraph tituloBoletim = new Paragraph("BOLETIM ESCOLAR", subtitulo);
            tituloBoletim.setAlignment(Element.ALIGN_CENTER);
            tituloBoletim.setSpacingAfter(20f);
            document.add(tituloBoletim);

            // ===== Dados do aluno =====
            Aluno aluno = alunoService.buscarPorId(alunoId);
            if (aluno == null) {
                document.close();
                return new ByteArrayInputStream(out.toByteArray());
            }

            PdfPTable dadosAluno = new PdfPTable(2);
            dadosAluno.setWidthPercentage(100);
            dadosAluno.setWidths(new float[]{1, 3});
            dadosAluno.setSpacingAfter(20f);

            adicionarLinhaInfo(dadosAluno, "Nome:", aluno.getNome(), cellBold, cellFont);
            adicionarLinhaInfo(dadosAluno, "Matrícula:",
                    aluno.getMatricula() != null ? aluno.getMatricula() : "-", cellBold, cellFont);
            adicionarLinhaInfo(dadosAluno, "Série:",
                    aluno.getSerie() != null ? aluno.getSerie().getNome() : "-", cellBold, cellFont);
            adicionarLinhaInfo(dadosAluno, "Turma:",
                    aluno.getTurma() != null ? aluno.getTurma().getNome() : "-", cellBold, cellFont);
            adicionarLinhaInfo(dadosAluno, "Data Nascimento:",
                    aluno.getDataNascimento() != null ?
                            aluno.getDataNascimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-",
                    cellBold, cellFont);
            if (aluno.getResponsavelFinanceiro() != null) {
                adicionarLinhaInfo(dadosAluno, "Responsável:",
                        aluno.getResponsavelFinanceiro().getNome(), cellBold, cellFont);
            }

            document.add(dadosAluno);

            // ===== Tabela de notas =====
            Paragraph tituloNotas = new Paragraph("Notas por Unidade", sectionTitle);
            tituloNotas.setSpacingAfter(8f);
            document.add(tituloNotas);

            List<Materia> materias = materiaService.listarTodas();
            List<Nota> notas = notaService.listarPorAluno(alunoId);

            Map<Long, Map<Integer, Nota>> notasPorMateria = new HashMap<>();
            for (Nota n : notas) {
                notasPorMateria.computeIfAbsent(n.getMateria().getId(), k -> new HashMap<>())
                        .put(n.getUnidade(), n);
            }

            PdfPTable tabela = new PdfPTable(7);
            tabela.setWidthPercentage(100);
            tabela.setWidths(new float[]{2.5f, 1, 1, 1, 1, 1.2f, 1.3f});
            tabela.setSpacingAfter(20f);

            // Cabeçalho
            Color headerColor = new Color(74, 111, 165);
            adicionarCelulaHeader(tabela, "Matéria", headerFont, headerColor);
            adicionarCelulaHeader(tabela, "1ª Un.", headerFont, headerColor);
            adicionarCelulaHeader(tabela, "2ª Un.", headerFont, headerColor);
            adicionarCelulaHeader(tabela, "3ª Un.", headerFont, headerColor);
            adicionarCelulaHeader(tabela, "4ª Un.", headerFont, headerColor);
            adicionarCelulaHeader(tabela, "Média", headerFont, headerColor);
            adicionarCelulaHeader(tabela, "Resultado", headerFont, headerColor);

            // Linhas de matérias
            for (Materia m : materias) {
                Map<Integer, Nota> unidades = notasPorMateria.get(m.getId());

                PdfPCell cellMateria = new PdfPCell(new Phrase(m.getNome(), cellBold));
                cellMateria.setPadding(6);
                cellMateria.setHorizontalAlignment(Element.ALIGN_LEFT);
                tabela.addCell(cellMateria);

                double soma = 0;
                int count = 0;

                for (int u = 1; u <= 4; u++) {
                    String valor = "-";
                    if (unidades != null) {
                        Nota n = unidades.get(u);
                        if (n != null && n.getMediaFinal() != null) {
                            valor = String.format(Locale.US, "%.1f", n.getMediaFinal());
                            soma += n.getMediaFinal();
                            count++;
                        }
                    }
                    PdfPCell c = new PdfPCell(new Phrase(valor, cellFont));
                    c.setPadding(6);
                    c.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c);
                }

                // Média
                String mediaStr = "-";
                Color corMedia = new Color(33, 37, 41);
                if (count > 0) {
                    double media = soma / count;
                    mediaStr = String.format(Locale.US, "%.1f", media);
                    corMedia = media >= 6 ? new Color(25, 135, 84) : new Color(220, 53, 69);
                }
                PdfPCell cellMedia = new PdfPCell(new Phrase(mediaStr,
                        new Font(Font.HELVETICA, 10, Font.BOLD, corMedia)));
                cellMedia.setPadding(6);
                cellMedia.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabela.addCell(cellMedia);

                // Resultado
                String resultado = "-";
                Color corResultado = new Color(108, 117, 125);
                if (count > 0) {
                    double media = soma / count;
                    if (media >= 6) {
                        resultado = "Aprovado";
                        corResultado = new Color(25, 135, 84);
                    } else {
                        resultado = "Reprovado";
                        corResultado = new Color(220, 53, 69);
                    }
                }
                PdfPCell cellResultado = new PdfPCell(new Phrase(resultado,
                        new Font(Font.HELVETICA, 10, Font.BOLD, corResultado)));
                cellResultado.setPadding(6);
                cellResultado.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabela.addCell(cellResultado);
            }

            document.add(tabela);

            // ===== Rodapé =====
            Paragraph dataEmissao = new Paragraph(
                    "Emitido em: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    smallFont);
            dataEmissao.setSpacingBefore(20f);
            document.add(dataEmissao);

            Paragraph assinatura = new Paragraph(
                    "\n\n___________________________________________\n" +
                    "Assinatura da Direção / Coordenação", smallFont);
            assinatura.setAlignment(Element.ALIGN_CENTER);
            assinatura.setSpacingBefore(30f);
            document.add(assinatura);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    private void adicionarLinhaInfo(PdfPTable tabela, String label, String valor,
                                     Font labelFont, Font valorFont) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, labelFont));
        cellLabel.setBorder(Rectangle.NO_BORDER);
        cellLabel.setPadding(4);
        tabela.addCell(cellLabel);

        PdfPCell cellValor = new PdfPCell(new Phrase(valor != null ? valor : "-", valorFont));
        cellValor.setBorder(Rectangle.NO_BORDER);
        cellValor.setPadding(4);
        tabela.addCell(cellValor);
    }

    private void adicionarCelulaHeader(PdfPTable tabela, String texto, Font fonte, Color cor) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, fonte));
        cell.setBackgroundColor(cor);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabela.addCell(cell);
    }
}
