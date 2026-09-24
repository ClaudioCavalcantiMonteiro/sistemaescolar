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
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.PdfContentByte;
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
import java.util.ArrayList;
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
    private MensalidadeService mensalidadeService;

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
    // HISTÓRICO ESCOLAR (1 ANO) — 2 PÁGINAS (Frente + Verso detalhado)
    // ==================================================================
    public ByteArrayInputStream gerarHistoricoEscolarPdf(Long historicoId) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font escolaNome = new Font(Font.HELVETICA, 15, Font.BOLD, new Color(30, 64, 175));
            Font escolaInfo = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(108, 117, 125));
            Font titulo = new Font(Font.HELVETICA, 13, Font.BOLD, new Color(44, 62, 80));
            Font sectionTitle = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Font headerFont = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(33, 37, 41));
            Font cellBold = new Font(Font.HELVETICA, 8, Font.BOLD, new Color(33, 37, 41));
            Font cellSmall = new Font(Font.HELVETICA, 7, Font.NORMAL, new Color(33, 37, 41));
            Font smallFont = new Font(Font.HELVETICA, 7, Font.NORMAL, new Color(108, 117, 125));

            HistoricoEscolar h = historicoRepository.findById(historicoId).orElse(null);
            if (h == null) {
                Paragraph erro = new Paragraph("Histórico não encontrado.", cellFont);
                erro.setAlignment(Element.ALIGN_CENTER);
                document.add(erro);
                document.close();
                return new ByteArrayInputStream(out.toByteArray());
            }

            Aluno aluno = h.getAluno();
            List<Escola> escolas = escolaRepository.findAll();
            Escola escola = escolas.isEmpty() ? null : escolas.get(0);

            // ===== PÁGINA 1 — FRENTE =====
            if (escola != null) {
                Paragraph nomeEsc = new Paragraph(escola.getNome().toUpperCase(), escolaNome);
                nomeEsc.setAlignment(Element.ALIGN_CENTER);
                document.add(nomeEsc);

                StringBuilder infoEscola = new StringBuilder();
                if (escola.getCnpj() != null && !escola.getCnpj().isEmpty()) {
                    infoEscola.append("CNPJ: ").append(escola.getCnpj());
                }
                if (escola.getEnderecoCompleto() != null && !escola.getEnderecoCompleto().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append("  |  ");
                    infoEscola.append(escola.getEnderecoCompleto());
                }
                if (escola.getTelefone() != null && !escola.getTelefone().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append("  |  ");
                    infoEscola.append("Tel: ").append(escola.getTelefone());
                }
                if (escola.getEmail() != null && !escola.getEmail().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append("  |  ");
                    infoEscola.append(escola.getEmail());
                }

                Paragraph info = new Paragraph(infoEscola.toString(), escolaInfo);
                info.setAlignment(Element.ALIGN_CENTER);
                info.setSpacingAfter(6f);
                document.add(info);
            } else {
                Paragraph nomePadrao = new Paragraph("SISTEMA ESCOLAR", escolaNome);
                nomePadrao.setAlignment(Element.ALIGN_CENTER);
                nomePadrao.setSpacingAfter(6f);
                document.add(nomePadrao);
            }

            Paragraph linhaSep = new Paragraph("_____________________________________________________________________________", escolaInfo);
            linhaSep.setAlignment(Element.ALIGN_CENTER);
            linhaSep.setSpacingAfter(12f);
            document.add(linhaSep);

            Paragraph tituloRel = new Paragraph("HISTÓRICO ESCOLAR", titulo);
            tituloRel.setAlignment(Element.ALIGN_CENTER);
            tituloRel.setSpacingAfter(16f);
            document.add(tituloRel);

            PdfPTable titSec1 = new PdfPTable(1);
            titSec1.setWidthPercentage(100);
            titSec1.setSpacingAfter(0f);
            PdfPCell cT1 = new PdfPCell(new Phrase("1. IDENTIFICAÇÃO DO ALUNO(A)", sectionTitle));
            cT1.setBackgroundColor(new Color(30, 64, 175));
            cT1.setPadding(6);
            cT1.setBorder(Rectangle.NO_BORDER);
            titSec1.addCell(cT1);
            document.add(titSec1);

            PdfPTable tabAluno = new PdfPTable(4);
            tabAluno.setWidthPercentage(100);
            tabAluno.setWidths(new float[]{1.3f, 3.7f, 1.3f, 2.5f});
            tabAluno.setSpacingAfter(12f);

            adicionarCampoFicha(tabAluno, "Nome:", aluno != null ? aluno.getNome() : "-");
            adicionarCampoFicha(tabAluno, "Nascimento:",
                    aluno != null && aluno.getDataNascimento() != null
                            ? aluno.getDataNascimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            : "-");
            adicionarCampoFicha(tabAluno, "Matrícula:",
                    aluno != null && aluno.getMatricula() != null ? aluno.getMatricula() : "-");
            adicionarCampoFicha(tabAluno, "Naturalidade:",
                    aluno != null && aluno.getNaturalidade() != null && !aluno.getNaturalidade().isEmpty()
                            ? aluno.getNaturalidade() : "-");
            adicionarCampoFicha(tabAluno, "Nacionalidade:",
                    aluno != null && aluno.getNacionalidade() != null && !aluno.getNacionalidade().isEmpty()
                            ? aluno.getNacionalidade() : "-");
            adicionarCampoFicha(tabAluno, "Sexo:",
                    aluno != null && aluno.getSexo() != null && !aluno.getSexo().isEmpty()
                            ? aluno.getSexo() : "-");
            adicionarCampoFicha(tabAluno, "Nome do Pai:",
                    aluno != null && aluno.getNomePai() != null && !aluno.getNomePai().isEmpty()
                            ? aluno.getNomePai() : "-");
            adicionarCampoFicha(tabAluno, "Nome da Mãe:",
                    aluno != null && aluno.getNomeMae() != null && !aluno.getNomeMae().isEmpty()
                            ? aluno.getNomeMae() : "-");
            adicionarCampoFicha(tabAluno, "CPF do Aluno:",
                    aluno != null && aluno.getCpfAluno() != null && !aluno.getCpfAluno().isEmpty()
                            ? aluno.getCpfAluno() : "-");
            adicionarCampoFicha(tabAluno, "RG do Aluno:",
                    aluno != null && aluno.getRgAluno() != null && !aluno.getRgAluno().isEmpty()
                            ? aluno.getRgAluno() : "-");
            adicionarCampoFicha(tabAluno, "Responsável:",
                    aluno != null && aluno.getResponsavelFinanceiro() != null
                            && aluno.getResponsavelFinanceiro().getNome() != null
                            ? aluno.getResponsavelFinanceiro().getNome() : "-");
            adicionarCampoFicha(tabAluno, "Telefone:",
                    aluno != null && aluno.getTelefone() != null && !aluno.getTelefone().isEmpty()
                            ? aluno.getTelefone() : "-");

            document.add(tabAluno);

            PdfPTable titSec2 = new PdfPTable(1);
            titSec2.setWidthPercentage(100);
            titSec2.setSpacingAfter(0f);
            PdfPCell cT2 = new PdfPCell(new Phrase("2. DADOS DO CURSO", sectionTitle));
            cT2.setBackgroundColor(new Color(30, 64, 175));
            cT2.setPadding(6);
            cT2.setBorder(Rectangle.NO_BORDER);
            titSec2.addCell(cT2);
            document.add(titSec2);

            PdfPTable tabCurso = new PdfPTable(4);
            tabCurso.setWidthPercentage(100);
            tabCurso.setWidths(new float[]{1.3f, 2f, 1.3f, 2f});
            tabCurso.setSpacingAfter(12f);

            adicionarCampoFicha(tabCurso, "Ano Letivo:",
                    h.getAnoLetivo() != null ? String.valueOf(h.getAnoLetivo()) : "-");
            adicionarCampoFicha(tabCurso, "Série:", h.getSerie() != null ? h.getSerie() : "-");
            adicionarCampoFicha(tabCurso, "Turma:", h.getTurma() != null ? h.getTurma() : "-");
            adicionarCampoFicha(tabCurso, "Turno:", h.getTurno() != null ? h.getTurno() : "-");
            adicionarCampoFicha(tabCurso, "Carga Horária:",
                    h.getCargaHorariaTotal() != null ? h.getCargaHorariaTotal() + "h" : "-");
            adicionarCampoFicha(tabCurso, "Dias Letivos:",
                    h.getDiasLetivos() != null ? String.valueOf(h.getDiasLetivos()) : "-");
            adicionarCampoFicha(tabCurso, "Média Aprov.:",
                    h.getMediaAprovacao() != null ? String.format(Locale.US, "%.1f", h.getMediaAprovacao()) : "-");

            String situacao = h.getSituacaoFinal() != null ? h.getSituacaoFinal() : "-";
            String situacaoExibir = situacao;
            Color corSit = new Color(108, 117, 125);

            if ("APROVADO".equalsIgnoreCase(situacao)) {
                corSit = new Color(25, 135, 84);
                situacaoExibir = "APROVADO";
            } else if ("APROVADO_COM_DEPENDENCIA".equalsIgnoreCase(situacao)) {
                corSit = new Color(243, 156, 18);
                situacaoExibir = "APROV. C/ DEPEND.";
            } else if ("REPROVADO".equalsIgnoreCase(situacao)) {
                corSit = new Color(220, 53, 69);
                situacaoExibir = "REPROVADO";
            } else if ("CURSANDO".equalsIgnoreCase(situacao)) {
                corSit = new Color(243, 156, 18);
                situacaoExibir = "CURSANDO";
            }

            PdfPCell cellLabelSit = new PdfPCell(new Phrase("Situação Final:", new Font(Font.HELVETICA, 7, Font.BOLD, new Color(30, 64, 175))));
            cellLabelSit.setBackgroundColor(new Color(240, 244, 248));
            cellLabelSit.setPadding(5);
            tabCurso.addCell(cellLabelSit);

            PdfPCell cellValorSit = new PdfPCell(new Phrase(situacaoExibir, new Font(Font.HELVETICA, 8, Font.BOLD, corSit)));
            cellValorSit.setPadding(5);
            tabCurso.addCell(cellValorSit);

            document.add(tabCurso);

            if (h.isExterno()) {
                PdfPTable tabOrigem = new PdfPTable(1);
                tabOrigem.setWidthPercentage(100);
                tabOrigem.setSpacingAfter(10f);
                PdfPCell cell = new PdfPCell();
                cell.setBackgroundColor(new Color(224, 242, 254));
                cell.setBorderColor(new Color(2, 132, 199));
                cell.setPadding(8);
                Paragraph p = new Paragraph();
                p.add(new Phrase("ESCOLA DE ORIGEM (TRANSFERÊNCIA)\n",
                        new Font(Font.HELVETICA, 8, Font.BOLD, new Color(7, 89, 133))));
                p.add(new Phrase(h.getEscolaOrigem(),
                        new Font(Font.HELVETICA, 10, Font.BOLD, new Color(7, 89, 133))));
                if (h.getCidadeOrigem() != null && !h.getCidadeOrigem().isEmpty()) {
                    p.add(new Phrase("\n" + h.getCidadeOrigem(),
                            new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(7, 89, 133))));
                }
                cell.addElement(p);
                tabOrigem.addCell(cell);
                document.add(tabOrigem);
            }

            if ("APROVADO_COM_DEPENDENCIA".equalsIgnoreCase(situacao)) {
                String deps = h.getResumoDependencia();
                if (deps != null && !deps.isEmpty()) {
                    PdfPTable tabDep = new PdfPTable(1);
                    tabDep.setWidthPercentage(100);
                    tabDep.setSpacingAfter(10f);
                    PdfPCell cell = new PdfPCell();
                    cell.setBackgroundColor(new Color(255, 243, 205));
                    cell.setBorderColor(new Color(255, 193, 7));
                    cell.setPadding(8);
                    Paragraph p = new Paragraph();
                    p.add(new Phrase("APROVADO COM DEPENDÊNCIA EM: ",
                            new Font(Font.HELVETICA, 8, Font.BOLD, new Color(133, 100, 4))));
                    p.add(new Phrase(deps,
                            new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(133, 100, 4))));
                    p.add(new Phrase("\nO aluno deverá cursar a(s) disciplina(s) em regime de dependência.",
                            new Font(Font.HELVETICA, 7, Font.ITALIC, new Color(133, 100, 4))));
                    cell.addElement(p);
                    tabDep.addCell(cell);
                    document.add(tabDep);
                }
            }

            if (h.getObservacoes() != null && !h.getObservacoes().isEmpty()) {
                Paragraph obs = new Paragraph();
                obs.setSpacingAfter(12f);
                obs.add(new Phrase("Observações: ", cellBold));
                obs.add(new Phrase(h.getObservacoes(), cellFont));
                document.add(obs);
            }

            String cidade = (escola != null && escola.getCidade() != null) ? escola.getCidade() : "___________________";
            String dataAtual = LocalDate.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",
                    new java.util.Locale("pt", "BR")));

            Paragraph localData = new Paragraph(cidade + ", " + dataAtual + ".", cellFont);
            localData.setAlignment(Element.ALIGN_RIGHT);
            localData.setSpacingBefore(24f);
            localData.setSpacingAfter(50f);
            document.add(localData);

            PdfPTable tabAss = new PdfPTable(2);
            tabAss.setWidthPercentage(85);
            tabAss.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabAss.setWidths(new float[]{1, 1});

            PdfPCell a1 = new PdfPCell();
            a1.setBorder(Rectangle.NO_BORDER);
            a1.setPaddingTop(20);
            Paragraph p1 = new Paragraph();
            p1.setAlignment(Element.ALIGN_CENTER);
            p1.add(new Phrase("___________________________________________\n", cellFont));
            p1.add(new Phrase("Direção\n", cellBold));
            p1.add(new Phrase("Assinatura e Carimbo", smallFont));
            a1.addElement(p1);
            tabAss.addCell(a1);

            PdfPCell a2 = new PdfPCell();
            a2.setBorder(Rectangle.NO_BORDER);
            a2.setPaddingTop(20);
            Paragraph p2 = new Paragraph();
            p2.setAlignment(Element.ALIGN_CENTER);
            p2.add(new Phrase("___________________________________________\n", cellFont));
            p2.add(new Phrase("Secretaria Escolar\n", cellBold));
            p2.add(new Phrase("Assinatura e Carimbo", smallFont));
            a2.addElement(p2);
            tabAss.addCell(a2);

            document.add(tabAss);

            // ===== PÁGINA 2 — VERSO (Boletim detalhado) =====
            document.newPage();

            if (escola != null) {
                Paragraph nomeEsc2 = new Paragraph(escola.getNome().toUpperCase(),
                        new Font(Font.HELVETICA, 12, Font.BOLD, new Color(30, 64, 175)));
                nomeEsc2.setAlignment(Element.ALIGN_CENTER);
                nomeEsc2.setSpacingAfter(2f);
                document.add(nomeEsc2);
            }

            Paragraph linhaSep2 = new Paragraph("_____________________________________________________________________________", escolaInfo);
            linhaSep2.setAlignment(Element.ALIGN_CENTER);
            linhaSep2.setSpacingAfter(10f);
            document.add(linhaSep2);

            Paragraph tituloBol = new Paragraph("BOLETIM DE NOTAS DETALHADO", titulo);
            tituloBol.setAlignment(Element.ALIGN_CENTER);
            tituloBol.setSpacingAfter(4f);
            document.add(tituloBol);

            Paragraph subAluno = new Paragraph();
            subAluno.setAlignment(Element.ALIGN_CENTER);
            subAluno.setSpacingAfter(12f);
            subAluno.add(new Phrase("Aluno: ", cellBold));
            subAluno.add(new Phrase((aluno != null ? aluno.getNome() : "-"), cellFont));
            subAluno.add(new Phrase("    |    Ano: ", cellBold));
            subAluno.add(new Phrase(String.valueOf(h.getAnoLetivo()), cellFont));
            subAluno.add(new Phrase("    |    Série: ", cellBold));
            subAluno.add(new Phrase(h.getSerie() != null ? h.getSerie() : "-", cellFont));
            subAluno.add(new Phrase("    |    Turma: ", cellBold));
            subAluno.add(new Phrase(h.getTurma() != null ? h.getTurma() : "-", cellFont));
            document.add(subAluno);

            List<Nota> todasNotas = new ArrayList<>();
            if (aluno != null && h.getAnoLetivo() != null) {
                try {
                    todasNotas = notaService.listarPorAlunoEAno(aluno.getId(), h.getAnoLetivo());
                } catch (Exception ignored) { }
            }

            Map<Long, Map<Integer, Nota>> notasPorMateriaUnidade = new HashMap<>();
            for (Nota n : todasNotas) {
                if (n.getMateria() == null) continue;
                notasPorMateriaUnidade
                        .computeIfAbsent(n.getMateria().getId(), k -> new HashMap<>())
                        .put(n.getUnidade(), n);
            }

            PdfPTable tabela = new PdfPTable(10);
            tabela.setWidthPercentage(100);
            tabela.setWidths(new float[]{3.6f, 0.7f, 0.75f, 0.75f, 0.75f, 0.75f, 0.75f, 0.9f, 0.7f, 1.15f});
            tabela.setSpacingAfter(12f);

            Color headerColor = new Color(30, 64, 175);

            adicionarCelulaHeaderCompacto(tabela, "Disciplina", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "CH", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "1ª Un.", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "2ª Un.", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "3ª Un.", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "4ª Un.", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Rec", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Média", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Faltas", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabela, "Resultado", headerFont, headerColor);

            if (h.getItems() == null || h.getItems().isEmpty()) {
                PdfPCell vazio = new PdfPCell(new Phrase("Nenhuma disciplina registrada.", cellFont));
                vazio.setColspan(10);
                vazio.setPadding(16);
                vazio.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabela.addCell(vazio);
            } else {
                for (ItemHistorico item : h.getItems()) {
                    PdfPCell c1 = new PdfPCell(new Phrase(
                            item.getNomeMateria() != null ? item.getNomeMateria() : "-", cellFont));
                    c1.setPadding(4);
                    tabela.addCell(c1);

                    PdfPCell c2 = new PdfPCell(new Phrase(
                            item.getCargaHoraria() != null ? String.valueOf(item.getCargaHoraria()) : "-", cellSmall));
                    c2.setPadding(4);
                    c2.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(c2);

                    Map<Integer, Nota> mapaUnid = (item.getMateria() != null)
                            ? notasPorMateriaUnidade.get(item.getMateria().getId())
                            : null;

                    for (int u = 1; u <= 4; u++) {
                        Nota notaU = mapaUnid != null ? mapaUnid.get(u) : null;
                        String notaStr = "-";
                        if (notaU != null && notaU.getMediaFinal() != null) {
                            notaStr = String.format(Locale.US, "%.1f", notaU.getMediaFinal());
                        }
                        PdfPCell nCell = new PdfPCell(new Phrase(notaStr, cellSmall));
                        nCell.setPadding(4);
                        nCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        tabela.addCell(nCell);
                    }

                    String recStr = "-";
                    if (mapaUnid != null) {
                        for (int u = 1; u <= 4; u++) {
                            Nota notaU = mapaUnid.get(u);
                            if (notaU != null && notaU.getRecuperacao() != null) {
                                recStr = String.format(Locale.US, "%.1f", notaU.getRecuperacao());
                            }
                        }
                    }
                    PdfPCell cRec = new PdfPCell(new Phrase(recStr, cellSmall));
                    cRec.setPadding(4);
                    cRec.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(cRec);

                    String mediaStr = item.getNotaFinal() != null
                            ? String.format(Locale.US, "%.2f", item.getNotaFinal()) : "-";
                    PdfPCell cMedia = new PdfPCell(new Phrase(mediaStr, cellBold));
                    cMedia.setPadding(4);
                    cMedia.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(cMedia);

                    String faltasStr = item.getFaltas() != null ? String.valueOf(item.getFaltas()) : "0";
                    PdfPCell cFaltas = new PdfPCell(new Phrase(faltasStr, cellSmall));
                    cFaltas.setPadding(4);
                    cFaltas.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(cFaltas);

                    String resultado = item.getResultado() != null ? item.getResultado() : "-";
                    Color corRes = "APROVADO".equalsIgnoreCase(resultado)
                            ? new Color(25, 135, 84)
                            : ("REPROVADO".equalsIgnoreCase(resultado)
                                ? new Color(220, 53, 69)
                                : new Color(108, 117, 125));
                    PdfPCell cRes = new PdfPCell(new Phrase(resultado,
                            new Font(Font.HELVETICA, 7, Font.BOLD, corRes)));
                    cRes.setPadding(4);
                    cRes.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(cRes);
                }
            }

            document.add(tabela);

            Paragraph totais = new Paragraph();
            totais.setAlignment(Element.ALIGN_RIGHT);
            totais.setSpacingAfter(10f);
            totais.add(new Phrase("Carga Horária Total: ", cellBold));
            totais.add(new Phrase((h.getCargaHorariaTotal() != null ? h.getCargaHorariaTotal() + "h" : "-") + "     ", cellFont));
            totais.add(new Phrase("Dias Letivos: ", cellBold));
            totais.add(new Phrase((h.getDiasLetivos() != null ? String.valueOf(h.getDiasLetivos()) : "-"), cellFont));
            document.add(totais);

            Paragraph localData2 = new Paragraph(cidade + ", " + dataAtual + ".", cellFont);
            localData2.setAlignment(Element.ALIGN_RIGHT);
            localData2.setSpacingBefore(24f);
            localData2.setSpacingAfter(40f);
            document.add(localData2);

            Paragraph assinaturaVerso = new Paragraph();
            assinaturaVerso.setAlignment(Element.ALIGN_CENTER);
            assinaturaVerso.add(new Phrase("___________________________________________\n", cellFont));
            assinaturaVerso.add(new Phrase("Direção / Coordenação\n", cellBold));
            assinaturaVerso.add(new Phrase("Assinatura e Carimbo", smallFont));
            document.add(assinaturaVerso);

            Paragraph rodape = new Paragraph();
            rodape.setAlignment(Element.ALIGN_CENTER);
            rodape.setSpacingBefore(20f);
            rodape.add(new Phrase("Documento emitido em "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + " pelo Sistema Escolar", smallFont));
            document.add(rodape);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // ==================================================================
    // HISTÓRICO ESCOLAR COMPLETO — TODOS OS ANOS DO ALUNO
    // ==================================================================
    public ByteArrayInputStream gerarHistoricoCompletoPdf(Long alunoId) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font escolaNome = new Font(Font.HELVETICA, 15, Font.BOLD, new Color(30, 64, 175));
            Font escolaInfo = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(108, 117, 125));
            Font titulo = new Font(Font.HELVETICA, 13, Font.BOLD, new Color(44, 62, 80));
            Font sectionTitle = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            Font headerFont = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(33, 37, 41));
            Font cellBold = new Font(Font.HELVETICA, 8, Font.BOLD, new Color(33, 37, 41));
            Font cellSmall = new Font(Font.HELVETICA, 7, Font.NORMAL, new Color(33, 37, 41));
            Font smallFont = new Font(Font.HELVETICA, 7, Font.NORMAL, new Color(108, 117, 125));

            Aluno aluno = alunoService.buscarPorId(alunoId);
            if (aluno == null) {
                Paragraph erro = new Paragraph("Aluno não encontrado.", cellFont);
                erro.setAlignment(Element.ALIGN_CENTER);
                document.add(erro);
                document.close();
                return new ByteArrayInputStream(out.toByteArray());
            }

            List<HistoricoEscolar> historicos = historicoRepository.findByAlunoIdWithItems(alunoId);
            historicos.sort((a, b) -> {
                Integer anoA = a.getAnoLetivo() != null ? a.getAnoLetivo() : 0;
                Integer anoB = b.getAnoLetivo() != null ? b.getAnoLetivo() : 0;
                return anoB.compareTo(anoA);
            });

            List<Escola> escolas = escolaRepository.findAll();
            Escola escola = escolas.isEmpty() ? null : escolas.get(0);

            // ===== PÁGINA 1 — FRENTE =====
            if (escola != null) {
                Paragraph nomeEsc = new Paragraph(escola.getNome().toUpperCase(), escolaNome);
                nomeEsc.setAlignment(Element.ALIGN_CENTER);
                document.add(nomeEsc);

                StringBuilder infoEscola = new StringBuilder();
                if (escola.getCnpj() != null && !escola.getCnpj().isEmpty()) {
                    infoEscola.append("CNPJ: ").append(escola.getCnpj());
                }
                if (escola.getEnderecoCompleto() != null && !escola.getEnderecoCompleto().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append("  |  ");
                    infoEscola.append(escola.getEnderecoCompleto());
                }
                if (escola.getTelefone() != null && !escola.getTelefone().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append("  |  ");
                    infoEscola.append("Tel: ").append(escola.getTelefone());
                }
                if (escola.getEmail() != null && !escola.getEmail().isEmpty()) {
                    if (infoEscola.length() > 0) infoEscola.append("  |  ");
                    infoEscola.append(escola.getEmail());
                }

                Paragraph info = new Paragraph(infoEscola.toString(), escolaInfo);
                info.setAlignment(Element.ALIGN_CENTER);
                info.setSpacingAfter(6f);
                document.add(info);
            }

            Paragraph linhaSep = new Paragraph("_____________________________________________________________________________", escolaInfo);
            linhaSep.setAlignment(Element.ALIGN_CENTER);
            linhaSep.setSpacingAfter(12f);
            document.add(linhaSep);

            Paragraph tituloRel = new Paragraph("HISTÓRICO ESCOLAR", titulo);
            tituloRel.setAlignment(Element.ALIGN_CENTER);
            tituloRel.setSpacingAfter(4f);
            document.add(tituloRel);

            Paragraph subtitulo = new Paragraph("(Documento Completo — Todos os Anos Letivos)", escolaInfo);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(16f);
            document.add(subtitulo);

            PdfPTable titSec1 = new PdfPTable(1);
            titSec1.setWidthPercentage(100);
            titSec1.setSpacingAfter(0f);
            PdfPCell cT1 = new PdfPCell(new Phrase("1. IDENTIFICAÇÃO DO ALUNO(A)", sectionTitle));
            cT1.setBackgroundColor(new Color(30, 64, 175));
            cT1.setPadding(6);
            cT1.setBorder(Rectangle.NO_BORDER);
            titSec1.addCell(cT1);
            document.add(titSec1);

            PdfPTable tabAluno = new PdfPTable(4);
            tabAluno.setWidthPercentage(100);
            tabAluno.setWidths(new float[]{1.3f, 3.7f, 1.3f, 2.5f});
            tabAluno.setSpacingAfter(12f);

            adicionarCampoFicha(tabAluno, "Nome:", aluno.getNome());
            adicionarCampoFicha(tabAluno, "Nascimento:",
                    aluno.getDataNascimento() != null
                            ? aluno.getDataNascimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            : "-");
            adicionarCampoFicha(tabAluno, "Matrícula:",
                    aluno.getMatricula() != null ? aluno.getMatricula() : "-");
            adicionarCampoFicha(tabAluno, "Naturalidade:",
                    aluno.getNaturalidade() != null && !aluno.getNaturalidade().isEmpty()
                            ? aluno.getNaturalidade() : "-");
            adicionarCampoFicha(tabAluno, "Nacionalidade:",
                    aluno.getNacionalidade() != null && !aluno.getNacionalidade().isEmpty()
                            ? aluno.getNacionalidade() : "-");
            adicionarCampoFicha(tabAluno, "Sexo:",
                    aluno.getSexo() != null && !aluno.getSexo().isEmpty()
                            ? aluno.getSexo() : "-");
            adicionarCampoFicha(tabAluno, "Nome do Pai:",
                    aluno.getNomePai() != null && !aluno.getNomePai().isEmpty()
                            ? aluno.getNomePai() : "-");
            adicionarCampoFicha(tabAluno, "Nome da Mãe:",
                    aluno.getNomeMae() != null && !aluno.getNomeMae().isEmpty()
                            ? aluno.getNomeMae() : "-");
            adicionarCampoFicha(tabAluno, "CPF do Aluno:",
                    aluno.getCpfAluno() != null && !aluno.getCpfAluno().isEmpty()
                            ? aluno.getCpfAluno() : "-");
            adicionarCampoFicha(tabAluno, "RG do Aluno:",
                    aluno.getRgAluno() != null && !aluno.getRgAluno().isEmpty()
                            ? aluno.getRgAluno() : "-");
            adicionarCampoFicha(tabAluno, "Responsável:",
                    aluno.getResponsavelFinanceiro() != null
                            && aluno.getResponsavelFinanceiro().getNome() != null
                            ? aluno.getResponsavelFinanceiro().getNome() : "-");
            adicionarCampoFicha(tabAluno, "Telefone:",
                    aluno.getTelefone() != null && !aluno.getTelefone().isEmpty()
                            ? aluno.getTelefone() : "-");

            document.add(tabAluno);

            PdfPTable titSec2 = new PdfPTable(1);
            titSec2.setWidthPercentage(100);
            titSec2.setSpacingAfter(0f);
            PdfPCell cT2 = new PdfPCell(new Phrase("2. RESUMO DOS ANOS LETIVOS", sectionTitle));
            cT2.setBackgroundColor(new Color(30, 64, 175));
            cT2.setPadding(6);
            cT2.setBorder(Rectangle.NO_BORDER);
            titSec2.addCell(cT2);
            document.add(titSec2);

            PdfPTable tabResumo = new PdfPTable(6);
            tabResumo.setWidthPercentage(100);
            tabResumo.setWidths(new float[]{1f, 2f, 1.5f, 1.3f, 1.5f, 2f});
            tabResumo.setSpacingAfter(16f);

            Color headerColor = new Color(30, 64, 175);
            adicionarCelulaHeaderCompacto(tabResumo, "Ano", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabResumo, "Série", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabResumo, "Turma", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabResumo, "Disciplinas", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabResumo, "Carga Horária", headerFont, headerColor);
            adicionarCelulaHeaderCompacto(tabResumo, "Situação Final", headerFont, headerColor);

            if (historicos.isEmpty()) {
                PdfPCell vazio = new PdfPCell(new Phrase("Nenhum histórico escolar registrado.", cellFont));
                vazio.setColspan(6);
                vazio.setPadding(14);
                vazio.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabResumo.addCell(vazio);
            } else {
                for (HistoricoEscolar h : historicos) {
                    PdfPCell cAno = new PdfPCell(new Phrase(
                            h.getAnoLetivo() != null ? String.valueOf(h.getAnoLetivo()) : "-", cellBold));
                    cAno.setPadding(5);
                    cAno.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabResumo.addCell(cAno);

                    PdfPCell cSerie = new PdfPCell(new Phrase(
                            h.getSerie() != null ? h.getSerie() : "-", cellFont));
                    cSerie.setPadding(5);
                    tabResumo.addCell(cSerie);

                    PdfPCell cTurma = new PdfPCell(new Phrase(
                            h.getTurma() != null ? h.getTurma() : "-", cellFont));
                    cTurma.setPadding(5);
                    cTurma.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabResumo.addCell(cTurma);

                    PdfPCell cDisc = new PdfPCell(new Phrase(
                            String.valueOf(h.getTotalDisciplinas()), cellFont));
                    cDisc.setPadding(5);
                    cDisc.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabResumo.addCell(cDisc);

                    PdfPCell cCH = new PdfPCell(new Phrase(
                            h.getCargaHorariaTotal() != null ? h.getCargaHorariaTotal() + "h" : "-", cellFont));
                    cCH.setPadding(5);
                    cCH.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabResumo.addCell(cCH);

                    String sit = h.getSituacaoFinal() != null ? h.getSituacaoFinal() : "-";
                    String sitExibir = sit;
                    Color corSit = new Color(108, 117, 125);

                    if ("APROVADO".equalsIgnoreCase(sit)) {
                        corSit = new Color(25, 135, 84);
                        sitExibir = "Aprovado";
                    } else if ("APROVADO_COM_DEPENDENCIA".equalsIgnoreCase(sit)) {
                        corSit = new Color(243, 156, 18);
                        sitExibir = "Aprovado c/ Dep.";
                    } else if ("REPROVADO".equalsIgnoreCase(sit)) {
                        corSit = new Color(220, 53, 69);
                        sitExibir = "Reprovado";
                    } else if ("CURSANDO".equalsIgnoreCase(sit)) {
                        corSit = new Color(243, 156, 18);
                        sitExibir = "Cursando";
                    }

                    PdfPCell cSit = new PdfPCell(new Phrase(sitExibir,
                            new Font(Font.HELVETICA, 8, Font.BOLD, corSit)));
                    cSit.setPadding(5);
                    cSit.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabResumo.addCell(cSit);
                }
            }

            document.add(tabResumo);

            String cidade = (escola != null && escola.getCidade() != null) ? escola.getCidade() : "___________________";
            String dataAtual = LocalDate.now().format(DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy",
                    new java.util.Locale("pt", "BR")));

            Paragraph localData = new Paragraph(cidade + ", " + dataAtual + ".", cellFont);
            localData.setAlignment(Element.ALIGN_RIGHT);
            localData.setSpacingBefore(24f);
            localData.setSpacingAfter(50f);
            document.add(localData);

            PdfPTable tabAss = new PdfPTable(2);
            tabAss.setWidthPercentage(85);
            tabAss.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabAss.setWidths(new float[]{1, 1});

            PdfPCell a1 = new PdfPCell();
            a1.setBorder(Rectangle.NO_BORDER);
            a1.setPaddingTop(20);
            Paragraph p1 = new Paragraph();
            p1.setAlignment(Element.ALIGN_CENTER);
            p1.add(new Phrase("___________________________________________\n", cellFont));
            p1.add(new Phrase("Direção\n", cellBold));
            p1.add(new Phrase("Assinatura e Carimbo", smallFont));
            a1.addElement(p1);
            tabAss.addCell(a1);

            PdfPCell a2 = new PdfPCell();
            a2.setBorder(Rectangle.NO_BORDER);
            a2.setPaddingTop(20);
            Paragraph p2 = new Paragraph();
            p2.setAlignment(Element.ALIGN_CENTER);
            p2.add(new Phrase("___________________________________________\n", cellFont));
            p2.add(new Phrase("Secretaria Escolar\n", cellBold));
            p2.add(new Phrase("Assinatura e Carimbo", smallFont));
            a2.addElement(p2);
            tabAss.addCell(a2);

            document.add(tabAss);

            for (HistoricoEscolar h : historicos) {
                document.newPage();

                if (escola != null) {
                    Paragraph nomeEsc2 = new Paragraph(escola.getNome().toUpperCase(),
                            new Font(Font.HELVETICA, 12, Font.BOLD, new Color(30, 64, 175)));
                    nomeEsc2.setAlignment(Element.ALIGN_CENTER);
                    nomeEsc2.setSpacingAfter(2f);
                    document.add(nomeEsc2);
                }

                Paragraph linhaSep2 = new Paragraph("_____________________________________________________________________________", escolaInfo);
                linhaSep2.setAlignment(Element.ALIGN_CENTER);
                linhaSep2.setSpacingAfter(10f);
                document.add(linhaSep2);

                Paragraph tituloBol = new Paragraph("BOLETIM DE NOTAS — " + h.getAnoLetivo(), titulo);
                tituloBol.setAlignment(Element.ALIGN_CENTER);
                tituloBol.setSpacingAfter(4f);
                document.add(tituloBol);

                Paragraph subAluno = new Paragraph();
                subAluno.setAlignment(Element.ALIGN_CENTER);
                subAluno.setSpacingAfter(12f);
                subAluno.add(new Phrase("Aluno: ", cellBold));
                subAluno.add(new Phrase(aluno.getNome(), cellFont));
                subAluno.add(new Phrase("    |    Série: ", cellBold));
                subAluno.add(new Phrase(h.getSerie() != null ? h.getSerie() : "-", cellFont));
                subAluno.add(new Phrase("    |    Turma: ", cellBold));
                subAluno.add(new Phrase(h.getTurma() != null ? h.getTurma() : "-", cellFont));
                document.add(subAluno);

                List<Nota> todasNotas = new ArrayList<>();
                if (h.getAnoLetivo() != null) {
                    try {
                        todasNotas = notaService.listarPorAlunoEAno(alunoId, h.getAnoLetivo());
                    } catch (Exception ignored) { }
                }

                Map<Long, Map<Integer, Nota>> notasPorMateriaUnidade = new HashMap<>();
                for (Nota n : todasNotas) {
                    if (n.getMateria() == null) continue;
                    notasPorMateriaUnidade
                            .computeIfAbsent(n.getMateria().getId(), k -> new HashMap<>())
                            .put(n.getUnidade(), n);
                }

                PdfPTable tabela = new PdfPTable(10);
                tabela.setWidthPercentage(100);
                tabela.setWidths(new float[]{3.6f, 0.7f, 0.75f, 0.75f, 0.75f, 0.75f, 0.75f, 0.9f, 0.7f, 1.15f});
                tabela.setSpacingAfter(12f);

                adicionarCelulaHeaderCompacto(tabela, "Disciplina", headerFont, headerColor);
                adicionarCelulaHeaderCompacto(tabela, "CH", headerFont, headerColor);
                adicionarCelulaHeaderCompacto(tabela, "1ª Un.", headerFont, headerColor);
                adicionarCelulaHeaderCompacto(tabela, "2ª Un.", headerFont, headerColor);
                adicionarCelulaHeaderCompacto(tabela, "3ª Un.", headerFont, headerColor);
                adicionarCelulaHeaderCompacto(tabela, "4ª Un.", headerFont, headerColor);
                adicionarCelulaHeaderCompacto(tabela, "Rec", headerFont, headerColor);
                adicionarCelulaHeaderCompacto(tabela, "Média", headerFont, headerColor);
                adicionarCelulaHeaderCompacto(tabela, "Faltas", headerFont, headerColor);
                adicionarCelulaHeaderCompacto(tabela, "Resultado", headerFont, headerColor);

                if (h.getItems() == null || h.getItems().isEmpty()) {
                    PdfPCell vazio = new PdfPCell(new Phrase("Nenhuma disciplina registrada.", cellFont));
                    vazio.setColspan(10);
                    vazio.setPadding(16);
                    vazio.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(vazio);
                } else {
                    for (ItemHistorico item : h.getItems()) {
                        PdfPCell c1 = new PdfPCell(new Phrase(
                                item.getNomeMateria() != null ? item.getNomeMateria() : "-", cellFont));
                        c1.setPadding(4);
                        tabela.addCell(c1);

                        PdfPCell c2 = new PdfPCell(new Phrase(
                                item.getCargaHoraria() != null ? String.valueOf(item.getCargaHoraria()) : "-", cellSmall));
                        c2.setPadding(4);
                        c2.setHorizontalAlignment(Element.ALIGN_CENTER);
                        tabela.addCell(c2);

                        Map<Integer, Nota> mapaUnid = (item.getMateria() != null)
                                ? notasPorMateriaUnidade.get(item.getMateria().getId())
                                : null;

                        for (int u = 1; u <= 4; u++) {
                            Nota notaU = mapaUnid != null ? mapaUnid.get(u) : null;
                            String notaStr = "-";
                            if (notaU != null && notaU.getMediaFinal() != null) {
                                notaStr = String.format(Locale.US, "%.1f", notaU.getMediaFinal());
                            }
                            PdfPCell nCell = new PdfPCell(new Phrase(notaStr, cellSmall));
                            nCell.setPadding(4);
                            nCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                            tabela.addCell(nCell);
                        }

                        String recStr = "-";
                        if (mapaUnid != null) {
                            for (int u = 1; u <= 4; u++) {
                                Nota notaU = mapaUnid.get(u);
                                if (notaU != null && notaU.getRecuperacao() != null) {
                                    recStr = String.format(Locale.US, "%.1f", notaU.getRecuperacao());
                                }
                            }
                        }
                        PdfPCell cRec = new PdfPCell(new Phrase(recStr, cellSmall));
                        cRec.setPadding(4);
                        cRec.setHorizontalAlignment(Element.ALIGN_CENTER);
                        tabela.addCell(cRec);

                        String mediaStr = item.getNotaFinal() != null
                                ? String.format(Locale.US, "%.2f", item.getNotaFinal()) : "-";
                        PdfPCell cMedia = new PdfPCell(new Phrase(mediaStr, cellBold));
                        cMedia.setPadding(4);
                        cMedia.setHorizontalAlignment(Element.ALIGN_CENTER);
                        tabela.addCell(cMedia);

                        String faltasStr = item.getFaltas() != null ? String.valueOf(item.getFaltas()) : "0";
                        PdfPCell cFaltas = new PdfPCell(new Phrase(faltasStr, cellSmall));
                        cFaltas.setPadding(4);
                        cFaltas.setHorizontalAlignment(Element.ALIGN_CENTER);
                        tabela.addCell(cFaltas);

                        String resultado = item.getResultado() != null ? item.getResultado() : "-";
                        Color corRes = "APROVADO".equalsIgnoreCase(resultado)
                                ? new Color(25, 135, 84)
                                : ("REPROVADO".equalsIgnoreCase(resultado)
                                    ? new Color(220, 53, 69)
                                    : new Color(108, 117, 125));
                        PdfPCell cRes = new PdfPCell(new Phrase(resultado,
                                new Font(Font.HELVETICA, 7, Font.BOLD, corRes)));
                        cRes.setPadding(4);
                        cRes.setHorizontalAlignment(Element.ALIGN_CENTER);
                        tabela.addCell(cRes);
                    }
                }

                document.add(tabela);

                Paragraph totais = new Paragraph();
                totais.setAlignment(Element.ALIGN_RIGHT);
                totais.setSpacingAfter(10f);
                totais.add(new Phrase("Carga Horária Total: ", cellBold));
                totais.add(new Phrase((h.getCargaHorariaTotal() != null ? h.getCargaHorariaTotal() + "h" : "-") + "     ", cellFont));
                totais.add(new Phrase("Dias Letivos: ", cellBold));
                totais.add(new Phrase((h.getDiasLetivos() != null ? String.valueOf(h.getDiasLetivos()) : "-"), cellFont));
                document.add(totais);

                Paragraph localData2 = new Paragraph(cidade + ", " + dataAtual + ".", cellFont);
                localData2.setAlignment(Element.ALIGN_RIGHT);
                localData2.setSpacingBefore(24f);
                localData2.setSpacingAfter(40f);
                document.add(localData2);

                Paragraph assinaturaVerso = new Paragraph();
                assinaturaVerso.setAlignment(Element.ALIGN_CENTER);
                assinaturaVerso.add(new Phrase("___________________________________________\n", cellFont));
                assinaturaVerso.add(new Phrase("Direção / Coordenação\n", cellBold));
                assinaturaVerso.add(new Phrase("Assinatura e Carimbo", smallFont));
                document.add(assinaturaVerso);
            }

            Paragraph rodape = new Paragraph();
            rodape.setAlignment(Element.ALIGN_CENTER);
            rodape.setSpacingBefore(20f);
            rodape.add(new Phrase("Documento emitido em "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + " pelo Sistema Escolar", smallFont));
            document.add(rodape);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // ==================================================================
    // HISTÓRICO OFICIAL COMPACTO (Retrato A4) — Padrão Ficha 18/19
    // ==================================================================
    public ByteArrayInputStream gerarHistoricoOficialPdf(Long alunoId) {
        Document document = new Document(PageSize.A4, 20, 20, 20, 20);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font escolaNomeF = new Font(Font.HELVETICA, 13, Font.BOLD, new Color(30, 64, 175));
            Font escolaInfoF = new Font(Font.HELVETICA, 6, Font.NORMAL, new Color(108, 117, 125));
            Font tituloF = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(30, 64, 175));
            Font labelF = new Font(Font.HELVETICA, 5.5f, Font.BOLD, new Color(30, 64, 175));
            Font valorF = new Font(Font.HELVETICA, 6, Font.NORMAL, new Color(33, 37, 41));
            Font headerTblF = new Font(Font.HELVETICA, 5, Font.BOLD, Color.WHITE);
            Font cellTblF = new Font(Font.HELVETICA, 5.5f, Font.NORMAL, new Color(33, 37, 41));
            Font cellTblBold = new Font(Font.HELVETICA, 5.5f, Font.BOLD, new Color(33, 37, 41));
            Font smallF = new Font(Font.HELVETICA, 5.5f, Font.NORMAL, new Color(108, 117, 125));

            Aluno aluno = alunoService.buscarPorId(alunoId);
            if (aluno == null) {
                Paragraph erro = new Paragraph("Aluno não encontrado.", valorF);
                erro.setAlignment(Element.ALIGN_CENTER);
                document.add(erro);
                document.close();
                return new ByteArrayInputStream(out.toByteArray());
            }

            List<HistoricoEscolar> historicos = historicoRepository.findByAlunoIdWithItems(alunoId);
            historicos.sort((a, b) -> {
                Integer anoA = a.getAnoLetivo() != null ? a.getAnoLetivo() : 0;
                Integer anoB = b.getAnoLetivo() != null ? b.getAnoLetivo() : 0;
                return anoA.compareTo(anoB);
            });

            List<Escola> escolas = escolaRepository.findAll();
            Escola escola = escolas.isEmpty() ? null : escolas.get(0);

            if (escola != null) {
                Paragraph nomeEsc = new Paragraph(escola.getNome().toUpperCase(), escolaNomeF);
                nomeEsc.setAlignment(Element.ALIGN_CENTER);
                document.add(nomeEsc);

                StringBuilder info = new StringBuilder();
                if (escola.getEnderecoCompleto() != null && !escola.getEnderecoCompleto().isEmpty()) {
                    info.append(escola.getEnderecoCompleto());
                }
                if (escola.getTelefone() != null && !escola.getTelefone().isEmpty()) {
                    if (info.length() > 0) info.append("  |  ");
                    info.append("Tel: ").append(escola.getTelefone());
                }
                if (escola.getEmail() != null && !escola.getEmail().isEmpty()) {
                    if (info.length() > 0) info.append("  |  ");
                    info.append(escola.getEmail());
                }
                if (escola.getCnpj() != null && !escola.getCnpj().isEmpty()) {
                    if (info.length() > 0) info.append("  |  ");
                    info.append("CNPJ: ").append(escola.getCnpj());
                }

                Paragraph pInfo = new Paragraph(info.toString(), escolaInfoF);
                pInfo.setAlignment(Element.ALIGN_CENTER);
                pInfo.setSpacingAfter(3f);
                document.add(pInfo);
            } else {
                Paragraph nomePadrao = new Paragraph("SISTEMA ESCOLAR", escolaNomeF);
                nomePadrao.setAlignment(Element.ALIGN_CENTER);
                nomePadrao.setSpacingAfter(3f);
                document.add(nomePadrao);
            }

            Paragraph linhaSep = new Paragraph("_____________________________________________________________________", escolaInfoF);
            linhaSep.setAlignment(Element.ALIGN_CENTER);
            linhaSep.setSpacingAfter(6f);
            document.add(linhaSep);

            Paragraph titulo = new Paragraph("HISTÓRICO ESCOLAR", tituloF);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(6f);
            document.add(titulo);

            PdfPTable tabAluno = new PdfPTable(4);
            tabAluno.setWidthPercentage(100);
            tabAluno.setWidths(new float[]{1.1f, 3.9f, 1.2f, 2.4f});
            tabAluno.setSpacingAfter(6f);

            adicionarCampoFichaCompacto(tabAluno, "Nome:", aluno.getNome(), labelF, valorF);
            adicionarCampoFichaCompacto(tabAluno, "Nascimento:",
                    aluno.getDataNascimento() != null
                            ? aluno.getDataNascimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            : "-", labelF, valorF);
            adicionarCampoFichaCompacto(tabAluno, "Matrícula:",
                    aluno.getMatricula() != null ? aluno.getMatricula() : "-", labelF, valorF);
            adicionarCampoFichaCompacto(tabAluno, "Responsável:",
                    aluno.getResponsavelFinanceiro() != null
                            && aluno.getResponsavelFinanceiro().getNome() != null
                            ? aluno.getResponsavelFinanceiro().getNome() : "-",
                    labelF, valorF);

            document.add(tabAluno);

            List<String> disciplinas = new ArrayList<>();
            for (HistoricoEscolar h : historicos) {
                if (h.getItems() == null) continue;
                for (ItemHistorico item : h.getItems()) {
                    String nome = item.getNomeMateria();
                    if (nome != null && !nome.trim().isEmpty() && !disciplinas.contains(nome)) {
                        disciplinas.add(nome);
                    }
                }
            }

            int totalDisc = disciplinas.size();

            if (historicos.isEmpty() || totalDisc == 0) {
                Paragraph vazio = new Paragraph("Nenhum histórico escolar registrado.", valorF);
                vazio.setAlignment(Element.ALIGN_CENTER);
                vazio.setSpacingBefore(20f);
                document.add(vazio);
            } else {
                int totalColunas = 3 + totalDisc;

                PdfPTable tabela = new PdfPTable(totalColunas);
                tabela.setWidthPercentage(100);

                float[] widths = new float[totalColunas];
                widths[0] = 0.55f;
                widths[1] = 1.0f;
                float larguraDisc = (10.0f - widths[0] - widths[1] - 1.3f) / totalDisc;
                for (int i = 0; i < totalDisc; i++) {
                    widths[2 + i] = larguraDisc;
                }
                widths[totalColunas - 1] = 1.3f;
                tabela.setWidths(widths);
                tabela.setSpacingAfter(8f);

                Color headerColor = new Color(30, 64, 175);

                adicionarCelulaHeaderCompacto(tabela, "Ano", headerTblF, headerColor);
                adicionarCelulaHeaderCompacto(tabela, "Série", headerTblF, headerColor);
                for (String disc : disciplinas) {
                    adicionarCelulaHeaderCompacto(tabela, disc, headerTblF, headerColor);
                }
                adicionarCelulaHeaderCompacto(tabela, "Situação", headerTblF, headerColor);

                for (HistoricoEscolar h : historicos) {
                    PdfPCell cAno = new PdfPCell(new Phrase(
                            h.getAnoLetivo() != null ? String.valueOf(h.getAnoLetivo()) : "-", cellTblBold));
                    cAno.setPadding(2);
                    cAno.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(cAno);

                    PdfPCell cSerie = new PdfPCell(new Phrase(
                            h.getSerie() != null ? h.getSerie() : "-", cellTblF));
                    cSerie.setPadding(2);
                    cSerie.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(cSerie);

                    for (String disc : disciplinas) {
                        String notaStr = "-";
                        if (h.getItems() != null) {
                            for (ItemHistorico item : h.getItems()) {
                                if (disc.equals(item.getNomeMateria()) && item.getNotaFinal() != null) {
                                    notaStr = String.format(Locale.US, "%.1f", item.getNotaFinal());
                                    break;
                                }
                            }
                        }
                        PdfPCell cNota = new PdfPCell(new Phrase(notaStr, cellTblF));
                        cNota.setPadding(2);
                        cNota.setHorizontalAlignment(Element.ALIGN_CENTER);
                        tabela.addCell(cNota);
                    }

                    String sit = h.getSituacaoFinal() != null ? h.getSituacaoFinal() : "-";
                    String sitExibir = sit;
                    Color corSit = new Color(108, 117, 125);

                    if ("APROVADO".equalsIgnoreCase(sit)) {
                        corSit = new Color(25, 135, 84);
                        sitExibir = "Aprov.";
                    } else if ("APROVADO_COM_DEPENDENCIA".equalsIgnoreCase(sit)) {
                        corSit = new Color(243, 156, 18);
                        sitExibir = "Aprov.c/Dep";
                    } else if ("REPROVADO".equalsIgnoreCase(sit)) {
                        corSit = new Color(220, 53, 69);
                        sitExibir = "Reprov.";
                    } else if ("CURSANDO".equalsIgnoreCase(sit)) {
                        corSit = new Color(243, 156, 18);
                        sitExibir = "Cursando";
                    } else if ("TRANSFERIDO".equalsIgnoreCase(sit)) {
                        corSit = new Color(108, 117, 125);
                        sitExibir = "Transf.";
                    } else if ("EVADIDO".equalsIgnoreCase(sit)) {
                        corSit = new Color(108, 117, 125);
                        sitExibir = "Evadido";
                    }

                    PdfPCell cSit = new PdfPCell(new Phrase(sitExibir,
                            new Font(Font.HELVETICA, 5.5f, Font.BOLD, corSit)));
                    cSit.setPadding(2);
                    cSit.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(cSit);
                }

                document.add(tabela);
            }

            StringBuilder obsConsolidada = new StringBuilder();
            for (HistoricoEscolar h : historicos) {
                if (h.getObservacoes() != null && !h.getObservacoes().isEmpty()) {
                    if (obsConsolidada.length() > 0) obsConsolidada.append("  |  ");
                    obsConsolidada.append("[").append(h.getAnoLetivo()).append("] ")
                            .append(h.getObservacoes());
                }
            }

            if (obsConsolidada.length() > 0) {
                Paragraph pObs = new Paragraph();
                pObs.setSpacingBefore(8f);
                pObs.setSpacingAfter(6f);
                pObs.add(new Phrase("Observações: ", cellTblBold));
                pObs.add(new Phrase(obsConsolidada.toString(), cellTblF));
                document.add(pObs);
            }

            String cidade = (escola != null && escola.getCidade() != null) ? escola.getCidade() : "___________________";
            String dataAtual = LocalDate.now().format(DateTimeFormatter.ofPattern(
                    "dd 'de' MMMM 'de' yyyy", new java.util.Locale("pt", "BR")));

            Paragraph localData = new Paragraph(cidade + ", " + dataAtual + ".", valorF);
            localData.setAlignment(Element.ALIGN_RIGHT);
            localData.setSpacingBefore(15f);
            localData.setSpacingAfter(25f);
            document.add(localData);

            PdfPTable tabAss = new PdfPTable(2);
            tabAss.setWidthPercentage(85);
            tabAss.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabAss.setWidths(new float[]{1, 1});

            PdfPCell a1 = new PdfPCell();
            a1.setBorder(Rectangle.NO_BORDER);
            a1.setPaddingTop(12);
            Paragraph p1 = new Paragraph();
            p1.setAlignment(Element.ALIGN_CENTER);
            p1.add(new Phrase("___________________________________________\n", cellTblF));
            p1.add(new Phrase("Direção\n", cellTblBold));
            p1.add(new Phrase("Assinatura e Carimbo", smallF));
            a1.addElement(p1);
            tabAss.addCell(a1);

            PdfPCell a2 = new PdfPCell();
            a2.setBorder(Rectangle.NO_BORDER);
            a2.setPaddingTop(12);
            Paragraph p2 = new Paragraph();
            p2.setAlignment(Element.ALIGN_CENTER);
            p2.add(new Phrase("___________________________________________\n", cellTblF));
            p2.add(new Phrase("Secretaria Escolar\n", cellTblBold));
            p2.add(new Phrase("Assinatura e Carimbo", smallF));
            a2.addElement(p2);
            tabAss.addCell(a2);

            document.add(tabAss);

            Paragraph rodape = new Paragraph();
            rodape.setAlignment(Element.ALIGN_CENTER);
            rodape.setSpacingBefore(12f);
            rodape.add(new Phrase("Documento emitido em "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + " pelo Sistema Escolar", smallF));
            document.add(rodape);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // ==================================================================
    // HISTÓRICO OFICIAL FRENTE/VERSO (2 PÁGINAS - Padrão Ficha 18/19)
    // ==================================================================
    public ByteArrayInputStream gerarHistoricoOficialFrenteVersoPdf(Long alunoId) {
        Document document = new Document(PageSize.A4, 30, 30, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font escolaNomeF = new Font(Font.HELVETICA, 15, Font.BOLD, new Color(30, 64, 175));
            Font escolaInfoF = new Font(Font.HELVETICA, 7, Font.NORMAL, new Color(108, 117, 125));
            Font tituloF = new Font(Font.HELVETICA, 13, Font.BOLD, new Color(30, 64, 175));
            Font sectionF = new Font(Font.HELVETICA, 8, Font.BOLD, Color.WHITE);
            Font labelF = new Font(Font.HELVETICA, 7, Font.BOLD, new Color(30, 64, 175));
            Font valorF = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(33, 37, 41));
            Font headerTblF = new Font(Font.HELVETICA, 6, Font.BOLD, Color.WHITE);
            Font cellTblF = new Font(Font.HELVETICA, 6.5f, Font.NORMAL, new Color(33, 37, 41));
            Font cellTblBold = new Font(Font.HELVETICA, 6.5f, Font.BOLD, new Color(33, 37, 41));
            Font smallF = new Font(Font.HELVETICA, 6.5f, Font.NORMAL, new Color(108, 117, 125));

            Aluno aluno = alunoService.buscarPorId(alunoId);
            if (aluno == null) {
                Paragraph erro = new Paragraph("Aluno não encontrado.", valorF);
                erro.setAlignment(Element.ALIGN_CENTER);
                document.add(erro);
                document.close();
                return new ByteArrayInputStream(out.toByteArray());
            }

            List<HistoricoEscolar> historicos = historicoRepository.findByAlunoIdWithItems(alunoId);
            historicos.sort((a, b) -> {
                Integer anoA = a.getAnoLetivo() != null ? a.getAnoLetivo() : 0;
                Integer anoB = b.getAnoLetivo() != null ? b.getAnoLetivo() : 0;
                return anoA.compareTo(anoB);
            });

            List<Escola> escolas = escolaRepository.findAll();
            Escola escola = escolas.isEmpty() ? null : escolas.get(0);

            // ===== PÁGINA 1 — FRENTE =====
            if (escola != null) {
                Paragraph nomeEsc = new Paragraph(escola.getNome().toUpperCase(), escolaNomeF);
                nomeEsc.setAlignment(Element.ALIGN_CENTER);
                document.add(nomeEsc);

                StringBuilder info = new StringBuilder();
                if (escola.getEnderecoCompleto() != null && !escola.getEnderecoCompleto().isEmpty()) {
                    info.append(escola.getEnderecoCompleto());
                }
                if (escola.getTelefone() != null && !escola.getTelefone().isEmpty()) {
                    if (info.length() > 0) info.append("  |  ");
                    info.append("Tel: ").append(escola.getTelefone());
                }
                if (escola.getEmail() != null && !escola.getEmail().isEmpty()) {
                    if (info.length() > 0) info.append("  |  ");
                    info.append(escola.getEmail());
                }
                if (escola.getCnpj() != null && !escola.getCnpj().isEmpty()) {
                    if (info.length() > 0) info.append("  |  ");
                    info.append("CNPJ: ").append(escola.getCnpj());
                }

                Paragraph pInfo = new Paragraph(info.toString(), escolaInfoF);
                pInfo.setAlignment(Element.ALIGN_CENTER);
                pInfo.setSpacingAfter(4f);
                document.add(pInfo);
            } else {
                Paragraph nomePadrao = new Paragraph("SISTEMA ESCOLAR", escolaNomeF);
                nomePadrao.setAlignment(Element.ALIGN_CENTER);
                nomePadrao.setSpacingAfter(4f);
                document.add(nomePadrao);
            }

            Paragraph linhaSep = new Paragraph("_____________________________________________________________________________", escolaInfoF);
            linhaSep.setAlignment(Element.ALIGN_CENTER);
            linhaSep.setSpacingAfter(12f);
            document.add(linhaSep);

            Paragraph titulo = new Paragraph("HISTÓRICO ESCOLAR", tituloF);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(4f);
            document.add(titulo);

            Paragraph subtitulo = new Paragraph("(Documento Oficial)", escolaInfoF);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(20f);
            document.add(subtitulo);

            PdfPTable titSec1 = new PdfPTable(1);
            titSec1.setWidthPercentage(100);
            titSec1.setSpacingAfter(0f);
            PdfPCell cT1 = new PdfPCell(new Phrase("1. IDENTIFICAÇÃO DO ALUNO(A)", sectionF));
            cT1.setBackgroundColor(new Color(30, 64, 175));
            cT1.setPadding(6);
            cT1.setBorder(Rectangle.NO_BORDER);
            titSec1.addCell(cT1);
            document.add(titSec1);

            PdfPTable tabAluno = new PdfPTable(4);
            tabAluno.setWidthPercentage(100);
            tabAluno.setWidths(new float[]{1.3f, 3.7f, 1.3f, 2.5f});
            tabAluno.setSpacingAfter(14f);

            adicionarCampoFicha(tabAluno, "Nome:", aluno.getNome());
            adicionarCampoFicha(tabAluno, "Data de Nascimento:",
                    aluno.getDataNascimento() != null
                            ? aluno.getDataNascimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            : "-");
            adicionarCampoFicha(tabAluno, "Matrícula:",
                    aluno.getMatricula() != null ? aluno.getMatricula() : "-");
            adicionarCampoFicha(tabAluno, "Naturalidade:",
                    aluno.getNaturalidade() != null && !aluno.getNaturalidade().isEmpty()
                            ? aluno.getNaturalidade() : "-");
            adicionarCampoFicha(tabAluno, "Nacionalidade:",
                    aluno.getNacionalidade() != null && !aluno.getNacionalidade().isEmpty()
                            ? aluno.getNacionalidade() : "-");
            adicionarCampoFicha(tabAluno, "Sexo:",
                    aluno.getSexo() != null && !aluno.getSexo().isEmpty()
                            ? aluno.getSexo() : "-");
            adicionarCampoFicha(tabAluno, "Nome do Pai:",
                    aluno.getNomePai() != null && !aluno.getNomePai().isEmpty()
                            ? aluno.getNomePai() : "-");
            adicionarCampoFicha(tabAluno, "Nome da Mãe:",
                    aluno.getNomeMae() != null && !aluno.getNomeMae().isEmpty()
                            ? aluno.getNomeMae() : "-");
            adicionarCampoFicha(tabAluno, "CPF do Aluno:",
                    aluno.getCpfAluno() != null && !aluno.getCpfAluno().isEmpty()
                            ? aluno.getCpfAluno() : "-");
            adicionarCampoFicha(tabAluno, "RG do Aluno:",
                    aluno.getRgAluno() != null && !aluno.getRgAluno().isEmpty()
                            ? aluno.getRgAluno() : "-");
            adicionarCampoFicha(tabAluno, "Responsável:",
                    aluno.getResponsavelFinanceiro() != null
                            && aluno.getResponsavelFinanceiro().getNome() != null
                            ? aluno.getResponsavelFinanceiro().getNome() : "-");
            adicionarCampoFicha(tabAluno, "Telefone:",
                    aluno.getTelefone() != null && !aluno.getTelefone().isEmpty()
                            ? aluno.getTelefone() : "-");

            document.add(tabAluno);

            PdfPTable titSec2 = new PdfPTable(1);
            titSec2.setWidthPercentage(100);
            titSec2.setSpacingAfter(0f);
            PdfPCell cT2 = new PdfPCell(new Phrase("2. DADOS DO CURSO", sectionF));
            cT2.setBackgroundColor(new Color(30, 64, 175));
            cT2.setPadding(6);
            cT2.setBorder(Rectangle.NO_BORDER);
            titSec2.addCell(cT2);
            document.add(titSec2);

            PdfPTable tabCurso = new PdfPTable(4);
            tabCurso.setWidthPercentage(100);
            tabCurso.setWidths(new float[]{1.3f, 2f, 1.3f, 2f});
            tabCurso.setSpacingAfter(14f);

            HistoricoEscolar primeiro = historicos.isEmpty() ? null : historicos.get(0);
            HistoricoEscolar ultimo = historicos.isEmpty() ? null : historicos.get(historicos.size() - 1);

            adicionarCampoFicha(tabCurso, "Ano Inicial:",
                    primeiro != null && primeiro.getAnoLetivo() != null
                            ? String.valueOf(primeiro.getAnoLetivo()) : "-");
            adicionarCampoFicha(tabCurso, "Ano Final:",
                    ultimo != null && ultimo.getAnoLetivo() != null
                            ? String.valueOf(ultimo.getAnoLetivo()) : "-");
            adicionarCampoFicha(tabCurso, "Série Inicial:",
                    primeiro != null && primeiro.getSerie() != null ? primeiro.getSerie() : "-");
            adicionarCampoFicha(tabCurso, "Série Final:",
                    ultimo != null && ultimo.getSerie() != null ? ultimo.getSerie() : "-");
            adicionarCampoFicha(tabCurso, "Total de Anos:",
                    String.valueOf(historicos.size()));
            adicionarCampoFicha(tabCurso, "Nível de Ensino:", "-");
            adicionarCampoFicha(tabCurso, "Turno:",
                    ultimo != null && ultimo.getTurno() != null ? ultimo.getTurno() : "-");

            String situacaoGeral = ultimo != null && ultimo.getSituacaoFinal() != null
                    ? ultimo.getSituacaoFinal() : "-";
            String sitExibir = situacaoGeral;
            Color corSit = new Color(108, 117, 125);

            if ("APROVADO".equalsIgnoreCase(situacaoGeral)) {
                corSit = new Color(25, 135, 84);
                sitExibir = "APROVADO";
            } else if ("APROVADO_COM_DEPENDENCIA".equalsIgnoreCase(situacaoGeral)) {
                corSit = new Color(243, 156, 18);
                sitExibir = "APROVADO C/ DEPEND.";
            } else if ("REPROVADO".equalsIgnoreCase(situacaoGeral)) {
                corSit = new Color(220, 53, 69);
                sitExibir = "REPROVADO";
            } else if ("CURSANDO".equalsIgnoreCase(situacaoGeral)) {
                corSit = new Color(243, 156, 18);
                sitExibir = "CURSANDO";
            } else if ("TRANSFERIDO".equalsIgnoreCase(situacaoGeral)) {
                sitExibir = "TRANSFERIDO";
            }

            PdfPCell cellLabelSit = new PdfPCell(new Phrase("Situação Atual:",
                    new Font(Font.HELVETICA, 7, Font.BOLD, new Color(30, 64, 175))));
            cellLabelSit.setBackgroundColor(new Color(240, 244, 248));
            cellLabelSit.setPadding(5);
            tabCurso.addCell(cellLabelSit);

            PdfPCell cellValorSit = new PdfPCell(new Phrase(sitExibir,
                    new Font(Font.HELVETICA, 8, Font.BOLD, corSit)));
            cellValorSit.setPadding(5);
            tabCurso.addCell(cellValorSit);

            document.add(tabCurso);

            String cidade = (escola != null && escola.getCidade() != null) ? escola.getCidade() : "___________________";
            String dataAtual = LocalDate.now().format(DateTimeFormatter.ofPattern(
                    "dd 'de' MMMM 'de' yyyy", new java.util.Locale("pt", "BR")));

            Paragraph localData = new Paragraph(cidade + ", " + dataAtual + ".", valorF);
            localData.setAlignment(Element.ALIGN_RIGHT);
            localData.setSpacingBefore(50f);
            localData.setSpacingAfter(60f);
            document.add(localData);

            PdfPTable tabAss = new PdfPTable(2);
            tabAss.setWidthPercentage(85);
            tabAss.setHorizontalAlignment(Element.ALIGN_CENTER);
            tabAss.setWidths(new float[]{1, 1});

            PdfPCell a1 = new PdfPCell();
            a1.setBorder(Rectangle.NO_BORDER);
            a1.setPaddingTop(15);
            Paragraph p1 = new Paragraph();
            p1.setAlignment(Element.ALIGN_CENTER);
            p1.add(new Phrase("___________________________________________\n", cellTblF));
            p1.add(new Phrase("Direção\n", cellTblBold));
            p1.add(new Phrase("Assinatura e Carimbo", smallF));
            a1.addElement(p1);
            tabAss.addCell(a1);

            PdfPCell a2 = new PdfPCell();
            a2.setBorder(Rectangle.NO_BORDER);
            a2.setPaddingTop(15);
            Paragraph p2 = new Paragraph();
            p2.setAlignment(Element.ALIGN_CENTER);
            p2.add(new Phrase("___________________________________________\n", cellTblF));
            p2.add(new Phrase("Secretaria Escolar\n", cellTblBold));
            p2.add(new Phrase("Assinatura e Carimbo", smallF));
            a2.addElement(p2);
            tabAss.addCell(a2);

            document.add(tabAss);

            // ===== PÁGINA 2 — VERSO =====
            document.newPage();

            if (escola != null) {
                Paragraph nomeEsc2 = new Paragraph(escola.getNome().toUpperCase(),
                        new Font(Font.HELVETICA, 12, Font.BOLD, new Color(30, 64, 175)));
                nomeEsc2.setAlignment(Element.ALIGN_CENTER);
                nomeEsc2.setSpacingAfter(2f);
                document.add(nomeEsc2);
            }

            Paragraph linhaSep2 = new Paragraph("_____________________________________________________________________________", escolaInfoF);
            linhaSep2.setAlignment(Element.ALIGN_CENTER);
            linhaSep2.setSpacingAfter(10f);
            document.add(linhaSep2);

            Paragraph tituloBol = new Paragraph("COMPONENTES CURRICULARES", tituloF);
            tituloBol.setAlignment(Element.ALIGN_CENTER);
            tituloBol.setSpacingAfter(4f);
            document.add(tituloBol);

            Paragraph subAluno = new Paragraph();
            subAluno.setAlignment(Element.ALIGN_CENTER);
            subAluno.setSpacingAfter(12f);
            subAluno.add(new Phrase("Aluno: ", cellTblBold));
            subAluno.add(new Phrase(aluno.getNome(), cellTblF));
            subAluno.add(new Phrase("    |    Matrícula: ", cellTblBold));
            subAluno.add(new Phrase(aluno.getMatricula() != null ? aluno.getMatricula() : "-", cellTblF));
            document.add(subAluno);

            List<String> disciplinas = new ArrayList<>();
            for (HistoricoEscolar h : historicos) {
                if (h.getItems() == null) continue;
                for (ItemHistorico item : h.getItems()) {
                    String nome = item.getNomeMateria();
                    if (nome != null && !nome.trim().isEmpty() && !disciplinas.contains(nome)) {
                        disciplinas.add(nome);
                    }
                }
            }

            int totalDisc = disciplinas.size();

            if (historicos.isEmpty() || totalDisc == 0) {
                Paragraph vazio = new Paragraph("Nenhum histórico escolar registrado.", valorF);
                vazio.setAlignment(Element.ALIGN_CENTER);
                vazio.setSpacingBefore(20f);
                document.add(vazio);
            } else {
                int totalColunas = 3 + totalDisc;

                PdfPTable tabela = new PdfPTable(totalColunas);
                tabela.setWidthPercentage(100);

                float[] widths = new float[totalColunas];
                widths[0] = 0.7f;
                widths[1] = 1.3f;
                float larguraDisc = (10.0f - widths[0] - widths[1] - 1.5f) / totalDisc;
                for (int i = 0; i < totalDisc; i++) {
                    widths[2 + i] = larguraDisc;
                }
                widths[totalColunas - 1] = 1.5f;
                tabela.setWidths(widths);
                tabela.setSpacingAfter(10f);

                Color headerColor = new Color(30, 64, 175);

                adicionarCelulaHeaderCompacto(tabela, "Ano", headerTblF, headerColor);
                adicionarCelulaHeaderCompacto(tabela, "Série", headerTblF, headerColor);
                for (String disc : disciplinas) {
                    adicionarCelulaHeaderCompacto(tabela, disc, headerTblF, headerColor);
                }
                adicionarCelulaHeaderCompacto(tabela, "Situação", headerTblF, headerColor);

                for (HistoricoEscolar h : historicos) {
                    PdfPCell cAno = new PdfPCell(new Phrase(
                            h.getAnoLetivo() != null ? String.valueOf(h.getAnoLetivo()) : "-", cellTblBold));
                    cAno.setPadding(3);
                    cAno.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(cAno);

                    PdfPCell cSerie = new PdfPCell(new Phrase(
                            h.getSerie() != null ? h.getSerie() : "-", cellTblF));
                    cSerie.setPadding(3);
                    cSerie.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(cSerie);

                    for (String disc : disciplinas) {
                        String notaStr = "-";
                        if (h.getItems() != null) {
                            for (ItemHistorico item : h.getItems()) {
                                if (disc.equals(item.getNomeMateria()) && item.getNotaFinal() != null) {
                                    notaStr = String.format(Locale.US, "%.1f", item.getNotaFinal());
                                    break;
                                }
                            }
                        }
                        PdfPCell cNota = new PdfPCell(new Phrase(notaStr, cellTblF));
                        cNota.setPadding(3);
                        cNota.setHorizontalAlignment(Element.ALIGN_CENTER);
                        tabela.addCell(cNota);
                    }

                    String sit = h.getSituacaoFinal() != null ? h.getSituacaoFinal() : "-";
                    String sitEx = sit;
                    Color corS = new Color(108, 117, 125);

                    if ("APROVADO".equalsIgnoreCase(sit)) {
                        corS = new Color(25, 135, 84);
                        sitEx = "Aprov.";
                    } else if ("APROVADO_COM_DEPENDENCIA".equalsIgnoreCase(sit)) {
                        corS = new Color(243, 156, 18);
                        sitEx = "Aprov.c/Dep";
                    } else if ("REPROVADO".equalsIgnoreCase(sit)) {
                        corS = new Color(220, 53, 69);
                        sitEx = "Reprov.";
                    } else if ("CURSANDO".equalsIgnoreCase(sit)) {
                        corS = new Color(243, 156, 18);
                        sitEx = "Cursando";
                    } else if ("TRANSFERIDO".equalsIgnoreCase(sit)) {
                        sitEx = "Transf.";
                    } else if ("EVADIDO".equalsIgnoreCase(sit)) {
                        sitEx = "Evadido";
                    }

                    PdfPCell cS = new PdfPCell(new Phrase(sitEx,
                            new Font(Font.HELVETICA, 6.5f, Font.BOLD, corS)));
                    cS.setPadding(3);
                    cS.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabela.addCell(cS);
                }

                document.add(tabela);
            }

            StringBuilder obsConsolidada = new StringBuilder();
            for (HistoricoEscolar h : historicos) {
                if (h.getObservacoes() != null && !h.getObservacoes().isEmpty()) {
                    if (obsConsolidada.length() > 0) obsConsolidada.append("  |  ");
                    obsConsolidada.append("[").append(h.getAnoLetivo()).append("] ")
                            .append(h.getObservacoes());
                }
            }

            if (obsConsolidada.length() > 0) {
                Paragraph pObs = new Paragraph();
                pObs.setSpacingBefore(12f);
                pObs.setSpacingAfter(8f);
                pObs.add(new Phrase("Observações: ", cellTblBold));
                pObs.add(new Phrase(obsConsolidada.toString(), cellTblF));
                document.add(pObs);
            }

            Paragraph localData2 = new Paragraph(cidade + ", " + dataAtual + ".", valorF);
            localData2.setAlignment(Element.ALIGN_RIGHT);
            localData2.setSpacingBefore(40f);
            localData2.setSpacingAfter(50f);
            document.add(localData2);

            Paragraph assinaturaVerso = new Paragraph();
            assinaturaVerso.setAlignment(Element.ALIGN_CENTER);
            assinaturaVerso.add(new Phrase("___________________________________________\n", cellTblF));
            assinaturaVerso.add(new Phrase("Direção / Coordenação\n", cellTblBold));
            assinaturaVerso.add(new Phrase("Assinatura e Carimbo", smallF));
            document.add(assinaturaVerso);

            Paragraph rodape = new Paragraph();
            rodape.setAlignment(Element.ALIGN_CENTER);
            rodape.setSpacingBefore(20f);
            rodape.add(new Phrase("Documento emitido em "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + " pelo Sistema Escolar", smallF));
            document.add(rodape);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }

    // ==================================================================
    // CARNÊ INDIVIDUAL
    // ==================================================================
    public ByteArrayInputStream gerarCarneIndividualPdf(Long alunoId, int mesInicio, int mesFim, int ano) {
        Aluno aluno = alunoService.buscarPorId(alunoId);
        if (aluno == null) {
            return new ByteArrayInputStream(new byte[0]);
        }
        return gerarCarnePdf(java.util.Collections.singletonList(aluno), mesInicio, mesFim, ano);
    }

    // ==================================================================
    // CARNÊ POR TURMA
    // ==================================================================
    public ByteArrayInputStream gerarCarneTurmaPdf(Long turmaId, int mesInicio, int mesFim, int ano) {
        List<Aluno> alunos = alunoService.buscarPorTurma(turmaId);
        if (alunos.isEmpty()) {
            return new ByteArrayInputStream(new byte[0]);
        }
        return gerarCarnePdf(alunos, mesInicio, mesFim, ano);
    }

    // ==================================================================
    // MÉTODO INTERNO — Gera o PDF de carnê para lista de alunos
    // Formato: A4 retrato | 6 talões por folha | auto-height
    // ==================================================================
    private ByteArrayInputStream gerarCarnePdf(List<Aluno> alunos, int mesInicio, int mesFim, int ano) {
        Document document = new Document(PageSize.A4, 10, 10, 10, 10);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();

            Font escolaF       = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(30, 64, 175));
            Font tituloF       = new Font(Font.HELVETICA, 9, Font.BOLD, new Color(30, 64, 175));
            Font numF          = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(108, 117, 125));
            Font labelF        = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(108, 117, 125));
            Font labelStrongF  = new Font(Font.HELVETICA, 8, Font.BOLD, new Color(108, 117, 125));
            Font valorF        = new Font(Font.HELVETICA, 9.5f, Font.BOLD, new Color(33, 37, 41));
            Font refF          = new Font(Font.HELVETICA, 11, Font.BOLD, new Color(30, 64, 175));
            Font vencF         = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(33, 37, 41));
            Font valorGrandeF  = new Font(Font.HELVETICA, 13, Font.BOLD, new Color(30, 64, 175));
            Font picoteF       = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(90, 90, 90));

            List<Escola> escolas = escolaRepository.findAll();
            Escola escola = escolas.isEmpty() ? null : escolas.get(0);
            String nomeEscola = (escola != null) ? escola.getNome() : "SISTEMA ESCOLAR";

            List<Mensalidade> todasMensalidades = new ArrayList<>();
            for (Aluno aluno : alunos) {
                List<Mensalidade> ms = mensalidadeService.listarPorAluno(aluno.getId());
                for (Mensalidade m : ms) {
                    if (m.getMesReferencia() == null) continue;
                    int mesRef = m.getMesReferencia().getMonthValue();
                    int anoRef = m.getMesReferencia().getYear();
                    if (anoRef == ano && mesRef >= mesInicio && mesRef <= mesFim) {
                        todasMensalidades.add(m);
                    }
                }
            }

            todasMensalidades.sort((a, b) -> {
                Long idA = a.getAluno().getId();
                Long idB = b.getAluno().getId();
                if (!idA.equals(idB)) return idA.compareTo(idB);
                return a.getMesReferencia().compareTo(b.getMesReferencia());
            });

            if (todasMensalidades.isEmpty()) {
                Paragraph vazio = new Paragraph("Nenhuma mensalidade encontrada no período informado.", labelF);
                vazio.setAlignment(Element.ALIGN_CENTER);
                document.add(vazio);
                document.close();
                return new ByteArrayInputStream(out.toByteArray());
            }

            int TALOES_POR_FOLHA = 6;
            int totalTal = todasMensalidades.size();
            int totalFolhas = (int) Math.ceil(totalTal / (double) TALOES_POR_FOLHA);

            DateTimeFormatter fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String[] mesesNomes = {"", "JANEIRO", "FEVEREIRO", "MARÇO", "ABRIL", "MAIO", "JUNHO",
                                    "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "DEZEMBRO"};

            String linhaPicote = "--- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- --- ---";

            for (int folha = 0; folha < totalFolhas; folha++) {

                PdfPTable tabela = new PdfPTable(1);
                tabela.setWidthPercentage(100);

                int inicio = folha * TALOES_POR_FOLHA;
                int fim = Math.min(inicio + TALOES_POR_FOLHA, totalTal);

                for (int i = inicio; i < fim; i++) {
                    Mensalidade m = todasMensalidades.get(i);
                    Aluno aluno = m.getAluno();

                    // ===== TALÃO (altura automática) =====
                    PdfPCell cell = new PdfPCell();
                    cell.setPadding(8);
                    cell.setBorder(Rectangle.NO_BORDER);
                    cell.setMinimumHeight(80f);   // mínimo, cresce se precisar

                    // Cabeçalho
                    PdfPTable header = new PdfPTable(3);
                    header.setWidthPercentage(100);
                    header.setWidths(new float[]{3f, 2f, 1.2f});

                    PdfPCell h1 = new PdfPCell(new Phrase("🎓 " + nomeEscola, escolaF));
                    h1.setBorder(Rectangle.BOTTOM);
                    h1.setBorderColor(new Color(30, 64, 175));
                    h1.setBorderWidth(1.5f);
                    h1.setPadding(2);
                    header.addCell(h1);

                    PdfPCell h2 = new PdfPCell(new Phrase("CARNÊ DE PAGAMENTO", tituloF));
                    h2.setBorder(Rectangle.BOTTOM);
                    h2.setBorderColor(new Color(30, 64, 175));
                    h2.setBorderWidth(1.5f);
                    h2.setHorizontalAlignment(Element.ALIGN_CENTER);
                    h2.setPadding(2);
                    header.addCell(h2);

                    String numTal = String.format("%06d", i + 1);
                    PdfPCell h3 = new PdfPCell(new Phrase("Nº " + numTal, numF));
                    h3.setBorder(Rectangle.BOTTOM);
                    h3.setBorderColor(new Color(30, 64, 175));
                    h3.setBorderWidth(1.5f);
                    h3.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    h3.setPadding(2);
                    header.addCell(h3);

                    cell.addElement(header);

                    // Linha aluno + matrícula
                    PdfPTable linha1 = new PdfPTable(3);
                    linha1.setWidthPercentage(100);
                    linha1.setWidths(new float[]{3.5f, 2f, 1.5f});
                    linha1.setSpacingBefore(4f);

                    PdfPCell l1a = new PdfPCell(new Phrase("Aluno: " + aluno.getNome(), valorF));
                    l1a.setBorder(Rectangle.NO_BORDER);
                    l1a.setPadding(1);
                    linha1.addCell(l1a);

                    PdfPCell l1b = new PdfPCell(new Phrase("Matrícula: " + (aluno.getMatricula() != null ? aluno.getMatricula() : "-"), labelF));
                    l1b.setBorder(Rectangle.NO_BORDER);
                    l1b.setPadding(1);
                    linha1.addCell(l1b);

                    String telefoneAluno = aluno.getTelefone() != null ? aluno.getTelefone() : "";
                    PdfPCell l1c = new PdfPCell(new Phrase("Tel: " + telefoneAluno, labelF));
                    l1c.setBorder(Rectangle.NO_BORDER);
                    l1c.setPadding(1);
                    l1c.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    linha1.addCell(l1c);

                    cell.addElement(linha1);

                    // Linha responsável
                    String nomeResp = (aluno.getResponsavelFinanceiro() != null
                            && aluno.getResponsavelFinanceiro().getNome() != null)
                            ? aluno.getResponsavelFinanceiro().getNome() : "-";
                    Paragraph linhaResp = new Paragraph("Responsável: " + nomeResp, labelF);
                    linhaResp.setSpacingBefore(2f);
                    cell.addElement(linhaResp);

                    // Rodapé: referência + vencimento + valor
                    PdfPTable rodape = new PdfPTable(3);
                    rodape.setWidthPercentage(100);
                    rodape.setWidths(new float[]{2f, 1.5f, 1.5f});
                    rodape.setSpacingBefore(8f);

                    String nomeMes = mesesNomes[m.getMesReferencia().getMonthValue()];

                    // Referência
                    Paragraph pRef = new Paragraph();
                    pRef.add(new Phrase("Referência", labelStrongF));
                    pRef.add(Chunk.NEWLINE);
                    pRef.add(new Phrase(nomeMes + " / " + m.getMesReferencia().getYear(), refF));
                    PdfPCell r1 = new PdfPCell(pRef);
                    r1.setBorder(Rectangle.TOP);
                    r1.setBorderColor(new Color(200, 200, 200));
                    r1.setPadding(4);
                    rodape.addCell(r1);

                    // Vencimento
                    Paragraph pVenc = new Paragraph();
                    pVenc.add(new Phrase("Vencimento", labelStrongF));
                    pVenc.add(Chunk.NEWLINE);
                    String dataVenc = m.getDataVencimento() != null ? m.getDataVencimento().format(fmtData) : "-";
                    pVenc.add(new Phrase(dataVenc, vencF));
                    PdfPCell r2 = new PdfPCell(pVenc);
                    r2.setBorder(Rectangle.TOP);
                    r2.setBorderColor(new Color(200, 200, 200));
                    r2.setPadding(4);
                    r2.setHorizontalAlignment(Element.ALIGN_CENTER);
                    rodape.addCell(r2);

                    // Valor
                    Paragraph pValor = new Paragraph();
                    pValor.add(new Phrase("Valor", labelStrongF));
                    pValor.add(Chunk.NEWLINE);
                    String valorTxt = "R$ " + String.format(Locale.US, "%.2f",
                            m.getValorOriginal() != null ? m.getValorOriginal() : 0.0);
                    pValor.add(new Phrase(valorTxt, valorGrandeF));
                    PdfPCell r3 = new PdfPCell(pValor);
                    r3.setBorder(Rectangle.TOP);
                    r3.setBorderColor(new Color(200, 200, 200));
                    r3.setPadding(4);
                    r3.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    rodape.addCell(r3);

                    cell.addElement(rodape);

                    // Código de barras
                    try {
                        PdfContentByte cb = writer.getDirectContent();
                        Barcode128 barcode = new Barcode128();
                        barcode.setCode(String.format("%010d", m.getId()));
                        barcode.setBarHeight(14);
                        barcode.setX(0.7f);
                        barcode.setFont(null);
                        Image imgBarcode = barcode.createImageWithBarcode(cb, null, null);
                        imgBarcode.scalePercent(70);
                        imgBarcode.setSpacingBefore(6f);
                        cell.addElement(imgBarcode);
                    } catch (Exception ignored) { }

                    tabela.addCell(cell);

                    // ===== PICOTE APÓS CADA TALÃO =====
                    PdfPCell picote = new PdfPCell(new Phrase(linhaPicote, picoteF));
                    picote.setBorder(Rectangle.NO_BORDER);
                    picote.setHorizontalAlignment(Element.ALIGN_CENTER);
                    picote.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    picote.setPaddingTop(4);
                    picote.setPaddingBottom(4);
                    picote.setFixedHeight(14f);
                    tabela.addCell(picote);
                }

                document.add(tabela);

                if (folha < totalFolhas - 1) {
                    document.newPage();
                }
            }

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
    // ==================================================================
    // DECLARAÇÃO DE MATRÍCULA
    // ==================================================================
    public ByteArrayInputStream gerarDeclaracaoMatricula(Long alunoId) {
        Document document = new Document(PageSize.A4, 50, 50, 60, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font escolaNomeF = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(30, 64, 175));
            Font escolaInfoF = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(108, 117, 125));
            Font tituloF = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(30, 64, 80));
            Font corpoF = new Font(Font.HELVETICA, 12, Font.NORMAL, new Color(33, 37, 41));
            Font corpoBoldF = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(33, 37, 41));
            Font pequenaF = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(108, 117, 125));

            Aluno aluno = alunoService.buscarPorId(alunoId);
            if (aluno == null) {
                document.close();
                return new ByteArrayInputStream(out.toByteArray());
            }

            List<Escola> escolas = escolaRepository.findAll();
            Escola escola = escolas.isEmpty() ? null : escolas.get(0);

            // ===== Cabeçalho =====
            if (escola != null) {
                Paragraph nomeEsc = new Paragraph(escola.getNome().toUpperCase(), escolaNomeF);
                nomeEsc.setAlignment(Element.ALIGN_CENTER);
                document.add(nomeEsc);

                StringBuilder info = new StringBuilder();
                if (escola.getCnpj() != null && !escola.getCnpj().isEmpty()) {
                    info.append("CNPJ: ").append(escola.getCnpj());
                }
                if (escola.getEnderecoCompleto() != null && !escola.getEnderecoCompleto().isEmpty()) {
                    if (info.length() > 0) info.append(" | ");
                    info.append(escola.getEnderecoCompleto());
                }
                if (escola.getTelefone() != null && !escola.getTelefone().isEmpty()) {
                    if (info.length() > 0) info.append(" | ");
                    info.append("Tel: ").append(escola.getTelefone());
                }
                if (escola.getEmail() != null && !escola.getEmail().isEmpty()) {
                    if (info.length() > 0) info.append(" | ");
                    info.append(escola.getEmail());
                }

                Paragraph infoP = new Paragraph(info.toString(), escolaInfoF);
                infoP.setAlignment(Element.ALIGN_CENTER);
                infoP.setSpacingAfter(6f);
                document.add(infoP);
            } else {
                Paragraph nomePadrao = new Paragraph("SISTEMA ESCOLAR", escolaNomeF);
                nomePadrao.setAlignment(Element.ALIGN_CENTER);
                nomePadrao.setSpacingAfter(6f);
                document.add(nomePadrao);
            }

            Paragraph linha = new Paragraph("___________________________________________________________________________", escolaInfoF);
            linha.setAlignment(Element.ALIGN_CENTER);
            linha.setSpacingAfter(30f);
            document.add(linha);

            // ===== Título =====
            Paragraph titulo = new Paragraph("DECLARAÇÃO DE MATRÍCULA", tituloF);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(40f);
            document.add(titulo);

            // ===== Corpo =====
            String nomeResponsavel = (aluno.getResponsavelFinanceiro() != null
                    && aluno.getResponsavelFinanceiro().getNome() != null)
                    ? aluno.getResponsavelFinanceiro().getNome() : "_______________________";

            String nomeSerie = (aluno.getSerie() != null) ? aluno.getSerie().getNome() : "____";
            String nomeTurma = (aluno.getTurma() != null) ? aluno.getTurma().getNome() : "____";

            Integer anoLetivo = aluno.getAnoLetivo() != null
                    ? aluno.getAnoLetivo()
                    : LocalDate.now().getYear();

            Paragraph p1 = new Paragraph();
            p1.setAlignment(Element.ALIGN_JUSTIFIED);
            p1.setLeading(24f);
            p1.setSpacingAfter(20f);
            p1.add(new Phrase("Declaramos, para os devidos fins, que ", corpoF));
            p1.add(new Phrase(aluno.getNome(), corpoBoldF));
            p1.add(new Phrase(", matrícula nº ", corpoF));
            p1.add(new Phrase(aluno.getMatricula() != null ? aluno.getMatricula() : "____", corpoBoldF));
            p1.add(new Phrase(", está regularmente matriculado(a) nesta instituição de ensino no ano letivo de ", corpoF));
            p1.add(new Phrase(String.valueOf(anoLetivo), corpoBoldF));
            p1.add(new Phrase(", cursando a ", corpoF));
            p1.add(new Phrase(nomeSerie, corpoBoldF));
            p1.add(new Phrase(", turma ", corpoF));
            p1.add(new Phrase(nomeTurma, corpoBoldF));
            p1.add(new Phrase(".", corpoF));
            document.add(p1);

            // ===== Responsável =====
            Paragraph p2 = new Paragraph();
            p2.setAlignment(Element.ALIGN_JUSTIFIED);
            p2.setLeading(24f);
            p2.setSpacingAfter(30f);
            p2.add(new Phrase("Responsável Financeiro: ", corpoF));
            p2.add(new Phrase(nomeResponsavel, corpoBoldF));
            if (aluno.getTelefone() != null && !aluno.getTelefone().isEmpty()) {
                p2.add(new Phrase(" — Telefone: ", corpoF));
                p2.add(new Phrase(aluno.getTelefone(), corpoBoldF));
            }
            document.add(p2);

            // ===== Finalidade =====
            Paragraph p3 = new Paragraph(
                "A presente declaração é emitida a pedido do interessado para fins de comprovação.",
                corpoF);
            p3.setAlignment(Element.ALIGN_JUSTIFIED);
            p3.setLeading(24f);
            p3.setSpacingAfter(50f);
            document.add(p3);

            // ===== Local e data =====
            String cidade = (escola != null && escola.getCidade() != null) ? escola.getCidade() : "___________________";
            String dataAtual = LocalDate.now().format(DateTimeFormatter.ofPattern(
                    "dd 'de' MMMM 'de' yyyy", new java.util.Locale("pt", "BR")));

            Paragraph localData = new Paragraph(cidade + ", " + dataAtual + ".", corpoF);
            localData.setAlignment(Element.ALIGN_RIGHT);
            localData.setSpacingAfter(80f);
            document.add(localData);

            // ===== Assinatura =====
            Paragraph assinatura = new Paragraph();
            assinatura.setAlignment(Element.ALIGN_CENTER);
            assinatura.add(new Phrase("___________________________________________\n", corpoF));
            assinatura.add(new Phrase("Direção / Secretaria\n", corpoBoldF));
            assinatura.add(new Phrase("Assinatura e Carimbo", pequenaF));
            document.add(assinatura);

            // ===== Rodapé =====
            Paragraph rodape = new Paragraph();
            rodape.setAlignment(Element.ALIGN_CENTER);
            rodape.setSpacingBefore(60f);
            rodape.add(new Phrase("Documento emitido em "
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    + " pelo Sistema Escolar", pequenaF));
            document.add(rodape);

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

    private void adicionarCampoFicha(PdfPTable tabela, String label, String valor) {
        Font labelFont = new Font(Font.HELVETICA, 7, Font.BOLD, new Color(30, 64, 175));
        Font valorFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(33, 37, 41));

        PdfPCell cellLabel = new PdfPCell(new Phrase(label, labelFont));
        cellLabel.setPadding(5);
        cellLabel.setBackgroundColor(new Color(240, 244, 248));
        tabela.addCell(cellLabel);

        PdfPCell cellValor = new PdfPCell(new Phrase(valor != null ? valor : "-", valorFont));
        cellValor.setPadding(5);
        tabela.addCell(cellValor);
    }

    private void adicionarCampoFichaCompacto(PdfPTable tabela, String label, String valor,
                                              Font labelFont, Font valorFont) {
        PdfPCell cellLabel = new PdfPCell(new Phrase(label, labelFont));
        cellLabel.setPadding(3);
        cellLabel.setBackgroundColor(new Color(240, 244, 248));
        tabela.addCell(cellLabel);

        PdfPCell cellValor = new PdfPCell(new Phrase(valor != null ? valor : "-", valorFont));
        cellValor.setPadding(3);
        tabela.addCell(cellValor);
    }
}