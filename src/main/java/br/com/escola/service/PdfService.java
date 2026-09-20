package br.com.escola.service;

import br.com.escola.model.Aluno;
import br.com.escola.model.Escola;
import br.com.escola.model.Frequencia;
import br.com.escola.model.HistoricoEscolar;
import br.com.escola.model.ItemHistorico;
import br.com.escola.model.Materia;
import br.com.escola.model.Mensalidade;
import br.com.escola.model.Nota;
import br.com.escola.model.Turma;
import br.com.escola.repository.EscolaRepository;
import br.com.escola.repository.HistoricoEscolarRepository;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unused")
@Service
public class PdfService {

    @Autowired
    private AlunoService alunoService;

    @Autowired
    private MateriaService materiaService;

    @Autowired
    private NotaService notaService;

    @Autowired
    private TurmaService turmaService;

    @Autowired
    private FrequenciaService frequenciaService;

    @Autowired
    private EscolaRepository escolaRepository;

    @Autowired
    private HistoricoEscolarRepository historicoRepository;

    // ==================================================================
    // BOLETIM INDIVIDUAL
    // ==================================================================
    public ByteArrayInputStream gerarBoletimPdf(Long alunoId) {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font escolaNome = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(30, 64, 175));
            Font escolaInfo = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(108, 117, 125));
            Font tituloBoletim = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(44, 62, 80));
            Font subtitulo = new Font(Font.HELVETICA, 11, Font.NORMAL, new Color(108, 117, 125));
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(33, 37, 41));
            Font cellBold = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(33, 37, 41));
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(108, 117, 125));

            Aluno aluno = alunoService.buscarPorId(alunoId);
            if (aluno == null) {
                document.close();
                return new ByteArrayInputStream(out.toByteArray());
            }

            List<Escola> escolas = escolaRepository.findAll();
            Escola escola = escolas.isEmpty() ? null : escolas.get(0);

            if (escola != null) {
                Paragraph nomeEsc = new Paragraph(escola.getNome().toUpperCase(), escolaNome);
                nomeEsc.setAlignment(Element.ALIGN_CENTER);
                document.add(nomeEsc);

                StringBuilder infoEscola = new StringBuilder();
                if (escola.getCnpj() != null && !escola.getCnpj().isEmpty()) {
                    infoEscola.append("CNPJ: ").append(escola.getCnpj());
                }
                if (escola.getEnderecoCompleto() != null && !escola.getEnderecoCompleto().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append(escola.getEnderecoCompleto());
                }
                if (escola.getTelefone() != null && !escola.getTelefone().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append("Tel: ").append(escola.getTelefone());
                }

                Paragraph info = new Paragraph(infoEscola.toString(), escolaInfo);
                info.setAlignment(Element.ALIGN_CENTER);
                info.setSpacingAfter(8f);
                document.add(info);
            } else {
                Paragraph nomePadrao = new Paragraph("SISTEMA ESCOLAR", escolaNome);
                nomePadrao.setAlignment(Element.ALIGN_CENTER);
                nomePadrao.setSpacingAfter(8f);
                document.add(nomePadrao);
            }

            Paragraph linha = new Paragraph("_______________________________________________________________________________", escolaInfo);
            linha.setAlignment(Element.ALIGN_CENTER);
            linha.setSpacingAfter(16f);
            document.add(linha);

            Paragraph titulo = new Paragraph("BOLETIM ESCOLAR", tituloBoletim);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(6f);
            document.add(titulo);

            Paragraph nomeAluno = new Paragraph(aluno.getNome(),
                    new Font(Font.HELVETICA, 14, Font.BOLD, new Color(30, 64, 175)));
            nomeAluno.setAlignment(Element.ALIGN_CENTER);
            nomeAluno.setSpacingAfter(4f);
            document.add(nomeAluno);

            String dadosAluno = "Série: " + (aluno.getSerie() != null ? aluno.getSerie().getNome() : "N/A") +
                    " | Turma: " + (aluno.getTurma() != null ? aluno.getTurma().getNome() : "N/A") +
                    " | Matrícula: " + (aluno.getMatricula() != null ? aluno.getMatricula() : "-");
            Paragraph dados = new Paragraph(dadosAluno, subtitulo);
            dados.setAlignment(Element.ALIGN_CENTER);
            dados.setSpacingAfter(20f);
            document.add(dados);

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

            Color headerColor = new Color(30, 64, 175);
            adicionarCelulaHeader(tabela, "Matéria", headerFont, headerColor);
            adicionarCelulaHeader(tabela, "1ª Un.", headerFont, headerColor);
            adicionarCelulaHeader(tabela, "2ª Un.", headerFont, headerColor);
            adicionarCelulaHeader(tabela, "3ª Un.", headerFont, headerColor);
            adicionarCelulaHeader(tabela, "4ª Un.", headerFont, headerColor);
            adicionarCelulaHeader(tabela, "Média", headerFont, headerColor);
            adicionarCelulaHeader(tabela, "Resultado", headerFont, headerColor);

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

            Paragraph dataEmissao = new Paragraph(
                    "Emitido em: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    smallFont);
            dataEmissao.setSpacingBefore(20f);
            dataEmissao.setAlignment(Element.ALIGN_CENTER);
            document.add(dataEmissao);

            Paragraph assinatura = new Paragraph(
                    "\n\n___________________________________________\n" +
                    "Assinatura da Direção / Coordenação", smallFont);
            assinatura.setAlignment(Element.ALIGN_CENTER);
            assinatura.setSpacingBefore(40f);
            document.add(assinatura);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // ==================================================================
    // RELATORIO DE ALUNOS POR TURMA
    // ==================================================================
    public ByteArrayInputStream gerarRelatorioAlunosPorTurma(Long turmaId) {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font escolaNome = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(30, 64, 175));
            Font escolaInfo = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(108, 117, 125));
            Font titulo = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(30, 64, 175));
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(33, 37, 41));
            Font cellBold = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(33, 37, 41));
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(108, 117, 125));

            Turma turma = turmaService.buscarPorId(turmaId);
            if (turma == null) {
                document.close();
                return new ByteArrayInputStream(out.toByteArray());
            }

            List<Escola> escolas = escolaRepository.findAll();
            Escola escola = escolas.isEmpty() ? null : escolas.get(0);

            if (escola != null) {
                Paragraph nomeEscola = new Paragraph(escola.getNome().toUpperCase(), escolaNome);
                nomeEscola.setAlignment(Element.ALIGN_CENTER);
                document.add(nomeEscola);

                StringBuilder infoEscola = new StringBuilder();
                if (escola.getCnpj() != null && !escola.getCnpj().isEmpty()) {
                    infoEscola.append("CNPJ: ").append(escola.getCnpj());
                }
                if (escola.getEnderecoCompleto() != null && !escola.getEnderecoCompleto().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append(escola.getEnderecoCompleto());
                }
                if (escola.getTelefone() != null && !escola.getTelefone().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append("Tel: ").append(escola.getTelefone());
                }

                Paragraph info = new Paragraph(infoEscola.toString(), escolaInfo);
                info.setAlignment(Element.ALIGN_CENTER);
                info.setSpacingAfter(20f);
                document.add(info);
            } else {
                Paragraph nomePadrao = new Paragraph("SISTEMA ESCOLAR", escolaNome);
                nomePadrao.setAlignment(Element.ALIGN_CENTER);
                nomePadrao.setSpacingAfter(20f);
                document.add(nomePadrao);
            }

            Paragraph linha = new Paragraph("_______________________________________________________________________________", escolaInfo);
            linha.setAlignment(Element.ALIGN_CENTER);
            linha.setSpacingAfter(20f);
            document.add(linha);

            Paragraph tituloRel = new Paragraph("RELAÇÃO DE ALUNOS POR TURMA", titulo);
            tituloRel.setAlignment(Element.ALIGN_CENTER);
            tituloRel.setSpacingAfter(8f);
            document.add(tituloRel);

            Paragraph dadosTurma = new Paragraph();
            dadosTurma.setAlignment(Element.ALIGN_CENTER);
            dadosTurma.setSpacingAfter(20f);
            dadosTurma.add(new Phrase("Turma: ", cellBold));
            dadosTurma.add(new Phrase(turma.getNome() + " | ", cellFont));
            dadosTurma.add(new Phrase("Série: ", cellBold));
            dadosTurma.add(new Phrase((turma.getSerie() != null ? turma.getSerie().getNome() : "N/A") + " | ", cellFont));
            dadosTurma.add(new Phrase("Ano: ", cellBold));
            dadosTurma.add(new Phrase((turma.getAno() != null ? String.valueOf(turma.getAno()) : "-"), cellFont));
            document.add(dadosTurma);

            List<Aluno> alunos = alunoService.buscarPorTurma(turmaId);

            if (alunos.isEmpty()) {
                Paragraph semAlunos = new Paragraph("Nenhum aluno cadastrado nesta turma.", cellFont);
                semAlunos.setAlignment(Element.ALIGN_CENTER);
                document.add(semAlunos);
            } else {
                PdfPTable tabela = new PdfPTable(5);
                tabela.setWidthPercentage(100);
                tabela.setWidths(new float[]{0.8f, 3.5f, 1.2f, 1.5f, 1.5f});
                tabela.setSpacingAfter(20f);

                Color headerColor = new Color(30, 64, 175);
                adicionarCelulaHeader(tabela, "Nº", headerFont, headerColor);
                adicionarCelulaHeader(tabela, "Nome do Aluno", headerFont, headerColor);
                adicionarCelulaHeader(tabela, "Matrícula", headerFont, headerColor);
                adicionarCelulaHeader(tabela, "Data Nasc.", headerFont, headerColor);
                adicionarCelulaHeader(tabela, "Status", headerFont, headerColor);

                int cont = 1;
                for (Aluno aluno : alunos) {
                    PdfPCell c1 = new PdfPCell(new Phrase(String.valueOf(cont), cellFont));
                    c1.setPadding(6);
                    c1.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c1);

                    PdfPCell c2 = new PdfPCell(new Phrase(aluno.getNome(), cellFont));
                    c2.setPadding(6);
                    c2.setHorizontalAlignment(Element.ALIGN_LEFT);
                    tabela.addCell(c2);

                    PdfPCell c3 = new PdfPCell(new Phrase(
                            aluno.getMatricula() != null ? aluno.getMatricula() : "-", cellFont));
                    c3.setPadding(6);
                    c3.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c3);

                    String dataNasc = (aluno.getDataNascimento() != null)
                            ? aluno.getDataNascimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            : "-";
                    PdfPCell c4 = new PdfPCell(new Phrase(dataNasc, cellFont));
                    c4.setPadding(6);
                    c4.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c4);

                    boolean ativo = aluno.getMatriculaAtiva() != null && aluno.getMatriculaAtiva();
                    String status = ativo ? "Ativa" : "Inativa";
                    Color corStatus = ativo ? new Color(25, 135, 84) : new Color(220, 53, 69);
                    PdfPCell c5 = new PdfPCell(new Phrase(status,
                            new Font(Font.HELVETICA, 10, Font.BOLD, corStatus)));
                    c5.setPadding(6);
                    c5.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c5);

                    cont++;
                }

                document.add(tabela);

                Paragraph total = new Paragraph("Total de alunos: " + alunos.size(), cellBold);
                total.setAlignment(Element.ALIGN_RIGHT);
                total.setSpacingAfter(20f);
                document.add(total);
            }

            Paragraph dataEmissao = new Paragraph(
                    "Emitido em: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    smallFont);
            dataEmissao.setSpacingBefore(20f);
            document.add(dataEmissao);

            Paragraph assinatura = new Paragraph(
                    "\n\n___________________________________________\n" +
                    "Assinatura da Direção / Coordenação", smallFont);
            assinatura.setAlignment(Element.ALIGN_CENTER);
            assinatura.setSpacingBefore(40f);
            document.add(assinatura);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // ==================================================================
    // RELATORIO DE FREQUENCIA MENSAL POR TURMA
    // ==================================================================
    public ByteArrayInputStream gerarRelatorioFrequenciaPdf(Long turmaId, Integer mes, Integer ano) {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font escolaNome = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(30, 64, 175));
            Font escolaInfo = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(108, 117, 125));
            Font titulo = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(44, 62, 80));
            Font headerFont = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(33, 37, 41));
            Font cellBold = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(33, 37, 41));
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(108, 117, 125));

            Turma turma = turmaService.buscarPorId(turmaId);
            if (turma == null) {
                document.close();
                return new ByteArrayInputStream(out.toByteArray());
            }

            if (mes == null || mes < 1 || mes > 12) mes = LocalDate.now().getMonthValue();
            if (ano == null || ano < 2000) ano = LocalDate.now().getYear();

            YearMonth yearMonth = YearMonth.of(ano, mes);
            LocalDate inicio = yearMonth.atDay(1);
            LocalDate fim = yearMonth.atEndOfMonth();

            List<Aluno> alunos = alunoService.buscarPorTurma(turmaId);
            List<Frequencia> registros = frequenciaService.listarPorTurmaEPeriodo(turmaId, inicio, fim);

            Map<Long, int[]> stats = new HashMap<>();
            for (Frequencia f : registros) {
                if (f.getAluno() == null) continue;
                int[] s = stats.computeIfAbsent(f.getAluno().getId(), k -> new int[2]);
                s[0]++;
                if (Boolean.TRUE.equals(f.getPresente())) s[1]++;
            }

            int totalRegistros = registros.size();
            int totalPresencas = 0;
            for (Frequencia f : registros) {
                if (Boolean.TRUE.equals(f.getPresente())) totalPresencas++;
            }
            double percentualGeral = totalRegistros > 0 ? (totalPresencas * 100.0 / totalRegistros) : 0;
            percentualGeral = Math.round(percentualGeral * 10.0) / 10.0;

            String[] meses = {"", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                              "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
            String nomeMes = meses[mes];

            List<Escola> escolas = escolaRepository.findAll();
            Escola escola = escolas.isEmpty() ? null : escolas.get(0);

            if (escola != null) {
                Paragraph nomeEsc = new Paragraph(escola.getNome().toUpperCase(), escolaNome);
                nomeEsc.setAlignment(Element.ALIGN_CENTER);
                document.add(nomeEsc);

                StringBuilder infoEscola = new StringBuilder();
                if (escola.getCnpj() != null && !escola.getCnpj().isEmpty()) {
                    infoEscola.append("CNPJ: ").append(escola.getCnpj());
                }
                if (escola.getEnderecoCompleto() != null && !escola.getEnderecoCompleto().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append(escola.getEnderecoCompleto());
                }
                if (escola.getTelefone() != null && !escola.getTelefone().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append("Tel: ").append(escola.getTelefone());
                }
                if (escola.getEmail() != null && !escola.getEmail().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append(escola.getEmail());
                }

                Paragraph info = new Paragraph(infoEscola.toString(), escolaInfo);
                info.setAlignment(Element.ALIGN_CENTER);
                info.setSpacingAfter(8f);
                document.add(info);
            } else {
                Paragraph nomePadrao = new Paragraph("SISTEMA ESCOLAR", escolaNome);
                nomePadrao.setAlignment(Element.ALIGN_CENTER);
                nomePadrao.setSpacingAfter(8f);
                document.add(nomePadrao);
            }

            Paragraph linha = new Paragraph("_______________________________________________________________________________", escolaInfo);
            linha.setAlignment(Element.ALIGN_CENTER);
            linha.setSpacingAfter(16f);
            document.add(linha);

            Paragraph tituloRel = new Paragraph("RELATÓRIO DE FREQUÊNCIA MENSAL", titulo);
            tituloRel.setAlignment(Element.ALIGN_CENTER);
            tituloRel.setSpacingAfter(6f);
            document.add(tituloRel);

            Paragraph dadosTurma = new Paragraph();
            dadosTurma.setAlignment(Element.ALIGN_CENTER);
            dadosTurma.setSpacingAfter(16f);
            dadosTurma.add(new Phrase("Turma: ", cellBold));
            dadosTurma.add(new Phrase(turma.getNome() + "  |  ", cellFont));
            dadosTurma.add(new Phrase("Série: ", cellBold));
            dadosTurma.add(new Phrase((turma.getSerie() != null ? turma.getSerie().getNome() : "N/A") + "  |  ", cellFont));
            dadosTurma.add(new Phrase("Período: ", cellBold));
            dadosTurma.add(new Phrase(nomeMes + " / " + ano, cellFont));
            document.add(dadosTurma);

            PdfPTable resumo = new PdfPTable(3);
            resumo.setWidthPercentage(100);
            resumo.setWidths(new float[]{1, 1, 1});
            resumo.setSpacingAfter(16f);

            adicionarCelulaResumo(resumo, "REGISTROS NO MÊS",
                    String.valueOf(totalRegistros), new Color(74, 111, 165));
            adicionarCelulaResumo(resumo, "PRESENÇAS",
                    String.valueOf(totalPresencas), new Color(25, 135, 84));
            Color corPercGeral = percentualGeral < 75 ? new Color(220, 53, 69) : new Color(25, 135, 84);
            adicionarCelulaResumo(resumo, "FREQUÊNCIA GERAL",
                    String.format(Locale.US, "%.1f%%", percentualGeral), corPercGeral);

            document.add(resumo);

            PdfPTable tabela = new PdfPTable(8);
            tabela.setWidthPercentage(100);
            tabela.setWidths(new float[]{0.4f, 3.2f, 0.9f, 0.9f, 0.9f, 0.7f, 1.0f, 1.3f});
            tabela.setSpacingAfter(16f);

            Color headerColor = new Color(30, 64, 175);
            adicionarCelulaHeaderCompacto(tabela, "#", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Aluno", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Matrícula", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Presenças", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Ausências", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Total", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Frequência", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Status", headerFont, headerColor);

            if (alunos.isEmpty()) {
                PdfPCell vazio = new PdfPCell(new Phrase("Nenhum aluno cadastrado nesta turma.", cellFont));
                vazio.setColspan(8);
                vazio.setPadding(16);
                vazio.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabela.addCell(vazio);
            } else {
                int idx = 1;
                for (Aluno aluno : alunos) {
                    int[] s = stats.getOrDefault(aluno.getId(), new int[]{0, 0});
                    int total = s[0];
                    int presencas = s[1];
                    int ausencias = total - presencas;
                    double perc = total > 0 ? (presencas * 100.0 / total) : 0;
                    perc = Math.round(perc * 10.0) / 10.0;

                    PdfPCell c1 = new PdfPCell(new Phrase(String.valueOf(idx), cellFont));
                    c1.setPadding(6);
                    c1.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c1);

                    PdfPCell c2 = new PdfPCell(new Phrase(aluno.getNome(), cellFont));
                    c2.setPadding(6);
                    c2.setHorizontalAlignment(Element.ALIGN_LEFT);
                    tabela.addCell(c2);

                    PdfPCell c3 = new PdfPCell(new Phrase(
                            aluno.getMatricula() != null ? aluno.getMatricula() : "-", cellFont));
                    c3.setPadding(6);
                    c3.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c3);

                    PdfPCell c4 = new PdfPCell(new Phrase(String.valueOf(presencas),
                            new Font(Font.HELVETICA, 10, Font.BOLD, new Color(25, 135, 84))));
                    c4.setPadding(6);
                    c4.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c4);

                    Color corAus = ausencias > 0 ? new Color(220, 53, 69) : new Color(33, 37, 41);
                    PdfPCell c5 = new PdfPCell(new Phrase(String.valueOf(ausencias),
                            new Font(Font.HELVETICA, 10, Font.BOLD, corAus)));
                    c5.setPadding(6);
                    c5.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c5);

                    PdfPCell c6 = new PdfPCell(new Phrase(String.valueOf(total), cellFont));
                    c6.setPadding(6);
                    c6.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c6);

                    Color corPercAluno = perc < 75 ? new Color(220, 53, 69) : new Color(25, 135, 84);
                    PdfPCell c7 = new PdfPCell(new Phrase(String.format(Locale.US, "%.1f%%", perc),
                            new Font(Font.HELVETICA, 10, Font.BOLD, corPercAluno)));
                    c7.setPadding(6);
                    c7.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c7);

                    boolean risco = total > 0 && perc < 75;
                    String status = risco ? "Abaixo de 75%" : "Regular";
                    Color corStatus = risco ? new Color(220, 53, 69) : new Color(25, 135, 84);
                    PdfPCell c8 = new PdfPCell(new Phrase(status,
                            new Font(Font.HELVETICA, 9, Font.BOLD, corStatus)));
                    c8.setPadding(6);
                    c8.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c8);

                    idx++;
                }
            }

            document.add(tabela);

            Paragraph assinatura = new Paragraph(
                    "\n\n___________________________________________\n" +
                    "Assinatura do Professor / Coordenação", smallFont);
            assinatura.setAlignment(Element.ALIGN_CENTER);
            assinatura.setSpacingBefore(40f);
            document.add(assinatura);

            Paragraph dataEmissao = new Paragraph(
                    "Emitido em: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    smallFont);
            dataEmissao.setAlignment(Element.ALIGN_CENTER);
            dataEmissao.setSpacingBefore(20f);
            document.add(dataEmissao);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // ==================================================================
    // PDF DA FREQUENCIA INDIVIDUAL DO ALUNO
    // ==================================================================
    public ByteArrayInputStream gerarPdfFrequenciaAluno(Long alunoId) {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font escolaNome = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(30, 64, 175));
            Font escolaInfo = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(108, 117, 125));
            Font titulo = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(44, 62, 80));
            Font subtitulo = new Font(Font.HELVETICA, 11, Font.NORMAL, new Color(108, 117, 125));
            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(33, 37, 41));
            Font cellBold = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(33, 37, 41));
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(108, 117, 125));

            Aluno aluno = alunoService.buscarPorId(alunoId);
            if (aluno == null) {
                document.close();
                return new ByteArrayInputStream(out.toByteArray());
            }

            List<Frequencia> registros = frequenciaService.listarPorAluno(alunoId);
            FrequenciaService.EstatisticasFrequencia estatisticas =
                    frequenciaService.calcularEstatisticas(alunoId);

            List<Escola> escolas = escolaRepository.findAll();
            Escola escola = escolas.isEmpty() ? null : escolas.get(0);

            if (escola != null) {
                Paragraph nomeEsc = new Paragraph(escola.getNome().toUpperCase(), escolaNome);
                nomeEsc.setAlignment(Element.ALIGN_CENTER);
                document.add(nomeEsc);

                StringBuilder infoEscola = new StringBuilder();
                if (escola.getCnpj() != null && !escola.getCnpj().isEmpty()) {
                    infoEscola.append("CNPJ: ").append(escola.getCnpj());
                }
                if (escola.getEnderecoCompleto() != null && !escola.getEnderecoCompleto().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append(escola.getEnderecoCompleto());
                }
                if (escola.getTelefone() != null && !escola.getTelefone().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append("Tel: ").append(escola.getTelefone());
                }
                if (escola.getEmail() != null && !escola.getEmail().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append(escola.getEmail());
                }

                Paragraph info = new Paragraph(infoEscola.toString(), escolaInfo);
                info.setAlignment(Element.ALIGN_CENTER);
                info.setSpacingAfter(8f);
                document.add(info);
            } else {
                Paragraph nomePadrao = new Paragraph("SISTEMA ESCOLAR", escolaNome);
                nomePadrao.setAlignment(Element.ALIGN_CENTER);
                nomePadrao.setSpacingAfter(8f);
                document.add(nomePadrao);
            }

            Paragraph linha = new Paragraph("_______________________________________________________________________________", escolaInfo);
            linha.setAlignment(Element.ALIGN_CENTER);
            linha.setSpacingAfter(16f);
            document.add(linha);

            Paragraph tituloRel = new Paragraph("FICHA DE FREQUÊNCIA DO ALUNO", titulo);
            tituloRel.setAlignment(Element.ALIGN_CENTER);
            tituloRel.setSpacingAfter(6f);
            document.add(tituloRel);

            Paragraph nomeAluno = new Paragraph(aluno.getNome(),
                    new Font(Font.HELVETICA, 13, Font.BOLD, new Color(30, 64, 175)));
            nomeAluno.setAlignment(Element.ALIGN_CENTER);
            nomeAluno.setSpacingAfter(4f);
            document.add(nomeAluno);

            String dadosAluno = "Série: " + (aluno.getSerie() != null ? aluno.getSerie().getNome() : "N/A") +
                    " | Turma: " + (aluno.getTurma() != null ? aluno.getTurma().getNome() : "N/A") +
                    " | Matrícula: " + (aluno.getMatricula() != null ? aluno.getMatricula() : "-");
            Paragraph dados = new Paragraph(dadosAluno, subtitulo);
            dados.setAlignment(Element.ALIGN_CENTER);
            dados.setSpacingAfter(16f);
            document.add(dados);

            PdfPTable resumo = new PdfPTable(4);
            resumo.setWidthPercentage(100);
            resumo.setWidths(new float[]{1, 1, 1, 1});
            resumo.setSpacingAfter(16f);

            adicionarCelulaResumo(resumo, "TOTAL DE REGISTROS",
                    String.valueOf(estatisticas.getTotal()), new Color(74, 111, 165));
            adicionarCelulaResumo(resumo, "PRESENÇAS",
                    String.valueOf(estatisticas.getPresencas()), new Color(25, 135, 84));
            adicionarCelulaResumo(resumo, "AUSÊNCIAS",
                    String.valueOf(estatisticas.getFaltas()), new Color(220, 53, 69));

            Color corPerc = estatisticas.isAbaixoMinimo()
                    ? new Color(220, 53, 69)
                    : new Color(25, 135, 84);
            adicionarCelulaResumo(resumo, "PERCENTUAL",
                    String.format(Locale.US, "%.1f%%", estatisticas.getPercentual()), corPerc);

            document.add(resumo);

            if (estatisticas.isAbaixoMinimo()) {
                Paragraph aviso = new Paragraph();
                aviso.setAlignment(Element.ALIGN_CENTER);
                aviso.setSpacingAfter(12f);
                aviso.add(new Phrase("ATENÇÃO: ",
                        new Font(Font.HELVETICA, 10, Font.BOLD, new Color(220, 53, 69))));
                aviso.add(new Phrase("Este aluno está com frequência abaixo do mínimo exigido (75%).",
                        new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(133, 100, 4))));
                document.add(aviso);
            }

            PdfPTable tabela = new PdfPTable(6);
            tabela.setWidthPercentage(100);
            tabela.setWidths(new float[]{1.2f, 1.3f, 1.5f, 1.2f, 2f, 2f});
            tabela.setSpacingAfter(16f);

            Color headerColor = new Color(30, 64, 175);
            adicionarCelulaHeader(tabela, "Data", headerFont, headerColor);
            adicionarCelulaHeader(tabela, "Turma", headerFont, headerColor);
            adicionarCelulaHeader(tabela, "Matéria", headerFont, headerColor);
            adicionarCelulaHeader(tabela, "Status", headerFont, headerColor);
            adicionarCelulaHeader(tabela, "Justificativa", headerFont, headerColor);
            adicionarCelulaHeader(tabela, "Observação", headerFont, headerColor);

            if (registros.isEmpty()) {
                PdfPCell vazio = new PdfPCell(new Phrase("Nenhum registro de frequência para este aluno.", cellFont));
                vazio.setColspan(6);
                vazio.setPadding(16);
                vazio.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabela.addCell(vazio);
            } else {
                DateTimeFormatter fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                for (Frequencia r : registros) {
                    PdfPCell c1 = new PdfPCell(new Phrase(
                            r.getData() != null ? r.getData().format(fmtData) : "-", cellFont));
                    c1.setPadding(5);
                    c1.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c1);

                    PdfPCell c2 = new PdfPCell(new Phrase(
                            r.getTurma() != null ? r.getTurma().getNome() : "-", cellFont));
                    c2.setPadding(5);
                    tabela.addCell(c2);

                    PdfPCell c3 = new PdfPCell(new Phrase(
                            r.getMateria() != null ? r.getMateria().getNome() : "Geral", cellFont));
                    c3.setPadding(5);
                    tabela.addCell(c3);

                    boolean presente = Boolean.TRUE.equals(r.getPresente());
                    String status = presente ? "Presente" : "Ausente";
                    Color corStatus = presente ? new Color(25, 135, 84) : new Color(220, 53, 69);
                    PdfPCell c4 = new PdfPCell(new Phrase(status,
                            new Font(Font.HELVETICA, 9, Font.BOLD, corStatus)));
                    c4.setPadding(5);
                    c4.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c4);

                    PdfPCell c5 = new PdfPCell(new Phrase(
                            r.getJustificativa() != null && !r.getJustificativa().isEmpty()
                                    ? r.getJustificativa() : "-", cellFont));
                    c5.setPadding(5);
                    tabela.addCell(c5);

                    PdfPCell c6 = new PdfPCell(new Phrase(
                            r.getObservacao() != null && !r.getObservacao().isEmpty()
                                    ? r.getObservacao() : "-", cellFont));
                    c6.setPadding(5);
                    tabela.addCell(c6);
                }
            }

            document.add(tabela);

            Paragraph assinatura = new Paragraph(
                    "\n\n___________________________________________\n" +
                    "Assinatura do Professor / Coordenação", smallFont);
            assinatura.setAlignment(Element.ALIGN_CENTER);
            assinatura.setSpacingBefore(40f);
            document.add(assinatura);

            Paragraph dataEmissao = new Paragraph(
                    "Emitido em: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    smallFont);
            dataEmissao.setAlignment(Element.ALIGN_CENTER);
            dataEmissao.setSpacingBefore(20f);
            document.add(dataEmissao);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // ==================================================================
    // RELATORIO FINANCEIRO (MENSALIDADES DO PERIODO)
    // ==================================================================
    public ByteArrayInputStream gerarRelatorioFinanceiroPdf(
            List<Mensalidade> mensalidades,
            String nomeMes,
            Integer ano,
            String nomeTurmaFiltro,
            double totalPrevisto,
            double totalRecebido,
            double totalAtrasado,
            int qtdAtrasadas) {

        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font escolaNome = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(30, 64, 175));
            Font escolaInfo = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(108, 117, 125));
            Font titulo = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(44, 62, 80));
            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(33, 37, 41));
            Font cellBold = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(33, 37, 41));
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(108, 117, 125));

            List<Escola> escolas = escolaRepository.findAll();
            Escola escola = escolas.isEmpty() ? null : escolas.get(0);

            if (escola != null) {
                Paragraph nomeEsc = new Paragraph(escola.getNome().toUpperCase(), escolaNome);
                nomeEsc.setAlignment(Element.ALIGN_CENTER);
                document.add(nomeEsc);

                StringBuilder infoEscola = new StringBuilder();
                if (escola.getCnpj() != null && !escola.getCnpj().isEmpty()) {
                    infoEscola.append("CNPJ: ").append(escola.getCnpj());
                }
                if (escola.getEnderecoCompleto() != null && !escola.getEnderecoCompleto().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append(escola.getEnderecoCompleto());
                }
                if (escola.getTelefone() != null && !escola.getTelefone().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append("Tel: ").append(escola.getTelefone());
                }
                if (escola.getEmail() != null && !escola.getEmail().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append(escola.getEmail());
                }

                Paragraph info = new Paragraph(infoEscola.toString(), escolaInfo);
                info.setAlignment(Element.ALIGN_CENTER);
                info.setSpacingAfter(8f);
                document.add(info);
            } else {
                Paragraph nomePadrao = new Paragraph("SISTEMA ESCOLAR", escolaNome);
                nomePadrao.setAlignment(Element.ALIGN_CENTER);
                nomePadrao.setSpacingAfter(8f);
                document.add(nomePadrao);
            }

            Paragraph linha = new Paragraph("_______________________________________________________________________________", escolaInfo);
            linha.setAlignment(Element.ALIGN_CENTER);
            linha.setSpacingAfter(16f);
            document.add(linha);

            Paragraph tituloRel = new Paragraph("RELATÓRIO FINANCEIRO", titulo);
            tituloRel.setAlignment(Element.ALIGN_CENTER);
            tituloRel.setSpacingAfter(6f);
            document.add(tituloRel);

            StringBuilder sub = new StringBuilder();
            sub.append("Período: ").append(nomeMes).append(" / ").append(ano);
            if (nomeTurmaFiltro != null && !nomeTurmaFiltro.isEmpty()) {
                sub.append("  |  Turma: ").append(nomeTurmaFiltro);
            }
            Paragraph subP = new Paragraph(sub.toString(),
                    new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(108, 117, 125)));
            subP.setAlignment(Element.ALIGN_CENTER);
            subP.setSpacingAfter(16f);
            document.add(subP);

            PdfPTable resumo = new PdfPTable(4);
            resumo.setWidthPercentage(100);
            resumo.setWidths(new float[]{1, 1, 1, 1});
            resumo.setSpacingAfter(16f);

            adicionarCelulaResumo(resumo, "TOTAL PREVISTO",
                    "R$ " + String.format(Locale.US, "%.2f", totalPrevisto), new Color(74, 111, 165));
            adicionarCelulaResumo(resumo, "TOTAL RECEBIDO",
                    "R$ " + String.format(Locale.US, "%.2f", totalRecebido), new Color(25, 135, 84));
            adicionarCelulaResumo(resumo, "EM ATRASO",
                    "R$ " + String.format(Locale.US, "%.2f", totalAtrasado), new Color(220, 53, 69));
            adicionarCelulaResumo(resumo, "ATRASADAS",
                    String.valueOf(qtdAtrasadas), new Color(243, 156, 18));

            document.add(resumo);

            PdfPTable tabela = new PdfPTable(7);
            tabela.setWidthPercentage(100);
            tabela.setWidths(new float[]{0.4f, 3.2f, 1.2f, 1.1f, 1.1f, 1.2f, 1.3f});
            tabela.setSpacingAfter(16f);

            Color headerColor = new Color(30, 64, 175);
            adicionarCelulaHeaderCompacto(tabela, "#", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Aluno", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Turma", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Referência", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Vencimento", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Valor", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Status", headerFont, headerColor);

            if (mensalidades == null || mensalidades.isEmpty()) {
                PdfPCell vazio = new PdfPCell(new Phrase("Nenhuma mensalidade encontrada no período.", cellFont));
                vazio.setColspan(7);
                vazio.setPadding(16);
                vazio.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabela.addCell(vazio);
            } else {
                DateTimeFormatter fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                DateTimeFormatter fmtMesAno = DateTimeFormatter.ofPattern("MM/yyyy");
                int idx = 1;

                for (Mensalidade m : mensalidades) {
                    PdfPCell c1 = new PdfPCell(new Phrase(String.valueOf(idx), cellFont));
                    c1.setPadding(5);
                    c1.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c1);

                    String nomeAluno = m.getAluno() != null ? m.getAluno().getNome() : "-";
                    PdfPCell c2 = new PdfPCell(new Phrase(nomeAluno, cellFont));
                    c2.setPadding(5);
                    tabela.addCell(c2);

                    String nomeTurma = (m.getAluno() != null && m.getAluno().getTurma() != null)
                            ? m.getAluno().getTurma().getNome() : "-";
                    PdfPCell c3 = new PdfPCell(new Phrase(nomeTurma, cellFont));
                    c3.setPadding(5);
                    tabela.addCell(c3);

                    String ref = m.getMesReferencia() != null
                            ? m.getMesReferencia().format(fmtMesAno) : "-";
                    PdfPCell c4 = new PdfPCell(new Phrase(ref, cellFont));
                    c4.setPadding(5);
                    c4.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c4);

                    String venc = m.getDataVencimento() != null
                            ? m.getDataVencimento().format(fmtData) : "-";
                    PdfPCell c5 = new PdfPCell(new Phrase(venc, cellFont));
                    c5.setPadding(5);
                    c5.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c5);

                    String valor = "R$ " + String.format(Locale.US, "%.2f",
                            m.getValorOriginal() != null ? m.getValorOriginal() : 0.0);
                    PdfPCell c6 = new PdfPCell(new Phrase(valor, cellBold));
                    c6.setPadding(5);
                    c6.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    tabela.addCell(c6);

                    String status;
                    Color corStatus;
                    if ("PAGA".equals(m.getStatus())) {
                        status = "Paga"; corStatus = new Color(25, 135, 84);
                    } else if ("CANCELADA".equals(m.getStatus())) {
                        status = "Cancelada"; corStatus = new Color(108, 117, 125);
                    } else if (m.isAtrasada()) {
                        status = "Atrasada"; corStatus = new Color(220, 53, 69);
                    } else {
                        status = "Pendente"; corStatus = new Color(243, 156, 18);
                    }
                    PdfPCell c7 = new PdfPCell(new Phrase(status,
                            new Font(Font.HELVETICA, 9, Font.BOLD, corStatus)));
                    c7.setPadding(5);
                    c7.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c7);

                    idx++;
                }
            }

            document.add(tabela);

            Paragraph assinatura = new Paragraph(
                    "\n\n___________________________________________\n" +
                    "Assinatura da Direção / Coordenação", smallFont);
            assinatura.setAlignment(Element.ALIGN_CENTER);
            assinatura.setSpacingBefore(40f);
            document.add(assinatura);

            Paragraph dataEmissao = new Paragraph(
                    "Emitido em: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    smallFont);
            dataEmissao.setAlignment(Element.ALIGN_CENTER);
            dataEmissao.setSpacingBefore(20f);
            document.add(dataEmissao);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // ==================================================================
    // DECLARACAO DE MATRICULA
    // ==================================================================
    public ByteArrayInputStream gerarDeclaracaoMatricula(Long alunoId) {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font escolaNome = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(30, 64, 175));
            Font escolaInfo = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(108, 117, 125));
            Font titulo = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(44, 62, 80));
            Font corpo = new Font(Font.HELVETICA, 11, Font.NORMAL, new Color(33, 37, 41));
            Font negrito = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(33, 37, 41));
            Font italico = new Font(Font.HELVETICA, 11, Font.ITALIC, new Color(33, 37, 41));
            Font smallFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(108, 117, 125));

            Aluno aluno = alunoService.buscarPorId(alunoId);
            if (aluno == null) {
                document.close();
                return new ByteArrayInputStream(out.toByteArray());
            }

            List<Escola> escolas = escolaRepository.findAll();
            Escola escola = escolas.isEmpty() ? null : escolas.get(0);

            if (escola != null) {
                Paragraph nomeEscola = new Paragraph(escola.getNome().toUpperCase(), escolaNome);
                nomeEscola.setAlignment(Element.ALIGN_CENTER);
                document.add(nomeEscola);

                StringBuilder infoEscola = new StringBuilder();
                if (escola.getCnpj() != null && !escola.getCnpj().isEmpty()) {
                    infoEscola.append("CNPJ: ").append(escola.getCnpj());
                }
                if (escola.getEnderecoCompleto() != null && !escola.getEnderecoCompleto().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append(escola.getEnderecoCompleto());
                }
                if (escola.getTelefone() != null && !escola.getTelefone().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append("Tel: ").append(escola.getTelefone());
                }

                Paragraph info = new Paragraph(infoEscola.toString(), escolaInfo);
                info.setAlignment(Element.ALIGN_CENTER);
                info.setSpacingAfter(20f);
                document.add(info);
            } else {
                Paragraph nomePadrao = new Paragraph("SISTEMA ESCOLAR", escolaNome);
                nomePadrao.setAlignment(Element.ALIGN_CENTER);
                nomePadrao.setSpacingAfter(20f);
                document.add(nomePadrao);
            }

            Paragraph linha = new Paragraph("_______________________________________________________________________________", escolaInfo);
            linha.setAlignment(Element.ALIGN_CENTER);
            linha.setSpacingAfter(30f);
            document.add(linha);

            Paragraph tituloDecl = new Paragraph("DECLARAÇÃO DE MATRÍCULA", titulo);
            tituloDecl.setAlignment(Element.ALIGN_CENTER);
            tituloDecl.setSpacingAfter(40f);
            document.add(tituloDecl);

            String nomeAluno = aluno.getNome() != null ? aluno.getNome() : "_____________________";
            String dataNasc = (aluno.getDataNascimento() != null)
                    ? aluno.getDataNascimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "____/____/______";
            String serie = (aluno.getSerie() != null) ? aluno.getSerie().getNome() : "________";
            String turma = (aluno.getTurma() != null) ? aluno.getTurma().getNome() : "________";
            String matricula = (aluno.getMatricula() != null) ? aluno.getMatricula() : "________";
            String anoLetivo = (aluno.getAnoLetivo() != null)
                    ? String.valueOf(aluno.getAnoLetivo())
                    : String.valueOf(LocalDate.now().getYear());

            Paragraph paragrafo1 = new Paragraph();
            paragrafo1.setAlignment(Element.ALIGN_JUSTIFIED);
            paragrafo1.setLeading(22f);
            paragrafo1.add(new Phrase("Declaramos, para os devidos fins, que o(a) aluno(a) ", corpo));
            paragrafo1.add(new Phrase(nomeAluno, negrito));
            paragrafo1.add(new Phrase(", nascido(a) em ", corpo));
            paragrafo1.add(new Phrase(dataNasc, negrito));
            paragrafo1.add(new Phrase(", encontra-se regularmente matriculado(a) nesta instituição de ensino no ano letivo de ", corpo));
            paragrafo1.add(new Phrase(anoLetivo, negrito));
            paragrafo1.add(new Phrase(", cursando a ", corpo));
            paragrafo1.add(new Phrase(serie, negrito));
            paragrafo1.add(new Phrase(" — turma ", corpo));
            paragrafo1.add(new Phrase(turma, negrito));
            paragrafo1.add(new Phrase(", sob o número de matrícula ", corpo));
            paragrafo1.add(new Phrase(matricula, negrito));
            paragrafo1.add(new Phrase(".", corpo));
            paragrafo1.setSpacingAfter(20f);
            document.add(paragrafo1);

            if (aluno.getResponsavelFinanceiro() != null
                    && aluno.getResponsavelFinanceiro().getNome() != null) {
                Paragraph paragrafoResp = new Paragraph();
                paragrafoResp.setAlignment(Element.ALIGN_JUSTIFIED);
                paragrafoResp.setLeading(22f);
                paragrafoResp.add(new Phrase("Responsável financeiro: ", corpo));
                paragrafoResp.add(new Phrase(aluno.getResponsavelFinanceiro().getNome(), negrito));
                if (aluno.getResponsavelFinanceiro().getCpf() != null) {
                    paragrafoResp.add(new Phrase(" — CPF: ", corpo));
                    paragrafoResp.add(new Phrase(aluno.getResponsavelFinanceiro().getCpf(), negrito));
                }
                paragrafoResp.add(new Phrase(".", corpo));
                paragrafoResp.setSpacingAfter(20f);
                document.add(paragrafoResp);
            }

            Paragraph paragrafoFinal = new Paragraph(
                    "Por ser verdade, firmo a presente declaração.", corpo);
            paragrafoFinal.setAlignment(Element.ALIGN_JUSTIFIED);
            paragrafoFinal.setSpacingAfter(50f);
            document.add(paragrafoFinal);

            String cidade = (escola != null && escola.getCidade() != null)
                    ? escola.getCidade() : "___________________";
            String dataAtual = LocalDate.now().format(DateTimeFormatter.ofPattern(
                    "dd 'de' MMMM 'de' yyyy", new java.util.Locale("pt", "BR")));

            Paragraph localData = new Paragraph(cidade + ", " + dataAtual + ".", italico);
            localData.setAlignment(Element.ALIGN_RIGHT);
            localData.setSpacingAfter(60f);
            document.add(localData);

            String nomeDiretor = (escola != null && escola.getDiretor() != null
                    && !escola.getDiretor().isEmpty())
                    ? escola.getDiretor() : "___________________________";

            Paragraph assinatura = new Paragraph(
                    "___________________________________________\n" + nomeDiretor + "\nDireção", corpo);
            assinatura.setAlignment(Element.ALIGN_CENTER);
            document.add(assinatura);

            Paragraph rodape = new Paragraph(
                    "\n\nDocumento emitido em " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " pelo Sistema Escolar", smallFont);
            rodape.setAlignment(Element.ALIGN_CENTER);
            rodape.setSpacingBefore(40f);
            document.add(rodape);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // ==================================================================
    // HISTÓRICO ESCOLAR
    // ==================================================================
    public ByteArrayInputStream gerarHistoricoEscolarPdf(Long historicoId) {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font escolaNome = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(30, 64, 175));
            Font escolaInfo = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(108, 117, 125));
            Font titulo = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(44, 62, 80));
            Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(33, 37, 41));
            Font cellBold = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(33, 37, 41));
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(108, 117, 125));

            HistoricoEscolar h = historicoRepository.findById(historicoId).orElse(null);

            if (h == null) {
                Paragraph erro = new Paragraph("Histórico não encontrado.", cellFont);
                erro.setAlignment(Element.ALIGN_CENTER);
                document.add(erro);
                document.close();
                return new ByteArrayInputStream(out.toByteArray());
            }

            List<Escola> escolas = escolaRepository.findAll();
            Escola escola = escolas.isEmpty() ? null : escolas.get(0);

            if (escola != null) {
                Paragraph nomeEsc = new Paragraph(escola.getNome().toUpperCase(), escolaNome);
                nomeEsc.setAlignment(Element.ALIGN_CENTER);
                document.add(nomeEsc);

                StringBuilder infoEscola = new StringBuilder();
                if (escola.getCnpj() != null && !escola.getCnpj().isEmpty()) {
                    infoEscola.append("CNPJ: ").append(escola.getCnpj());
                }
                if (escola.getEnderecoCompleto() != null && !escola.getEnderecoCompleto().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append(escola.getEnderecoCompleto());
                }
                if (escola.getTelefone() != null && !escola.getTelefone().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append("Tel: ").append(escola.getTelefone());
                }
                if (escola.getEmail() != null && !escola.getEmail().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append(" | ");
                    infoEscola.append(escola.getEmail());
                }

                Paragraph info = new Paragraph(infoEscola.toString(), escolaInfo);
                info.setAlignment(Element.ALIGN_CENTER);
                info.setSpacingAfter(8f);
                document.add(info);
            } else {
                Paragraph nomePadrao = new Paragraph("SISTEMA ESCOLAR", escolaNome);
                nomePadrao.setAlignment(Element.ALIGN_CENTER);
                nomePadrao.setSpacingAfter(8f);
                document.add(nomePadrao);
            }

            Paragraph linha = new Paragraph("_______________________________________________________________________________", escolaInfo);
            linha.setAlignment(Element.ALIGN_CENTER);
            linha.setSpacingAfter(16f);
            document.add(linha);

            Paragraph tituloRel = new Paragraph("HISTÓRICO ESCOLAR", titulo);
            tituloRel.setAlignment(Element.ALIGN_CENTER);
            tituloRel.setSpacingAfter(12f);
            document.add(tituloRel);

            Aluno aluno = h.getAluno();

            Paragraph dadosAluno = new Paragraph();
            dadosAluno.setAlignment(Element.ALIGN_LEFT);
            dadosAluno.setLeading(18f);
            dadosAluno.setSpacingAfter(12f);
            dadosAluno.add(new Phrase("Aluno(a): ", cellBold));
            dadosAluno.add(new Phrase((aluno != null ? aluno.getNome() : "-") + "\n", cellFont));

            if (aluno != null && aluno.getDataNascimento() != null) {
                dadosAluno.add(new Phrase("Data de Nascimento: ", cellBold));
                dadosAluno.add(new Phrase(aluno.getDataNascimento()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "\n", cellFont));
            }

            if (aluno != null && aluno.getMatricula() != null) {
                dadosAluno.add(new Phrase("Matrícula: ", cellBold));
                dadosAluno.add(new Phrase(aluno.getMatricula() + "\n", cellFont));
            }

            if (aluno != null && aluno.getResponsavelFinanceiro() != null
                    && aluno.getResponsavelFinanceiro().getNome() != null) {
                dadosAluno.add(new Phrase("Responsável: ", cellBold));
                dadosAluno.add(new Phrase(aluno.getResponsavelFinanceiro().getNome() + "\n", cellFont));
            }

            dadosAluno.add(new Phrase("Ano Letivo: ", cellBold));
            dadosAluno.add(new Phrase(h.getAnoLetivo() + "    ", cellFont));
            dadosAluno.add(new Phrase("Série: ", cellBold));
            dadosAluno.add(new Phrase(h.getSerie() + "    ", cellFont));
            dadosAluno.add(new Phrase("Turma: ", cellBold));
            dadosAluno.add(new Phrase(h.getTurma() + "    ", cellFont));
            dadosAluno.add(new Phrase("Turno: ", cellBold));
            dadosAluno.add(new Phrase((h.getTurno() != null ? h.getTurno() : "-"), cellFont));

            document.add(dadosAluno);

            PdfPTable resumo = new PdfPTable(3);
            resumo.setWidthPercentage(100);
            resumo.setWidths(new float[]{1, 1, 1});
            resumo.setSpacingAfter(14f);

            adicionarCelulaResumo(resumo, "DISCIPLINAS",
                    String.valueOf(h.getTotalDisciplinas()), new Color(74, 111, 165));
            adicionarCelulaResumo(resumo, "APROVADAS",
                    String.valueOf(h.getTotalAprovadas()), new Color(25, 135, 84));

            String situacao = h.getSituacaoFinal() != null ? h.getSituacaoFinal() : "-";
            Color corSituacao = new Color(108, 117, 125);
            if ("APROVADO".equalsIgnoreCase(situacao)) corSituacao = new Color(25, 135, 84);
            else if ("REPROVADO".equalsIgnoreCase(situacao)) corSituacao = new Color(220, 53, 69);
            else if ("CURSANDO".equalsIgnoreCase(situacao)) corSituacao = new Color(243, 156, 18);

            adicionarCelulaResumo(resumo, "SITUAÇÃO FINAL", situacao, corSituacao);

            document.add(resumo);

            PdfPTable tabela = new PdfPTable(5);
            tabela.setWidthPercentage(100);
            tabela.setWidths(new float[]{4f, 1.2f, 1.0f, 1.0f, 1.3f});
            tabela.setSpacingAfter(16f);

            Color headerColor = new Color(30, 64, 175);
            adicionarCelulaHeaderCompacto(tabela, "Disciplina", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "CH (h)", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Nota", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Faltas", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Resultado", headerFont, headerColor);

            if (h.getItems() == null || h.getItems().isEmpty()) {
                PdfPCell vazio = new PdfPCell(new Phrase("Nenhuma disciplina registrada.", cellFont));
                vazio.setColspan(5);
                vazio.setPadding(16);
                vazio.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabela.addCell(vazio);
            } else {
                for (ItemHistorico item : h.getItems()) {
                    PdfPCell c1 = new PdfPCell(new Phrase(
                            item.getNomeMateria() != null ? item.getNomeMateria() : "-", cellFont));
                    c1.setPadding(5);
                    tabela.addCell(c1);

                    PdfPCell c2 = new PdfPCell(new Phrase(
                            item.getCargaHoraria() != null ? String.valueOf(item.getCargaHoraria()) : "-", cellFont));
                    c2.setPadding(5);
                    c2.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c2);

                    String nota = item.getNotaFinal() != null
                            ? String.format(Locale.US, "%.2f", item.getNotaFinal()) : "-";
                    PdfPCell c3 = new PdfPCell(new Phrase(nota, cellBold));
                    c3.setPadding(5);
                    c3.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c3);

                    PdfPCell c4 = new PdfPCell(new Phrase(
                            item.getFaltas() != null ? String.valueOf(item.getFaltas()) : "0", cellFont));
                    c4.setPadding(5);
                    c4.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c4);

                    String resultado = item.getResultado() != null ? item.getResultado() : "-";
                    Color corRes = "APROVADO".equalsIgnoreCase(resultado)
                            ? new Color(25, 135, 84)
                            : ("REPROVADO".equalsIgnoreCase(resultado) ? new Color(220, 53, 69) : new Color(108, 117, 125));
                    PdfPCell c5 = new PdfPCell(new Phrase(resultado,
                            new Font(Font.HELVETICA, 9, Font.BOLD, corRes)));
                    c5.setPadding(5);
                    c5.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c5);
                }
            }

            document.add(tabela);

            Paragraph totais = new Paragraph();
            totais.setAlignment(Element.ALIGN_RIGHT);
            totais.setSpacingAfter(12f);
            totais.add(new Phrase("Carga Horária Total: ", cellBold));
            totais.add(new Phrase((h.getCargaHorariaTotal() != null ? h.getCargaHorariaTotal() + "h" : "-") + "    ", cellFont));
            totais.add(new Phrase("Dias Letivos: ", cellBold));
            totais.add(new Phrase((h.getDiasLetivos() != null ? String.valueOf(h.getDiasLetivos()) : "-"), cellFont));
            document.add(totais);

            if (h.getObservacoes() != null && !h.getObservacoes().isEmpty()) {
                Paragraph obs = new Paragraph();
                obs.setAlignment(Element.ALIGN_LEFT);
                obs.setSpacingAfter(20f);
                obs.add(new Phrase("Observações: ", cellBold));
                obs.add(new Phrase(h.getObservacoes(), cellFont));
                document.add(obs);
            }

            Paragraph assinatura = new Paragraph(
                    "\n\n___________________________________________\n" +
                    "Assinatura da Direção / Coordenação", smallFont);
            assinatura.setAlignment(Element.ALIGN_CENTER);
            assinatura.setSpacingBefore(50f);
            document.add(assinatura);

            Paragraph dataEmissao = new Paragraph(
                    "Emitido em: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    smallFont);
            dataEmissao.setAlignment(Element.ALIGN_CENTER);
            dataEmissao.setSpacingBefore(20f);
            document.add(dataEmissao);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // ==================================================================
    // METODOS AUXILIARES
    // ==================================================================
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

    private void adicionarCelulaHeaderCompacto(PdfPTable tabela, String texto, Font fonte, Color cor) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, fonte));
        cell.setBackgroundColor(cor);
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setNoWrap(false);
        tabela.addCell(cell);
    }

    private void adicionarCelulaResumo(PdfPTable tabela, String label, String valor, Color cor) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(12);
        cell.setBorderWidth(1f);
        cell.setBorderColor(new Color(226, 232, 240));
        cell.setBackgroundColor(new Color(248, 250, 252));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph valorP = new Paragraph(valor, new Font(Font.HELVETICA, 16, Font.BOLD, cor));
        valorP.setAlignment(Element.ALIGN_CENTER);

        Paragraph labelP = new Paragraph(label, new Font(Font.HELVETICA, 8, Font.BOLD, new Color(108, 117, 125)));
        labelP.setAlignment(Element.ALIGN_CENTER);
        labelP.setSpacingBefore(4);

        cell.addElement(valorP);
        cell.addElement(labelP);
        tabela.addCell(cell);
    }
}