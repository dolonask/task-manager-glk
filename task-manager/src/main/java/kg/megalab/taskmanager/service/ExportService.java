package kg.megalab.taskmanager.service;

import kg.megalab.taskmanager.dto.analytics.AnalyticsSummaryResponse;
import kg.megalab.taskmanager.dto.analytics.BoardMemberAnalyticsResponse;
import kg.megalab.taskmanager.dto.analytics.DepartmentAnalyticsResponse;
import kg.megalab.taskmanager.dto.analytics.RegistryResponse;
import kg.megalab.taskmanager.dto.analytics.TransferRequestAnalyticsResponse;
import kg.megalab.taskmanager.dto.task.TaskListItemResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Renders task/analytics data as XLSX (Apache POI) or PDF (PDFBox) for the /export/* endpoints. */
@Service
public class ExportService {

    private static final Map<String, String> STATUS_LABELS = Map.of(
            "new", "Новая", "in_progress", "В работе", "done", "Выполнено", "overdue", "Просрочено");
    private static final Map<String, String> PRIORITY_LABELS = Map.of(
            "high", "Высокий", "medium", "Средний", "low", "Низкий");

    private static String statusLabel(String status) {
        return STATUS_LABELS.getOrDefault(status, status);
    }

    private static String priorityLabel(String priority) {
        return PRIORITY_LABELS.getOrDefault(priority, priority);
    }

    // ---------------------------------------------------------------- XLSX

    public byte[] tasksXlsx(List<TaskListItemResponse> tasks) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Задачи");
            String[] headers = {
                    "Задача", "СП", "Постановщик", "Ответственный", "Приоритет",
                    "Первоначальный срок", "Текущий срок", "Статус", "Прогресс, %"
            };
            int[] widths = {40, 24, 24, 24, 14, 18, 18, 14, 12};
            writeHeaderRow(wb, sheet, headers, widths);

            int rowIdx = 1;
            for (TaskListItemResponse t : tasks) {
                Row row = sheet.createRow(rowIdx++);
                setCell(row, 0, t.title());
                setCell(row, 1, t.departmentName());
                setCell(row, 2, t.creatorName());
                setCell(row, 3, t.assigneeName() != null ? t.assigneeName() : "—");
                setCell(row, 4, priorityLabel(t.priority()));
                setCell(row, 5, String.valueOf(t.initialDeadline()));
                setCell(row, 6, String.valueOf(t.currentDeadline()));
                setCell(row, 7, statusLabel(t.status()));
                setCell(row, 8, Math.round(t.progress() * 100));
            }
            return toBytes(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] analyticsXlsx(AnalyticsSummaryResponse summary, List<DepartmentAnalyticsResponse> departments,
                                 List<BoardMemberAnalyticsResponse> boardMembers,
                                 TransferRequestAnalyticsResponse transferStats, RegistryResponse registry) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet summarySheet = wb.createSheet("Сводка");
            writeHeaderRow(wb, summarySheet, new String[]{"Показатель", "Значение"}, new int[]{36, 18});
            Object[][] summaryRows = {
                    {"Всего задач", summary.totalTasks()},
                    {"Выполнено", summary.doneTasks()},
                    {"В работе", summary.inProgressTasks()},
                    {"Просрочено", summary.overdueTasks()},
                    {"Процент исполнения, %", Math.round(summary.completionRate() * 100)},
                    {"Соблюдение сроков, %", Math.round(summary.onTimeRate() * 100)},
                    {"Всего заявок на перенос", transferStats.total()},
                    {"Согласовано, %", Math.round(transferStats.approvedRate() * 100)},
                    {"Отклонено, %", Math.round(transferStats.rejectedRate() * 100)},
                    {"Средний перенос, дней", Math.round(transferStats.avgShiftDays() * 10) / 10.0},
            };
            int r = 1;
            for (Object[] pair : summaryRows) {
                Row row = summarySheet.createRow(r++);
                setCell(row, 0, String.valueOf(pair[0]));
                setCell(row, 1, String.valueOf(pair[1]));
            }

            XSSFSheet deptSheet = wb.createSheet("СП");
            writeHeaderRow(wb, deptSheet, new String[]{"СП", "Всего задач", "Выполнено", "Просрочено", "% исполнения"},
                    new int[]{30, 14, 14, 14, 14});
            r = 1;
            for (DepartmentAnalyticsResponse d : departments) {
                Row row = deptSheet.createRow(r++);
                setCell(row, 0, d.departmentName());
                setCell(row, 1, d.totalTasks());
                setCell(row, 2, d.doneTasks());
                setCell(row, 3, d.overdueTasks());
                setCell(row, 4, Math.round(d.completionRate() * 100));
            }

            XSSFSheet boardSheet = wb.createSheet("Правление");
            writeHeaderRow(wb, boardSheet, new String[]{"Член Правления", "Поставлено задач", "Выполнено", "Просрочено"},
                    new int[]{30, 18, 14, 14});
            r = 1;
            for (BoardMemberAnalyticsResponse bm : boardMembers) {
                Row row = boardSheet.createRow(r++);
                setCell(row, 0, bm.fullName());
                setCell(row, 1, bm.totalTasks());
                setCell(row, 2, bm.statusBreakdown().getOrDefault("done", 0L));
                setCell(row, 3, bm.statusBreakdown().getOrDefault("overdue", 0L));
            }

            XSSFSheet registrySheet = wb.createSheet("Реестр");
            writeHeaderRow(wb, registrySheet, new String[]{"Задача", "СП", "Срок", "Статус", "Категория"},
                    new int[]{40, 24, 18, 14, 20});
            r = 1;
            for (TaskListItemResponse t : registry.upcoming()) {
                Row row = registrySheet.createRow(r++);
                setCell(row, 0, t.title());
                setCell(row, 1, t.departmentName());
                setCell(row, 2, String.valueOf(t.currentDeadline()));
                setCell(row, 3, statusLabel(t.status()));
                setCell(row, 4, "Ближайший срок");
            }
            for (TaskListItemResponse t : registry.overdue()) {
                Row row = registrySheet.createRow(r++);
                setCell(row, 0, t.title());
                setCell(row, 1, t.departmentName());
                setCell(row, 2, String.valueOf(t.currentDeadline()));
                setCell(row, 3, statusLabel(t.status()));
                setCell(row, 4, "Просрочено");
            }

            return toBytes(wb);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeHeaderRow(XSSFWorkbook wb, XSSFSheet sheet, String[] headers, int[] colWidths) {
        Font boldFont = wb.createFont();
        boldFont.setBold(true);
        CellStyle headerStyle = wb.createCellStyle();
        headerStyle.setFont(boldFont);

        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, colWidths[i] * 256);
        }
    }

    private void setCell(Row row, int col, String value) {
        row.createCell(col).setCellValue(value);
    }

    private void setCell(Row row, int col, long value) {
        row.createCell(col).setCellValue(value);
    }

    private void setCell(Row row, int col, double value) {
        row.createCell(col).setCellValue(value);
    }

    private byte[] toBytes(XSSFWorkbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }

    // ----------------------------------------------------------------- PDF

    public byte[] tasksPdf(List<TaskListItemResponse> tasks) {
        try (PDDocument doc = new PDDocument(); PdfWriter w = new PdfWriter(doc)) {
            w.title("Реестр задач");
            String[] headers = {"Задача", "СП", "Ответственный", "Приоритет", "Срок", "Статус"};
            float[] widths = {0.30f, 0.18f, 0.18f, 0.12f, 0.10f, 0.12f};
            List<String[]> rows = tasks.stream()
                    .map(t -> new String[]{
                            t.title(), t.departmentName(), t.assigneeName() != null ? t.assigneeName() : "—",
                            priorityLabel(t.priority()), String.valueOf(t.currentDeadline()), statusLabel(t.status())
                    })
                    .toList();
            w.table(headers, widths, rows);
            return w.toBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] analyticsPdf(AnalyticsSummaryResponse summary, List<DepartmentAnalyticsResponse> departments,
                                List<BoardMemberAnalyticsResponse> boardMembers,
                                TransferRequestAnalyticsResponse transferStats, RegistryResponse registry) {
        try (PDDocument doc = new PDDocument(); PdfWriter w = new PdfWriter(doc)) {
            w.title("Аналитика");

            w.heading("Сводные показатели");
            w.table(new String[]{"Показатель", "Значение"}, new float[]{0.7f, 0.3f}, List.of(
                    new String[]{"Всего задач", String.valueOf(summary.totalTasks())},
                    new String[]{"Выполнено", String.valueOf(summary.doneTasks())},
                    new String[]{"В работе", String.valueOf(summary.inProgressTasks())},
                    new String[]{"Просрочено", String.valueOf(summary.overdueTasks())},
                    new String[]{"Процент исполнения", pct(summary.completionRate())},
                    new String[]{"Соблюдение сроков", pct(summary.onTimeRate())},
                    new String[]{"Всего заявок на перенос", String.valueOf(transferStats.total())},
                    new String[]{"Согласовано", pct(transferStats.approvedRate())},
                    new String[]{"Отклонено", pct(transferStats.rejectedRate())},
                    new String[]{"Средний перенос, дней", String.format(Locale.ROOT, "%.1f", transferStats.avgShiftDays())}
            ));

            w.heading("Нагрузка по структурным подразделениям");
            w.table(new String[]{"СП", "Всего", "Выполнено", "Просрочено", "% исполнения"},
                    new float[]{0.4f, 0.15f, 0.15f, 0.15f, 0.15f},
                    departments.stream()
                            .map(d -> new String[]{
                                    d.departmentName(), String.valueOf(d.totalTasks()), String.valueOf(d.doneTasks()),
                                    String.valueOf(d.overdueTasks()), pct(d.completionRate())
                            })
                            .toList());

            w.heading("Задачи по членам Правления");
            w.table(new String[]{"Член Правления", "Поставлено", "Выполнено", "Просрочено"},
                    new float[]{0.4f, 0.2f, 0.2f, 0.2f},
                    boardMembers.stream()
                            .map(bm -> new String[]{
                                    bm.fullName(), String.valueOf(bm.totalTasks()),
                                    String.valueOf(bm.statusBreakdown().getOrDefault("done", 0L)),
                                    String.valueOf(bm.statusBreakdown().getOrDefault("overdue", 0L))
                            })
                            .toList());

            w.heading("Реестр ближайших сроков и просрочек");
            List<String[]> registryRows = new java.util.ArrayList<>();
            for (TaskListItemResponse t : registry.upcoming()) {
                registryRows.add(new String[]{t.title(), t.departmentName(), String.valueOf(t.currentDeadline()), "Ближайший срок"});
            }
            for (TaskListItemResponse t : registry.overdue()) {
                registryRows.add(new String[]{t.title(), t.departmentName(), String.valueOf(t.currentDeadline()), "Просрочено"});
            }
            w.table(new String[]{"Задача", "СП", "Срок", "Категория"}, new float[]{0.4f, 0.25f, 0.15f, 0.2f}, registryRows);

            return w.toBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String pct(double rate) {
        return Math.round(rate * 100) + "%";
    }

    /** Minimal paginated PDF table/heading writer, using an embedded Cyrillic-capable font. */
    private static final class PdfWriter implements AutoCloseable {
        private static final float MARGIN = 36f;
        private static final float ROW_HEIGHT = 16f;
        private static final float TITLE_SIZE = 16f;
        private static final float HEADING_SIZE = 12f;
        private static final float BODY_SIZE = 9f;

        private final PDDocument doc;
        private final PDFont font;
        private final PDRectangle pageSize;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        PdfWriter(PDDocument doc) throws IOException {
            this.doc = doc;
            this.pageSize = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()); // landscape
            try (InputStream fontStream = new ClassPathResource("fonts/DejaVuSans.ttf").getInputStream()) {
                this.font = PDType0Font.load(doc, fontStream);
            }
            newPage();
        }

        private void newPage() throws IOException {
            if (stream != null) {
                stream.close();
            }
            page = new PDPage(pageSize);
            doc.addPage(page);
            stream = new PDPageContentStream(doc, page);
            y = pageSize.getHeight() - MARGIN;
        }

        private void ensureSpace(float needed) throws IOException {
            if (y - needed < MARGIN) {
                newPage();
            }
        }

        private void writeLine(String text, float size, boolean bold) throws IOException {
            ensureSpace(size + 6);
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(sanitize(text));
            stream.endText();
            y -= size + 6;
        }

        void title(String text) throws IOException {
            writeLine(text, TITLE_SIZE, true);
            y -= 6;
        }

        void heading(String text) throws IOException {
            ensureSpace(HEADING_SIZE + 14);
            y -= 8;
            writeLine(text, HEADING_SIZE, true);
        }

        void table(String[] headers, float[] colWidthRatios, List<String[]> rows) throws IOException {
            float usableWidth = pageSize.getWidth() - 2 * MARGIN;
            float[] colWidths = new float[colWidthRatios.length];
            for (int i = 0; i < colWidthRatios.length; i++) {
                colWidths[i] = usableWidth * colWidthRatios[i];
            }
            drawTableRow(headers, colWidths, true);
            for (String[] row : rows) {
                drawTableRow(row, colWidths, false);
            }
            if (rows.isEmpty()) {
                writeLine("Нет данных", BODY_SIZE, false);
            }
            y -= 10;
        }

        private void drawTableRow(String[] cells, float[] colWidths, boolean header) throws IOException {
            ensureSpace(ROW_HEIGHT);
            float x = MARGIN;
            stream.setFont(font, BODY_SIZE);
            for (int i = 0; i < cells.length; i++) {
                float maxWidth = colWidths[i] - 4;
                String text = truncateToWidth(cells[i] == null ? "" : cells[i], maxWidth);
                stream.beginText();
                stream.newLineAtOffset(x, y);
                stream.showText(sanitize(text));
                stream.endText();
                x += colWidths[i];
            }
            y -= ROW_HEIGHT;
            if (header) {
                stream.setLineWidth(0.5f);
                stream.moveTo(MARGIN, y + ROW_HEIGHT - 4);
                stream.lineTo(MARGIN + sum(colWidths), y + ROW_HEIGHT - 4);
                stream.stroke();
            }
        }

        private float sum(float[] values) {
            float total = 0;
            for (float v : values) {
                total += v;
            }
            return total;
        }

        private String truncateToWidth(String text, float maxWidth) throws IOException {
            String sanitized = sanitize(text);
            if (font.getStringWidth(sanitized) / 1000 * BODY_SIZE <= maxWidth) {
                return sanitized;
            }
            String ellipsis = "…";
            StringBuilder sb = new StringBuilder();
            for (char c : sanitized.toCharArray()) {
                String candidate = sb.toString() + c + ellipsis;
                if (font.getStringWidth(candidate) / 1000 * BODY_SIZE > maxWidth) {
                    break;
                }
                sb.append(c);
            }
            return sb + ellipsis;
        }

        /** DejaVu Sans doesn't cover every possible Unicode code point (e.g. some emoji); drop what it can't encode. */
        private String sanitize(String text) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                try {
                    font.encode(String.valueOf(c));
                    sb.append(c);
                } catch (IOException | IllegalArgumentException e) {
                    sb.append('?');
                }
            }
            return sb.toString();
        }

        byte[] toBytes() throws IOException {
            stream.close();
            stream = null;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }

        @Override
        public void close() throws IOException {
            if (stream != null) {
                stream.close();
            }
        }
    }
}
