package br.com.escola.service;

import br.com.escola.model.Aluno;
import br.com.escola.model.Escola;
import br.com.escola.model.Materia;
import br.com.escola.model.Nota;
import br.com.escola.model.Turma;
import br.com.escola.repository.EscolaRepository;
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

    @Autowired
    private TurmaService turmaService;

    @Autowired
    private EscolaRepository escolaRepository;

    // ==================================================================
    // BOLETIM INDIVIDUAL (com cabecalho da escola)
    // ==================================================================
    public ByteArrayInputStream gerarBoletimPdf(Long alunoId) {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fontes
            Font escolaNome = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(30, 64, 175));
            Font escolaInfo = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(108, 117, 125));
            Font tituloBoletim = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(44, 62, 80));
            Font subtitulo = new Font(Font.HELVETICA, 11, Font.NORMAL, new Color(108, 117, 125));
            Font sectionTitle = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(30, 64, 175));
            Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            Font cellFont = new Font(Font.HELVETICA, 10, Font.NORMAL, new Color(33, 37, 41));
            Font cellBold = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(33, 37, 41));
            Font smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(108, 117, 125));

            // Buscar aluno
            Aluno aluno = alunoService.buscarPorId(alunoId);
            if (aluno == null) {
                document.close();
                return new ByteArrayInputStream(out.toByteArray());
            }

            // Buscar escola
            List<Escola> escolas = escolaRepository.findAll();
            Escola escola = escolas.isEmpty() ? null : escolas.get(0);

            // ===== CABECALHO DA ESCOLA =====
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

            // Linha separadora
            Paragraph linha = new Paragraph("_______________________________________________________________________________", escolaInfo);
            linha.setAlignment(Element.ALIGN_CENTER);
            linha.setSpacingAfter(16f);
            document.add(linha);

            // Titulo
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

            // ===== TABELA DE NOTAS =====
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

            // ===== RODAPE =====
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
}